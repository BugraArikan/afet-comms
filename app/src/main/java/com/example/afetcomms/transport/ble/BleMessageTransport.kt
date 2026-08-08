package com.example.afetcomms.transport.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import com.example.afetcomms.data.model.MessageType
import com.example.afetcomms.data.model.SenderType
import com.example.afetcomms.transport.MessagePayload
import com.example.afetcomms.transport.MessageTransport
import com.example.afetcomms.transport.TransportListener
import com.example.afetcomms.transport.TransportMode
import com.example.afetcomms.transport.TransportState
import com.example.afetcomms.util.FamilyCodeGenerator
import java.util.concurrent.ConcurrentHashMap

/**
 * BLE iletişimi: tarama → bağlan → oku → **kendi presence'ını yaz**.
 * İki telefon aynı anda birbirine client olarak bağlanmaya çalışınca tek yönlü kalıyordu;
 * write-back ile karşı tarafın GATT sunucusuna doğrudan yazılır.
 */
@SuppressLint("MissingPermission")
class BleMessageTransport(
    private val context: Context
) : MessageTransport {

    override val name: String = "BLE"

    private val mainHandler = Handler(Looper.getMainLooper())
    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter: BluetoothAdapter? = bluetoothManager.adapter

    private var listener: TransportListener? = null
    private var state = TransportState.IDLE
    private var familyId: String = ""
    private var mode: TransportMode = TransportMode.FAMILY

    private var gattServer: BluetoothGattServer? = null
    private var messageCharacteristic: BluetoothGattCharacteristic? = null
    private var advertiser: BluetoothLeAdvertiser? = null
    private var scanner: BluetoothLeScanner? = null

    private var pendingPayload: MessagePayload? = null
    private var lastPresencePayload: MessagePayload? = null
    private var pendingClearRunnable: Runnable? = null
    private var urgentExchangeUntilMs = 0L
    private val connectingDevices = ConcurrentHashMap.newKeySet<String>()
    private val lastConnectAttemptMs = ConcurrentHashMap<String, Long>()
    private val recentlySeenMsgIds = ConcurrentHashMap.newKeySet<String>()

    private val keepAliveRunnable = object : Runnable {
        override fun run() {
            try {
                scanner?.stopScan(scanCallback)
            } catch (_: Exception) {
            }
            startScanning()
            refreshAdvertisedPayload()
            mainHandler.postDelayed(this, KEEP_ALIVE_MS)
        }
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            updateState(TransportState.RUNNING, "Yayın aktif")
        }

        override fun onStartFailure(errorCode: Int) {
            updateState(TransportState.ERROR, "Yayın hatası: $errorCode")
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.device?.let { tryExchangeWithPeer(it) }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            results?.forEach { tryExchangeWithPeer(it.device) }
        }
    }

    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onCharacteristicReadRequest(
            device: BluetoothDevice?,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic?
        ) {
            if (device == null || characteristic?.uuid != BleConstants.MESSAGE_CHAR_UUID) return
            val full = payloadBytesForAdvertise()
            val end = minOf(full.size, offset + BleConstants.MAX_GATT_PAYLOAD)
            val value = if (offset < full.size) full.copyOfRange(offset, end) else ByteArray(0)
            gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice?,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic?,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            if (device == null || value == null) return
            if (responseNeeded) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
            }
            handleIncomingBytes(value)
        }
    }

    override fun start(familyId: String, mode: TransportMode) {
        this.familyId = FamilyCodeGenerator.normalizeFamilyCode(familyId)
        this.mode = mode
        if (adapter == null || !adapter.isEnabled) {
            updateState(TransportState.ERROR, "Bluetooth kapalı")
            return
        }
        updateState(TransportState.STARTING, "Başlatılıyor")
        setupGattServer()
        startAdvertising()
        startScanning()
        mainHandler.removeCallbacks(keepAliveRunnable)
        mainHandler.postDelayed(keepAliveRunnable, KEEP_ALIVE_MS)
    }

    override fun stop() {
        mainHandler.removeCallbacks(keepAliveRunnable)
        pendingClearRunnable?.let { mainHandler.removeCallbacks(it) }
        pendingClearRunnable = null
        urgentExchangeUntilMs = 0L
        try {
            scanner?.stopScan(scanCallback)
        } catch (_: Exception) {
        }
        try {
            advertiser?.stopAdvertising(advertiseCallback)
        } catch (_: Exception) {
        }
        gattServer?.close()
        gattServer = null
        messageCharacteristic = null
        connectingDevices.clear()
        updateState(TransportState.IDLE, "Durduruldu")
    }

    override fun send(payload: MessagePayload) {
        when (payload.type) {
            MessageType.PRESENCE -> {
                lastPresencePayload = payload
                if (pendingPayload == null || pendingPayload?.type == MessageType.PRESENCE) {
                    pendingPayload = payload
                    refreshAdvertisedPayload()
                }
            }
            else -> {
                pendingPayload = payload
                refreshAdvertisedPayload()
                restartAdvertising()
                schedulePendingPayloadExpiry()
                triggerUrgentExchange()
            }
        }
        listener?.onSendResult(payload.msgId, true, name)
    }

    override fun setListener(listener: TransportListener?) {
        this.listener = listener
    }

    override fun currentState(): TransportState = state

    private fun payloadBytesForAdvertise(): ByteArray {
        val payload = pendingPayload ?: lastPresencePayload ?: return ByteArray(0)
        return payload.toJsonBytes()
    }

    private fun payloadBytesForWriteBack(): ByteArray? {
        val payload = lastPresencePayload ?: return null
        val bytes = payload.toJsonBytes()
        return if (bytes.size <= BleConstants.MAX_GATT_PAYLOAD) bytes else null
    }

    private fun schedulePendingPayloadExpiry() {
        pendingClearRunnable?.let { mainHandler.removeCallbacks(it) }
        val runnable = Runnable {
            if (pendingPayload?.type != MessageType.PRESENCE) {
                pendingPayload = lastPresencePayload
                refreshAdvertisedPayload()
            }
        }
        pendingClearRunnable = runnable
        mainHandler.postDelayed(runnable, PENDING_OUTBOUND_TTL_MS)
    }

    private fun triggerUrgentExchange() {
        urgentExchangeUntilMs = System.currentTimeMillis() + URGENT_EXCHANGE_WINDOW_MS
        try {
            scanner?.stopScan(scanCallback)
        } catch (_: Exception) {
        }
        startScanning()
        mainHandler.postDelayed({ startScanning() }, 1_500L)
        mainHandler.postDelayed({ startScanning() }, 4_000L)
    }

    private fun refreshAdvertisedPayload() {
        val bytes = payloadBytesForAdvertise()
        if (bytes.isNotEmpty()) {
            messageCharacteristic?.value = bytes
        }
    }

    private fun setupGattServer() {
        gattServer?.close()
        gattServer = bluetoothManager.openGattServer(context, gattServerCallback) ?: run {
            updateState(TransportState.ERROR, "GATT sunucu açılamadı")
            return
        }

        val service = BluetoothGattService(
            BleConstants.SERVICE_UUID,
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        )
        val characteristic = BluetoothGattCharacteristic(
            BleConstants.MESSAGE_CHAR_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ or
                BluetoothGattCharacteristic.PROPERTY_WRITE or
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
            BluetoothGattCharacteristic.PERMISSION_READ or
                BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        val descriptor = BluetoothGattDescriptor(
            UUID_CLIENT_CONFIG,
            BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
        )
        characteristic.addDescriptor(descriptor)
        service.addCharacteristic(characteristic)
        gattServer?.addService(service)
        messageCharacteristic = characteristic
    }

    private fun startAdvertising() {
        advertiser = adapter?.bluetoothLeAdvertiser
        if (advertiser == null) {
            updateState(TransportState.ERROR, "BLE reklam desteklenmiyor")
            return
        }
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(true)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addServiceUuid(ParcelUuid(BleConstants.SERVICE_UUID))
            .build()

        advertiser?.startAdvertising(settings, data, advertiseCallback)
    }

    private fun restartAdvertising() {
        try {
            advertiser?.stopAdvertising(advertiseCallback)
        } catch (_: Exception) {
        }
        mainHandler.postDelayed({ startAdvertising() }, 300)
    }

    private fun startScanning() {
        scanner = adapter?.bluetoothLeScanner ?: return
        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(BleConstants.SERVICE_UUID))
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        scanner?.startScan(listOf(filter), settings, scanCallback)
    }

    private fun tryExchangeWithPeer(device: BluetoothDevice) {
        val address = device.address ?: return
        val now = System.currentTimeMillis()
        val lastAttempt = lastConnectAttemptMs[address] ?: 0L
        val cooldown = if (now < urgentExchangeUntilMs) {
            CONNECT_URGENT_COOLDOWN_MS
        } else {
            CONNECT_COOLDOWN_MS
        }
        if (now - lastAttempt < cooldown) return
        if (connectingDevices.contains(address)) return

        lastConnectAttemptMs[address] = now
        connectingDevices.add(address)

        device.connectGatt(context, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    val mtuRequested = gatt?.requestMtu(REQUESTED_MTU) ?: false
                    if (!mtuRequested) {
                        gatt?.discoverServices()
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    finishExchange(address, gatt)
                }
            }

            override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
                gatt?.discoverServices()
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                if (status != BluetoothGatt.GATT_SUCCESS || gatt == null) {
                    finishExchange(address, gatt)
                    return
                }
                val service = gatt.getService(BleConstants.SERVICE_UUID) ?: run {
                    finishExchange(address, gatt)
                    return
                }
                val char = service.getCharacteristic(BleConstants.MESSAGE_CHAR_UUID) ?: run {
                    finishExchange(address, gatt)
                    return
                }
                gatt.readCharacteristic(char)
            }

            override fun onCharacteristicRead(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?,
                status: Int
            ) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    @Suppress("DEPRECATION")
                    val value = characteristic?.value
                    if (value != null && value.isNotEmpty()) {
                        handleIncomingBytes(value)
                    }
                }
                writeBackToPeer(gatt, characteristic, address)
            }

            override fun onCharacteristicWrite(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?,
                status: Int
            ) {
                finishExchange(address, gatt)
            }
        })
    }

    private fun writeBackToPeer(
        gatt: BluetoothGatt?,
        remoteChar: BluetoothGattCharacteristic?,
        address: String
    ) {
        val bytes = payloadBytesForWriteBack()
        if (bytes == null || remoteChar == null || gatt == null) {
            finishExchange(address, gatt)
            return
        }
        remoteChar.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        remoteChar.value = bytes
        val queued = gatt.writeCharacteristic(remoteChar)
        if (!queued) {
            finishExchange(address, gatt)
        }
    }

    private fun finishExchange(address: String, gatt: BluetoothGatt?) {
        connectingDevices.remove(address)
        try {
            gatt?.disconnect()
            gatt?.close()
        } catch (_: Exception) {
        }
    }

    private fun handleIncomingBytes(bytes: ByteArray) {
        val payload = MessagePayload.fromJsonBytes(bytes) ?: return
        if (!shouldAcceptIncoming(payload)) return
        if (payload.type == MessageType.PRESENCE) {
            listener?.onMessageReceived(payload, name)
            return
        }
        if (!recentlySeenMsgIds.add(payload.msgId)) return
        if (recentlySeenMsgIds.size > 200) {
            recentlySeenMsgIds.clear()
            recentlySeenMsgIds.add(payload.msgId)
        }
        listener?.onMessageReceived(payload, name)
    }

    private fun shouldAcceptIncoming(payload: MessagePayload): Boolean {
        return when (mode) {
            TransportMode.RESCUER ->
                payload.type == MessageType.SOS && payload.senderType == SenderType.CITIZEN
            TransportMode.FAMILY ->
                familyIdsMatch(payload.familyId, familyId)
        }
    }

    private fun familyIdsMatch(remote: String, local: String): Boolean {
        return FamilyCodeGenerator.normalizeFamilyCode(remote) ==
            FamilyCodeGenerator.normalizeFamilyCode(local)
    }

    private fun updateState(newState: TransportState, detail: String) {
        state = newState
        listener?.onStateChanged(newState, detail, name)
    }

    companion object {
        private const val KEEP_ALIVE_MS = 20_000L
        private const val CONNECT_COOLDOWN_MS = 8_000L
        private const val CONNECT_URGENT_COOLDOWN_MS = 2_000L
        private const val URGENT_EXCHANGE_WINDOW_MS = 30_000L
        private const val PENDING_OUTBOUND_TTL_MS = 90_000L
        private const val REQUESTED_MTU = 512
        private val UUID_CLIENT_CONFIG =
            java.util.UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }
}

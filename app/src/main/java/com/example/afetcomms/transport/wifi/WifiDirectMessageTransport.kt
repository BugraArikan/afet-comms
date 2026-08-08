package com.example.afetcomms.transport.wifi

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import com.example.afetcomms.data.model.MessageType
import com.example.afetcomms.data.model.SenderType
import com.example.afetcomms.transport.MessagePayload
import com.example.afetcomms.transport.MessageTransport
import com.example.afetcomms.transport.TransportListener
import com.example.afetcomms.transport.TransportMode
import com.example.afetcomms.transport.TransportState
import com.example.afetcomms.util.FamilyCodeGenerator
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

@SuppressLint("MissingPermission")
class WifiDirectMessageTransport(
    private val context: Context
) : MessageTransport {

    override val name: String = "WiFiDirect"

    private val executor = Executors.newSingleThreadExecutor()
    private var manager: WifiP2pManager? = null
    private var channel: WifiP2pManager.Channel? = null
    private var listener: TransportListener? = null
    private var state = TransportState.IDLE
    private var familyId: String = ""
    private var mode: TransportMode = TransportMode.FAMILY
    private val pendingSend = AtomicReference<MessagePayload?>(null)
    private var serverSocket: ServerSocket? = null

    private val receiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                    val enabled = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1) ==
                        WifiP2pManager.WIFI_P2P_STATE_ENABLED
                    if (!enabled) updateState(TransportState.ERROR, "Wi-Fi Direct kapalı")
                }
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    manager?.requestPeers(channel) { peers -> onPeersAvailable(peers) }
                }
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    manager?.requestConnectionInfo(channel) { info -> onConnectionInfo(info) }
                }
            }
        }
    }

    override fun start(familyId: String, mode: TransportMode) {
        this.familyId = FamilyCodeGenerator.normalizeFamilyCode(familyId)
        this.mode = mode
        manager = context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
        if (manager == null) {
            updateState(TransportState.ERROR, "Wi-Fi Direct desteklenmiyor")
            return
        }
        channel = manager?.initialize(context, context.mainLooper, null)
        val filter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        }
        context.registerReceiver(receiver, filter)
        updateState(TransportState.STARTING, "Keşif başlıyor")
        startServer()
        discoverPeers()
    }

    override fun stop() {
        try {
            context.unregisterReceiver(receiver)
        } catch (_: Exception) {
        }
        serverSocket?.close()
        serverSocket = null
        updateState(TransportState.IDLE, "Durduruldu")
    }

    override fun send(payload: MessagePayload) {
        pendingSend.set(payload)
        discoverPeers()
        listener?.onSendResult(payload.msgId, true, name)
    }

    override fun setListener(listener: TransportListener?) {
        this.listener = listener
    }

    override fun currentState(): TransportState = state

    private fun discoverPeers() {
        manager?.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                updateState(TransportState.RUNNING, "Cihaz aranıyor")
            }

            override fun onFailure(reason: Int) {
                updateState(TransportState.ERROR, "Keşif hatası: $reason")
            }
        })
    }

    private fun onPeersAvailable(peers: WifiP2pDeviceList?) {
        val device = peers?.deviceList?.firstOrNull() ?: return
        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
        }
        manager?.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {}
            override fun onFailure(reason: Int) {
                updateState(TransportState.ERROR, "Bağlantı hatası: $reason")
            }
        })
    }

    private fun onConnectionInfo(info: WifiP2pInfo?) {
        if (info == null || !info.groupFormed) return
        val payload = pendingSend.getAndSet(null) ?: return
        executor.execute {
            try {
                val host = info.groupOwnerAddress?.hostAddress ?: return@execute
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(host, SERVER_PORT), 5000)
                    val out = DataOutputStream(socket.getOutputStream())
                    val bytes = payload.toJsonBytes()
                    out.writeInt(bytes.size)
                    out.write(bytes)
                    out.flush()
                }
            } catch (_: Exception) {
                listener?.onSendResult(payload.msgId, false, name)
            }
        }
    }

    private fun startServer() {
        executor.execute {
            try {
                serverSocket?.close()
                serverSocket = ServerSocket(SERVER_PORT)
                updateState(TransportState.RUNNING, "Sunucu dinliyor :$SERVER_PORT")
                listenLoop@ while (serverSocket?.isClosed == false) {
                    val client = serverSocket?.accept() ?: return@execute
                    client.use { socket ->
                        val payload = readPayloadFromSocket(socket) ?: return@use
                        if (shouldAcceptIncoming(payload)) {
                            listener?.onMessageReceived(payload, name)
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun readPayloadFromSocket(socket: Socket): MessagePayload? {
        val input = socket.getInputStream()
        val sizeBytes = ByteArray(4)
        var read = 0
        while (read < 4) {
            val r = input.read(sizeBytes, read, 4 - read)
            if (r <= 0) return null
            read += r
        }
        val size = java.nio.ByteBuffer.wrap(sizeBytes).int
        if (size <= 0 || size > 65536) return null
        val data = ByteArray(size)
        var offset = 0
        while (offset < size) {
            val r = input.read(data, offset, size - offset)
            if (r <= 0) return null
            offset += r
        }
        return MessagePayload.fromJsonBytes(data)
    }

    private fun shouldAcceptIncoming(payload: MessagePayload): Boolean {
        return when (mode) {
            TransportMode.RESCUER ->
                payload.type == MessageType.SOS && payload.senderType == SenderType.CITIZEN
            TransportMode.FAMILY ->
                FamilyCodeGenerator.normalizeFamilyCode(payload.familyId) == familyId
        }
    }

    private fun updateState(newState: TransportState, detail: String) {
        state = newState
        listener?.onStateChanged(newState, detail, name)
    }

    companion object {
        private const val SERVER_PORT = 8988
    }
}

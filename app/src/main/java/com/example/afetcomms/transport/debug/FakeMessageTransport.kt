package com.example.afetcomms.transport.debug

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.example.afetcomms.data.model.AccountRole
import com.example.afetcomms.data.model.MessageType
import com.example.afetcomms.data.model.SenderType
import com.example.afetcomms.transport.MessagePayload
import com.example.afetcomms.transport.MessageTransport
import com.example.afetcomms.transport.TransportListener
import com.example.afetcomms.transport.TransportMode
import com.example.afetcomms.transport.TransportState
import com.example.afetcomms.util.AppPreferences
import java.util.UUID

/**
 * Tek cihazda test: gönderimi başarılı sayar.
 * Aile modunda sahte aile yanıtı; kurtarıcı modunda yakındaki vatandaş SOS simülasyonu.
 */
class FakeMessageTransport(
    private val context: Context,
    private val simulatedPeerId: String = "SIM_Aile_Uyesi",
    private val receiveDelayMs: Long = 2500L
) : MessageTransport {

    override val name: String = "Simülasyon"

    private val handler = Handler(Looper.getMainLooper())
    private var listener: TransportListener? = null
    private var state = TransportState.IDLE
    private var familyId: String = ""

    override fun start(familyId: String, mode: TransportMode) {
        this.familyId = familyId
        updateState(TransportState.RUNNING, "Simülasyon aktif (tek cihaz testi)")
        if (isRescuerAccount()) {
            scheduleDemoCitizenHelpCalls()
        }
    }

    override fun stop() {
        handler.removeCallbacksAndMessages(null)
        updateState(TransportState.IDLE, "Durduruldu")
    }

    override fun send(payload: MessagePayload) {
        listener?.onSendResult(payload.msgId, true, name)
        if (payload.type == MessageType.PRESENCE) return
        if (!shouldSimulateFamilyReply(payload)) return

        handler.postDelayed({
            val simLat = 41.00820
            val simLng = 28.97840
            val simType = if (payload.type == MessageType.SOS) MessageType.SOS else payload.type
            val simContent = if (simType == MessageType.SOS) {
                "Yardım lazım! SOS (simülasyon) [konum: $simLat, $simLng]"
            } else {
                "[Simülasyon yanıt] ${payload.content}"
            }
            val peerPayload = MessagePayload(
                msgId = "${payload.msgId}_peer_${UUID.randomUUID().toString().take(8)}",
                senderId = simulatedPeerId,
                familyId = payload.familyId,
                type = simType,
                content = simContent,
                createdAt = System.currentTimeMillis(),
                ttlSeconds = payload.ttlSeconds,
                priority = if (simType == MessageType.SOS) 1 else payload.priority,
                senderType = SenderType.CITIZEN,
                senderDisplayName = "Ayşe Yılmaz (Sim)"
            )
            listener?.onMessageReceived(peerPayload, name)
        }, receiveDelayMs)
    }

    private fun scheduleDemoCitizenHelpCalls() {
        val demos = listOf(
            DemoHelpCall("SIM_VATANDAS_MEHMET", "Mehmet Yılmaz", "FAM_SIM01", "Yardım lazım! SOS — mahsur kaldık."),
            DemoHelpCall("SIM_VATANDAS_AYSE", "Ayşe Demir", "FAM_SIM02", "Su baskını, acil kurtarma gerekli!")
        )
        demos.forEachIndexed { index, demo ->
            handler.postDelayed({
                deliverCitizenHelpCall(demo)
            }, 2000L + index * receiveDelayMs)
        }
    }

    private fun deliverCitizenHelpCall(demo: DemoHelpCall) {
        val payload = MessagePayload(
            msgId = "sim_sos_${UUID.randomUUID()}",
            senderId = demo.senderId,
            familyId = demo.familyId,
            type = MessageType.SOS,
            content = demo.content,
            createdAt = System.currentTimeMillis(),
            ttlSeconds = 300,
            priority = 1,
            senderType = SenderType.CITIZEN,
            senderDisplayName = demo.displayName
        )
        listener?.onMessageReceived(payload, name)
    }

    private fun shouldSimulateFamilyReply(payload: MessagePayload): Boolean {
        if (isRescuerAccount()) return false
        if (!AppPreferences.simAutoReplyEnabled(context)) return false
        if (payload.senderId == simulatedPeerId) return false
        if (payload.content.startsWith("[Simülasyon yanıt]")) return false
        if (payload.type == MessageType.SOS) return true
        return AppPreferences.simReplyToCheckIn(context)
    }

    private fun isRescuerAccount(): Boolean {
        val prefs = context.getSharedPreferences(AppPreferences.PREFS_NAME, Context.MODE_PRIVATE)
        return AccountRole.fromStorage(prefs.getString(AppPreferences.KEY_ACCOUNT_ROLE, null)) ==
            AccountRole.RESCUER
    }

    override fun setListener(listener: TransportListener?) {
        this.listener = listener
    }

    override fun currentState(): TransportState = state

    private fun updateState(newState: TransportState, detail: String) {
        state = newState
        listener?.onStateChanged(newState, detail, name)
    }

    private data class DemoHelpCall(
        val senderId: String,
        val displayName: String,
        val familyId: String,
        val content: String
    )
}

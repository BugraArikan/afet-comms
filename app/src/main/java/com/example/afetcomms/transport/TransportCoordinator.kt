package com.example.afetcomms.transport

import com.example.afetcomms.data.local.MessageEntity
import com.example.afetcomms.data.model.MessageStatus
import com.example.afetcomms.data.model.MessageType
import com.example.afetcomms.data.model.SenderType
import com.example.afetcomms.data.repo.MemberRepository
import com.example.afetcomms.data.repo.MessageRepository
import com.example.afetcomms.util.FamilyCodeGenerator
import com.example.afetcomms.util.LocationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class TransportCoordinator(
    private val messageRepository: MessageRepository,
    private val memberRepository: MemberRepository,
    private val transports: List<MessageTransport>,
    private val meshRelayEnabled: Boolean = true
) : TransportListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var familyId: String = ""
    private var localSenderId: String = ""
    private var mode: TransportMode = TransportMode.FAMILY

    /** msgId -> kalan transport sayısı (hepsi fail olursa FAILED) */
    private val pendingAcks = ConcurrentHashMap<String, AtomicInteger>()
    private val pendingSuccess = ConcurrentHashMap.newKeySet<String>()
    private val relayedMsgIds = ConcurrentHashMap.newKeySet<String>()

    var onSosReceived: ((MessageEntity) -> Unit)? = null
    var onSosSent: ((MessageEntity) -> Unit)? = null
    var onRescuerCheckinReceived: ((MessageEntity) -> Unit)? = null

    fun start(
        familyId: String,
        localSenderId: String,
        mode: TransportMode = TransportMode.FAMILY
    ) {
        this.familyId = FamilyCodeGenerator.normalizeFamilyCode(familyId)
        this.localSenderId = localSenderId
        this.mode = mode
        transports.forEach { transport ->
            transport.setListener(this)
            transport.start(familyId, mode)
        }
        flushOutbox()
        retryFailed()
    }

    fun announceLocalPresence(
        displayName: String,
        relationRole: String,
        latitude: Double? = null,
        longitude: Double? = null
    ) {
        if (familyId.isBlank() || localSenderId.isBlank()) return
        if (mode != TransportMode.FAMILY) return
        val coords = if (latitude != null && longitude != null) {
            LocationHelper.Coordinates(latitude, longitude)
        } else {
            null
        }
        val content = "presence" + LocationHelper.formatForMessage(coords)
        val payload = MessagePayload(
            msgId = "presence_${localSenderId}_${System.currentTimeMillis()}",
            senderId = localSenderId,
            familyId = FamilyCodeGenerator.normalizeFamilyCode(familyId),
            type = MessageType.PRESENCE,
            content = content,
            createdAt = System.currentTimeMillis(),
            ttlSeconds = 90,
            priority = 0,
            senderDisplayName = displayName,
            relationRole = relationRole
        )
        scope.launch {
            transports.forEach { it.send(payload) }
        }
    }

    fun stop() {
        transports.forEach { it.stop() }
        pendingAcks.clear()
        pendingSuccess.clear()
        relayedMsgIds.clear()
    }

    fun sendMessage(entity: MessageEntity, targetSenderId: String? = null) {
        scope.launch {
            messageRepository.insertMessage(entity)
            val payload = MessagePayload.fromEntity(entity).copy(targetSenderId = targetSenderId)
            dispatchToTransports(payload)
            if (entity.type == MessageType.SOS) {
                onSosSent?.invoke(entity)
            }
        }
    }

    fun flushOutbox() {
        scope.launch {
            val outbox = messageRepository.getOutboxMessages()
            outbox.forEach { entity ->
                dispatchToTransports(MessagePayload.fromEntity(entity))
            }
        }
    }

    fun retryFailed() {
        scope.launch {
            val failed = messageRepository.getFailedMessages()
            failed.forEach { entity ->
                messageRepository.updateStatus(entity.msgId, MessageStatus.OUTBOX)
                dispatchToTransports(MessagePayload.fromEntity(entity))
            }
        }
    }

    fun purgeExpiredMessages() {
        scope.launch {
            messageRepository.deleteExpiredMessages(System.currentTimeMillis())
        }
    }

    private fun dispatchToTransports(payload: MessagePayload) {
        if (transports.isEmpty()) {
            scope.launch {
                messageRepository.updateStatus(payload.msgId, MessageStatus.FAILED)
            }
            return
        }
        pendingAcks[payload.msgId] = AtomicInteger(transports.size)
        pendingSuccess.remove(payload.msgId)
        transports.forEach { it.send(payload) }

        scope.launch {
            delay(SEND_TIMEOUT_MS)
            if (pendingSuccess.contains(payload.msgId)) return@launch
            if (pendingAcks.remove(payload.msgId) != null) {
                messageRepository.updateStatus(payload.msgId, MessageStatus.FAILED)
            }
        }
    }

    private fun onTransportFinished(msgId: String, success: Boolean) {
        if (success) pendingSuccess.add(msgId)
        val remaining = pendingAcks[msgId]?.decrementAndGet()
        if (remaining == null) return
        if (remaining > 0) return

        pendingAcks.remove(msgId)
        scope.launch {
            if (pendingSuccess.contains(msgId)) {
                messageRepository.updateStatus(msgId, MessageStatus.SENT)
            } else {
                messageRepository.updateStatus(msgId, MessageStatus.FAILED)
            }
            pendingSuccess.remove(msgId)
        }
    }

    override fun onMessageReceived(payload: MessagePayload, transportName: String) {
        if (payload.senderId == localSenderId) return

        if (payload.type == MessageType.PRESENCE) {
            if (mode != TransportMode.FAMILY) return
            if (!familyIdsMatch(payload.familyId, familyId)) return
            scope.launch {
                memberRepository.applyPresenceAnnouncement(payload)
            }
            return
        }

        when (mode) {
            TransportMode.FAMILY -> if (!familyIdsMatch(payload.familyId, familyId)) return
            TransportMode.RESCUER -> {
                if (payload.type != MessageType.SOS) return
                if (payload.senderType != SenderType.CITIZEN) return
            }
        }
        scope.launch {
            if (messageRepository.messageExists(payload.msgId)) return@launch
            val entity = MessageEntity(
                msgId = payload.msgId,
                senderId = payload.senderId,
                familyId = payload.familyId,
                type = payload.type,
                content = payload.content,
                createdAt = payload.createdAt,
                ttlSeconds = payload.ttlSeconds,
                priority = payload.priority,
                status = MessageStatus.RECEIVED,
                senderType = payload.senderType.storageValue,
                senderDisplayName = payload.senderDisplayName.ifBlank { payload.senderId },
                rescuerId = payload.rescuerId
            )
            messageRepository.insertMessage(entity)
            if (entity.type == MessageType.CHECKIN && payload.senderType == SenderType.RESCUER) {
                payload.targetSenderId?.let { memberRepository.clearActiveSos(it) }
                onRescuerCheckinReceived?.invoke(entity)
            } else {
                memberRepository.applyIncomingMessage(payload, entity.type)
            }
            relayIfNeeded(payload)
            if (entity.type == MessageType.SOS) {
                onSosReceived?.invoke(entity)
            }
        }
    }

    /** Basit mesh: TTL > 1 ise bir atlama daha yayınla (aynı msgId, dedupe korur). */
    private fun relayIfNeeded(payload: MessagePayload) {
        if (payload.type == MessageType.PRESENCE) return
        if (!meshRelayEnabled) return
        if (payload.ttlSeconds <= 1) return
        if (!relayedMsgIds.add(payload.msgId)) return
        val relayPayload = payload.copy(ttlSeconds = payload.ttlSeconds - 1)
        transports.forEach { it.send(relayPayload) }
    }

    override fun onStateChanged(state: TransportState, detail: String, transportName: String) {
        TransportStateHolder.update(transportName, state, detail)
    }

    override fun onSendResult(msgId: String, success: Boolean, transportName: String) {
        onTransportFinished(msgId, success)
    }

    private fun familyIdsMatch(remote: String, local: String): Boolean {
        return FamilyCodeGenerator.normalizeFamilyCode(remote) ==
            FamilyCodeGenerator.normalizeFamilyCode(local)
    }

    companion object {
        private const val SEND_TIMEOUT_MS = 15_000L
    }
}

object TransportStateHolder {
    private val states = mutableMapOf<String, Pair<TransportState, String>>()
    var listener: ((String, TransportState, String) -> Unit)? = null

    fun update(transportName: String, state: TransportState, detail: String) {
        states[transportName] = state to detail
        listener?.invoke(transportName, state, detail)
    }

    fun get(transportName: String): Pair<TransportState, String>? = states[transportName]
}

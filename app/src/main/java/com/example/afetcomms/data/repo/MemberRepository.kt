package com.example.afetcomms.data.repo

import androidx.lifecycle.LiveData
import com.example.afetcomms.data.local.MemberDao
import com.example.afetcomms.data.local.MemberEntity
import com.example.afetcomms.data.model.MemberConnectionStatus
import com.example.afetcomms.data.model.MemberRelation
import com.example.afetcomms.data.model.MessageType
import com.example.afetcomms.transport.MessagePayload
import com.example.afetcomms.util.FamilyCodeGenerator
import com.example.afetcomms.util.MessageLocationParser

class MemberRepository(private val memberDao: MemberDao) {

    companion object {
        const val PRESENCE_STALE_MS = 120_000L
    }

    suspend fun insertMember(member: MemberEntity) {
        memberDao.insertMember(member)
    }

    fun getMembersByFamily(familyId: String): LiveData<List<MemberEntity>> {
        return memberDao.getMembersByFamily(familyId)
    }

    fun observeFamilyStatusBoard(familyId: String): LiveData<List<MemberEntity>> {
        return memberDao.observeFamilyStatusBoard(familyId)
    }

    suspend fun deleteMember(userId: String) {
        memberDao.deleteMember(userId)
    }

    suspend fun refreshPresenceTimeouts(familyId: String) {
        val cutoff = System.currentTimeMillis() - PRESENCE_STALE_MS
        memberDao.markStaleMembersAway(
            familyId = familyId,
            cutoffMillis = cutoff,
            connectedStatus = MemberConnectionStatus.CONNECTED.storageValue,
            awayStatus = MemberConnectionStatus.AWAY.storageValue
        )
    }

    suspend fun applyOutgoingMessage(
        userId: String,
        userCode: String,
        displayName: String,
        familyId: String,
        relationRole: String,
        messageType: MessageType,
        content: String
    ) {
        val now = System.currentTimeMillis()
        val coords = MessageLocationParser.parse(content)
        val existing = memberDao.getMemberById(userId)
        if (existing == null) {
            memberDao.insertMember(
                MemberEntity(
                    userId = userId,
                    userCode = userCode,
                    displayName = displayName,
                    familyId = familyId,
                    relationRole = relationRole,
                    connectionStatus = MemberConnectionStatus.CONNECTED.storageValue,
                    lastLatitude = coords?.latitude,
                    lastLongitude = coords?.longitude,
                    lastLocationAtMillis = coords?.let { now },
                    activeSos = messageType == MessageType.SOS,
                    lastSeenAtMillis = now
                )
            )
        } else {
            memberDao.updateMemberPresence(
                userId = userId,
                connectionStatus = MemberConnectionStatus.CONNECTED.storageValue,
                lastSeenAtMillis = now,
                lastLatitude = coords?.latitude ?: existing.lastLatitude,
                lastLongitude = coords?.longitude ?: existing.lastLongitude,
                lastLocationAtMillis = if (coords != null) now else existing.lastLocationAtMillis,
                activeSos = messageType == MessageType.SOS
            )
        }
    }

    suspend fun ensureLocalMember(
        userId: String,
        userCode: String,
        displayName: String,
        familyId: String,
        relationRole: String
    ) {
        if (memberDao.getMemberById(userId) != null) return
        memberDao.insertMember(
            MemberEntity(
                userId = userId,
                userCode = userCode,
                displayName = displayName,
                familyId = familyId,
                relationRole = relationRole,
                connectionStatus = MemberConnectionStatus.CONNECTED.storageValue,
                lastSeenAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun applyPresenceAnnouncement(payload: MessagePayload) {
        val now = System.currentTimeMillis()
        val coords = MessageLocationParser.parse(payload.content)
        val existing = memberDao.getMemberById(payload.senderId)
        val displayName = payload.senderDisplayName.ifBlank { payload.senderId }
        val relation = payload.relationRole?.takeIf { it.isNotBlank() }
            ?: existing?.relationRole
            ?: MemberRelation.DIGER.storageValue

        memberDao.insertMember(
            MemberEntity(
                userId = payload.senderId,
                userCode = existing?.userCode ?: payload.senderId,
                displayName = displayName,
                familyId = FamilyCodeGenerator.normalizeFamilyCode(payload.familyId),
                relationRole = relation,
                connectionStatus = MemberConnectionStatus.CONNECTED.storageValue,
                lastLatitude = coords?.latitude ?: existing?.lastLatitude,
                lastLongitude = coords?.longitude ?: existing?.lastLongitude,
                lastLocationAtMillis = when {
                    coords != null -> now
                    else -> existing?.lastLocationAtMillis
                },
                activeSos = existing?.activeSos ?: false,
                lastSeenAtMillis = now
            )
        )
    }

    suspend fun clearActiveSos(userId: String) {
        memberDao.clearActiveSos(userId)
    }

    suspend fun applyIncomingMessage(payload: MessagePayload, messageType: MessageType) {
        val now = System.currentTimeMillis()
        val coords = MessageLocationParser.parse(payload.content)
        val existing = memberDao.getMemberById(payload.senderId)
        val displayName = payload.senderDisplayName.ifBlank { payload.senderId }
        val relation = payload.relationRole?.takeIf { it.isNotBlank() }
            ?: existing?.relationRole
            ?: MemberRelation.DIGER.storageValue
        val activeSos = messageType == MessageType.SOS

        if (existing == null) {
            memberDao.insertMember(
                MemberEntity(
                    userId = payload.senderId,
                    userCode = payload.senderId,
                    displayName = displayName,
                    familyId = payload.familyId,
                    relationRole = relation,
                    connectionStatus = MemberConnectionStatus.CONNECTED.storageValue,
                    lastLatitude = coords?.latitude,
                    lastLongitude = coords?.longitude,
                    lastLocationAtMillis = coords?.let { now },
                    activeSos = activeSos,
                    lastSeenAtMillis = now
                )
            )
        } else {
            memberDao.updateMemberPresence(
                userId = payload.senderId,
                connectionStatus = MemberConnectionStatus.CONNECTED.storageValue,
                lastSeenAtMillis = now,
                lastLatitude = coords?.latitude ?: existing.lastLatitude,
                lastLongitude = coords?.longitude ?: existing.lastLongitude,
                lastLocationAtMillis = if (coords != null) now else existing.lastLocationAtMillis,
                activeSos = if (messageType == MessageType.SOS) true else false
            )
        }
    }
}

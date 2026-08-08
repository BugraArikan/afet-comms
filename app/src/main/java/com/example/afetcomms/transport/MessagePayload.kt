package com.example.afetcomms.transport

import com.example.afetcomms.data.local.MessageEntity
import com.example.afetcomms.data.model.MessageType
import com.example.afetcomms.data.model.SenderType
import org.json.JSONObject

data class MessagePayload(
    val msgId: String,
    val senderId: String,
    val familyId: String,
    val type: MessageType,
    val content: String,
    val createdAt: Long,
    val ttlSeconds: Int,
    val priority: Int,
    val senderType: SenderType = SenderType.CITIZEN,
    val senderDisplayName: String = "",
    val rescuerId: String? = null,
    val relationRole: String? = null,
    val targetSenderId: String? = null
) {
    fun toJsonBytes(): ByteArray = toJsonString().toByteArray(Charsets.UTF_8)

    fun toJsonString(): String {
        val json = JSONObject()
        json.put("msgId", msgId)
        json.put("senderId", senderId)
        json.put("familyId", familyId)
        json.put("type", type.name)
        json.put("content", content)
        json.put("createdAt", createdAt)
        json.put("ttlSeconds", ttlSeconds)
        json.put("priority", priority)
        json.put("senderType", senderType.storageValue)
        json.put("senderDisplayName", senderDisplayName.ifBlank { senderId })
        if (rescuerId != null) {
            json.put("rescuerId", rescuerId)
        }
        if (relationRole != null) {
            json.put("relationRole", relationRole)
        }
        if (targetSenderId != null) {
            json.put("targetSenderId", targetSenderId)
        }
        return json.toString()
    }

    companion object {
        fun fromEntity(entity: MessageEntity): MessagePayload = MessagePayload(
            msgId = entity.msgId,
            senderId = entity.senderId,
            familyId = entity.familyId,
            type = entity.type,
            content = entity.content,
            createdAt = entity.createdAt,
            ttlSeconds = entity.ttlSeconds,
            priority = entity.priority,
            senderType = SenderType.fromStorage(entity.senderType),
            senderDisplayName = entity.senderDisplayName.ifBlank { entity.senderId },
            rescuerId = entity.rescuerId,
            relationRole = null
        )

        fun fromJsonBytes(bytes: ByteArray): MessagePayload? = fromJsonString(String(bytes, Charsets.UTF_8))

        fun fromJsonString(jsonString: String): MessagePayload? {
            return try {
                val json = JSONObject(jsonString)
                val senderType = SenderType.fromStorage(
                    json.optString("senderType", SenderType.CITIZEN.storageValue)
                )
                val senderId = json.getString("senderId")
                MessagePayload(
                    msgId = json.getString("msgId"),
                    senderId = senderId,
                    familyId = json.getString("familyId"),
                    type = MessageType.valueOf(json.getString("type")),
                    content = json.getString("content"),
                    createdAt = json.getLong("createdAt"),
                    ttlSeconds = json.getInt("ttlSeconds"),
                    priority = json.getInt("priority"),
                    senderType = senderType,
                    senderDisplayName = json.optString("senderDisplayName", senderId),
                    rescuerId = json.optString("rescuerId").takeIf { it.isNotBlank() },
                    relationRole = json.optString("relationRole").takeIf { it.isNotBlank() },
                    targetSenderId = json.optString("targetSenderId").takeIf { it.isNotBlank() }
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}

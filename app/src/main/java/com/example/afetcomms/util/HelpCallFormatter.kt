package com.example.afetcomms.util

import com.example.afetcomms.data.local.MessageEntity
import com.example.afetcomms.data.model.MessageType
import com.example.afetcomms.data.model.SenderType

object HelpCallFormatter {

    fun title(entity: MessageEntity): String {
        val name = entity.senderDisplayName.ifBlank { entity.senderId }
        val prefix = when (SenderType.fromStorage(entity.senderType)) {
            SenderType.RESCUER -> "Kurtarıcı $name"
            SenderType.CITIZEN -> "Vatandaş $name"
        }
        val suffix = when (entity.type) {
            MessageType.SOS -> "SOS"
            MessageType.CHECKIN -> "Güvendeyim"
            MessageType.PRESENCE -> "Varlık"
        }
        return "$prefix - $suffix"
    }

    fun subtitle(entity: MessageEntity): String {
        val parts = mutableListOf<String>()
        if (entity.senderId.isNotBlank()) parts.add("ID: ${entity.senderId}")
        if (entity.familyId.isNotBlank()) parts.add("Aile: ${entity.familyId}")
        parts.add(entity.status)
        return parts.joinToString(" · ")
    }
}

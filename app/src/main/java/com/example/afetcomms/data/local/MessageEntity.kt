package com.example.afetcomms.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.afetcomms.data.model.MessageType
import com.example.afetcomms.data.model.SenderType

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val msgId: String,

    val senderId: String,
    val familyId: String,
    val type: MessageType,
    val content: String,
    val createdAt: Long,
    val ttlSeconds: Int,
    val priority: Int,
    val status: String,
    val senderType: String = SenderType.CITIZEN.storageValue,
    val senderDisplayName: String = "",
    val rescuerId: String? = null
)

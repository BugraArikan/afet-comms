package com.example.afetcomms.data.repo

import androidx.lifecycle.LiveData
import com.example.afetcomms.data.local.MessageDao
import com.example.afetcomms.data.local.MessageEntity
import com.example.afetcomms.data.model.MessageStatus

class MessageRepository(
    private val messageDao: MessageDao
) {

    suspend fun insertMessage(message: MessageEntity) {
        messageDao.insertMessage(message)
    }

    fun getAllMessages(): LiveData<List<MessageEntity>> {
        return messageDao.getAllMessages()
    }

    suspend fun deleteMessageById(messageId: String) {
        messageDao.deleteMessageById(messageId)
    }

    fun getMessagesByFamily(familyId: String): LiveData<List<MessageEntity>> {
        return messageDao.getMessagesByFamily(familyId)
    }

    suspend fun getOutboxMessages(): List<MessageEntity> {
        return messageDao.getMessagesByStatus(MessageStatus.OUTBOX)
    }

    suspend fun updateStatus(messageId: String, status: String) {
        messageDao.updateStatus(messageId, status)
    }

    suspend fun messageExists(messageId: String): Boolean {
        return messageDao.messageExists(messageId) > 0
    }

    suspend fun getFailedMessages(): List<MessageEntity> {
        return messageDao.getMessagesByStatus(MessageStatus.FAILED)
    }

    suspend fun deleteExpiredMessages(now: Long = System.currentTimeMillis()) {
        messageDao.deleteExpiredMessages(now)
    }

    suspend fun countFailedByFamily(familyId: String): Int {
        return messageDao.countByStatusAndFamily(MessageStatus.FAILED, familyId)
    }

    suspend fun countOutboxByFamily(familyId: String): Int {
        return messageDao.countByStatusAndFamily(MessageStatus.OUTBOX, familyId)
    }

    fun getNearbyHelpCalls(): LiveData<List<MessageEntity>> {
        return messageDao.getNearbyHelpCalls(MessageStatus.RECEIVED)
    }

}
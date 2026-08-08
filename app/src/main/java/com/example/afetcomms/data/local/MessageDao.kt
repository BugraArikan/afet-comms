package com.example.afetcomms.data.local

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("SELECT * FROM messages ORDER BY createdAt DESC")
    fun getAllMessages(): LiveData<List<MessageEntity>>

    @Query("DELETE FROM messages WHERE msgId = :messageId")
    suspend fun deleteMessageById(messageId: String)

    @Query("SELECT * FROM messages WHERE familyId = :familyId ORDER BY createdAt DESC")
    fun getMessagesByFamily(familyId: String): LiveData<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE status = :status ORDER BY priority DESC, createdAt ASC")
    suspend fun getMessagesByStatus(status: String): List<MessageEntity>

    @Query("UPDATE messages SET status = :status WHERE msgId = :messageId")
    suspend fun updateStatus(messageId: String, status: String)

    @Query("SELECT COUNT(*) FROM messages WHERE msgId = :messageId")
    suspend fun messageExists(messageId: String): Int

    @Query(
        """
        DELETE FROM messages
        WHERE (:now - createdAt) > (ttlSeconds * 1000)
        """
    )
    suspend fun deleteExpiredMessages(now: Long)

    @Query("SELECT COUNT(*) FROM messages WHERE status = :status AND familyId = :familyId")
    suspend fun countByStatusAndFamily(status: String, familyId: String): Int

    @Query(
        """
        SELECT * FROM messages
        WHERE type = 'SOS' AND status = :receivedStatus AND senderType = 'CITIZEN'
        ORDER BY priority DESC, createdAt DESC
        """
    )
    fun getNearbyHelpCalls(receivedStatus: String): LiveData<List<MessageEntity>>

}
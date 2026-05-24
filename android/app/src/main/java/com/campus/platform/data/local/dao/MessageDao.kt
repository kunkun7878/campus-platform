package com.campus.platform.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.campus.platform.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Upsert
    suspend fun upsertMessage(message: MessageEntity)

    @Upsert
    suspend fun upsertAllMessages(messages: List<MessageEntity>)

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    fun getMessagesByConversationId(conversationId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getMessageById(id: String): MessageEntity?

    @Query("UPDATE messages SET localStatus = :status WHERE id = :messageId")
    suspend fun updateLocalStatus(messageId: String, status: String)

    @Query("UPDATE messages SET isRead = 1 WHERE conversationId = :conversationId AND isRead = 0")
    suspend fun markAllAsRead(conversationId: String)

    @Query("SELECT COUNT(*) FROM messages WHERE conversationId = :conversationId AND isRead = 0 AND senderId != :currentUserId")
    suspend fun getUnreadCount(conversationId: String, currentUserId: String): Int

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessage(id: String)

    @Query("DELETE FROM messages")
    suspend fun deleteAllMessages()
}

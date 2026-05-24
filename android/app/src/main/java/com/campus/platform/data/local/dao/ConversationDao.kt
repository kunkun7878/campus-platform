package com.campus.platform.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.campus.platform.data.local.entity.ConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {

    @Upsert
    suspend fun upsertConversation(conversation: ConversationEntity)

    @Upsert
    suspend fun upsertAllConversations(conversations: List<ConversationEntity>)

    @Query("SELECT * FROM conversations WHERE user1Id = :userId OR user2Id = :userId ORDER BY lastMessageAt DESC")
    fun getConversationsByUserId(userId: String): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getConversationById(id: String): ConversationEntity?

    @Query("SELECT * FROM conversations WHERE (user1Id = :user1 AND user2Id = :user2) OR (user1Id = :user2 AND user2Id = :user1)")
    suspend fun getConversationBetweenUsers(user1: String, user2: String): ConversationEntity?

    @Query("DELETE FROM conversations")
    suspend fun deleteAllConversations()
}

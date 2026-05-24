package com.campus.platform.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val user1Id: String,
    val user2Id: String,
    val lastMessage: String? = null,
    val lastMessageAt: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderId: String,
    val content: String,
    val isRead: Boolean = false,
    val createdAt: String? = null,
    /**
     * Room-local only — not persisted to Supabase.
     * See [C0-1] 乐观 UI 方案：SENDING / SENT / FAILED。
     */
    val localStatus: String = LOCAL_STATUS_SENDING,
) {
    companion object {
        const val LOCAL_STATUS_SENDING = "SENDING"
        const val LOCAL_STATUS_SENT = "SENT"
        const val LOCAL_STATUS_FAILED = "FAILED"
    }
}

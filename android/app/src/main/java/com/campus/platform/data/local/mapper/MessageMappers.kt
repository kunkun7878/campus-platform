package com.campus.platform.data.local.mapper

import com.campus.platform.data.local.entity.ConversationEntity
import com.campus.platform.data.local.entity.MessageEntity

// ── ConversationDto ───────────────────────────────────────

data class ConversationDto(
    val id: String,
    val user1Id: String,
    val user2Id: String,
    val lastMessage: String?,
    val lastMessageAt: String?,
    val createdAt: String?,
    val updatedAt: String?,
)

fun ConversationEntity.toDto(): ConversationDto = ConversationDto(
    id = id,
    user1Id = user1Id,
    user2Id = user2Id,
    lastMessage = lastMessage,
    lastMessageAt = lastMessageAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun ConversationDto.toEntity(): ConversationEntity = ConversationEntity(
    id = id,
    user1Id = user1Id,
    user2Id = user2Id,
    lastMessage = lastMessage,
    lastMessageAt = lastMessageAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

// ── MessageDto ────────────────────────────────────────────

data class MessageDto(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val content: String,
    val isRead: Boolean,
    val localStatus: String, // Room-local, not in Supabase payload
    val createdAt: String?,
)

fun MessageEntity.toDto(): MessageDto = MessageDto(
    id = id,
    conversationId = conversationId,
    senderId = senderId,
    content = content,
    isRead = isRead,
    localStatus = localStatus,
    createdAt = createdAt,
)

fun MessageDto.toEntity(): MessageEntity = MessageEntity(
    id = id,
    conversationId = conversationId,
    senderId = senderId,
    content = content,
    isRead = isRead,
    localStatus = localStatus,
    createdAt = createdAt,
)

// ── GroupMessageDto ────────────────────────────────────────

data class GroupMessageDto(
    val id: String,
    val groupId: String,
    val senderId: String,
    val content: String,
    val messageType: String,
    val createdAt: String?,
) {
    companion object {
        const val TYPE_USER = "user"
        const val TYPE_SYSTEM = "system"
    }
}

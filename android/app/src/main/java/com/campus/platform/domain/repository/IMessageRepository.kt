package com.campus.platform.domain.repository

import com.campus.platform.data.local.mapper.ConversationDto
import com.campus.platform.data.local.mapper.GroupMessageDto
import com.campus.platform.data.local.mapper.MessageDto
import kotlinx.coroutines.flow.Flow

interface IMessageRepository {

    // ── Conversations ───────────────────────────────────────────

    fun getConversations(): Flow<List<ConversationDto>>

    suspend fun getConversationById(id: String): ConversationDto?

    /**
     * 创建/获取与 targetUserId 的会话。
     * 调用方不需要传自己的 ID——Repository 内部从 SupabaseClient.auth 获取。
     */
    suspend fun createConversation(targetUserId: String): ConversationDto

    // ── Messages ───────────────────────────────────────────────

    fun getMessages(conversationId: String): Flow<List<MessageDto>>

    /**
     * 发送消息。调用方不需要传 senderId——
     * Repository 内部从 SupabaseClient.auth.currentUser 获取。
     */
    suspend fun sendMessage(conversationId: String, content: String): MessageDto

    suspend fun deleteMessage(id: String)

    suspend fun markAsRead(conversationId: String)

    // ── Refresh ─────────────────────────────────────────────────

    suspend fun refreshConversations()

    suspend fun refreshMessages(conversationId: String)

    // ── Group Messages ─────────────────────────────────────────
    // C3b-1: Group chat messages (in-memory, no Room cache yet)

    fun observeGroupMessages(groupId: String): Flow<List<GroupMessageDto>>

    suspend fun sendGroupMessage(groupId: String, content: String): GroupMessageDto
}

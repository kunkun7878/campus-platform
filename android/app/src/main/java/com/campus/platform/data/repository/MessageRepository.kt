package com.campus.platform.data.repository

import android.util.Log
import com.campus.platform.data.local.dao.ConversationDao
import com.campus.platform.data.local.dao.MessageDao
import com.campus.platform.data.local.entity.MessageEntity
import com.campus.platform.data.local.mapper.ConversationDto
import com.campus.platform.data.local.mapper.GroupMessageDto
import com.campus.platform.data.local.mapper.MessageDto
import com.campus.platform.data.local.mapper.toDto
import com.campus.platform.data.local.mapper.toEntity
import com.campus.platform.data.realtime.RealtimeConnectionManager
import com.campus.platform.domain.repository.IMessageRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

// ── Supabase REST API DTOs (private) ───────────────────────

@Serializable
private data class ConversationApiDto(
    val id: String,
    @SerialName("user1_id") val user1Id: String,
    @SerialName("user2_id") val user2Id: String,
    @SerialName("last_message") val lastMessage: String? = null,
    @SerialName("last_message_at") val lastMessageAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
private data class MessageApiDto(
    val id: String,
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("sender_id") val senderId: String,
    val content: String,
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
)

// ── Group Messages API DTO ─────────────────────────────────

@Serializable
internal data class GroupMessageApiDto(
    val id: String,
    @SerialName("group_id") val groupId: String,
    @SerialName("sender_id") val senderId: String,
    val content: String,
    @SerialName("message_type") val messageType: String = "user",
    @SerialName("created_at") val createdAt: String? = null,
)

// ── API <-> Mapper DTO bridges ─────────────────────────────

private fun ConversationApiDto.toMapperDto() = ConversationDto(
    id, user1Id, user2Id, lastMessage, lastMessageAt, createdAt, updatedAt,
)

private fun ConversationDto.toApiDto() = ConversationApiDto(
    id, user1Id, user2Id, lastMessage, lastMessageAt, createdAt, updatedAt,
)

private fun MessageApiDto.toMapperDto() = MessageDto(
    id, conversationId, senderId, content, isRead,
    localStatus = MessageEntity.LOCAL_STATUS_SENT,
    createdAt = createdAt,
)

private fun MessageDto.toApiDto() = MessageApiDto(
    id, conversationId, senderId, content, isRead, createdAt,
)

// ── Group message bridges ──────────────────────────────────

private fun GroupMessageApiDto.toMapperDto() = GroupMessageDto(
    id, groupId, senderId, content, messageType, createdAt,
)

// ── Repository ─────────────────────────────────────────────

@Singleton
class MessageRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val realtimeManager: RealtimeConnectionManager,
) : IMessageRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ── In-memory group messages state ──────────────────────
    // Map<groupId, List<GroupMessageDto>> — no Room cache for group messages yet

    private val _groupMessages = MutableStateFlow<Map<String, List<GroupMessageDto>>>(emptyMap())

    // Track which groups we're already subscribed to for Realtime CDC
    private val subscribedGroups = mutableSetOf<String>()

    // ── Conversations ───────────────────────────────────────

    override fun getConversations(): Flow<List<ConversationDto>> {
        val userId = currentUserId()
        if (userId == null) return emptyFlow()
        scope.launch { refreshConversations() }
        return conversationDao.getConversationsByUserId(userId)
            .map { it.map { e -> e.toDto() } }
    }

    override suspend fun getConversationById(id: String): ConversationDto? {
        return conversationDao.getConversationById(id)?.toDto()
    }

    override suspend fun createConversation(targetUserId: String): ConversationDto {
        val currentId = requireCurrentUserId()

        // Check if conversation already exists locally
        val existing = conversationDao.getConversationBetweenUsers(currentId, targetUserId)
        if (existing != null) return existing.toDto()

        // Create via Supabase (server assigns UUID + timestamp)
        val result = supabase.postgrest
            .from("conversations")
            .insert(
                ConversationApiDto(
                    id = UUID.randomUUID().toString(),
                    user1Id = currentId,
                    user2Id = targetUserId,
                )
            ) { select() }
            .decodeSingle<ConversationApiDto>()

        val entity = result.toMapperDto().toEntity()
        conversationDao.upsertConversation(entity)
        return entity.toDto()
    }

    // ── Messages ────────────────────────────────────────────

    override fun getMessages(conversationId: String): Flow<List<MessageDto>> {
        scope.launch { refreshMessages(conversationId) }
        return messageDao.getMessagesByConversationId(conversationId)
            .map { it.map { e -> e.toDto() } }
    }

    override suspend fun sendMessage(conversationId: String, content: String): MessageDto {
        val senderId = requireCurrentUserId()
        val localId = UUID.randomUUID().toString()
        val now = java.time.OffsetDateTime.now().toString()

        // Step 1 — Optimistic: write to Room with SENDING
        val optimistic = MessageEntity(
            id = localId,
            conversationId = conversationId,
            senderId = senderId,
            content = content,
            isRead = false,
            localStatus = MessageEntity.LOCAL_STATUS_SENDING,
            createdAt = now,
        )
        messageDao.upsertMessage(optimistic)

        // Step 2 — Fire-and-forget to Supabase, then update Room status
        scope.launch {
            try {
                val result = supabase.postgrest
                    .from("messages")
                    .insert(
                        MessageApiDto(
                            id = localId,
                            conversationId = conversationId,
                            senderId = senderId,
                            content = content,
                        )
                    ) { select() }
                    .decodeSingle<MessageApiDto>()

                // Server confirmed — upsert the true record with SENT status
                val confirmed = result.toMapperDto().toEntity()
                    .copy(localStatus = MessageEntity.LOCAL_STATUS_SENT)
                messageDao.upsertMessage(confirmed)

                // Update conversation's last_message
                scope.launch { updateConversationLastMessage(conversationId, content, confirmed.createdAt) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(javaClass.simpleName, "sendMessage failed", e)
                messageDao.updateLocalStatus(localId, MessageEntity.LOCAL_STATUS_FAILED)
            }
        }

        return optimistic.toDto()
    }

    override suspend fun deleteMessage(id: String) {
        messageDao.deleteMessage(id)
    }

    override suspend fun markAsRead(conversationId: String) {
        messageDao.markAllAsRead(conversationId)
        // Best-effort update on Supabase
        scope.launch {
            try {
                val currentId = currentUserId() ?: return@launch
                supabase.postgrest
                    .from("messages")
                    .update(
                        mapOf("is_read" to true),
                    ) {
                        filter {
                            eq("conversation_id", conversationId)
                            neq("sender_id", currentId)
                        }
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(javaClass.simpleName, "markAsRead Supabase error", e)
            }
        }
    }

    // ── Group Messages ──────────────────────────────────────

    override fun observeGroupMessages(groupId: String): Flow<List<GroupMessageDto>> {
        // Subscribe to Realtime CDC once per group
        if (subscribedGroups.add(groupId)) {
            realtimeManager.subscribeToGroupMessages(groupId)

            // Collect realtime events and merge into local state
            scope.launch {
                try {
                    realtimeManager.incomingGroupMessages.collect { payload ->
                        val dto = GroupMessageDto(
                            id = payload.id,
                            groupId = payload.groupId,
                            senderId = payload.senderId,
                            content = payload.content,
                            messageType = payload.messageType,
                            createdAt = payload.createdAt,
                        )
                        val current = _groupMessages.value[dto.groupId] ?: emptyList()
                        // Deduplicate by id
                        if (current.none { it.id == dto.id }) {
                            val updated = (current + dto).sortedByDescending { it.createdAt }
                            _groupMessages.value = _groupMessages.value + (dto.groupId to updated)
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Realtime group message collect error", e)
                }
            }
        }

        // Initial fetch from REST
        scope.launch { refreshGroupMessages(groupId) }

        return _groupMessages.map { it[groupId] ?: emptyList() }
    }

    override suspend fun sendGroupMessage(groupId: String, content: String): GroupMessageDto {
        val senderId = requireCurrentUserId()
        val localId = UUID.randomUUID().toString()
        val now = java.time.OffsetDateTime.now().toString()

        // Optimistic local add
        val optimistic = GroupMessageDto(
            id = localId,
            groupId = groupId,
            senderId = senderId,
            content = content,
            messageType = GroupMessageDto.TYPE_USER,
            createdAt = now,
        )
        val current = _groupMessages.value[groupId] ?: emptyList()
        _groupMessages.value = _groupMessages.value + (groupId to (listOf(optimistic) + current))

        // Fire to Supabase
        scope.launch {
            try {
                val result = supabase.postgrest
                    .from("group_messages")
                    .insert(
                        GroupMessageApiDto(
                            id = localId,
                            groupId = groupId,
                            senderId = senderId,
                            content = content,
                            messageType = GroupMessageDto.TYPE_USER,
                        )
                    ) { select() }
                    .decodeSingle<GroupMessageApiDto>()

                val confirmed = result.toMapperDto()
                // Replace optimistic with confirmed
                val updated = _groupMessages.value[groupId]?.map {
                    if (it.id == localId) confirmed else it
                } ?: listOf(confirmed)
                _groupMessages.value = _groupMessages.value + (groupId to updated)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "sendGroupMessage failed", e)
                // Mark failed in local state
                val failed = optimistic.copy()
                // Keep the optimistic message but we won't show error in UI per requirements
            }
        }

        return optimistic
    }

    // ── Refresh ─────────────────────────────────────────────

    override suspend fun refreshConversations() {
        val userId = currentUserId() ?: return
        try {
            val result = supabase.postgrest
                .from("conversations")
                .select {
                    filter {
                        or {
                            eq("user1_id", userId)
                            eq("user2_id", userId)
                        }
                    }
                }
                .decodeList<ConversationApiDto>()
            conversationDao.upsertAllConversations(result.map { it.toMapperDto().toEntity() })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, "refreshConversations error", e)
        }
    }

    override suspend fun refreshMessages(conversationId: String) {
        try {
            // Snapshot local isRead state BEFORE upserting server data.
            // The server may lack an UPDATE RLS policy for messages, so
            // locally-marked reads can be silently rejected.  If we blindly
            // upsert server isRead=false on top of local isRead=true, the
            // unread badge (and read state) flip-flops on every refresh.
            val localIsReadMap: Map<String, Boolean> =
                messageDao.getMessagesByConversationId(conversationId)
                    .first()
                    .filter { it.isRead }
                    .associate { it.id to true }

            val result = supabase.postgrest
                .from("messages")
                .select {
                    filter { eq("conversation_id", conversationId) }
                    order("created_at", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
                }
                .decodeList<MessageApiDto>()
            val remote = result.map { it.toMapperDto() }

            messageDao.upsertAllMessages(
                remote.map { dto ->
                    // Preserve locally-marked isRead if the server still reports false
                    val isRead = localIsReadMap[dto.id] ?: dto.isRead
                    dto.toEntity().copy(
                        localStatus = MessageEntity.LOCAL_STATUS_SENT,
                        isRead = isRead,
                    )
                }
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, "refreshMessages error", e)
        }
    }

    // ── Group message refresh ───────────────────────────────

    private suspend fun refreshGroupMessages(groupId: String) {
        try {
            val result = supabase.postgrest
                .from("group_messages")
                .select {
                    filter { eq("group_id", groupId) }
                    order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    limit(100)
                }
                .decodeList<GroupMessageApiDto>()

            val dtos = result.map { it.toMapperDto() }
            // Merge with existing — keep local optimistic messages that haven't been confirmed
            val existing = _groupMessages.value[groupId] ?: emptyList()
            val merged = (dtos + existing.filter { e ->
                // Keep local messages that aren't yet on the server
                dtos.none { it.id == e.id } &&
                    e.messageType == GroupMessageDto.TYPE_USER
            }).distinctBy { it.id }
                .sortedByDescending { it.createdAt }
            _groupMessages.value = _groupMessages.value + (groupId to merged)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "refreshGroupMessages error", e)
        }
    }

    // ── Helpers ─────────────────────────────────────────────

    private fun currentUserId(): String? {
        return try {
            supabase.auth.currentUserOrNull()?.id
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, "Failed to get current user", e)
            null
        }
    }

    private fun requireCurrentUserId(): String {
        return currentUserId() ?: error("No authenticated user")
    }

    private suspend fun updateConversationLastMessage(
        conversationId: String,
        content: String,
        timestamp: String?,
    ) {
        try {
            supabase.postgrest
                .from("conversations")
                .update(
                    buildMap<String, Any> {
                        put("last_message", content)
                        if (timestamp != null) {
                            put("last_message_at", timestamp)
                        }
                    },
                ) {
                    filter { eq("id", conversationId) }
                }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, "updateConversationLastMessage error", e)
        }
    }

    private fun <T> emptyFlow(): Flow<List<T>> = kotlinx.coroutines.flow.flowOf(emptyList())

    companion object {
        private const val TAG = "MessageRepository"
    }
}

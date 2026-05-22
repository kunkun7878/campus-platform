package com.campus.platform.data.repository

import com.campus.platform.data.local.dao.UserDao
import com.campus.platform.data.local.mapper.NotificationDto
import com.campus.platform.data.local.mapper.toDto
import com.campus.platform.data.local.mapper.toEntity
import com.campus.platform.domain.repository.INotificationRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class NotificationApiDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    val type: String,
    val title: String,
    val body: String? = null,
    @SerialName("ref_type") val refType: String? = null,
    @SerialName("ref_id") val refId: String? = null,
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("read_at") val readAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

private fun NotificationApiDto.toMapperDto() = NotificationDto(
    id, userId, type, title, body, refType, refId, isRead, readAt, createdAt,
)

private fun NotificationDto.toApiDto() = NotificationApiDto(
    id, userId, type, title, body, refType, refId, isRead, readAt, createdAt,
)

@Singleton
class NotificationRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val userDao: UserDao,
) : INotificationRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun getNotifications(userId: String): Flow<List<NotificationDto>> {
        scope.launch { refreshNotifications(userId) }
        return userDao.getNotificationsByUserId(userId).map { it.map { e -> e.toDto() } }
    }

    override fun getUnreadCount(userId: String): Flow<Int> {
        return userDao.getUnreadCountByUserId(userId)
    }

    override suspend fun markAsRead(id: String) {
        val now = Instant.now().toString()
        supabase.postgrest
            .from("notifications")
            .update(mapOf("is_read" to true, "read_at" to now)) { filter { eq("id", id) } }
        userDao.markAsRead(id, now)
    }

    override suspend fun markAllAsRead(userId: String) {
        val now = Instant.now().toString()
        supabase.postgrest
            .from("notifications")
            .update(mapOf("is_read" to true, "read_at" to now)) {
                filter { eq("user_id", userId) }
            }
        userDao.markAllAsRead(userId, now)
    }

    override suspend fun deleteNotification(id: String) {
        supabase.postgrest
            .from("notifications")
            .delete { filter { eq("id", id) } }
        userDao.deleteNotificationById(id)
    }

    override suspend fun refreshNotifications(userId: String) {
        try {
            val result = supabase.postgrest
                .from("notifications")
                .select { filter { eq("user_id", userId) } }
                .decodeList<NotificationApiDto>()
            userDao.upsertAllNotifications(result.map { it.toMapperDto().toEntity() })
        } catch (e: Exception) { if (e is CancellationException) throw e }
    }
}

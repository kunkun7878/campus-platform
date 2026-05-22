package com.campus.platform.domain.repository

import com.campus.platform.data.local.mapper.NotificationDto
import kotlinx.coroutines.flow.Flow

interface INotificationRepository {

    fun getNotifications(userId: String): Flow<List<NotificationDto>>

    fun getUnreadCount(userId: String): Flow<Int>

    suspend fun markAsRead(id: String)

    suspend fun markAllAsRead(userId: String)

    suspend fun deleteNotification(id: String)

    suspend fun refreshNotifications(userId: String)
}

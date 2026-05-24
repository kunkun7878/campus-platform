package com.campus.platform.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Creates FCM notification channels on app startup (API 26+).
 *
 * Channels map to Campus notification types:
 *   chat        → direct messages (HIGH importance, heads-up)
 *   order       → runner/market order status changes (HIGH)
 *   community   → post comments, likes, moderation (DEFAULT)
 *   system      → platform announcements, account alerts (DEFAULT)
 *   lost_found  → lost & found claim updates (DEFAULT)
 *
 * Channel IDs are referenced by the EdgeFn in the FCM android.notification.channel_id
 * field so that incoming notifications route to the correct channel.
 */
@Singleton
class NotificationChannelSetup @Inject constructor() {

    companion object {
        const val CHANNEL_CHAT = "chat"
        const val CHANNEL_ORDER = "order"
        const val CHANNEL_COMMUNITY = "community"
        const val CHANNEL_SYSTEM = "system"
        const val CHANNEL_LOST_FOUND = "lost_found"

        /**
         * Map internal notification type to the matching FCM channel id.
         * Used by the push-notification EdgeFn when building the FCM payload.
         */
        fun channelForType(type: String): String = when (type) {
            "chat", "group_chat" -> CHANNEL_CHAT
            "order_status", "after_sale", "review" -> CHANNEL_ORDER
            "community" -> CHANNEL_COMMUNITY
            "lost_found" -> CHANNEL_LOST_FOUND
            else -> CHANNEL_SYSTEM
        }
    }

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channels = listOf(
            NotificationChannel(
                CHANNEL_CHAT,
                "聊天消息",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "私聊与群聊消息推送"
                enableVibration(true)
                setShowBadge(true)
            },
            NotificationChannel(
                CHANNEL_ORDER,
                "订单动态",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "跑腿/二手订单状态变更与售后通知"
                enableVibration(true)
                setShowBadge(true)
            },
            NotificationChannel(
                CHANNEL_COMMUNITY,
                "社区互动",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "帖子评论、点赞与审核结果通知"
                enableVibration(true)
                setShowBadge(true)
            },
            NotificationChannel(
                CHANNEL_SYSTEM,
                "系统通知",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "平台公告、账号安全与活动通知"
                enableVibration(true)
                setShowBadge(true)
            },
            NotificationChannel(
                CHANNEL_LOST_FOUND,
                "失物招领",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "失物认领申请与归还状态通知"
                enableVibration(true)
                setShowBadge(true)
            },
        )

        manager.createNotificationChannels(channels)
    }

    /**
     * Whether the app currently holds the POST_NOTIFICATIONS permission.
     * API < 33 always returns true (permission granted at install time).
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}

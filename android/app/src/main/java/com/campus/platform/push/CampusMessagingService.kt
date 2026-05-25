package com.campus.platform.push

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.campus.platform.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Firebase Cloud Messaging service — handles token refresh and incoming messages.
 *
 * Token lifecycle:
 *   onNewToken() → FcmTokenManager.saveToken() (upsert to Supabase)
 *
 * Message delivery (data + notification payload):
 *   foreground → skip system notification (Realtime already delivered the content)
 *   background → show system notification with deep-link PendingIntent
 *
 * The EdgeFn sends a combined message: notification block (for system tray)
 * + data block (for deep-link routing and notification_id).
 */
@AndroidEntryPoint
class CampusMessagingService : FirebaseMessagingService() {

    @Inject lateinit var tokenManager: FcmTokenManager
    @Inject lateinit var foregroundTracker: ForegroundTracker
    @Inject lateinit var channelSetup: NotificationChannelSetup
    @Inject lateinit var deepLinkHandler: NotificationDeepLinkHandler

    // ── Token ────────────────────────────────────────────────

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "New FCM token: ${token.take(12)}...")
        // Cache the token in the companion. The token will be saved to Supabase
        // (linked to the user) when AuthModule/AuthRepository calls
        // FcmTokenManager.saveToken() with a real userId after login succeeds.
        pendingToken = token
    }

    // ── Message ──────────────────────────────────────────────

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "Message received from: ${message.from}")

        // Always skip if in foreground — Realtime already delivered the payload
        if (foregroundTracker.isInForeground) {
            Log.d(TAG, "App in foreground — skipping notification display")
            return
        }

        val data = message.data
        val notification = message.notification

        val title = notification?.title
            ?: data["title"]
            ?: getString(com.campus.platform.R.string.app_name)
        val body = notification?.body ?: data["body"] ?: ""

        showNotification(
            notificationId = data["notification_id"],
            channelId = data["channel_id"]
                ?: NotificationChannelSetup.channelForType(data["type"] ?: "system"),
            title = title,
            body = body,
            data = data,
        )
    }

    // ── Notification Display ─────────────────────────────────

    @SuppressLint("MissingPermission")
    private fun showNotification(
        notificationId: String?,
        channelId: String,
        title: String,
        body: String,
        data: Map<String, String>,
    ) {
        // Resolve deep-link target
        val target = deepLinkHandler.resolve(data)

        // Build content intent
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("fcm_route", target.route)
            if (target.notificationId != null) {
                putExtra("notification_id", target.notificationId)
            }
            // Copy all data payload keys so MainActivity can use them
            for ((key, value) in data) {
                putExtra("fcm_$key", value)
            }
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId?.hashCode() ?: System.currentTimeMillis().toInt(),
            intent,
            pendingIntentFlags,
        )

        // Build notification
        val notificationIdInt = notificationId?.hashCode()
            ?: System.currentTimeMillis().toInt()

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(com.campus.platform.R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)

        // Show
        if (channelSetup.hasNotificationPermission(this)) {
            NotificationManagerCompat.from(this).notify(notificationIdInt, builder.build())
        }
    }

    companion object {
        private const val TAG = "CampusFcmService"

        /** Latest FCM token cached from onNewToken, consumed after login. */
        @Volatile
        var pendingToken: String? = null
            private set
    }
}

package com.campus.platform.push

import com.campus.platform.navigation.CampusRoutes
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parses FCM data payload and maps notification metadata to a navigation target.
 *
 * The EdgeFn push-notification sends a data payload with:
 *   type      — notification type (chat, order_status, review, system, after_sale,
 *               lost_found, community, group_chat)
 *   ref_type  — referenced entity type (runner_order, market_order, community_post,
 *               community_comment, lost_found_item, lost_found_claim, conversation,
 *               official_group)
 *   ref_id    — referenced entity UUID
 *   notification_id — notifications table UUID (for mark-as-read)
 *
 * Returns a [DeepLinkTarget] that the MainActivity consumes to navigate.
 */
@Singleton
class NotificationDeepLinkHandler @Inject constructor() {

    fun resolve(data: Map<String, String>): DeepLinkTarget {
        val type = data["type"] ?: ""
        val refType = data["ref_type"]
        val refId = data["ref_id"]
        val notificationId = data["notification_id"]

        if (refId.isNullOrBlank()) {
            return DeepLinkTarget(defaultTabForType(type), notificationId)
        }

        val route = entityRoute(type, refType, refId)
        return DeepLinkTarget(
            route = route ?: defaultTabForType(type),
            notificationId = notificationId,
        )
    }

    private fun entityRoute(type: String, refType: String?, refId: String): String? {
        return when (refType) {
            "runner_order" -> when (type) {
                "after_sale" -> CampusRoutes.AfterSaleDetail.createRoute(refId)
                else -> CampusRoutes.OrderDetail.createRoute(refId)
            }
            "market_order" -> CampusRoutes.MarketOrderDetail.createRoute(refId)
            "community_post", "community_comment" ->
                CampusRoutes.PostDetail.createRoute(refId)
            "lost_found_item", "lost_found_claim" ->
                CampusRoutes.LostDetailRoute.createRoute(refId)
            "conversation" ->
                CampusRoutes.ChatDetail.createRoute(refId)
            "official_group" ->
                CampusRoutes.GroupChat.createRoute(refId)
            else -> null
        }
    }

    private fun defaultTabForType(type: String): String = when (type) {
        "chat", "group_chat" -> CampusRoutes.Message.route
        "order_status", "after_sale", "review" -> CampusRoutes.Home.route
        "community" -> CampusRoutes.Community.route
        "lost_found" -> CampusRoutes.Home.route
        "system" -> CampusRoutes.Profile.route
        else -> CampusRoutes.Home.route
    }
}

/**
 * Navigation target resolved from an FCM notification tap.
 * Handled by MainActivity.onNewIntent → navController.navigate().
 */
data class DeepLinkTarget(
    /** Navigation route string (e.g. "post-detail/abc123") */
    val route: String,
    /** Optional notifications table UUID to mark as read on open */
    val notificationId: String? = null,
)

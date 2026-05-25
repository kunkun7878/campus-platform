package com.campus.platform.navigation

import kotlinx.serialization.Serializable

// ═══════════════════════════════════════════════════════
// 类型安全路由 — Navigation Compose type-safe API
// ═══════════════════════════════════════════════════════

// ── 首页子屏（使用 type-safe 路由） ──
@Serializable
data class GoodsDetail(val goodsId: String)

@Serializable
data class LostDetail(val lostId: String)

@Serializable
data class LostClaim(val lostId: String)

@Serializable
data class AnnouncementDetail(val announcementId: String)

// ── Tab 嵌套图类型（用于 hasRoute<T>() 判定） ──
@Serializable
data object HomeGraph

@Serializable
data object PublishGraph

@Serializable
data object CommunityGraph

@Serializable
data object MessageGraph

@Serializable
data object ProfileGraph

// ═══════════════════════════════════════════════════════
// 字符串路由 — sealed class 集中管理其余路由
// ═══════════════════════════════════════════════════════

/**
 * 校园聚合平台 — 全量路由定义。
 *
 * 所有 screen 用 sealed class 表达，类型安全且 IDE 自动补全友好。
 * 参数化路由提供 createRoute 工厂方法，避免拼接字符串分散各处。
 *
 * GoodsDetail / LostDetail / LostClaim / AnnouncementDetail 已迁移至
 * 顶层 @Serializable 类型安全路由，不使用字符串路由。
 */
sealed class CampusRoutes(val route: String) {

    // ── 全局 ────────────────────────────────────────────
    data object SchoolSelect : CampusRoutes("school-select")

    // ── 认证 ────────────────────────────────────────────
    data object Splash : CampusRoutes("splash")
    data object Login : CampusRoutes("login")
    data object Register : CampusRoutes("register")
    data object PasswordReset : CampusRoutes("password-reset")
    data object AccountDelete : CampusRoutes("account-delete")
    data object UserAgreement : CampusRoutes("user-agreement")
    data object PrivacyPolicy : CampusRoutes("privacy-policy")

    // ── Bottom Nav Tab 根路由 ──────────────────────────
    data object Home : CampusRoutes("home")
    data object PublishHub : CampusRoutes("publish-hub")
    data object Community : CampusRoutes("community")
    data object Message : CampusRoutes("message")
    data object Profile : CampusRoutes("profile")

    // ── 市集子屏 ─────────────────────────────────────────
    data object MarketPublish : CampusRoutes("market-publish")
    data object LostPublish : CampusRoutes("lost-publish")

    // String-based aliases for type-safe routes (used by FCM deep links)
    data object LostDetailRoute : CampusRoutes("lost-detail/{lostId}") {
        fun createRoute(lostId: String) = "lost-detail/$lostId"
    }
    data object GoodsDetailRoute : CampusRoutes("goods-detail/{goodsId}") {
        fun createRoute(goodsId: String) = "goods-detail/$goodsId"
    }

    data object OrderDetail : CampusRoutes("order-detail/{orderId}") {
        fun createRoute(orderId: String) = "order-detail/$orderId"
    }

    data object MarketOrderDetail : CampusRoutes("market-order-detail/{orderId}") {
        fun createRoute(orderId: String) = "market-order-detail/$orderId"
    }

    // ── 发布子屏 ─────────────────────────────────────────
    data object Publish : CampusRoutes("publish")

    data object OrderList : CampusRoutes("order-list/{tab}") {
        fun createRoute(tab: String = "published") = "order-list/$tab"
    }

    data object AfterSaleApply : CampusRoutes("after-sale-apply/{orderId}") {
        fun createRoute(orderId: String) = "after-sale-apply/$orderId"
    }

    data object AfterSaleDetail : CampusRoutes("after-sale-detail/{saleId}") {
        fun createRoute(saleId: String) = "after-sale-detail/$saleId"
    }

    // ── 社区子屏 ─────────────────────────────────────────
    data object PostDetail : CampusRoutes("post-detail/{postId}") {
        fun createRoute(postId: String) = "post-detail/$postId"
    }

    data object PostCreate : CampusRoutes("post-create")

    data object GroupChat : CampusRoutes("group-chat/{chatId}") {
        fun createRoute(chatId: String) = "group-chat/$chatId"
    }

    // ── 消息子屏 ─────────────────────────────────────────
    data object ChatDetail : CampusRoutes("chat-detail/{chatId}") {
        fun createRoute(chatId: String) = "chat-detail/$chatId"
    }

    // ── 个人中心子屏（12 条） ─────────────────────────────
    data object Wallet : CampusRoutes("profile/wallet")
    data object RunnerApply : CampusRoutes("profile/runner-apply")
    data object AddressManage : CampusRoutes("profile/address-manage")
    data object Coupons : CampusRoutes("profile/coupons")
    data object Invite : CampusRoutes("profile/invite")
    data object Feedback : CampusRoutes("profile/feedback")
    data object About : CampusRoutes("profile/about")
    data object MyPublished : CampusRoutes("profile/my-published")
    data object MySold : CampusRoutes("profile/my-sold")
    data object MyBought : CampusRoutes("profile/my-bought")
    data object MyFavorites : CampusRoutes("profile/my-favorites")

    // ── Agent 后台（8 条） ────────────────────────────────
    data object AgentDashboard : CampusRoutes("agent/dashboard")
    data object AgentReviewList : CampusRoutes("agent/review-list")
    data object AgentReviewDetail : CampusRoutes("agent/review-detail/{postId}") {
        fun createRoute(postId: String) = "agent/review-detail/$postId"
    }
    data object AgentUserList : CampusRoutes("agent/user-list")
    data object AgentUserDetail : CampusRoutes("agent/user-detail/{userId}") {
        fun createRoute(userId: String) = "agent/user-detail/$userId"
    }
    data object AgentAnnouncementList : CampusRoutes("agent/announcement-list")
    data object AgentAnnouncementEdit : CampusRoutes("agent/announcement-edit/{announcementId}") {
        fun createRoute(announcementId: String) = "agent/announcement-edit/$announcementId"
    }
    data object AgentRunnerReview : CampusRoutes("agent/runner-review")
}

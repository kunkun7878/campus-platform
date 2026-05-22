package com.campus.platform.navigation

/**
 * Bottom Nav 五个 Tab 的定义。
 *
 * label 用于 UI 展示，iconDescription 用于 Material Icons 映射。
 * 实际图标在 CampusBottomNav 组件内根据 name 分发，
 * 避免在这里持有 Composable 引用（enum 不支持）。
 */
enum class CampusBottomNavItem(
    val route: String,
    val label: String,
    val iconDescription: String,
) {
    Home(
        route = CampusRoutes.Home.route,
        label = "首页",
        iconDescription = "Home",
    ),
    PublishHub(
        route = CampusRoutes.PublishHub.route,
        label = "发布",
        iconDescription = "AddCircle",
    ),
    Community(
        route = CampusRoutes.Community.route,
        label = "社区",
        iconDescription = "Forum",
    ),
    Message(
        route = CampusRoutes.Message.route,
        label = "消息",
        iconDescription = "Message",
    ),
    Profile(
        route = CampusRoutes.Profile.route,
        label = "我的",
        iconDescription = "Person",
    );

}

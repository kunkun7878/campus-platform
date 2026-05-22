package com.campus.platform.navigation

import com.campus.platform.data.model.Profile

/**
 * App 启动时的目标目的地。
 *
 * 由 [determineStartDestination] 根据 auth state 和 profile 决定。
 */
sealed class AppStartDestination(val route: String) {
    /** 已登录 + 已选校 → 进入首页 */
    data object Home : AppStartDestination(CampusRoutes.Home.route)

    /** 已登录 + 未选校 → 进入选校页 */
    data object SchoolSelect : AppStartDestination(CampusRoutes.SchoolSelect.route)

    /** 未登录 → 进入登录页 */
    data object Login : AppStartDestination(CampusRoutes.Login.route)
}

/**
 * 根据认证状态和用户 profile 决定 App 启动目标。
 *
 * @param isAuthenticated 当前 Supabase session 是否有效
 * @param profile 当前用户的 profile 记录（可能为 null）
 */
fun determineStartDestination(
    isAuthenticated: Boolean,
    profile: Profile?,
): AppStartDestination {
    if (!isAuthenticated) return AppStartDestination.Login
    if (profile == null || profile.schoolId == null || profile.campusId == null) {
        return AppStartDestination.SchoolSelect
    }
    return AppStartDestination.Home
}

package com.campus.platform.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import com.campus.platform.navigation.campusScreenConfigFor

/**
 * 校园平台主脚手架。
 *
 * 根据当前路由动态决定是否展示 [CampusHeroBar] 和 [CampusBottomNav]，
 * 决策逻辑委托给 [campusScreenConfigFor] 函数。
 *
 * HeroBar 的 brand / meta 文字取自 [CampusScreenConfig]，与 HTML 原型 screenConfigs 一致。
 * BottomNav 的选中态通过 hasRoute<T>() 判定，不再依赖 currentParentRoute 字符串。
 *
 * @param currentRoute       当前 NavHost 路由字符串，用于脚手架配置决策。
 * @param currentDestination 当前 NavDestination，用于 BottomNav hasRoute<T>() 判定。
 * @param navController      导航控制器，用于 BottomNav re-click 弹回。
 * @param onNavigate         底部导航点击回调。
 * @param content            页面内容 Composable（已含 innerPadding 处理）。
 */
@Composable
fun CampusMainScaffold(
    currentRoute: String?,
    currentDestination: NavDestination?,
    navController: NavHostController,
    onNavigate: (String) -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    val config = campusScreenConfigFor(currentRoute)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (config.showHeroBar) {
                CampusHeroBar(
                    brand = config.brand ?: "校园聚合平台",
                    meta = config.meta ?: "",
                    searchPlaceholder = config.search,
                    modifier = Modifier.statusBarsPadding(),
                )
            }
        },
        bottomBar = {
            if (config.showBottomNav) {
                CampusBottomNav(
                    currentDestination = currentDestination,
                    navController = navController,
                    onNavigate = onNavigate,
                )
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            content(Modifier.fillMaxSize())
        }
    }
}

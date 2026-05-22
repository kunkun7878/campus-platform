package com.campus.platform.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import com.campus.platform.navigation.CampusBottomNavItem
import com.campus.platform.navigation.CommunityGraph
import com.campus.platform.navigation.HomeGraph
import com.campus.platform.navigation.MessageGraph
import com.campus.platform.navigation.ProfileGraph
import com.campus.platform.navigation.PublishGraph

/**
 * 校园平台底部导航栏。
 *
 * 五个 Tab：首页、发布、社区、消息、我的。
 * 图标根据 [CampusBottomNavItem.iconDescription] 分发 filled/outlined 两套，
 * 选中态使用 filled 变体，非选中态使用 outlined。
 *
 * 点击已选中的 Tab 会 popBackStack 回到该 Tab 的根路由，
 * 实现"再次点击同一 Tab 返回根页面"的交互。
 *
 * 选中判定使用 hasRoute<T>() 匹配嵌套图类型（HomeGraph 等），
 * 自动覆盖嵌套图内所有子页面，不再依赖硬编码字符串。
 *
 * @param currentDestination 当前 NavDestination，用于 hasRoute<T>() 判定选中 Tab。
 * @param navController      导航控制器，用于 re-click 弹回根路由。
 * @param onNavigate         点击非选中 Tab 时的回调，传入目标 route。
 */
@Composable
fun CampusBottomNav(
    currentDestination: NavDestination?,
    navController: NavHostController,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        CampusBottomNavItem.entries.forEach { tab ->
            val selected = tab.isSelected(currentDestination)

            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (selected) {
                        // 再次点击已选中的 Tab：弹回该 Tab 根路由
                        navController.popBackStack(tab.route, inclusive = false)
                    } else {
                        onNavigate(tab.route)
                    }
                },
                icon = {
                    Icon(
                        imageVector = bottomNavIconFor(tab, selected),
                        contentDescription = tab.label,
                    )
                },
                label = {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        }
    }
}

/**
 * 判定当前 NavDestination 是否属于该 Tab。
 *
 * 使用 hasRoute<T>() 匹配嵌套图类型（如 HomeGraph），
 * 自动覆盖嵌套图内所有子页面。相比旧版 currentParentRoute 字符串比较，
 * 此方案类型安全且无硬编码。
 */
private fun CampusBottomNavItem.isSelected(
    currentDestination: NavDestination?,
): Boolean {
    if (currentDestination == null) return false
    return when (this) {
        CampusBottomNavItem.Home -> currentDestination.hasRoute<HomeGraph>()
        CampusBottomNavItem.PublishHub -> currentDestination.hasRoute<PublishGraph>()
        CampusBottomNavItem.Community -> currentDestination.hasRoute<CommunityGraph>()
        CampusBottomNavItem.Message -> currentDestination.hasRoute<MessageGraph>()
        CampusBottomNavItem.Profile -> currentDestination.hasRoute<ProfileGraph>()
    }
}

/**
 * 根据 [tab] 和选中状态返回对应的 Material Icon。
 */
@Composable
private fun bottomNavIconFor(tab: CampusBottomNavItem, selected: Boolean): ImageVector {
    return when (tab) {
        CampusBottomNavItem.Home -> if (selected) Icons.Filled.Home else Icons.Outlined.Home
        CampusBottomNavItem.PublishHub ->
            if (selected) Icons.Filled.AddCircle else Icons.Outlined.AddCircle
        CampusBottomNavItem.Community ->
            if (selected) Icons.Filled.Forum else Icons.Outlined.Forum
        CampusBottomNavItem.Message ->
            if (selected) Icons.AutoMirrored.Filled.Message else Icons.AutoMirrored.Outlined.Message
        CampusBottomNavItem.Profile ->
            if (selected) Icons.Filled.Person else Icons.Outlined.Person
    }
}

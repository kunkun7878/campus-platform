package com.campus.platform.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.campus.platform.data.auth.AuthRepository
import com.campus.platform.data.model.Profile
import com.campus.platform.data.school.SchoolRepository
import com.campus.platform.ui.component.CampusMainScaffold
import com.campus.platform.ui.screen.auth.AccountDeleteScreen
import com.campus.platform.ui.screen.auth.LoginScreen
import com.campus.platform.ui.screen.auth.PasswordResetScreen
import com.campus.platform.ui.screen.auth.RegisterScreen
import com.campus.platform.ui.screen.community.CommunityScreen
import com.campus.platform.ui.screen.community.GroupChatScreen
import com.campus.platform.ui.screen.community.PostCreateScreen
import com.campus.platform.ui.screen.community.PostDetailScreen
import com.campus.platform.ui.screen.global.SchoolSelectScreen
import com.campus.platform.ui.screen.home.AnnouncementDetailScreen
import com.campus.platform.ui.screen.home.GoodsDetailScreen
import com.campus.platform.ui.screen.home.HomeScreen
import com.campus.platform.ui.screen.home.LostClaimScreen
import com.campus.platform.ui.screen.home.LostDetailScreen
import com.campus.platform.ui.screen.market.AfterSaleApplyScreen
import com.campus.platform.ui.screen.market.AfterSaleDetailScreen
import com.campus.platform.ui.screen.market.LostPublishScreen
import com.campus.platform.ui.screen.market.MarketPublishScreen
import com.campus.platform.ui.screen.market.OrderDetailScreen
import com.campus.platform.ui.screen.market.OrderListScreen
import com.campus.platform.ui.screen.message.ChatDetailScreen
import com.campus.platform.ui.screen.message.MessageScreen
import com.campus.platform.ui.screen.profile.AboutScreen
import com.campus.platform.ui.screen.profile.AddressManageScreen
import com.campus.platform.ui.screen.profile.CouponsScreen
import com.campus.platform.ui.screen.profile.FeedbackScreen
import com.campus.platform.ui.screen.profile.InviteScreen
import com.campus.platform.ui.screen.profile.MyBoughtScreen
import com.campus.platform.ui.screen.profile.MyFavoritesScreen
import com.campus.platform.ui.screen.profile.MyPublishedScreen
import com.campus.platform.ui.screen.profile.MySoldScreen
import com.campus.platform.ui.screen.profile.ProfileScreen
import com.campus.platform.ui.screen.profile.RunnerApplyScreen
import com.campus.platform.ui.screen.profile.WalletScreen
import com.campus.platform.ui.screen.publish.PublishHubScreen
import com.campus.platform.ui.screen.publish.PublishScreen

/**
 * 校园平台全量导航图。
 *
 * Phase 2 更新：
 * - startDestination 使用 AuthGuard 动态判定（通过 Splash screen 中转）
 * - 新增 PasswordReset、AccountDelete 路由
 * - 认证 screen 全部接收 AuthRepository/SchoolRepository 参数
 * - 5 个 Tab 嵌套图保持不变
 *
 * 认证流程：
 * App 启动 → Splash 检查 auth → Login / SchoolSelect / Home
 * Login → 成功 → SchoolSelect（未选校）/ Home（已选校）
 * Register → 成功 → SchoolSelect
 * PasswordReset → 成功 → SchoolSelect（未选校）/ Home（已选校）
 * SchoolSelect → 确认 → Home（清空回退栈）
 * AccountDelete → 完成 → Login（清空回退栈）
 */
@Composable
fun CampusNavGraph(
    navController: NavHostController,
    authRepository: AuthRepository,
    schoolRepository: SchoolRepository,
    modifier: Modifier = Modifier,
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination: NavDestination? = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    CampusMainScaffold(
        currentRoute = currentRoute,
        currentDestination = currentDestination,
        navController = navController,
        onNavigate = { route ->
            navController.navigate(route) {
                popUpTo(navController.graph.startDestinationRoute ?: "splash") {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        },
    ) { contentModifier ->
        NavHost(
            navController = navController,
            startDestination = "splash",
            modifier = contentModifier,
        ) {
            // ═══════════════════════════════════════════
            // Splash — AuthGuard 判定入口
            // ═══════════════════════════════════════════
            composable("splash") {
                var destination by remember { mutableStateOf<AppStartDestination?>(null) }
                var profile by remember { mutableStateOf<Profile?>(null) }

                LaunchedEffect(Unit) {
                    val session = authRepository.getSession()
                    val isAuth = session != null
                    if (isAuth) {
                        profile = authRepository.getProfile()
                    }
                    destination = determineStartDestination(isAuth, profile)
                }

                if (destination != null) {
                    LaunchedEffect(destination) {
                        val target = destination?.route ?: return@LaunchedEffect
                        navController.navigate(target) {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            // ═══════════════════════════════════════════
            // 全局 / 认证（不在任何 Tab 嵌套图内）
            // ═══════════════════════════════════════════

            // Post-auth 路由：统一处理登录/注册后的 profile 检查
            composable("post-auth") {
                LaunchedEffect(Unit) {
                    val profile = authRepository.getProfile()
                    val target = if (profile != null && profile.schoolId != null && profile.campusId != null) {
                        CampusRoutes.Home.route
                    } else {
                        CampusRoutes.SchoolSelect.route
                    }
                    navController.navigate(target) {
                        popUpTo("splash") { inclusive = true }
                    }
                }
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            composable(CampusRoutes.SchoolSelect.route) {
                SchoolSelectScreen(
                    authRepository = authRepository,
                    schoolRepository = schoolRepository,
                    onSchoolSelected = {
                        navController.navigate(CampusRoutes.Home.route) {
                            popUpTo("splash") { inclusive = true }
                        }
                    },
                )
            }

            composable(CampusRoutes.Login.route) {
                LoginScreen(
                    authRepository = authRepository,
                    onLoginSuccess = {
                        navController.navigate("post-auth")
                    },
                    onNavigateToRegister = {
                        navController.navigate(CampusRoutes.Register.route)
                    },
                    onNavigateToPasswordReset = {
                        navController.navigate(CampusRoutes.PasswordReset.route)
                    },
                )
            }

            composable(CampusRoutes.Register.route) {
                RegisterScreen(
                    authRepository = authRepository,
                    onRegisterSuccess = {
                        navController.navigate(CampusRoutes.SchoolSelect.route) {
                            popUpTo(CampusRoutes.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.popBackStack()
                    },
                )
            }

            composable(CampusRoutes.PasswordReset.route) {
                PasswordResetScreen(
                    authRepository = authRepository,
                    onNavigateToLogin = {
                        navController.popBackStack()
                    },
                )
            }

            composable(CampusRoutes.AccountDelete.route) {
                AccountDeleteScreen(
                    authRepository = authRepository,
                    onDeleteComplete = {
                        navController.navigate(CampusRoutes.Login.route) {
                            popUpTo("splash") { inclusive = true }
                        }
                    },
                    onCancel = {
                        navController.popBackStack()
                    },
                )
            }

            // ═══════════════════════════════════════════
            // HomeTab — 首页
            // ═══════════════════════════════════════════
            navigation<HomeGraph>(
                startDestination = CampusRoutes.Home.route,
            ) {
                composable(CampusRoutes.Home.route) {
                    HomeScreen()
                }

                composable<GoodsDetail> {
                    GoodsDetailScreen()
                }
                composable<LostDetail> {
                    LostDetailScreen()
                }
                composable<LostClaim> {
                    LostClaimScreen()
                }
                composable(
                    route = CampusRoutes.OrderDetail.route,
                    arguments = listOf(navArgument("orderId") { type = NavType.StringType }),
                ) {
                    OrderDetailScreen()
                }
                composable(CampusRoutes.OrderList.route) {
                    OrderListScreen()
                }
                composable(
                    route = CampusRoutes.AfterSaleApply.route,
                    arguments = listOf(navArgument("orderId") { type = NavType.StringType }),
                ) {
                    AfterSaleApplyScreen()
                }
                composable(
                    route = CampusRoutes.AfterSaleDetail.route,
                    arguments = listOf(navArgument("saleId") { type = NavType.StringType }),
                ) {
                    AfterSaleDetailScreen()
                }
                composable<AnnouncementDetail> {
                    AnnouncementDetailScreen()
                }
            }

            // ═══════════════════════════════════════════
            // PublishTab — 发布
            // ═══════════════════════════════════════════
            navigation<PublishGraph>(
                startDestination = CampusRoutes.PublishHub.route,
            ) {
                composable(CampusRoutes.PublishHub.route) {
                    PublishHubScreen()
                }
                composable(CampusRoutes.Publish.route) {
                    PublishScreen()
                }
                composable(CampusRoutes.MarketPublish.route) {
                    MarketPublishScreen()
                }
                composable(CampusRoutes.LostPublish.route) {
                    LostPublishScreen()
                }
            }

            // ═══════════════════════════════════════════
            // CommunityTab — 社区
            // ═══════════════════════════════════════════
            navigation<CommunityGraph>(
                startDestination = CampusRoutes.Community.route,
            ) {
                composable(CampusRoutes.Community.route) {
                    CommunityScreen()
                }
                composable(
                    route = CampusRoutes.PostDetail.route,
                    arguments = listOf(navArgument("postId") { type = NavType.StringType }),
                ) {
                    PostDetailScreen()
                }
                composable(CampusRoutes.PostCreate.route) {
                    PostCreateScreen()
                }
                composable(
                    route = CampusRoutes.GroupChat.route,
                    arguments = listOf(navArgument("chatId") { type = NavType.StringType }),
                ) {
                    GroupChatScreen()
                }
            }

            // ═══════════════════════════════════════════
            // MessageTab — 消息
            // ═══════════════════════════════════════════
            navigation<MessageGraph>(
                startDestination = CampusRoutes.Message.route,
            ) {
                composable(CampusRoutes.Message.route) {
                    MessageScreen()
                }
                composable(
                    route = CampusRoutes.ChatDetail.route,
                    arguments = listOf(navArgument("chatId") { type = NavType.StringType }),
                ) {
                    ChatDetailScreen()
                }
            }

            // ═══════════════════════════════════════════
            // ProfileTab — 个人中心
            // ═══════════════════════════════════════════
            navigation<ProfileGraph>(
                startDestination = CampusRoutes.Profile.route,
            ) {
                composable(CampusRoutes.Profile.route) {
                    ProfileScreen()
                }
                composable(CampusRoutes.Wallet.route) {
                    WalletScreen()
                }
                composable(CampusRoutes.RunnerApply.route) {
                    RunnerApplyScreen()
                }
                composable(CampusRoutes.AddressManage.route) {
                    AddressManageScreen()
                }
                composable(CampusRoutes.Coupons.route) {
                    CouponsScreen()
                }
                composable(CampusRoutes.Invite.route) {
                    InviteScreen()
                }
                composable(CampusRoutes.Feedback.route) {
                    FeedbackScreen()
                }
                composable(CampusRoutes.About.route) {
                    AboutScreen()
                }
                composable(CampusRoutes.MyPublished.route) {
                    MyPublishedScreen()
                }
                composable(CampusRoutes.MySold.route) {
                    MySoldScreen()
                }
                composable(CampusRoutes.MyBought.route) {
                    MyBoughtScreen()
                }
                composable(CampusRoutes.MyFavorites.route) {
                    MyFavoritesScreen()
                }
            }
        }
    }
}

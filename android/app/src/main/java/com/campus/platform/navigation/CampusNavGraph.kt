package com.campus.platform.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.campus.platform.ui.component.CampusMainScaffold
import com.campus.platform.ui.component.ScreenPlaceholder
import com.campus.platform.ui.screen.agent.AgentDashboardScreen
import com.campus.platform.ui.screen.agent.AgentReviewListScreen
import com.campus.platform.ui.screen.agent.AgentReviewDetailScreen
import com.campus.platform.ui.screen.agent.AgentUserListScreen
import com.campus.platform.ui.screen.agent.AgentUserDetailScreen
import com.campus.platform.ui.screen.agent.AgentAnnouncementListScreen
import com.campus.platform.ui.screen.agent.AgentAnnouncementEditScreen
import com.campus.platform.ui.screen.agent.AgentRunnerReviewScreen
import com.campus.platform.ui.screen.auth.AccountDeleteScreen
import com.campus.platform.ui.screen.auth.LoginScreen
import com.campus.platform.ui.screen.auth.PasswordResetScreen
import com.campus.platform.ui.screen.auth.PrivacyPolicyScreen
import com.campus.platform.ui.screen.auth.RegisterScreen
import com.campus.platform.ui.screen.auth.UserAgreementScreen
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
import com.campus.platform.ui.screen.market.LostPublishScreen
import com.campus.platform.ui.screen.market.MarketOrderDetailScreen
import com.campus.platform.ui.screen.market.MarketPublishScreen
import com.campus.platform.ui.screen.runner.AfterSaleApplyScreen
import com.campus.platform.ui.screen.runner.AfterSaleDetailScreen
import com.campus.platform.ui.screen.runner.OrderDetailScreen
import com.campus.platform.ui.screen.runner.OrderListScreen
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
import com.campus.platform.ui.viewmodel.SplashViewModel

/**
 * 校园平台全量导航图。
 *
 * Phase 3 更新（ViewModel 全面迁移）：
 * - 移除 authRepository / schoolRepository 参数，所有依赖由 ViewModel 通过 Hilt 注入
 * - Splash 和 post-auth 路由使用 SplashViewModel
 * - 所有 Screen 调用改为 (viewModel, navController) 签名
 * - Screen 内部通过 navController.navigate() 跳转，不再需要 lambda 回调
 *
 * 认证流程：
 * App 启动 → Splash 检查 auth → Login / SchoolSelect / Home
 * Login → 成功 → post-auth → SchoolSelect（未选校）/ Home（已选校）
 * Register → 成功 → SchoolSelect
 * PasswordReset → 成功 → post-auth → SchoolSelect / Home
 * SchoolSelect → 确认 → Home（清空回退栈）
 * AccountDelete → 完成 → Login（清空回退栈）
 */
@Composable
fun CampusNavGraph(
    navController: NavHostController,
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
            // Splash — AuthGuard 判定入口（使用 SplashViewModel）
            // ═══════════════════════════════════════════
            composable("splash") {
                val splashViewModel: SplashViewModel = hiltViewModel()
                val uiState by splashViewModel.uiState.collectAsState()

                LaunchedEffect(uiState) {
                    if (uiState is SplashViewModel.SplashUiState.Destination) {
                        val state = uiState as SplashViewModel.SplashUiState.Destination
                        navController.navigate(state.target.route) {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    when (val state = uiState) {
                        is SplashViewModel.SplashUiState.Loading ->
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        is SplashViewModel.SplashUiState.Error ->
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.error,
                            )
                        is SplashViewModel.SplashUiState.Destination ->
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // ═══════════════════════════════════════════
            // 全局 / 认证（不在任何 Tab 嵌套图内）
            // ═══════════════════════════════════════════

            // Post-auth 路由：统一处理登录/注册后的 profile 检查
            composable("post-auth") {
                val splashViewModel: SplashViewModel = hiltViewModel()
                val uiState by splashViewModel.uiState.collectAsState()

                LaunchedEffect(Unit) {
                    splashViewModel.determinePostAuthDestination()
                }

                LaunchedEffect(uiState) {
                    if (uiState is SplashViewModel.SplashUiState.Destination) {
                        val state = uiState as SplashViewModel.SplashUiState.Destination
                        navController.navigate(state.target.route) {
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

            composable(CampusRoutes.SchoolSelect.route) {
                SchoolSelectScreen(navController = navController)
            }

            composable(CampusRoutes.Login.route) {
                LoginScreen(navController = navController)
            }

            composable(CampusRoutes.Register.route) {
                RegisterScreen(navController = navController)
            }

            composable(CampusRoutes.PasswordReset.route) {
                PasswordResetScreen(navController = navController)
            }

            composable(CampusRoutes.AccountDelete.route) {
                AccountDeleteScreen(navController = navController)
            }

            composable(CampusRoutes.UserAgreement.route) {
                UserAgreementScreen(navController = navController)
            }

            composable(CampusRoutes.PrivacyPolicy.route) {
                PrivacyPolicyScreen(navController = navController)
            }

            // ═══════════════════════════════════════════
            // HomeTab — 首页
            // ═══════════════════════════════════════════
            navigation<HomeGraph>(
                startDestination = CampusRoutes.Home.route,
            ) {
                composable(CampusRoutes.Home.route) {
                    HomeScreen(navController = navController)
                }

                composable<GoodsDetail> {
                    GoodsDetailScreen(navController = navController)
                }
                // String-based alias for FCM deep links
                composable(
                    route = CampusRoutes.GoodsDetailRoute.route,
                    arguments = listOf(navArgument("goodsId") { type = NavType.StringType }),
                ) {
                    GoodsDetailScreen(navController = navController)
                }
                composable<LostDetail> {
                    LostDetailScreen(navController = navController)
                }
                // String-based alias for FCM deep links
                composable(
                    route = CampusRoutes.LostDetailRoute.route,
                    arguments = listOf(navArgument("lostId") { type = NavType.StringType }),
                ) {
                    LostDetailScreen(navController = navController)
                }
                composable<LostClaim> {
                    LostClaimScreen(navController = navController)
                }
                composable(
                    route = CampusRoutes.OrderDetail.route,
                    arguments = listOf(navArgument("orderId") { type = NavType.StringType }),
                ) {
                    OrderDetailScreen(navController = navController)
                }
                composable(
                    route = CampusRoutes.OrderList.route,
                    arguments = listOf(navArgument("tab") { type = NavType.StringType; defaultValue = "published" }),
                ) {
                    OrderListScreen(navController = navController)
                }
                composable(
                    route = CampusRoutes.AfterSaleApply.route,
                    arguments = listOf(navArgument("orderId") { type = NavType.StringType }),
                ) {
                    AfterSaleApplyScreen(navController = navController)
                }
                composable(
                    route = CampusRoutes.AfterSaleDetail.route,
                    arguments = listOf(navArgument("saleId") { type = NavType.StringType }),
                ) {
                    AfterSaleDetailScreen(navController = navController)
                }
                composable<AnnouncementDetail> {
                    AnnouncementDetailScreen(navController = navController)
                }
            }

            // ═══════════════════════════════════════════
            // PublishTab — 发布
            // ═══════════════════════════════════════════
            navigation<PublishGraph>(
                startDestination = CampusRoutes.PublishHub.route,
            ) {
                composable(CampusRoutes.PublishHub.route) {
                    PublishHubScreen(navController = navController)
                }
                composable(CampusRoutes.Publish.route) {
                    PublishScreen(navController = navController)
                }
                composable(CampusRoutes.MarketPublish.route) {
                    MarketPublishScreen(navController = navController)
                }
                composable(CampusRoutes.LostPublish.route) {
                    LostPublishScreen(navController = navController)
                }
            }

            // ═══════════════════════════════════════════
            // CommunityTab — 社区
            // ═══════════════════════════════════════════
            navigation<CommunityGraph>(
                startDestination = CampusRoutes.Community.route,
            ) {
                composable(CampusRoutes.Community.route) {
                    CommunityScreen(navController = navController)
                }
                composable(
                    route = CampusRoutes.PostDetail.route,
                    arguments = listOf(navArgument("postId") { type = NavType.StringType }),
                ) {
                    PostDetailScreen(navController = navController)
                }
                composable(CampusRoutes.PostCreate.route) {
                    PostCreateScreen(navController = navController)
                }
                composable(
                    route = CampusRoutes.GroupChat.route,
                    arguments = listOf(navArgument("chatId") { type = NavType.StringType }),
                ) {
                    GroupChatScreen(navController = navController)
                }
            }

            // ═══════════════════════════════════════════
            // MessageTab — 消息
            // ═══════════════════════════════════════════
            navigation<MessageGraph>(
                startDestination = CampusRoutes.Message.route,
            ) {
                composable(CampusRoutes.Message.route) {
                    MessageScreen(navController = navController)
                }
                composable(
                    route = CampusRoutes.ChatDetail.route,
                    arguments = listOf(navArgument("chatId") { type = NavType.StringType }),
                ) {
                    ChatDetailScreen(navController = navController)
                }
            }

            // ═══════════════════════════════════════════
            // ProfileTab — 个人中心
            // ═══════════════════════════════════════════
            navigation<ProfileGraph>(
                startDestination = CampusRoutes.Profile.route,
            ) {
                composable(CampusRoutes.Profile.route) {
                    ProfileScreen(navController = navController)
                }
                composable(CampusRoutes.Wallet.route) {
                    WalletScreen(navController = navController)
                }
                composable(CampusRoutes.RunnerApply.route) {
                    RunnerApplyScreen(navController = navController)
                }
                composable(CampusRoutes.AddressManage.route) {
                    AddressManageScreen(navController = navController)
                }
                composable(CampusRoutes.Coupons.route) {
                    CouponsScreen(navController = navController)
                }
                composable(CampusRoutes.Invite.route) {
                    InviteScreen(navController = navController)
                }
                composable(CampusRoutes.Feedback.route) {
                    FeedbackScreen(navController = navController)
                }
                composable(CampusRoutes.About.route) {
                    AboutScreen(navController = navController)
                }
                composable(CampusRoutes.MyPublished.route) {
                    MyPublishedScreen(navController = navController)
                }
                composable(CampusRoutes.MySold.route) {
                    MySoldScreen(navController = navController)
                }
                composable(CampusRoutes.MyBought.route) {
                    MyBoughtScreen(navController = navController)
                }
                composable(CampusRoutes.MyFavorites.route) {
                    MyFavoritesScreen(navController = navController)
                }
                composable(
                    route = CampusRoutes.MarketOrderDetail.route,
                    arguments = listOf(navArgument("orderId") { type = NavType.StringType }),
                ) {
                    MarketOrderDetailScreen(navController = navController)
                }

                // ── Agent 后台（8 条） ──
                composable(CampusRoutes.AgentDashboard.route) {
                    AgentDashboardScreen(navController = navController)
                }
                composable(CampusRoutes.AgentReviewList.route) {
                    AgentReviewListScreen(navController = navController)
                }
                composable(
                    route = CampusRoutes.AgentReviewDetail.route,
                    arguments = listOf(navArgument("postId") { type = NavType.StringType }),
                ) {
                    AgentReviewDetailScreen(navController = navController)
                }
                composable(CampusRoutes.AgentUserList.route) {
                    AgentUserListScreen(navController = navController)
                }
                composable(
                    route = CampusRoutes.AgentUserDetail.route,
                    arguments = listOf(navArgument("userId") { type = NavType.StringType }),
                ) {
                    AgentUserDetailScreen(navController = navController)
                }
                composable(CampusRoutes.AgentAnnouncementList.route) {
                    AgentAnnouncementListScreen(navController = navController)
                }
                composable(
                    route = CampusRoutes.AgentAnnouncementEdit.route,
                    arguments = listOf(navArgument("announcementId") { type = NavType.StringType }),
                ) {
                    AgentAnnouncementEditScreen(navController = navController)
                }
                composable(CampusRoutes.AgentRunnerReview.route) {
                    AgentRunnerReviewScreen(navController = navController)
                }
            }
        }
    }
}

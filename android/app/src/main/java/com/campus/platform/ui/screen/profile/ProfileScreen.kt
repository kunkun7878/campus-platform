package com.campus.platform.ui.screen.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.campus.platform.data.model.Profile
import com.campus.platform.navigation.CampusRoutes
import com.campus.platform.ui.viewmodel.UiState
import com.campus.platform.ui.viewmodel.profile.ProfileViewModel

/** 我的 — Profile Tab 根页面 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val isAgent by viewModel.isAgent.collectAsStateWithLifecycle()
    val schoolName by viewModel.schoolName.collectAsStateWithLifecycle()
    val pendingReviewCount by viewModel.pendingReviewCount.collectAsStateWithLifecycle()
    val navOnSignOut by viewModel.navOnSignOut.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 退出登录后导航到 Login 并清空回退栈
    LaunchedEffect(navOnSignOut) {
        if (navOnSignOut) {
            navController.navigate(CampusRoutes.Login.route) {
                popUpTo("splash") { inclusive = true }
            }
            viewModel.onSignOutConsumed()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("我的") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            // ── 1. 用户信息卡片 ──────────────────────────────
            UserInfoCard(
                profileState = profileState,
                schoolName = schoolName,
                onClick = { Toast.makeText(context, "即将开放", Toast.LENGTH_SHORT).show() },
            )

            // ── 2. Agent 入口（仅 isAgent 时显示） ──────────
            if (isAgent) {
                AgentEntryCard(
                    pendingReviewCount = pendingReviewCount,
                    onClick = { navController.navigate(CampusRoutes.AgentDashboard.route) },
                )
            }

            // ── 3. 服务中心 ──────────────────────────────────
            SectionHeader("服务中心")
            ProfileMenuItem(
                icon = Icons.Filled.AccountBalanceWallet,
                title = "我的钱包",
                onClick = { navController.navigate(CampusRoutes.Wallet.route) },
            )
            HorizontalDivider(modifier = Modifier.padding(start = 52.dp))
            ProfileMenuItem(
                icon = Icons.Filled.LocationOn,
                title = "地址管理",
                onClick = { navController.navigate(CampusRoutes.AddressManage.route) },
            )
            HorizontalDivider(modifier = Modifier.padding(start = 52.dp))
            ProfileMenuItem(
                icon = Icons.Filled.CardGiftcard,
                title = "我的优惠券",
                onClick = { navController.navigate(CampusRoutes.Coupons.route) },
            )
            HorizontalDivider(modifier = Modifier.padding(start = 52.dp))
            ProfileMenuItem(
                icon = Icons.Filled.Feedback,
                title = "意见反馈",
                onClick = { navController.navigate(CampusRoutes.Feedback.route) },
            )
            HorizontalDivider(modifier = Modifier.padding(start = 52.dp))
            ProfileMenuItem(
                icon = Icons.Filled.PersonAdd,
                title = "邀请好友",
                onClick = { navController.navigate(CampusRoutes.Invite.route) },
            )
            HorizontalDivider(modifier = Modifier.padding(start = 52.dp))
            ProfileMenuItem(
                icon = Icons.Filled.Info,
                title = "关于我们",
                onClick = { navController.navigate(CampusRoutes.About.route) },
            )
            HorizontalDivider(modifier = Modifier.padding(start = 52.dp))
            ProfileMenuItem(
                icon = Icons.AutoMirrored.Filled.DirectionsRun,
                title = "成为跑腿员",
                onClick = { navController.navigate(CampusRoutes.RunnerApply.route) },
            )

            // ── 4. 跑腿订单（已有） ──────────────────────────
            SectionHeader("跑腿订单")
            ProfileMenuItem(
                icon = Icons.AutoMirrored.Filled.DirectionsRun,
                title = "我发布的跑腿",
                onClick = { navController.navigate(CampusRoutes.OrderList.createRoute("published")) },
            )
            HorizontalDivider(modifier = Modifier.padding(start = 52.dp))
            ProfileMenuItem(
                icon = Icons.Filled.ShoppingBag,
                title = "我接的单",
                onClick = { navController.navigate(CampusRoutes.OrderList.createRoute("bought")) },
            )

            // ── 5. 二手交易（已有） ──────────────────────────
            SectionHeader("我的二手交易")
            ProfileMenuItem(
                icon = Icons.Filled.ShoppingCart,
                title = "我发布的",
                onClick = { navController.navigate(CampusRoutes.MyPublished.route) },
            )
            HorizontalDivider(modifier = Modifier.padding(start = 52.dp))
            ProfileMenuItem(
                icon = Icons.AutoMirrored.Filled.DirectionsRun,
                title = "我卖出的",
                onClick = { navController.navigate(CampusRoutes.MySold.route) },
            )
            HorizontalDivider(modifier = Modifier.padding(start = 52.dp))
            ProfileMenuItem(
                icon = Icons.Filled.ShoppingBag,
                title = "我买到的",
                onClick = { navController.navigate(CampusRoutes.MyBought.route) },
            )
            HorizontalDivider(modifier = Modifier.padding(start = 52.dp))
            ProfileMenuItem(
                icon = Icons.Filled.Favorite,
                title = "我的收藏",
                onClick = { navController.navigate(CampusRoutes.MyFavorites.route) },
            )

            // ── 6. 设置 ──────────────────────────────────────
            SectionHeader("设置")
            ProfileMenuItem(
                icon = Icons.Filled.Settings,
                title = "系统设置",
                onClick = { Toast.makeText(context, "即将开放", Toast.LENGTH_SHORT).show() },
            )
            Spacer(modifier = Modifier.height(12.dp))
            LogoutButton(onClick = { viewModel.signOut() })

            // 底部留白
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ── Section Header ───────────────────────────────────────────────

@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(top = 20.dp, bottom = 8.dp),
    )
}

// ── Menu Item ────────────────────────────────────────────────────

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )
    }
}

// ── User Info Card ───────────────────────────────────────────────

@Composable
private fun UserInfoCard(
    profileState: UiState<Profile>,
    schoolName: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        when (profileState) {
            is UiState.Loading -> {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "加载中...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            is UiState.Error -> {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.errorContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "加载失败",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                        )
                        if (profileState.message.isNotBlank()) {
                            Text(
                                text = profileState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            is UiState.Success -> {
                val profile = profileState.data
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 头像
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (!profile.avatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(profile.avatarUrl)
                                    .build(),
                                contentDescription = "头像",
                                modifier = Modifier.matchParentSize().clip(CircleShape),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = profile.nickname ?: "未设置昵称",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = schoolName ?: profile.schoolId ?: "未知学校",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                }
            }
        }
    }
}

// ── Agent Entry Card ─────────────────────────────────────────────

@Composable
private fun AgentEntryCard(
    pendingReviewCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.AdminPanelSettings,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "代理服务中心",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "管理社区内容、审核帖子",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (pendingReviewCount > 0) {
                Badge(
                    containerColor = MaterialTheme.colorScheme.error,
                ) {
                    Text(
                        text = pendingReviewCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onError,
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
        }
    }
}

// ── Logout Button ─────────────────────────────────────────────────

@Composable
private fun LogoutButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.error,
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Logout,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "退出登录")
    }
}

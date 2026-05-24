package com.campus.platform.ui.screen.profile

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.campus.platform.navigation.CampusRoutes
import com.campus.platform.ui.viewmodel.profile.ProfileViewModel

/** 我的 — Profile Tab 根页面 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    navController: NavController,
    modifier: Modifier = Modifier,
) {
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
            // ── 跑腿相关 ──────────────────────────────────
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

            // ── 二手交易 ──────────────────────────────────
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

package com.campus.platform.ui.screen.runner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.campus.platform.navigation.CampusRoutes
import com.campus.platform.ui.component.runner.OrderCard
import com.campus.platform.ui.component.runner.RunnerEmptyState
import com.campus.platform.ui.viewmodel.UiState
import com.campus.platform.ui.viewmodel.runner.OrderListItem
import com.campus.platform.ui.viewmodel.runner.OrderListUiData
import com.campus.platform.ui.viewmodel.runner.OrderListViewModel

// ── 待评价 pill 色 ─────────────────────────────────────────
private val ReviewPillOrange = Color(0xFFFF8C00)

// ── Tab 定义 ───────────────────────────────────────────────
private data class OrderTab(val key: String, val label: String)

private val tabs = listOf(
    OrderTab("published", "我发布的"),
    OrderTab("bought", "我接的单"),
)

// ── 状态子筛选 ─────────────────────────────────────────────
private data class StatusFilter(val key: String, val label: String)

private val statusFilters = listOf(
    StatusFilter("all", "全部"),
    StatusFilter("in_progress", "进行中"),
    StatusFilter("completed", "已完成"),
    StatusFilter("after_sale", "售后中"),
)

// ═════════════════════════════════════════════════════════════
// Screen
// ═════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OrderListScreen(
    viewModel: OrderListViewModel = hiltViewModel(),
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    // 从路由参数读取默认 tab，并与 ViewModel 同步
    val routeTab = navController.currentBackStackEntry
        ?.arguments
        ?.getString("tab")
        ?: "published"

    LaunchedEffect(routeTab) {
        if (routeTab in listOf("published", "bought")) {
            viewModel.setTab(routeTab)
        }
    }

    // ── 状态收集 ──────────────────────────────────────────
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("跑腿订单") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
    ) { innerPadding ->
        when (val state = uiState) {
            is UiState.Loading -> {
                // ── Loading ───────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            is UiState.Error -> {
                // ── Error ─────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    RunnerEmptyState(
                        title = "加载失败",
                        subtitle = state.message,
                        actionLabel = "重试",
                        onAction = { viewModel.refresh() },
                    )
                }
            }

            is UiState.Success -> {
                val data = state.data
                OrderListContent(
                    data = data,
                    innerPadding = innerPadding,
                    onTabClick = { viewModel.setTab(it) },
                    onFilterClick = { viewModel.setFilter(it) },
                    onItemClick = { navigateId ->
                        val route = CampusRoutes.OrderDetail.createRoute(navigateId)
                        navController.navigate(route)
                    },
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════
// Content (Success state)
// ═════════════════════════════════════════════════════════════

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OrderListContent(
    data: OrderListUiData,
    innerPadding: PaddingValues,
    onTabClick: (String) -> Unit,
    onFilterClick: (String) -> Unit,
    onItemClick: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
    ) {
        // ── Tab Row ───────────────────────────────────────
        val selectedTabIndex = data.activeTab.coerceIn(0, tabs.lastIndex)

        TabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = {},
        ) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = selectedTabIndex == index
                Tab(
                    selected = isSelected,
                    onClick = { onTabClick(tab.key) },
                    text = {
                        Text(
                            text = tab.label,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
            }
        }

        // ── FilterChip Row ────────────────────────────────
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            statusFilters.forEachIndexed { index, filter ->
                val isSelected = data.activeFilter == index
                FilterChip(
                    selected = isSelected,
                    onClick = { onFilterClick(filter.key) },
                    label = {
                        Text(
                            text = filter.label,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            }
        }

        // ── 内容区 ────────────────────────────────────────
        Box(modifier = Modifier.fillMaxSize()) {
            if (data.orders.isEmpty()) {
                val isPublishedTab = data.activeTab == 0
                RunnerEmptyState(
                    title = if (isPublishedTab) "暂无发布的跑腿" else "暂无接单记录",
                    subtitle = if (isPublishedTab)
                        "去发布跑腿赚点零花钱吧"
                    else
                        "去接单广场看看有没有合适的跑腿",
                    modifier = Modifier.align(Alignment.Center),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        horizontal = 16.dp,
                        vertical = 4.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(
                        items = data.orders,
                        key = { it.navigateId },
                    ) { item ->
                        OrderListItemCard(
                            item = item,
                            onClick = { onItemClick(item.navigateId) },
                        )
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════
// Item Card
// ═════════════════════════════════════════════════════════════

@Composable
private fun OrderListItemCard(
    item: OrderListItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        OrderCard(
            title = item.title,
            taskType = item.taskType,
            typeLabel = item.typeLabel,
            status = item.status,
            statusLabel = item.statusLabel,
            amount = item.amount,
            time = item.time ?: "",
            onClick = onClick,
        )

        // "待评价" pill：已完成且当前用户尚未评价时显示
        if (item.isPendingReview) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(ReviewPillOrange.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(
                    text = "待评价",
                    style = MaterialTheme.typography.labelSmall,
                    color = ReviewPillOrange,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

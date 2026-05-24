package com.campus.platform.ui.screen.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.campus.platform.data.local.mapper.MarketListingDto
import com.campus.platform.data.local.mapper.MarketOrderDto
import com.campus.platform.navigation.CampusRoutes
import com.campus.platform.ui.component.ChipFilterBar
import com.campus.platform.ui.component.MarketOrderCard
import com.campus.platform.ui.component.runner.RunnerEmptyState
import com.campus.platform.ui.viewmodel.UiState
import com.campus.platform.ui.viewmodel.profile.MySoldFilter
import com.campus.platform.ui.viewmodel.profile.MySoldViewModel

// ── Filter chip mapping ──────────────────────────────────────

private val soldFilterLabels = listOf("全部", "待面交", "已完成")

private fun labelToFilterKey(index: Int): String = when (index) {
    0 -> MySoldFilter.ALL
    1 -> MySoldFilter.PENDING_MEETUP
    2 -> MySoldFilter.COMPLETED
    else -> MySoldFilter.ALL
}

private fun filterKeyToIndex(key: String): Int = when (key) {
    MySoldFilter.ALL -> 0
    MySoldFilter.PENDING_MEETUP -> 1
    MySoldFilter.COMPLETED -> 2
    else -> 0
}

// ── Helpers ──────────────────────────────────────────────────

private fun formatPrice(price: Int): String = "¥$price"

private fun statusLabel(status: String): String = when (status) {
    "pending" -> "待确认"
    "accepted" -> "待面交"
    "completed" -> "已完成"
    "cancelled" -> "已取消"
    else -> status
}

private fun formatTime(timestamp: String?): String {
    if (timestamp.isNullOrBlank()) return ""
    return try {
        timestamp.replace("T", " ").take(16)
    } catch (_: Exception) {
        timestamp.take(16)
    }
}

private fun shortId(id: String): String {
    return if (id.length > 8) id.take(8) + "..." else id
}

// ── Screen ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MySoldScreen(
    viewModel: MySoldViewModel = hiltViewModel(),
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activeFilter by viewModel.activeFilter.collectAsStateWithLifecycle()
    val listingMap by viewModel.listingMap.collectAsStateWithLifecycle()

    var isRefreshing by remember { mutableStateOf(false) }
    // Cache last successfully loaded orders to avoid flashing during refresh
    var cachedOrders by remember { mutableStateOf<List<MarketOrderDto>?>(null) }

    // Track loaded data and sync refresh state
    LaunchedEffect(uiState) {
        when (uiState) {
            is UiState.Success -> cachedOrders = (uiState as UiState.Success).data
            is UiState.Error -> { /* Real error — keep previous cache */ }
            else -> {}
        }
        isRefreshing = false
    }

    val displayOrders = when (uiState) {
        is UiState.Success -> (uiState as UiState.Success).data
        else -> cachedOrders
    }

    val isLoadingAndNoCache = uiState is UiState.Loading && cachedOrders == null
    val isRealError = uiState is UiState.Error && cachedOrders == null

    val selectedFilterIndex = filterKeyToIndex(activeFilter)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("我卖出的") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            // ── Filter chips ──
            ChipFilterBar(
                items = soldFilterLabels,
                selectedIndex = selectedFilterIndex,
                onSelected = { index ->
                    viewModel.setFilter(labelToFilterKey(index))
                },
            )

            // ── Content area ──
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                when {
                    // Initial loading — spinner
                    isLoadingAndNoCache -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    // Real error — retry
                    isRealError -> {
                        RunnerEmptyState(
                            title = "加载失败",
                            subtitle = (uiState as UiState.Error).message,
                            actionLabel = "重试",
                            onAction = { viewModel.loadSoldOrders() },
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }

                    // Empty or data — runtime check with smart cast
                    else -> {
                        val orders = displayOrders
                        if (orders != null && orders.isNotEmpty()) {
                        PullToRefreshBox(
                            isRefreshing = isRefreshing,
                            onRefresh = {
                                isRefreshing = true
                                viewModel.loadSoldOrders()
                            },
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    horizontal = 16.dp,
                                    vertical = 4.dp,
                                ),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                items(
                                    items = orders,
                                    key = { it.id },
                                ) { order ->
                                    val listing: MarketListingDto? = listingMap[order.listingId]
                                    MarketOrderCard(
                                        title = listing?.title ?: "商品已下架",
                                        counterparty = "买家：${shortId(order.buyerId)}",
                                        price = listing?.let { formatPrice(it.price) } ?: "-",
                                        status = order.status,
                                        statusLabel = statusLabel(order.status),
                                        time = formatTime(order.createdAt),
                                        onClick = {
                                            navController.navigate(
                                                CampusRoutes.MarketOrderDetail.createRoute(order.id)
                                            )
                                        },
                                    )
                                }
                            }
                        }
                        } else {
                            RunnerEmptyState(
                                title = "还没有卖出的商品",
                                subtitle = "发布的商品有人购买后会显示在这里",
                                modifier = Modifier.align(Alignment.Center),
                            )
                        }
                    }
                }
            }
        }
    }
}

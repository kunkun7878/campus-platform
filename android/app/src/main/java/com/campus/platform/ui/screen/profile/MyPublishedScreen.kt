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
import androidx.compose.material3.TopAppBarDefaults
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
import com.campus.platform.data.local.entity.MarketListingEntity
import com.campus.platform.data.local.mapper.PublishedEntry
import com.campus.platform.navigation.CampusRoutes
import com.campus.platform.navigation.GoodsDetail
import com.campus.platform.ui.component.ChipFilterBar
import com.campus.platform.ui.component.MarketCardVariant
import com.campus.platform.ui.component.MarketFeedCard
import com.campus.platform.ui.component.MarketFeedItem
import com.campus.platform.ui.component.MarketUiMapper
import com.campus.platform.ui.component.runner.RunnerEmptyState
import com.campus.platform.ui.viewmodel.UiState
import com.campus.platform.ui.viewmodel.profile.MyPublishedViewModel

/**
 * 「我发布的」跨类型商品列表。
 *
 * 顶部为 FilterChip 筛选栏（全部 | 跑腿 | 二手 | 失物），
 * 下方展示统一 [PublishedEntry] 列表。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPublishedScreen(
    viewModel: MyPublishedViewModel = hiltViewModel(),
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activeFilterIndex by viewModel.activeFilterIndex.collectAsStateWithLifecycle()
    val listingOrderMap by viewModel.listingOrderMap.collectAsStateWithLifecycle()

    var isRefreshing by remember { mutableStateOf(false) }
    var cachedEntries by remember { mutableStateOf<List<PublishedEntry>?>(null) }

    LaunchedEffect(uiState) {
        when (uiState) {
            is UiState.Success -> cachedEntries = (uiState as UiState.Success).data
            is UiState.Error -> {}
            else -> {}
        }
        isRefreshing = false
    }

    val displayEntries = when (uiState) {
        is UiState.Success -> (uiState as UiState.Success).data
        else -> cachedEntries
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("我发布的") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
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
                .padding(innerPadding),
        ) {
            Text(
                text = "跑腿·二手·失物",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )

            ChipFilterBar(
                items = MyPublishedViewModel.FILTER_LABELS,
                selectedIndex = activeFilterIndex,
                onSelected = { viewModel.selectFilter(it) },
            )

            when (val state = uiState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                is UiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
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
                    val data = displayEntries
                    if (data.isNullOrEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            RunnerEmptyState(
                                title = "还没有发布任何内容",
                                subtitle = "去发布你的第一条内容吧",
                                actionLabel = "去发布",
                                onAction = { navController.navigate(CampusRoutes.PublishHub.route) },
                            )
                        }
                    } else {
                        PullToRefreshBox(
                            isRefreshing = isRefreshing,
                            onRefresh = {
                                isRefreshing = true
                                viewModel.refresh()
                            },
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    horizontal = 16.dp,
                                    vertical = 8.dp,
                                ),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                items(
                                    items = data,
                                    key = { it.id },
                                ) { entry ->
                                    val item = entry.toMarketFeedItem()
                                    MarketFeedCard(
                                        item = item,
                                        variant = MarketCardVariant.MY_PUBLISHED,
                                        onClick = {
                                            when (entry) {
                                                is PublishedEntry.Market -> {
                                                    val listing = entry.listing
                                                    when (listing.status) {
                                                        MarketListingEntity.STATUS_SOLD -> {
                                                            val orderId = listingOrderMap[listing.id]
                                                            if (orderId != null) {
                                                                navController.navigate(
                                                                    CampusRoutes.MarketOrderDetail.createRoute(orderId),
                                                                )
                                                            } else {
                                                                navController.navigate(GoodsDetail(listing.id))
                                                            }
                                                        }
                                                        else -> {
                                                            navController.navigate(GoodsDetail(listing.id))
                                                        }
                                                    }
                                                }
                                                is PublishedEntry.Runner -> {
                                                    navController.navigate(
                                                        CampusRoutes.OrderDetail.createRoute(entry.id)
                                                    )
                                                }
                                                is PublishedEntry.LostFound -> {
                                                    navController.navigate(
                                                        CampusRoutes.LostDetailRoute.createRoute(entry.id)
                                                    )
                                                }
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── PublishedEntry → MarketFeedItem ─────────────────────────────────────

private fun PublishedEntry.toMarketFeedItem(): MarketFeedItem = MarketFeedItem(
    id = id,
    images = MarketUiMapper.parseImages(images),
    title = title,
    price = when (this) {
        is PublishedEntry.Market -> "¥${listing.price}"
        is PublishedEntry.Runner -> "¥${task.price + task.tip}"
        is PublishedEntry.LostFound -> if (item.reward > 0) "¥${item.reward}" else "--"
    },
    condition = when (this) {
        is PublishedEntry.Market -> MarketUiMapper.conditionDisplay(listing.condition)
        is PublishedEntry.Runner -> "跑腿"
        is PublishedEntry.LostFound -> when (item.type) { "lost" -> "失物" else -> "招领" }
    },
    category = when (this) {
        is PublishedEntry.Market -> listing.category
        is PublishedEntry.Runner -> task.type
        is PublishedEntry.LostFound -> item.category
    },
    time = MarketUiMapper.formatTime(createdAt),
    status = MarketUiMapper.statusDisplay(status),
    favoriteCount = when (this) {
        is PublishedEntry.Market -> listing.favoriteCount
        else -> 0
    },
    isFavorite = false,
)

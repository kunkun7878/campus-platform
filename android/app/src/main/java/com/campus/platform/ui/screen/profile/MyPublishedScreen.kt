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
import com.campus.platform.data.local.mapper.MarketListingDto
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
import kotlinx.serialization.json.Json

/**
 * 「我发布的」跨类型商品列表。
 *
 * 顶部为 FilterChip 筛选栏（全部 | 跑腿 | 二手 | 失物），
 * 下方根据 [MyPublishedViewModel.uiState] 展示 Loading / Error / Empty / List 四态。
 *
 * 点击行为按商品状态分支：
 * - active → 跳转 GoodsDetail
 * - sold → 有订单映射时跳转 MarketOrderDetail，否则 GoodsDetail
 * - 其它 → GoodsDetail
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
    val isUnderDevelopment by viewModel.isUnderDevelopment.collectAsStateWithLifecycle()
    val listingOrderMap by viewModel.listingOrderMap.collectAsStateWithLifecycle()

    var isRefreshing by remember { mutableStateOf(false) }
    var cachedListings by remember { mutableStateOf<List<MarketListingDto>?>(null) }

    // Sync refresh state
    LaunchedEffect(uiState) {
        when (uiState) {
            is UiState.Success -> cachedListings = (uiState as UiState.Success).data
            is UiState.Error -> { /* Real error — keep previous cache */ }
            else -> {}
        }
        isRefreshing = false
    }

    val displayListings = when (uiState) {
        is UiState.Success -> (uiState as UiState.Success).data
        else -> cachedListings
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
            // 副标题 — 来源类型
            Text(
                text = "跑腿·二手·失物",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )

            // FilterChip 筛选栏
            ChipFilterBar(
                items = MyPublishedViewModel.FILTER_LABELS,
                selectedIndex = activeFilterIndex,
                onSelected = { viewModel.selectFilter(it) },
            )

            // 内容区
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
                    val data = displayListings
                    if (data.isNullOrEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isUnderDevelopment) {
                                RunnerEmptyState(
                                    title = "功能开发中",
                                    subtitle = "该分类功能将在后续版本上线",
                                )
                            } else {
                                RunnerEmptyState(
                                    title = "还没有发布任何商品",
                                    subtitle = "去发布你的第一件好物吧",
                                    actionLabel = "去发布",
                                    onAction = { navController.navigate(CampusRoutes.PublishHub.route) },
                                )
                            }
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
                                ) { listing ->
                                    val item = listing.toMarketFeedItem()
                                    MarketFeedCard(
                                        item = item,
                                        variant = MarketCardVariant.MY_PUBLISHED,
                                        onClick = {
                                            when (listing.status) {
                                                MarketListingEntity.STATUS_ACTIVE -> {
                                                    navController.navigate(
                                                        GoodsDetail(listing.id),
                                                    )
                                                }
                                                MarketListingEntity.STATUS_SOLD -> {
                                                    val orderId = listingOrderMap[listing.id]
                                                    if (orderId != null) {
                                                        navController.navigate(
                                                            CampusRoutes.MarketOrderDetail.createRoute(orderId),
                                                        )
                                                    } else {
                                                        navController.navigate(
                                                            GoodsDetail(listing.id),
                                                        )
                                                    }
                                                }
                                                else -> {
                                                    navController.navigate(
                                                        GoodsDetail(listing.id),
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

// ── 映射工具 ──────────────────────────────────────────────────────────

/** [MarketListingDto] → [MarketFeedItem] 映射 */
private fun MarketListingDto.toMarketFeedItem(): MarketFeedItem = MarketFeedItem(
    id = id,
    images = MarketUiMapper.parseImages(images),
    title = title,
    price = "¥$price",
    condition = MarketUiMapper.conditionDisplay(condition),
    category = category,
    time = MarketUiMapper.formatTime(createdAt),
    status = MarketUiMapper.statusDisplay(status),
    // TODO Phase 7: favoriteCount 需要后端添加 favorite_count 字段 + trigger，当前暂为 0
    favoriteCount = 0,
    isFavorite = false,
)

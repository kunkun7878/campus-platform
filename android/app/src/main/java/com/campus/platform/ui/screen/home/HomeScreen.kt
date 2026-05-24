package com.campus.platform.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.campus.platform.data.local.mapper.LostFoundItemDto
import com.campus.platform.data.local.mapper.MarketListingDto
import com.campus.platform.navigation.CampusRoutes
import com.campus.platform.navigation.GoodsDetail
import com.campus.platform.navigation.LostDetail
import com.campus.platform.ui.component.LostEmptyState
import com.campus.platform.ui.component.LostItemCard
import com.campus.platform.ui.component.MarketCardVariant
import com.campus.platform.ui.component.MarketFeedCard
import com.campus.platform.ui.component.MarketFeedItem
import com.campus.platform.ui.component.MarketUiMapper
import com.campus.platform.ui.component.runner.RunnerEmptyState
import com.campus.platform.ui.component.runner.RunnerTaskCard
import com.campus.platform.ui.component.runner.RunnerTypeFilter
import com.campus.platform.ui.viewmodel.UiState
import com.campus.platform.ui.viewmodel.home.HomeViewModel
import kotlinx.coroutines.launch

/**
 * 首页。
 *
 * 顶部为三个 FilterChip 作为子视图选择器（跑腿 / 二手物品 / 失物招领），
 * 与 HTML 原型 viewConfigs 的 runner/market/lost 标签一一对应。
 * 下方使用 HorizontalPager 实现左右滑动切换子视图，
 * FilterChip 与 Pager 页面状态双向同步。
 *
 * page 0（跑腿）: 搜索栏 + 类型筛选 Chip + 任务卡片列表，含加载/空/错态。
 * page 1（二手物品）: 搜索栏 + 商品卡片列表，含加载/空/错态，支持收藏切换。
 * page 2（失物招领）: 搜索栏 + 类型筛选 + 失物卡片列表，含加载/空/错态。
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    val tabs = HomeSubView.entries
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })
    val scope = rememberCoroutineScope()
    val currentTab = tabs[pagerState.currentPage]

    // Runner tab state
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val selectedType by viewModel.selectedType.collectAsStateWithLifecycle()
    val searchKeyword by viewModel.searchKeyword.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isInitialLoading by viewModel.isInitialLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val navToOrderId by viewModel.navToOrderId.collectAsStateWithLifecycle()

    // Market tab state
    val marketListings by viewModel.marketListings.collectAsStateWithLifecycle()
    val marketFavoriteIds by viewModel.marketFavoriteIds.collectAsStateWithLifecycle()
    val marketSearchQuery by viewModel.marketSearchQuery.collectAsStateWithLifecycle()

    // Lost tab state
    val lostItems by viewModel.lostItems.collectAsStateWithLifecycle()
    val lostType by viewModel.lostType.collectAsStateWithLifecycle()
    val lostSearchKeyword by viewModel.lostSearchKeyword.collectAsStateWithLifecycle()
    val isLostRefreshing by viewModel.isLostRefreshing.collectAsStateWithLifecycle()

    // 观察 ViewModel 发出的导航事件（已解析正确的 orderId）
    LaunchedEffect(navToOrderId) {
        navToOrderId?.let { resolvedId ->
            navController.navigate(CampusRoutes.OrderDetail.createRoute(resolvedId))
            viewModel.onNavEventConsumed()
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // FilterChip 选择器
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            tabs.forEachIndexed { index, tab ->
                FilterChip(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    label = {
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                )
            }
        }

        // 滑动页面
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            when (page) {
                0 -> RunnerTab(
                    tasks = tasks,
                    selectedType = selectedType,
                    searchKeyword = searchKeyword,
                    isRefreshing = isRefreshing,
                    isInitialLoading = isInitialLoading,
                    error = error,
                    searchHint = tabs[0].searchHint,
                    onTypeSelected = { viewModel.selectType(it) },
                    onSearchKeywordChange = { viewModel.onSearchKeywordChange(it) },
                    onRefresh = { viewModel.refresh() },
                    onTaskClick = { taskId ->
                        viewModel.onTaskClick(taskId)
                    },
                    onPublishClick = {
                        navController.navigate(CampusRoutes.PublishHub.route)
                    },
                )

                1 -> MarketTab(
                    uiState = marketListings,
                    favoriteIds = marketFavoriteIds,
                    searchQuery = marketSearchQuery,
                    searchHint = tabs[1].searchHint,
                    isRefreshing = isRefreshing,
                    onSearchQueryChange = { viewModel.onMarketSearchQueryChange(it) },
                    onRefresh = { viewModel.refresh() },
                    onCardClick = { listingId ->
                        navController.navigate(GoodsDetail(listingId))
                    },
                    onFavoriteClick = { listingId ->
                        viewModel.toggleMarketFavorite(listingId)
                    },
                    onPublishClick = {
                        navController.navigate(CampusRoutes.PublishHub.route)
                    },
                )

                2 -> LostTab(
                    uiState = lostItems,
                    selectedType = lostType,
                    searchKeyword = lostSearchKeyword,
                    isRefreshing = isLostRefreshing,
                    searchHint = tabs[2].searchHint,
                    onTypeSelected = { viewModel.selectLostType(it) },
                    onSearchKeywordChange = { viewModel.onLostSearchKeywordChange(it) },
                    onRefresh = { viewModel.refreshLost() },
                    onItemClick = { lostId ->
                        navController.navigate(LostDetail(lostId))
                    },
                    onPublishClick = {
                        navController.navigate(CampusRoutes.PublishHub.route)
                    },
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// Runner tab content
// ═══════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RunnerTab(
    tasks: List<com.campus.platform.data.local.mapper.RunnerTaskDto>,
    selectedType: String?,
    searchKeyword: String,
    isRefreshing: Boolean,
    isInitialLoading: Boolean,
    error: String?,
    searchHint: String,
    onTypeSelected: (String?) -> Unit,
    onSearchKeywordChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onTaskClick: (String) -> Unit,
    onPublishClick: () -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // ── 搜索栏 ──
            item(key = "runner_search") {
                OutlinedTextField(
                    value = searchKeyword,
                    onValueChange = onSearchKeywordChange,
                    placeholder = {
                        Text(
                            text = searchHint,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "搜索",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    trailingIcon = {
                        if (searchKeyword.isNotEmpty()) {
                            IconButton(onClick = { onSearchKeywordChange("") }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "清除搜索",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                )
            }

            // ── 类型筛选 Chip ──
            item(key = "runner_filter") {
                RunnerTypeFilter(
                    selectedType = selectedType,
                    onTypeSelected = onTypeSelected,
                )
            }

            // ── 内容区：加载 / 错误 / 空 / 卡片列表 ──
            if (isInitialLoading && tasks.isEmpty()) {
                item(key = "runner_loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 64.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            } else if (error != null && tasks.isEmpty()) {
                item(key = "runner_error") {
                    RunnerEmptyState(
                        title = error ?: "加载失败",
                        subtitle = "请检查网络后重试",
                        actionLabel = "重试",
                        onAction = onRefresh,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else if (tasks.isEmpty()) {
                item(key = "runner_empty") {
                    RunnerEmptyState(
                        title = "暂无跑腿任务",
                        subtitle = "发布第一个跑腿需求吧",
                        actionLabel = "去发布",
                        onAction = onPublishClick,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                items(
                    items = tasks,
                    key = { it.id },
                ) { task ->
                    RunnerTaskCard(
                        title = task.title,
                        reward = "¥${task.price}",
                        tip = if (task.tip > 0) "+¥${task.tip}" else null,
                        pickupAddress = task.pickupAddr ?: "未指定",
                        deliveryAddress = task.deliveryAddr ?: "未指定",
                        publishTime = HomeViewModel.formatTime(task.createdAt),
                        taskType = task.type,
                        status = task.status,
                        statusLabel = HomeViewModel.statusLabel(task.status),
                        onClick = { onTaskClick(task.id) },
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// Market tab content
// ═══════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MarketTab(
    uiState: UiState<List<MarketListingDto>>,
    favoriteIds: Set<String>,
    searchQuery: String,
    searchHint: String,
    isRefreshing: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onCardClick: (String) -> Unit,
    onFavoriteClick: (String) -> Unit,
    onPublishClick: () -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // ── 搜索栏 ──
            item(key = "market_search") {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                        Text(
                            text = searchHint,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "搜索",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "清除搜索",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                )
            }

            // ── 内容区：加载 / 错误 / 空 / 卡片列表 ──
            when (uiState) {
                is UiState.Loading -> {
                    item(key = "market_loading") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 64.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }

                is UiState.Error -> {
                    item(key = "market_error") {
                        RunnerEmptyState(
                            title = "加载失败",
                            subtitle = uiState.message,
                            actionLabel = "重试",
                            onAction = onRefresh,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                is UiState.Success -> {
                    val rawData = uiState.data
                    val filteredData = if (searchQuery.isBlank()) rawData
                        else rawData.filter { listing ->
                            listing.title.contains(searchQuery, ignoreCase = true) ||
                            listing.description?.contains(searchQuery, ignoreCase = true) == true
                        }
                    if (filteredData.isEmpty()) {
                        item(key = "market_empty") {
                            RunnerEmptyState(
                                title = if (searchQuery.isNotBlank()) "未找到相关商品" else "暂无商品",
                                subtitle = if (searchQuery.isNotBlank()) "换个关键词试试" else "发布第一个商品吧",
                                actionLabel = if (searchQuery.isNotBlank()) "清除搜索" else "去发布",
                                onAction = {
                                    if (searchQuery.isNotBlank()) onSearchQueryChange("") else onPublishClick()
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    } else {
                        items(
                            items = filteredData,
                            key = { it.id },
                        ) { listing ->
                            val item = listing.toHomeMarketFeedItem(favoriteIds)
                            MarketFeedCard(
                                item = item,
                                variant = MarketCardVariant.HOME,
                                onClick = { onCardClick(listing.id) },
                                onFavoriteClick = {
                                    onFavoriteClick(listing.id)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// Lost tab content
// ═══════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun LostTab(
    uiState: UiState<List<LostFoundItemDto>>,
    selectedType: String?,
    searchKeyword: String,
    isRefreshing: Boolean,
    searchHint: String,
    onTypeSelected: (String?) -> Unit,
    onSearchKeywordChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onItemClick: (String) -> Unit,
    onPublishClick: () -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // ── 搜索栏 ──
            item(key = "lost_search") {
                OutlinedTextField(
                    value = searchKeyword,
                    onValueChange = onSearchKeywordChange,
                    placeholder = {
                        Text(
                            text = searchHint,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "搜索",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    trailingIcon = {
                        if (searchKeyword.isNotEmpty()) {
                            IconButton(onClick = { onSearchKeywordChange("") }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "清除搜索",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                )
            }

            // ── 失物类型筛选 Chip ──
            item(key = "lost_filter") {
                val lostTypeFilterItems = listOf(
                    null to "全部",
                    "lost" to "失物",
                    "found" to "招领",
                )

                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    lostTypeFilterItems.forEach { (type, label) ->
                        val isSelected = selectedType == type
                        FilterChip(
                            selected = isSelected,
                            onClick = { onTypeSelected(type) },
                            label = {
                                Text(
                                    text = label,
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
            }

            // ── 内容区：加载 / 错误 / 空 / 卡片列表 ──
            when (uiState) {
                is UiState.Loading -> {
                    item(key = "lost_loading") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 64.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }

                is UiState.Error -> {
                    item(key = "lost_error") {
                        LostEmptyState(
                            title = "加载失败",
                            subtitle = "请检查网络后下拉重试",
                            actionLabel = "重试",
                            onAction = onRefresh,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                is UiState.Success -> {
                    val rawData = uiState.data
                    // 客户端类型筛选
                    val typeFiltered = if (selectedType != null) {
                        rawData.filter { it.type == selectedType }
                    } else rawData
                    // 客户端搜索筛选
                    val filteredData = if (searchKeyword.isBlank()) typeFiltered
                        else typeFiltered.filter { item ->
                            item.title.contains(searchKeyword, ignoreCase = true) ||
                            item.description?.contains(searchKeyword, ignoreCase = true) == true ||
                            item.location?.contains(searchKeyword, ignoreCase = true) == true
                        }

                    if (filteredData.isEmpty()) {
                        item(key = "lost_empty") {
                            val hasActiveFilter = selectedType != null || searchKeyword.isNotBlank()
                            LostEmptyState(
                                title = if (hasActiveFilter) "未找到相关启事" else "暂无失物招领",
                                subtitle = when {
                                    searchKeyword.isNotBlank() -> "换个关键词试试"
                                    selectedType != null -> "当前分类下暂无启事"
                                    else -> "发布第一条失物招领吧"
                                },
                                actionLabel = when {
                                    searchKeyword.isNotBlank() -> "清除搜索"
                                    selectedType != null -> "查看全部"
                                    else -> "去发布"
                                },
                                onAction = {
                                    when {
                                        searchKeyword.isNotBlank() -> onSearchKeywordChange("")
                                        selectedType != null -> onTypeSelected(null)
                                        else -> onPublishClick()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    } else {
                        items(
                            items = filteredData,
                            key = { it.id },
                        ) { item ->
                            LostItemCard(
                                title = item.title,
                                description = item.description,
                                type = item.type,
                                status = item.status,
                                category = item.category,
                                location = item.location,
                                reward = item.reward,
                                onClick = { onItemClick(item.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Market tab 映射工具 ──────────────────────────────────────────────

/** [MarketListingDto] → [MarketFeedItem] 映射（HOME 场景，含收藏状态） */
private fun MarketListingDto.toHomeMarketFeedItem(
    favoriteIds: Set<String>,
): MarketFeedItem = MarketFeedItem(
    id = id,
    images = MarketUiMapper.parseImages(images),
    title = title,
    price = "¥$price",
    condition = MarketUiMapper.conditionDisplay(condition),
    category = category,
    time = HomeViewModel.formatTime(createdAt),
    status = MarketUiMapper.statusDisplay(status),
    // TODO Phase 7: favoriteCount 需要后端添加 favorite_count 字段 + trigger，当前暂为 0
    favoriteCount = 0,
    isFavorite = id in favoriteIds,
)

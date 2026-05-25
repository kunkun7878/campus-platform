package com.campus.platform.ui.screen.profile

import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.campus.platform.data.local.mapper.FavoriteEntry
import com.campus.platform.navigation.CampusRoutes
import com.campus.platform.navigation.GoodsDetail
import com.campus.platform.ui.component.ChipFilterBar
import com.campus.platform.ui.component.MarketCardVariant
import com.campus.platform.ui.component.MarketFeedCard
import com.campus.platform.ui.component.MarketFeedItem
import com.campus.platform.ui.component.MarketUiMapper
import com.campus.platform.ui.component.runner.RunnerEmptyState
import com.campus.platform.ui.viewmodel.UiState
import com.campus.platform.ui.viewmodel.profile.MyFavoritesViewModel
import kotlinx.coroutines.delay

/**
 * 「我的收藏」跨类型收藏列表。
 *
 * 顶部为 FilterChip 筛选栏（全部 | 跑腿 | 二手 | 失物 | 帖子），
 * 下方展示统一 [FavoriteEntry] 列表。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyFavoritesScreen(
    viewModel: MyFavoritesViewModel = hiltViewModel(),
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activeFilterIndex by viewModel.activeFilterIndex.collectAsStateWithLifecycle()

    var isRefreshing by remember { mutableStateOf(false) }
    var cachedEntries by remember { mutableStateOf<List<FavoriteEntry>?>(null) }

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

    val removingIds = remember { mutableStateListOf<String>() }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("我的收藏") },
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
            ChipFilterBar(
                items = MyFavoritesViewModel.FILTER_LABELS,
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
                                title = "收藏夹是空的",
                                subtitle = "浏览时点击收藏，好物不再错过",
                                actionLabel = "去逛逛",
                                onAction = { navController.navigate(CampusRoutes.Home.route) },
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
                                    val entryId = entry.id
                                    val isExiting = entryId in removingIds

                                    if (isExiting) {
                                        LaunchedEffect(entryId) {
                                            delay(400L)
                                            viewModel.removeFavorite(entryId)
                                            removingIds.remove(entryId)
                                        }
                                    }

                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = !isExiting,
                                        exit = fadeOut() + shrinkVertically(),
                                    ) {
                                        val item = entry.toMarketFeedItem()
                                        MarketFeedCard(
                                            item = item,
                                            variant = MarketCardVariant.MY_FAVORITES,
                                            onClick = {
                                                when (entry) {
                                                    is FavoriteEntry.Market -> {
                                                        navController.navigate(GoodsDetail(entryId))
                                                    }
                                                    is FavoriteEntry.Runner -> {
                                                        navController.navigate(
                                                            CampusRoutes.OrderDetail.createRoute(entryId)
                                                        )
                                                    }
                                                    is FavoriteEntry.LostFound -> {
                                                        navController.navigate(
                                                            CampusRoutes.LostDetailRoute.createRoute(entryId)
                                                        )
                                                    }
                                                }
                                            },
                                            onUnfavoriteClick = {
                                                if (entryId !in removingIds) {
                                                    removingIds.add(entryId)
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
}

// ── FavoriteEntry → MarketFeedItem ──────────────────────────────────────

private fun FavoriteEntry.toMarketFeedItem(): MarketFeedItem = MarketFeedItem(
    id = id,
    images = MarketUiMapper.parseImages(images),
    title = title,
    price = when (this) {
        is FavoriteEntry.Market -> "¥${listing.price}"
        is FavoriteEntry.Runner -> "¥${task.price + task.tip}"
        is FavoriteEntry.LostFound -> if (item.reward > 0) "¥${item.reward}" else "--"
    },
    condition = when (this) {
        is FavoriteEntry.Market -> MarketUiMapper.conditionDisplay(listing.condition)
        is FavoriteEntry.Runner -> "跑腿"
        is FavoriteEntry.LostFound -> when (item.type) { "lost" -> "失物" else -> "招领" }
    },
    category = when (this) {
        is FavoriteEntry.Market -> listing.category
        is FavoriteEntry.Runner -> task.type
        is FavoriteEntry.LostFound -> item.category
    },
    time = MarketUiMapper.formatTime(createdAt),
    status = "",
    favoriteCount = when (this) {
        is FavoriteEntry.Market -> listing.favoriteCount
        else -> 0
    },
    isFavorite = true,
)

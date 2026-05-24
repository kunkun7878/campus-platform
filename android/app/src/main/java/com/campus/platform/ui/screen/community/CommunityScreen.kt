package com.campus.platform.ui.screen.community

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.campus.platform.data.local.mapper.OfficialGroupDto
import com.campus.platform.navigation.CampusRoutes
import com.campus.platform.ui.component.community.ChannelTabBar
import com.campus.platform.ui.component.community.GroupEntryCard
import com.campus.platform.ui.component.community.PostCard
import com.campus.platform.ui.viewmodel.UiState
import com.campus.platform.ui.viewmodel.community.CommunityViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    viewModel: CommunityViewModel = hiltViewModel(),
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    val selectedChannelIndex by viewModel.selectedChannelIndex.collectAsState()
    val posts by viewModel.posts.collectAsState()
    val officialGroups by viewModel.officialGroups.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val navToPostId by viewModel.navToPostId.collectAsState()
    val navToGroupId by viewModel.navToGroupId.collectAsState()

    // 导航事件
    LaunchedEffect(navToPostId) {
        navToPostId?.let { id ->
            navController.navigate(CampusRoutes.PostDetail.createRoute(id))
            viewModel.onNavPostConsumed()
        }
    }
    LaunchedEffect(navToGroupId) {
        navToGroupId?.let { id ->
            navController.navigate(CampusRoutes.GroupChat.createRoute(id))
            viewModel.onNavGroupConsumed()
        }
    }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(CampusRoutes.PostCreate.route) },
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(
                    imageVector = Icons.Filled.Create,
                    contentDescription = "发布帖子",
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // 频道 Tab 栏
            ChannelTabBar(
                selectedIndex = selectedChannelIndex,
                onChannelSelected = { viewModel.selectChannel(it) },
            )

            // 频道列表 HorizontalPager
            val pagerState = rememberPagerState(
                initialPage = selectedChannelIndex,
                pageCount = { COMMUNITY_PAGE_COUNT },
            )
            val coroutineScope = rememberCoroutineScope()

            LaunchedEffect(selectedChannelIndex) {
                pagerState.animateScrollToPage(selectedChannelIndex)
            }
            LaunchedEffect(pagerState.currentPage) {
                if (pagerState.currentPage != selectedChannelIndex) {
                    viewModel.selectChannel(pagerState.currentPage)
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.5f),
            ) { page ->
                // 每个 channel 的内容：帖流 + 下拉刷新
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    when (val state = posts) {
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
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Filled.ErrorOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                                        modifier = Modifier.size(48.dp),
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "加载失败",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "请下拉刷新重试",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                        is UiState.Success -> {
                            val postList = state.data
                            if (postList.isEmpty()) {
                                // 频道无帖 空态
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(horizontal = 32.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Forum,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                            modifier = Modifier.size(56.dp),
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "频道暂无帖子",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "点击右下角按钮发布第一条帖子",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(vertical = 8.dp),
                                ) {
                                    items(postList, key = { it.id }) { post ->
                                        PostCard(
                                            post = post,
                                            isLiked = false, // 列表页不跟踪单条点赞
                                            onLike = {},
                                            onPostClick = { viewModel.onPostClick(post.id) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── 底部官方群 Section ──
            OfficialGroupsSection(
                groups = officialGroups,
                onGroupClick = { viewModel.onGroupClick(it.id) },
            )
        }

        // Error snackbar-like display
        if (errorMessage != null) {
            LaunchedEffect(errorMessage) {
                // auto-dismiss after 3 seconds
                kotlinx.coroutines.delay(3_000)
                viewModel.clearError()
            }
        }
    }
}

@Composable
private fun OfficialGroupsSection(
    groups: UiState<List<OfficialGroupDto>>,
    onGroupClick: (OfficialGroupDto) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp),
    ) {
        Text(
            text = "官方社群",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        when (groups) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            is UiState.Error -> { /* silently ignore */ }
            is UiState.Success -> {
                if (groups.data.isEmpty()) {
                    // 学校无群 空态
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 32.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Group,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(40.dp),
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "学校暂无官方群",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "敬请期待更多社群",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 4.dp),
                    ) {
                        items(groups.data, key = { it.id }) { group ->
                            GroupEntryCard(
                                group = group,
                                onClick = { onGroupClick(group) },
                            )
                        }
                    }
                }
            }
        }
    }
}

private const val COMMUNITY_PAGE_COUNT = 5

package com.campus.platform.ui.screen.message

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.campus.platform.data.local.mapper.ConversationDto
import com.campus.platform.data.local.mapper.NotificationDto
import com.campus.platform.data.model.Profile
import com.campus.platform.navigation.CampusRoutes
import com.campus.platform.ui.theme.Error
import com.campus.platform.ui.theme.OnSurfaceVariant
import com.campus.platform.ui.theme.Primary
import com.campus.platform.ui.viewmodel.UiState
import com.campus.platform.ui.viewmodel.message.ConversationItem
import com.campus.platform.ui.viewmodel.message.MessageViewModel

/** 消息 Tab 根页面 — 双 Tab（私信 + 通知） */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageScreen(
    viewModel: MessageViewModel = hiltViewModel(),
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val conversationsState by viewModel.conversationsState.collectAsState()
    val notificationsState by viewModel.notificationsState.collectAsState()
    val unreadNotificationCount by viewModel.unreadNotificationCount.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val navOnNotification by viewModel.navOnNotification.collectAsState()

    // 通知点击 → 根据 refType 跳转对应路由
    LaunchedEffect(navOnNotification) {
        navOnNotification?.let { route ->
            navController.navigate(route)
            viewModel.onNavNotificationConsumed()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("消息") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // ── Tab 栏 ─────────────────────────────────────
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = Primary,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    text = { Text("私信") },
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = "私信",
                            modifier = Modifier.size(20.dp),
                        )
                    },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("通知")
                            if (unreadNotificationCount > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Badge { Text("$unreadNotificationCount") }
                            }
                        }
                    },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (unreadNotificationCount > 0) {
                                    Badge { Text("$unreadNotificationCount") }
                                }
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = "通知",
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    },
                )
            }

            // ── 内容区 ─────────────────────────────────────
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize(),
            ) {
                when (selectedTab) {
                    0 -> ConversationsTab(
                        state = conversationsState,
                        onConversationClick = { convId ->
                            navController.navigate(CampusRoutes.ChatDetail.createRoute(convId))
                        },
                    )
                    1 -> NotificationsTab(
                        state = notificationsState,
                        onMarkAllRead = { viewModel.markAllNotificationsAsRead() },
                        onMarkAsRead = { viewModel.markNotificationAsRead(it) },
                        onDelete = { viewModel.deleteNotification(it) },
                        onNotificationClick = { viewModel.onNotificationClick(it) },
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// 私信 Tab
// ═══════════════════════════════════════════════════════════

@Composable
private fun ConversationsTab(
    state: UiState<List<ConversationItem>>,
    onConversationClick: (String) -> Unit,
) {
    when (state) {
        is UiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = Primary)
            }
        }
        is UiState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = null,
                        tint = OnSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "加载失败",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Error,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "请下拉刷新重试",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant,
                    )
                }
            }
        }
        is UiState.Success -> {
            val items = state.data
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = null,
                            tint = OnSurfaceVariant,
                            modifier = Modifier.size(48.dp),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "暂无私信",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "与同学聊天后将在这里显示",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant,
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 4.dp),
                ) {
                    items(items, key = { it.conversation.id }) { item ->
                        ConversationCard(
                            item = item,
                            onClick = { onConversationClick(item.conversation.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationCard(
    item: ConversationItem,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 头像
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                val name = item.otherUser?.nickname ?: (
                    if (item.conversation.user1Id == item.conversation.user2Id) "我" else "TA"
                )
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(24.dp),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 中间文本
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.otherUser?.nickname
                        ?: item.otherUser?.phone
                        ?: "用户${item.otherUser?.id?.take(6) ?: ""}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.conversation.lastMessage ?: "暂无消息",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // 右侧：时间 + 未读角标
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = item.conversation.lastMessageAt?.take(16)?.replace("T", " ") ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant,
                    maxLines = 1,
                )
                if (item.unreadCount > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Badge { Text("${item.unreadCount}") }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// 通知 Tab
// ═══════════════════════════════════════════════════════════

@Composable
private fun NotificationsTab(
    state: UiState<List<NotificationDto>>,
    onMarkAllRead: () -> Unit,
    onMarkAsRead: (String) -> Unit,
    onDelete: (String) -> Unit,
    onNotificationClick: (NotificationDto) -> Unit,
) {
    when (state) {
        is UiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = Primary)
            }
        }
        is UiState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = null,
                        tint = OnSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "加载失败",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Error,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "请下拉刷新重试",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant,
                    )
                }
            }
        }
        is UiState.Success -> {
            val items = state.data
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = null,
                            tint = OnSurfaceVariant,
                            modifier = Modifier.size(48.dp),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "暂无通知",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariant,
                        )
                    }
                }
            } else {
                // 全部已读按钮
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onMarkAllRead) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("全部已读")
                    }
                }

                LazyColumn(
                    contentPadding = PaddingValues(vertical = 4.dp),
                ) {
                    items(items, key = { it.id }) { notification ->
                        NotificationCard(
                            notification = notification,
                            onMarkAsRead = { onMarkAsRead(notification.id) },
                            onDelete = { onDelete(notification.id) },
                            onClick = { onNotificationClick(notification) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: NotificationDto,
    onMarkAsRead: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
) {
    val bgColor = if (notification.isRead) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            // 未读小圆点
            if (!notification.isRead) {
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Primary),
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else {
                Spacer(modifier = Modifier.width(16.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    // 时间
                    val time = notification.createdAt?.take(16)?.replace("T", " ") ?: ""
                    if (time.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = time,
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceVariant,
                        )
                    }
                }
                if (!notification.body.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = notification.body,
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // 操作按钮
            Column {
                if (!notification.isRead) {
                    IconButton(
                        onClick = onMarkAsRead,
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "标为已读",
                            tint = Primary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "删除",
                        tint = OnSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

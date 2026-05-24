package com.campus.platform.ui.screen.message

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.campus.platform.data.local.entity.MessageEntity
import com.campus.platform.data.local.mapper.MessageDto
import com.campus.platform.ui.component.message.ChatInputBar
import com.campus.platform.ui.component.message.ImageMessageBubble
import com.campus.platform.ui.component.message.MessageBubble
import com.campus.platform.ui.component.message.isImageMessage
import com.campus.platform.ui.theme.Error
import com.campus.platform.ui.theme.OnSurfaceVariant
import com.campus.platform.ui.theme.Primary
import com.campus.platform.ui.viewmodel.UiState
import com.campus.platform.ui.viewmodel.message.ChatDetailViewModel

/** 聊天详情页 — 消息气泡 + 输入栏 + 乐观UI + 已读 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    viewModel: ChatDetailViewModel = hiltViewModel(),
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val isUploadingImage by viewModel.isUploadingImage.collectAsState()

    val data = (uiState as? UiState.Success)?.data
    val currentUserId = data?.currentUserId ?: ""

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = data?.otherUser?.nickname
                            ?: data?.otherUser?.phone
                            ?: data?.conversation?.user2Id?.take(8)
                            ?: "聊天",
                        maxLines = 1,
                    )
                },
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
                ),
            )
        },
        bottomBar = {
            ChatInputBar(
                text = inputText,
                onTextChange = { viewModel.onInputChange(it) },
                onSend = { viewModel.sendMessage() },
                isSending = isSending,
                onPickImage = { viewModel.sendImage(it) },
                isUploadingImage = isUploadingImage,
            )
        },
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        when (val state = uiState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = Primary)
                }
            }

            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
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
                            text = "请返回后重试",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant,
                        )
                    }
                }
            }

            is UiState.Success -> {
                val isCounterpartyDeleted = state.data.otherUser?.deletedAt != null
                Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    // 对方已注销 提示横幅
                    if (isCounterpartyDeleted) {
                        DeletedUserBanner()
                    }

                    ChatMessagesList(
                        messages = state.data.messages,
                        currentUserId = state.data.currentUserId,
                        onResend = { msgId -> viewModel.resendMessage(msgId) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// 消息列表
// ═══════════════════════════════════════════════════════════

@Composable
private fun ChatMessagesList(
    messages: List<MessageDto>,
    currentUserId: String,
    onResend: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    if (messages.isEmpty()) {
        Box(
            modifier = modifier,
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
                    text = "暂无消息",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "发送第一条消息吧",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant,
                )
            }
        }
        return
    }

    // 自动滚动到底部（新消息到达或初始加载时）
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        itemsIndexed(messages, key = { _, msg -> msg.id }) { index, message ->
            val isMine = message.senderId == currentUserId
            val time = message.createdAt?.let { formatMessageTime(it) }
            val prevSender = messages.getOrNull(index - 1)?.senderId
            val showSenderName = !isMine && (index == 0 || prevSender != message.senderId)

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
            ) {
                if (isImageMessage(message.content)) {
                    ImageMessageBubble(
                        imageUrl = message.content,
                        isMine = isMine,
                        time = time,
                        senderName = if (showSenderName) "用户${message.senderId.take(6)}" else null,
                    )
                } else {
                    MessageBubble(
                        content = message.content,
                        isMine = isMine,
                        status = message.localStatus,
                        time = time,
                        senderName = if (showSenderName) "用户${message.senderId.take(6)}" else null,
                        onResend = if (isMine && message.localStatus == MessageEntity.LOCAL_STATUS_FAILED) {
                            { onResend(message.id) }
                        } else null,
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// Helpers
// ═══════════════════════════════════════════════════════════

// ═══════════════════════════════════════════════════════════
// 对方已注销 提示横幅
// ═══════════════════════════════════════════════════════════

@Composable
private fun DeletedUserBanner(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.errorContainer,
                RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.PersonOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = "对方已注销账号",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                text = "无法再发送新消息",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
            )
        }
    }
}

/**
 * 将 ISO 8601 时间戳格式化为短时间展示。
 * 今天显示 HH:mm，更早显示 MM-dd HH:mm。
 */
private fun formatMessageTime(isoTimestamp: String): String {
    return try {
        val cleaned = isoTimestamp
            .replace("T", " ")
            .substringBefore("+")
            .substringBefore("Z")
        if (cleaned.length >= 16) {
            cleaned.substring(5, 16).replace("-", "/").replace(" ", " ")
        } else {
            cleaned
        }
    } catch (_: Exception) {
        isoTimestamp.take(16)
    }
}

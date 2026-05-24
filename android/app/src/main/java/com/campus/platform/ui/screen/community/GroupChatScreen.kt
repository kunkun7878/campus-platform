package com.campus.platform.ui.screen.community

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.campus.platform.data.local.mapper.GroupMemberDto
import com.campus.platform.ui.component.message.ChatInputBar
import com.campus.platform.ui.component.message.ImageMessageBubble
import com.campus.platform.ui.component.message.isImageMessage
import com.campus.platform.ui.theme.OnSurfaceVariant
import com.campus.platform.ui.theme.Primary
import com.campus.platform.ui.viewmodel.community.GroupChatViewModel
import com.campus.platform.ui.viewmodel.community.GroupMessageUi
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs

// ── Time helpers ───────────────────────────────────────────

private fun formatMessageTime(iso: String?): String {
    if (iso == null) return ""
    return try {
        val instant = Instant.parse(iso)
        val local = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
        val now = LocalDateTime.now()
        val fmt = if (local.toLocalDate() == now.toLocalDate()) {
            DateTimeFormatter.ofPattern("HH:mm")
        } else {
            DateTimeFormatter.ofPattern("MM-dd HH:mm")
        }
        local.format(fmt)
    } catch (_: Exception) {
        ""
    }
}

private fun minutesBetween(a: String?, b: String?): Long {
    if (a == null || b == null) return Long.MAX_VALUE
    return try {
        val t1 = Instant.parse(a)
        val t2 = Instant.parse(b)
        ChronoUnit.MINUTES.between(t1, t2).let { if (it < 0) -it else it }
    } catch (_: Exception) {
        Long.MAX_VALUE
    }
}

// ── Screen ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupChatScreen(
    viewModel: GroupChatViewModel = hiltViewModel(),
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    val group by viewModel.group.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val isMember by viewModel.isMember.collectAsState()
    val members by viewModel.members.collectAsState()
    val onlineCount by viewModel.onlineCount.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val isUploadingImage by viewModel.isUploadingImage.collectAsState()
    val isJoining by viewModel.isJoining.collectAsState()
    val isLeaving by viewModel.isLeaving.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    val context = LocalContext.current
    val listState = rememberLazyListState()

    LaunchedEffect(toastMessage) {
        toastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    // Auto-scroll to bottom when new message arrives
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = group?.name ?: "群聊",
                            maxLines = 1,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        if (isMember) {
                            Text(
                                text = "$onlineCount 在线",
                                style = MaterialTheme.typography.labelSmall,
                                color = OnSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
                actions = {
                    if (isMember) {
                        IconButton(
                            onClick = { viewModel.leaveGroup() },
                            enabled = !isLeaving,
                        ) {
                            if (isLeaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                    contentDescription = "退出群聊",
                                    tint = OnSurfaceVariant,
                                )
                            }
                        }
                    }
                },
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
            if (group == null) {
                // Loading state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else if (!isMember) {
                // ── Non-member view: group info + join button + members ──
                NonMemberContent(
                    groupName = group!!.name,
                    groupDescription = group!!.description,
                    memberCount = group!!.memberCount,
                    members = members,
                    isJoining = isJoining,
                    onJoin = { viewModel.joinGroup() },
                )
            } else {
                // ── Chat messages ──
                LazyColumn(
                    state = listState,
                    reverseLayout = true,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    itemsIndexed(
                        items = messages,
                        key = { _, msg -> msg.id },
                    ) { index, message ->
                        val prevMessage = messages.getOrNull(index + 1) // reversed: next = older
                        val showTime = index == messages.lastIndex ||
                            minutesBetween(message.createdAt, prevMessage?.createdAt) > 5

                        MessageBubble(
                            message = message,
                            showTime = showTime,
                        )
                    }
                }

                // ── Typing indicator for sending state ──
                AnimatedVisibility(
                    visible = isSending,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Text(
                            text = "发送中…",
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceVariant,
                        )
                    }
                }

                // ── Input bar ──
                ChatInputBar(
                    text = inputText,
                    onTextChange = { viewModel.updateInputText(it) },
                    onSend = { viewModel.sendMessage() },
                    isSending = isSending,
                    onPickImage = { viewModel.sendImage(it) },
                    isUploadingImage = isUploadingImage,
                )
            }
        }
    }
}

// ── Non-member content ─────────────────────────────────────

@Composable
private fun NonMemberContent(
    groupName: String,
    groupDescription: String?,
    memberCount: Int,
    members: List<GroupMemberDto>,
    isJoining: Boolean,
    onJoin: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        // Group info card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Group,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(40.dp),
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = groupName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )

                if (!groupDescription.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = groupDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = OnSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$memberCount 成员",
                        style = MaterialTheme.typography.labelMedium,
                        color = OnSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onJoin,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isJoining,
                ) {
                    if (isJoining) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("加入群聊")
                    }
                }
            }
        }

        // Member list
        Text(
            text = "群成员",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        ) {
            items(members, key = { it.id }) { member ->
                MemberRow(member = member)
            }
        }
    }
}

// ── Member row ─────────────────────────────────────────────

@Composable
private fun MemberRow(member: GroupMemberDto) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = OnSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = member.userId.take(8),
            style = MaterialTheme.typography.bodyMedium,
        )

        Spacer(modifier = Modifier.weight(1f))

        if (member.role == "owner" || member.role == "admin") {
            Text(
                text = if (member.role == "owner") "群主" else "管理员",
                style = MaterialTheme.typography.labelSmall,
                color = Primary,
            )
        }
    }
}

// ── Message bubble ─────────────────────────────────────────

@Composable
private fun MessageBubble(
    message: GroupMessageUi,
    showTime: Boolean,
) {
    // System messages: centered, gray
    if (message.isSystemMessage) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
        return
    }

    // Time separator (shown when time gap > 5 min between messages)
    // Placeholder – time shown inline with message

    val isMine = message.isMine

    // Image message — use shared ImageMessageBubble
    if (isImageMessage(message.content)) {
        ImageMessageBubble(
            imageUrl = message.content,
            isMine = isMine,
            time = if (showTime) formatMessageTime(message.createdAt) else null,
            senderName = if (!isMine) message.senderNickname else null,
        )
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
    ) {
        // Sender info for others
        if (!isMine) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = OnSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = message.senderNickname,
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant,
                    maxLines = 1,
                )
            }
        }

        // Bubble box
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(
                    color = if (isMine) Primary else MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isMine) 16.dp else 4.dp,
                        bottomEnd = if (isMine) 4.dp else 16.dp,
                    ),
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface,
                    lineHeight = 22.sp,
                ),
            )
        }

        // Time
        if (showTime) {
            Text(
                text = formatMessageTime(message.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceVariant,
                modifier = Modifier.padding(
                    top = 2.dp,
                    start = if (isMine) 0.dp else 4.dp,
                    end = if (isMine) 4.dp else 0.dp,
                ),
            )
        }
    }
}


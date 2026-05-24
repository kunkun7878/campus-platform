package com.campus.platform.ui.screen.community

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.campus.platform.ui.component.StatusBadge
import com.campus.platform.ui.component.community.CommentItem
import com.campus.platform.ui.component.community.formatPostTime
import com.campus.platform.ui.theme.Error
import com.campus.platform.ui.theme.OnSurfaceVariant
import com.campus.platform.ui.theme.Primary
import com.campus.platform.ui.viewmodel.UiState
import com.campus.platform.ui.viewmodel.community.PostDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    viewModel: PostDetailViewModel = hiltViewModel(),
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    val post by viewModel.post.collectAsState()
    val isLiked by viewModel.isLiked.collectAsState()
    val likeCount by viewModel.likeCount.collectAsState()
    val comments by viewModel.comments.collectAsState()
    val commentText by viewModel.commentText.collectAsState()
    val isSendingComment by viewModel.isSendingComment.collectAsState()
    val actionError by viewModel.actionError.collectAsState()
    val navBack by viewModel.navBack.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()

    val context = LocalContext.current

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editTitle by remember { mutableStateOf("") }
    var editContent by remember { mutableStateOf("") }

    // Toast errors
    LaunchedEffect(actionError) {
        actionError?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearActionError()
        }
    }

    // 删除成功回退
    LaunchedEffect(navBack) {
        if (navBack) {
            navController.popBackStack()
            viewModel.onNavBackConsumed()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("帖子详情", maxLines = 1) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回",
                            )
                        }
                    },
                    actions = {
                        val isOwnPost = post?.authorId == currentUserId && currentUserId != null
                        if (isOwnPost) {
                            IconButton(onClick = {
                                editTitle = post?.title ?: ""
                                editContent = post?.content ?: ""
                                showEditDialog = true
                            }) {
                                Icon(imageVector = Icons.Filled.Edit, contentDescription = "编辑")
                            }
                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "删除",
                                    tint = Error,
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                // ── 帖子正文 ──
                item {
                    post?.let { p ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = p.title,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        text = p.authorId.take(8),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Primary,
                                    )
                                    Text(
                                        text = formatPostTime(p.createdAt),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = OnSurfaceVariant,
                                    )
                                    if (p.status == "reviewing") {
                                        StatusBadge(
                                            status = "审核中",
                                            color = androidx.compose.ui.graphics.Color(0xFFF5A623),
                                        )
                                    }
                                    if (p.status == "blocked") {
                                        StatusBadge(status = "已拒绝", color = Error)
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = p.content,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                // 点赞区
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { viewModel.toggleLike() },
                                            modifier = Modifier.size(36.dp),
                                        ) {
                                            Icon(
                                                imageVector = if (isLiked) Icons.Filled.Favorite
                                                    else Icons.Filled.FavoriteBorder,
                                                contentDescription = if (isLiked) "取消点赞" else "点赞",
                                                tint = if (isLiked) Error else OnSurfaceVariant,
                                                modifier = Modifier.size(22.dp),
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "$likeCount 点赞",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (isLiked) Error else OnSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "评论 (${p.commentCount})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }

                // ── 评论列表 ──
                when (val state = comments) {
                    is UiState.Loading -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(color = Primary)
                            }
                        }
                    }
                    is UiState.Error -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(text = state.message, color = Error)
                            }
                        }
                    }
                    is UiState.Success -> {
                        if (state.data.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "暂无评论",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = OnSurfaceVariant,
                                    )
                                }
                            }
                        } else {
                            items(state.data, key = { it.id }) { comment ->
                                CommentItem(
                                    comment = comment,
                                    isOwnComment = comment.authorId == currentUserId,
                                    onDelete = { viewModel.deleteComment(comment.id) },
                                )
                            }
                        }
                    }
                }

                // 底部留白
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        // ── 底部评论输入栏 ──
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = commentText,
                onValueChange = { viewModel.setCommentText(it) },
                placeholder = { Text("写评论...") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
                enabled = !isSendingComment,
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = { viewModel.submitComment() },
                enabled = commentText.isNotBlank() && !isSendingComment,
            ) {
                if (isSendingComment) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "发送",
                        tint = if (commentText.isNotBlank()) Primary else OnSurfaceVariant,
                    )
                }
            }
        }
    }

    // ── 删除确认对话框 ──
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("删除后无法恢复，确定要删除这条帖子吗？") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deletePost()
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Error,
                    ),
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            },
        )
    }

    // ── 编辑对话框 ──
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("编辑帖子") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("标题") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editContent,
                        onValueChange = { editContent = it },
                        label = { Text("内容") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    showEditDialog = false
                    viewModel.updatePost(editTitle, editContent)
                }) {
                    Text("保存")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEditDialog = false }) {
                    Text("取消")
                }
            },
        )
    }
}

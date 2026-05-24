package com.campus.platform.ui.component.community

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.campus.platform.data.local.mapper.CommunityCommentDto
import com.campus.platform.ui.theme.OnSurfaceVariant

/**
 * 评论列表项。
 *
 * @param comment      评论数据
 * @param isOwnComment 是否属于当前登录用户（控制删除按钮可见性）
 * @param onDelete     删除回调
 * @param modifier     修饰符
 */
@Composable
fun CommentItem(
    comment: CommunityCommentDto,
    isOwnComment: Boolean,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 头像占位
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = (comment.authorId.firstOrNull()?.toString() ?: "?").uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = comment.authorId.take(8),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = commentTimeLabel(comment.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant,
                )
            }
            if (isOwnComment) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "删除评论",
                        tint = OnSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = comment.content,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private fun commentTimeLabel(iso: String?): String {
    if (iso == null) return ""
    return try {
        val cleaned = iso.substring(0, minOf(16, iso.length))
        cleaned.replace("T", " ")
    } catch (_: Exception) {
        iso
    }
}

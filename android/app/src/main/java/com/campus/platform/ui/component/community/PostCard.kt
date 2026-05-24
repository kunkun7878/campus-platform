package com.campus.platform.ui.component.community

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.campus.platform.data.local.mapper.CommunityPostDto
import com.campus.platform.ui.component.StatusBadge
import com.campus.platform.ui.theme.Error
import com.campus.platform.ui.theme.OnSurfaceVariant
import com.campus.platform.ui.theme.Primary

/**
 * 帖子列表卡片。
 *
 * @param post      帖子数据
 * @param isLiked   当前用户是否已点赞
 * @param onLike    点赞回调
 * @param onPostClick 点击卡片跳转详情
 * @param modifier  修饰符
 */
@Composable
fun PostCard(
    post: CommunityPostDto,
    isLiked: Boolean,
    onLike: () -> Unit,
    onPostClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onPostClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // ── 标题行 ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = post.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (post.status == "reviewing") {
                    StatusBadge(
                        status = "审核中",
                        color = PostCardColors.reviewingColor,
                    )
                }
                if (post.status == "blocked") {
                    StatusBadge(
                        status = "已拒绝",
                        color = Error,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── 正文 ──
            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── 底部操作栏 ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 左侧：点赞 + 评论
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onLike,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (isLiked) "取消点赞" else "点赞",
                            tint = if (isLiked) Error else OnSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    if (post.likeCount > 0) {
                        Text(
                            text = "${post.likeCount}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isLiked) Error else OnSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Icon(
                        imageVector = Icons.Filled.ChatBubbleOutline,
                        contentDescription = null,
                        tint = OnSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                    if (post.commentCount > 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${post.commentCount}",
                            style = MaterialTheme.typography.labelMedium,
                            color = OnSurfaceVariant,
                        )
                    }
                }

                // 右侧：发布时间
                Text(
                    text = formatPostTime(post.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant,
                )
            }
        }
    }
}

/** 格式化工整的发布时间 */
internal fun formatPostTime(iso: String?): String {
    if (iso == null) return ""
    return try {
        val cleaned = iso.substring(0, minOf(16, iso.length))
        cleaned.replace("T", " ")
    } catch (_: Exception) {
        iso
    }
}

private object PostCardColors {
    val reviewingColor = androidx.compose.ui.graphics.Color(0xFFF5A623)
}

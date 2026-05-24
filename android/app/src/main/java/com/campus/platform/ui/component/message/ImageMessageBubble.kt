package com.campus.platform.ui.component.message

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.campus.platform.ui.theme.OnSurfaceVariant

/**
 * 图片消息气泡 — 聊天中的图片消息展示。
 *
 * 使用 Coil SubcomposeAsyncImage：加载中显示 spinner，失败时显示破碎图标。
 * 气泡圆角遵循聊天气泡风格（自己的右下角小尖、别人的左下角小尖）。
 */
@Composable
fun ImageMessageBubble(
    imageUrl: String,
    isMine: Boolean,
    time: String?,
    senderName: String?,
    modifier: Modifier = Modifier,
) {
    val bubbleShape = if (isMine) {
        RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp)
    } else {
        RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)
    }

    Column(
        modifier = modifier
            .widthIn(max = 260.dp)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
    ) {
        // Sender name (for others only)
        if (!isMine && !senderName.isNullOrBlank()) {
            Text(
                text = senderName,
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, bottom = 2.dp),
            )
        }

        // Image with loading / error states
        SubcomposeAsyncImage(
            model = imageUrl,
            contentDescription = "图片消息",
            modifier = Modifier
                .widthIn(max = 240.dp)
                .height(180.dp) // fixed height → fills available width up to 240dp
                .clip(bubbleShape),
            contentScale = ContentScale.Crop,
            loading = {
                Box(
                    modifier = Modifier
                        .widthIn(max = 240.dp)
                        .height(180.dp)
                        .background(if (isMine) OnSurfaceVariant.copy(alpha = 0.15f) else OnSurfaceVariant.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 2.5.dp,
                    )
                }
            },
            error = {
                Box(
                    modifier = Modifier
                        .widthIn(max = 240.dp)
                        .height(180.dp)
                        .background(if (isMine) OnSurfaceVariant.copy(alpha = 0.15f) else OnSurfaceVariant.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.BrokenImage,
                            contentDescription = "图片加载失败",
                            tint = OnSurfaceVariant,
                            modifier = Modifier.size(32.dp),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "加载失败",
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceVariant,
                        )
                    }
                }
            },
        )

        // Time
        if (!time.isNullOrBlank()) {
            Text(
                text = time,
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceVariant,
                modifier = Modifier.padding(
                    start = if (isMine) 0.dp else 12.dp,
                    end = if (isMine) 8.dp else 0.dp,
                    top = 2.dp,
                ),
            )
        }
    }
}

/**
 * 判断消息内容是否为图片 URL。
 *
 * 规则: 内容以 http 开头且包含 Supabase Storage 路径特征。
 * 用于 ChatDetailScreen / GroupChatScreen 中路由到文本气泡或图片气泡。
 */
fun isImageMessage(content: String): Boolean {
    return content.startsWith("http") &&
        (content.contains("/storage/v1/") || content.contains("supabase"))
}

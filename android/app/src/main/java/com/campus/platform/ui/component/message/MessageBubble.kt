package com.campus.platform.ui.component.message

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.campus.platform.data.local.entity.MessageEntity
import com.campus.platform.ui.theme.Error
import com.campus.platform.ui.theme.OnSurfaceVariant
import com.campus.platform.ui.theme.Primary
import com.campus.platform.ui.theme.SurfaceVariant

/**
 * 消息气泡 — 支持发送/接收两端 + 消息状态指示器。
 *
 * - [isMine] = true：右对齐、品牌色背景
 * - [isMine] = false：左对齐、灰色背景
 * - [status] 显示发送状态图标（SENDING/SENT/FAILED），仅对 isMine=true 生效
 */
@Composable
fun MessageBubble(
    content: String,
    isMine: Boolean,
    status: String,
    time: String?,
    senderName: String?,
    onResend: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val bubbleColor = if (isMine) Primary else SurfaceVariant
    val textColor = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val shape = if (isMine) {
        RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp)
    } else {
        RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)
    }

    Column(
        modifier = modifier
            .widthIn(max = 300.dp)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
    ) {
        // 发送者名称（仅对方消息显示）
        if (!isMine && !senderName.isNullOrBlank()) {
            Text(
                text = senderName,
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceVariant,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 8.dp, bottom = 2.dp),
            )
        }

        Row(
            horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Bottom,
        ) {
            // 失败消息的重发提示（仅自己的消息）
            if (isMine && status == MessageEntity.LOCAL_STATUS_FAILED && onResend != null) {
                StatusIcon(
                    icon = Icons.Filled.ErrorOutline,
                    contentDescription = "发送失败",
                    tint = Error,
                    onClick = onResend,
                )
                Spacer(modifier = Modifier.width(4.dp))
            }

            // 气泡
            Box(
                modifier = Modifier
                    .clip(shape)
                    .background(bubbleColor)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                )
            }

            // 状态图标（自己的消息在气泡右边，对方的不显示）
            if (isMine) {
                Spacer(modifier = Modifier.width(4.dp))
                when (status) {
                    MessageEntity.LOCAL_STATUS_SENDING -> {
                        StatusIcon(
                            icon = Icons.Filled.Schedule,
                            contentDescription = "发送中",
                            tint = OnSurfaceVariant,
                        )
                    }
                    MessageEntity.LOCAL_STATUS_SENT -> {
                        StatusIcon(
                            icon = Icons.Filled.Check,
                            contentDescription = "已发送",
                            tint = Primary,
                        )
                    }
                    // FAILED 已在气泡左侧显示重试按钮
                }
            }
        }

        // 时间
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

@Composable
private fun StatusIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: androidx.compose.ui.graphics.Color,
    onClick: (() -> Unit)? = null,
) {
    val iconModifier = Modifier.size(16.dp)
    if (onClick != null) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = iconModifier.clickable { onClick() },
        )
    } else {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = iconModifier,
        )
    }
}

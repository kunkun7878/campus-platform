package com.campus.platform.ui.component.runner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

// 类型标签色
private val OrderTypeColorPickup = Color(0xFF2D6BFF)
private val OrderTypeColorDelivery = Color(0xFF12B7AE)
private val OrderTypeColorPurchase = Color(0xFFFF9A62)
private val OrderTypeColorUniversal = Color(0xFF8B5CF6)

// 状态色
private val OrderStatusColorPublished = Color(0xFF2D6BFF)
private val OrderStatusColorAssigned = Color(0xFF12B7AE)
private val OrderStatusColorInProgress = Color(0xFFFF9A62)
private val OrderStatusColorCompleted = Color(0xFF7C89A6)
private val OrderStatusColorCancelled = Color(0xFFF45F89)

/**
 * 订单列表卡片，用于 OrderListScreen。
 *
 * 紧凑型白色卡片，比 RunnerTaskCard 更小。
 * 左侧：任务标题 + 类型标签 + 金额 + 时间。
 * 右侧：当前状态 badge。
 *
 * @param title       任务标题。
 * @param taskType    任务类型：pickup / delivery / purchase / universal。
 * @param typeLabel   类型展示标签，如 "帮取"。
 * @param status      订单状态：published / assigned / in_progress / completed / cancelled。
 * @param statusLabel 状态展示文本，如 "待接单"。
 * @param amount      金额字符串，如 "¥15"。
 * @param time        时间文本。
 * @param onClick     点击回调。
 */
@Composable
fun OrderCard(
    title: String,
    taskType: String,
    typeLabel: String,
    status: String,
    statusLabel: String,
    amount: String,
    time: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val typeColor = when (taskType) {
        "pickup" -> OrderTypeColorPickup
        "delivery" -> OrderTypeColorDelivery
        "purchase" -> OrderTypeColorPurchase
        "universal" -> OrderTypeColorUniversal
        else -> OrderTypeColorPickup
    }

    val statusColor = when (status) {
        "published" -> OrderStatusColorPublished
        "assigned" -> OrderStatusColorAssigned
        "in_progress" -> OrderStatusColorInProgress
        "accepted" -> OrderStatusColorInProgress
        "delivering" -> OrderStatusColorInProgress
        "delivered" -> OrderStatusColorCompleted
        "completed" -> OrderStatusColorCompleted
        "cancelled" -> OrderStatusColorCancelled
        "after_sale" -> OrderStatusColorCancelled
        else -> OrderStatusColorPublished
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 左侧内容
            Column(modifier = Modifier.weight(1f)) {
                // 标题
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(6.dp))

                // 类型标签 + 金额
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(typeColor.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = typeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = typeColor,
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = amount,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 时间
                Text(
                    text = time,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 右侧：状态 badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(statusColor.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

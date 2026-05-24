package com.campus.platform.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

/**
 * 二手订单紧凑卡片（my-sold / my-bought 共用）。
 *
 * 横向布局参照 [OrderCard]：左侧展示商品标题、对方昵称、价格、时间；
 * 右侧展示订单状态 pill。
 *
 * @param title        商品标题，单行省略展示。
 * @param counterparty 对方昵称，如 "买家：李同学" 或 "卖家：小明"。
 * @param price        价格字符串，如 "¥88"。主色加粗展示，参照 [RunnerPriceTag]。
 * @param status       订单状态 key：pending / accepted / completed / cancelled。
 * @param statusLabel  状态展示文本，如 "待确认"、"已接受"、"已完成"、"已取消"。
 * @param time         时间文本。
 * @param onClick      点击回调（必传）。
 * @param modifier     可选修饰符。
 */
@Composable
fun MarketOrderCard(
    title: String,
    counterparty: String,
    price: String,
    status: String,
    statusLabel: String,
    time: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusColor = marketOrderStatusColor(status)

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
            // 左侧：商品信息
            Column(modifier = Modifier.weight(1f)) {
                // 商品标题（单行省略）
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(6.dp))

                // 对方昵称 + 价格
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = counterparty,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = price,
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

            // 右侧：订单状态 pill
            StatusBadge(status = statusLabel, color = statusColor)
        }
    }
}

// ── 订单状态颜色（public，供 MarketOrderDetailScreen 复用）──

val MarketOrderStatusColorPending = Color(0xFFFF9A62)
val MarketOrderStatusColorAccepted = Color(0xFF2D6BFF)
val MarketOrderStatusColorCompleted = Color(0xFF12B7AE)
val MarketOrderStatusColorCancelled = Color(0xFF7C89A6)

fun marketOrderStatusColor(status: String): Color = when (status) {
    "pending" -> MarketOrderStatusColorPending
    "accepted" -> MarketOrderStatusColorAccepted
    "completed" -> MarketOrderStatusColorCompleted
    "cancelled" -> MarketOrderStatusColorCancelled
    else -> MarketOrderStatusColorPending
}
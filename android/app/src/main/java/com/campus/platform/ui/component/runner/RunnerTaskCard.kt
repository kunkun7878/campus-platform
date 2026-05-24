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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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

// 任务类型装饰色
private val TaskColorPickup = Color(0xFF2D6BFF)      // brand
private val TaskColorDelivery = Color(0xFF12B7AE)    // accent
private val TaskColorPurchase = Color(0xFFFF9A62)    // warm
private val TaskColorUniversal = Color(0xFF8B5CF6)   // purple

// 状态 badge 色
private val StatusColorPublished = Color(0xFF2D6BFF)
private val StatusColorAssigned = Color(0xFF12B7AE)
private val StatusColorInProgress = Color(0xFFFF9A62)
private val StatusColorCompleted = Color(0xFF7C89A6)
private val StatusColorCancelled = Color(0xFFF45F89)

/**
 * 跑腿任务卡片，用于首页跑腿列表。
 *
 * 白色卡片，圆角 16dp，浅阴影。顶部彩色装饰条根据任务类型显示不同颜色。
 * 主体包含标题、赏金（含小费）、取件地址 + 送达地址、发布时间、状态 badge。
 *
 * @param title          任务标题。
 * @param reward         赏金字符串，如 "¥15"。
 * @param tip            小费字符串，如 "+¥5"。为 null 或空白时不显示。
 * @param pickupAddress  取件地址。
 * @param deliveryAddress 送达地址。
 * @param publishTime    发布时间文本。
 * @param taskType       任务类型：pickup / delivery / purchase / universal。
 * @param status         任务状态：published / assigned / in_progress / completed / cancelled。
 * @param statusLabel    状态展示文本，如 "待接单"、"配送中"。
 * @param onClick        点击回调。
 */
@Composable
fun RunnerTaskCard(
    title: String,
    reward: String,
    tip: String?,
    pickupAddress: String,
    deliveryAddress: String,
    publishTime: String,
    taskType: String,
    status: String,
    statusLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accentColor = when (taskType) {
        "pickup" -> TaskColorPickup
        "delivery" -> TaskColorDelivery
        "purchase" -> TaskColorPurchase
        "universal" -> TaskColorUniversal
        else -> TaskColorPickup
    }

    val statusColor = when (status) {
        "published" -> StatusColorPublished
        "assigned" -> StatusColorAssigned
        "in_progress" -> StatusColorInProgress
        "completed" -> StatusColorCompleted
        "cancelled" -> StatusColorCancelled
        else -> StatusColorPublished
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column {
            // 顶部彩色装饰条
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(accentColor),
            )

            Column(modifier = Modifier.padding(14.dp)) {
                // 标题
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 赏金行（复用 RunnerPriceTag 的样式）
                RunnerPriceTag(amount = reward, tip = tip)

                Spacer(modifier = Modifier.height(8.dp))

                // 地址行：取件 → 送达
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = pickupAddress,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.height(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = deliveryAddress,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 底部行：发布时间 + 状态 badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = publishTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(statusColor.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                        )
                    }
                }
            }
        }
    }
}

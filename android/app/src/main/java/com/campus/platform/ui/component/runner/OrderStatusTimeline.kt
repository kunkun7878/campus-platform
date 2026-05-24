package com.campus.platform.ui.component.runner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// 时间线节点颜色
private val TimelineDotCompleted = Color(0xFF12B7AE)    // accent / 绿
private val TimelineDotCurrent = Color(0xFF2D6BFF)      // brand / 蓝
private val TimelineDotPending = Color(0xFFC4C6D0)      // 灰

// 连接线颜色
private val TimelineLineCompleted = Color(0xFF2D6BFF)   // brand
private val TimelineLinePending = Color(0xFFE0E4F2)     // muted 底色

/**
 * 时间线事件数据模型。
 *
 * @param event       事件标识（英文 key），如 "published", "runner_accepted", "picked_up" 等。
 * @param description 事件描述文本。
 * @param timestamp   时间戳展示文本。
 * @param isCompleted 是否已完成。
 */
data class TimelineEvent(
    val event: String,
    val description: String?,
    val timestamp: String,
    val isCompleted: Boolean,
)

/**
 * 订单/售后状态时间线组件，用于 OrderDetailScreen 和 AfterSaleDetailScreen。
 *
 * 垂直时间线：每节点由一个圆点 + 竖线 + 事件内容组成。
 * 圆点颜色：已完成 = 绿 / 当前进行中 = 蓝 / 待处理 = 灰。
 * 连接线颜色：已完成段 = brand 色 / 待处理段 = muted 色。
 *
 * 支持两种 event 文本映射：
 * - order：订单时间线（event → 中文映射）。
 * - after_sale：售后时间线（event → 中文映射）。
 *
 * @param events         时间线事件列表，按发生顺序从前到后排列。
 * @param eventLabelMap  事件 key → 展示文本的映射表。
 * @param modifier       修饰符。
 */
@Composable
fun OrderStatusTimeline(
    events: List<TimelineEvent>,
    eventLabelMap: Map<String, String>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        events.forEachIndexed { index, event ->
            val isLastItem = index == events.lastIndex
            val dotColor = when {
                event.isCompleted -> TimelineDotCompleted
                index == events.indexOfFirst { !it.isCompleted } -> TimelineDotCurrent
                else -> TimelineDotPending
            }
            val lineColor = when {
                event.isCompleted -> TimelineLineCompleted
                else -> TimelineLinePending
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
            ) {
                // 圆点 + 竖线列
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(32.dp),
                ) {
                    // 圆点
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(dotColor),
                    )
                    // 连接线（非最后一项时展示）
                    if (!isLastItem) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .weight(1f)
                                .background(lineColor),
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // 事件内容
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = if (isLastItem) 0.dp else 12.dp),
                ) {
                    Text(
                        text = eventLabelMap[event.event] ?: event.event,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (!event.isCompleted) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (!event.isCompleted)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!event.description.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = event.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = event.timestamp,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
            }
        }
    }
}

// ── 预置 event 文本映射 ──────────────────────────────────────────

/** 订单时间线 event → 中文 映射 */
val ORDER_TIMELINE_LABELS: Map<String, String> = mapOf(
    "published" to "已发布",
    "runner_accepted" to "跑腿员已接单",
    "picked_up" to "已取件",
    "in_delivery" to "配送中",
    "delivered" to "已送达",
    "buyer_confirmed" to "买家已收货",
    "cancelled" to "已取消",
    "auto_cancelled" to "超时自动取消",
    "buyer_reviewed" to "买家已评价",
)

/** 售后时间线 event → 中文 映射 */
val AFTER_SALE_TIMELINE_LABELS: Map<String, String> = mapOf(
    "created" to "提交申请",
    "processing" to "处理中",
    "approved" to "已通过",
    "rejected" to "已驳回",
    "completed" to "已完成",
)

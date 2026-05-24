package com.campus.platform.ui.component

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MonetizationOn
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

/**
 * 失物/招领列表卡片组件。
 *
 * 用于首页失物 Feed 和列表场景中展示单条失物/招领启事。
 *
 * 视觉：左侧颜色条区分类型（失物=橙、招领=绿），右侧内容区含标题/描述/元信息。
 *
 * @param title         物品标题
 * @param description   物品描述（可截断）
 * @param type          类型："lost" 或 "found"
 * @param status        状态："active" / "claimed" / "closed"
 * @param category      分类标签
 * @param location      丢失/捡到地点
 * @param reward        悬赏金额（0 表示无悬赏）
 * @param onClick       卡片点击回调
 * @param modifier      修饰符
 */
@Composable
fun LostItemCard(
    title: String,
    description: String?,
    type: String,
    status: String,
    category: String,
    location: String?,
    reward: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accentColor = if (type == "found") Color(0xFF0A7A5E) else Color(0xFFB05E00)
    val bgColor = if (type == "found") Color(0xFFE7FBF7) else Color(0xFFFFF3E6)

    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Color indicator bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(140.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                    .background(accentColor),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp),
            ) {
                // Header row: type badge + status badge
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StatusBadge(
                        status = if (type == "found") "招领" else "失物",
                        color = accentColor,
                    )
                    StatusBadge(
                        status = when (status) {
                            "active" -> "寻找中"
                            "claimed" -> "已认领"
                            "closed" -> "已关闭"
                            else -> status
                        },
                        color = when (status) {
                            "active" -> Color(0xFF0A7A5E)
                            "claimed" -> Color(0xFF1565C0)
                            "closed" -> Color(0xFF9E9E9E)
                            else -> Color(0xFF757575)
                        },
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Title
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                // Description
                if (!description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Footer meta
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (!location.isNullOrBlank()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = location,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    if (reward > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "¥$reward",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100),
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = null,
                                tint = Color(0xFFE65100),
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

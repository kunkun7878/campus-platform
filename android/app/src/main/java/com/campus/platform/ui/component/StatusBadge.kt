package com.campus.platform.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * 通用状态标签 pill 组件，用于商品状态、订单状态等场景。
 *
 * 半透明底色 + 实色文字 + 可选图标，圆角胶囊造型。
 *
 * 典型用法：
 * - 商品状态：在售 / 已售出 / 已下架
 * - 订单状态：待面交 / 已完成 / 已取消
 *
 * @param status 状态文本，如 "在售"、"待面交"。
 * @param color  标签颜色，影响文字色和半透明底色。
 * @param icon   可选前置图标。
 * @param modifier 修饰符。
 */
@Composable
fun StatusBadge(
    status: String,
    color: Color,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = status,
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

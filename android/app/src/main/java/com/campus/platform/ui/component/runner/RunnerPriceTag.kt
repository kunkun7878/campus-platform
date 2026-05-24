package com.campus.platform.ui.component.runner

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 跑腿价格/赏金标签，可内嵌到卡片中。
 *
 * 显示主金额（如 ¥15）和可选的小费（如 +¥5）。
 * 主金额用 brand 色加粗，小费用 accent 色。
 *
 * @param amount 主金额字符串，如 "¥15"。
 * @param tip    小费字符串，如 "+¥5"。为 null 或空白时不显示。
 */
@Composable
fun RunnerPriceTag(
    amount: String,
    tip: String?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = amount,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        if (!tip.isNullOrBlank()) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = tip,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

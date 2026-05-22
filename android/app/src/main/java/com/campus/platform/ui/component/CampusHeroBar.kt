package com.campus.platform.ui.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 校园 Hero 顶栏 — 展示当前学校、天气等信息。
 *
 * 文字内容源自 [com.campus.platform.navigation.CampusScreenConfig] 的 brand / meta 字段，
 * 与 HTML 原型 screenConfigs 保持一致，不在此组件内硬编码。
 *
 * @param brand            标题文字（对应原型 screenConfigs.brand）。
 * @param meta             副标题行，包含位置与天气信息（对应原型 screenConfigs.meta）。
 * @param searchPlaceholder 搜索框 placeholder 文字，非 null 且非空字符串时在 brand 下方渲染只读搜索栏。
 * @param onSearchClick     搜索栏点击回调。
 */
@Composable
fun CampusHeroBar(
    brand: String,
    meta: String,
    modifier: Modifier = Modifier,
    searchPlaceholder: String? = null,
    onSearchClick: () -> Unit = {},
) {
    // 将 meta 按 " · " 拆分，取前两段作为位置和天气展示。
    // 原型格式示例："主校区 · 晴 26°C · 默认进入跑腿平台"
    val metaParts = meta.split(" · ")
    val locationText = metaParts.getOrElse(0) { "" }
    val weatherText = metaParts.getOrElse(1) { "" }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            )
            .clip(MaterialTheme.shapes.medium)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        // 第一行：位置 + 天气
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (locationText.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = "当前位置",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                    Text(
                        text = locationText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (weatherText.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Filled.WbSunny,
                        contentDescription = "天气",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                    Text(
                        text = weatherText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // 第二行：标题
        Text(
            text = brand,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        // 搜索栏（只读外观，不可编辑，点击触发 onSearchClick）
        if (!searchPlaceholder.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onSearchClick() }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "搜索",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = searchPlaceholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
        }
    }
}

package com.campus.platform.ui.component.runner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 跑腿类型筛选 Chip 组，用于首页跑腿列表。
 *
 * 选项包含"全部"和四种任务类型（帮取/帮送/帮买/万能帮）。
 * 选中态用 brand 色填充，未选中用浅底 + 描边。
 *
 * 使用 FlowRow 做自动换行布局。
 *
 * @param selectedType   当前选中的类型，null 表示"全部"。
 * @param onTypeSelected 选中回调，参数为类型 key，null 表示选中"全部"。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RunnerTypeFilter(
    selectedType: String?,
    onTypeSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = listOf(
        null to "全部",
        "pickup" to "帮取",
        "delivery" to "帮送",
        "purchase" to "帮买",
        "universal" to "万能帮",
    )

    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { (type, label) ->
            val isSelected = selectedType == type
            FilterChip(
                selected = isSelected,
                onClick = { onTypeSelected(type) },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    }
}

package com.campus.platform.ui.component

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 通用筛选 Chip 条，从 [RunnerTypeFilter] 中提取通用逻辑。
 *
 * 横向可滚动 Row 排列 Chip 列表。选中 Chip 使用 filled 样式（主色填充），
 * 未选中使用 outlined 样式。点击触发 [onSelected] 回调。
 *
 * 不关心业务含义 —— 只负责渲染 Chip 列表和选中状态。
 *
 * @param items         Chip 文案列表。
 * @param selectedIndex 当前选中项索引。
 * @param onSelected    选中回调，参数为被点击项的索引。
 * @param modifier      可选修饰符。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChipFilterBar(
    items: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEachIndexed { index, label ->
            val isSelected = selectedIndex == index
            FilterChip(
                selected = isSelected,
                onClick = { onSelected(index) },
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

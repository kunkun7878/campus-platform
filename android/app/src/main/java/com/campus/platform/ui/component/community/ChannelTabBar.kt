package com.campus.platform.ui.component.community

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.campus.platform.ui.theme.OnSurfaceVariant
import com.campus.platform.ui.theme.Primary

/** 频道定义 */
data class ChannelInfo(
    val section: String,
    val label: String,
)

/** 6 个社区频道（与 DB section CHECK 约束保持同步） */
val COMMUNITY_CHANNELS = listOf(
    ChannelInfo(section = "campus_wall", label = "校园墙"),
    ChannelInfo(section = "discussion", label = "讨论区"),
    ChannelInfo(section = "lost_found", label = "失物招领"),
    ChannelInfo(section = "second_hand", label = "二手交易"),
    ChannelInfo(section = "help", label = "求助答疑"),
    ChannelInfo(section = "announcement", label = "公告区"),
)

/**
 * 频道 Tab 栏组件。
 *
 * @param selectedIndex 当前选中的频道索引
 * @param onChannelSelected 频道选中回调
 * @param modifier 修饰符
 */
@Composable
fun ChannelTabBar(
    selectedIndex: Int,
    onChannelSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    TabRow(
        selectedTabIndex = selectedIndex,
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = Primary,
        indicator = { tabPositions ->
            if (selectedIndex < tabPositions.size) {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                    color = Primary,
                )
            }
        },
    ) {
        COMMUNITY_CHANNELS.forEachIndexed { index, channel ->
            Tab(
                selected = selectedIndex == index,
                onClick = { onChannelSelected(index) },
                text = {
                    Text(
                        text = channel.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selectedIndex == index) Primary else OnSurfaceVariant,
                    )
                },
            )
        }
    }
}

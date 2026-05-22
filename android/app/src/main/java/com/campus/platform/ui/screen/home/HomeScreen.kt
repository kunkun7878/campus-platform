package com.campus.platform.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.campus.platform.ui.component.ScreenPlaceholder
import com.campus.platform.ui.screen.home.HomeSubView
import kotlinx.coroutines.launch

/**
 * 首页。
 *
 * 顶部为三个 FilterChip 作为子视图选择器（跑腿 / 二手物品 / 失物招领），
 * 与 HTML 原型 viewConfigs 的 runner/market/lost 标签一一对应。
 * 下方使用 HorizontalPager 实现左右滑动切换子视图，
 * FilterChip 与 Pager 页面状态双向同步。
 */
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val tabs = HomeSubView.entries
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxWidth()) {
        // FilterChip 选择器
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            tabs.forEachIndexed { index, tab ->
                FilterChip(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    label = {
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                )
            }
        }

        // 滑动页面
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            when (page) {
                0 -> ScreenPlaceholder(
                    title = "${tabs[0].label} — ${tabs[0].searchHint}",
                    modifier = Modifier.fillMaxWidth(),
                )
                1 -> ScreenPlaceholder(
                    title = "${tabs[1].label} — ${tabs[1].searchHint}",
                    modifier = Modifier.fillMaxWidth(),
                )
                2 -> ScreenPlaceholder(
                    title = "${tabs[2].label} — ${tabs[2].searchHint}",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

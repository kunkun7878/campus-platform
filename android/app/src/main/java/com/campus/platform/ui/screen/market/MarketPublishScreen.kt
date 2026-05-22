package com.campus.platform.ui.screen.market

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.campus.platform.ui.component.ScreenPlaceholder
import com.campus.platform.ui.viewmodel.market.MarketPublishViewModel

/** 发布商品 */
@Composable
fun MarketPublishScreen(
    viewModel: MarketPublishViewModel = hiltViewModel(),
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    ScreenPlaceholder("发布商品")
}

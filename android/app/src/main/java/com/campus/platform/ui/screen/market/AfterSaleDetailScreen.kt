package com.campus.platform.ui.screen.market

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.campus.platform.ui.component.ScreenPlaceholder
import com.campus.platform.ui.viewmodel.market.AfterSaleDetailViewModel

/** 售后详情 */
@Composable
fun AfterSaleDetailScreen(
    viewModel: AfterSaleDetailViewModel = hiltViewModel(),
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    ScreenPlaceholder("售后详情")
}

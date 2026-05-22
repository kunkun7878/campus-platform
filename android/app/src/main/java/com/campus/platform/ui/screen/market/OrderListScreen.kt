package com.campus.platform.ui.screen.market

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.campus.platform.ui.component.ScreenPlaceholder
import com.campus.platform.ui.viewmodel.market.OrderListViewModel

/** 订单列表 */
@Composable
fun OrderListScreen(
    viewModel: OrderListViewModel = hiltViewModel(),
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    ScreenPlaceholder("订单列表")
}

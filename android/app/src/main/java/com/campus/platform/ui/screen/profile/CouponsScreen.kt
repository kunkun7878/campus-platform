package com.campus.platform.ui.screen.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.campus.platform.ui.component.ScreenPlaceholder
import com.campus.platform.ui.viewmodel.profile.CouponsViewModel

/** 优惠券 */
@Composable
fun CouponsScreen(
    viewModel: CouponsViewModel = hiltViewModel(),
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    ScreenPlaceholder("优惠券")
}

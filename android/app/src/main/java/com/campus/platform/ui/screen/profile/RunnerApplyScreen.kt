package com.campus.platform.ui.screen.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.campus.platform.ui.component.ScreenPlaceholder
import com.campus.platform.ui.viewmodel.profile.RunnerApplyViewModel

/** 跑腿申请 */
@Composable
fun RunnerApplyScreen(
    viewModel: RunnerApplyViewModel = hiltViewModel(),
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    ScreenPlaceholder("跑腿申请")
}

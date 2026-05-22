package com.campus.platform.ui.screen.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.campus.platform.ui.component.ScreenPlaceholder
import com.campus.platform.ui.viewmodel.profile.FeedbackViewModel

/** 意见反馈 */
@Composable
fun FeedbackScreen(
    viewModel: FeedbackViewModel = hiltViewModel(),
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    ScreenPlaceholder("意见反馈")
}

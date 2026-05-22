package com.campus.platform.ui.screen.publish

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.campus.platform.ui.component.ScreenPlaceholder
import com.campus.platform.ui.viewmodel.publish.PublishHubViewModel

/** 发布中心 — Publish Tab 根页面 */
@Composable
fun PublishHubScreen(
    viewModel: PublishHubViewModel = hiltViewModel(),
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    ScreenPlaceholder("发布中心")
}

package com.campus.platform.ui.screen.publish

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.campus.platform.ui.component.ScreenPlaceholder
import com.campus.platform.ui.viewmodel.publish.PublishViewModel

/** 发布页面（快捷发布入口） */
@Composable
fun PublishScreen(
    viewModel: PublishViewModel = hiltViewModel(),
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    ScreenPlaceholder("发布")
}

package com.campus.platform.ui.screen.message

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.campus.platform.ui.component.ScreenPlaceholder
import com.campus.platform.ui.viewmodel.message.MessageViewModel

/** 消息 — Message Tab 根页面 */
@Composable
fun MessageScreen(
    viewModel: MessageViewModel = hiltViewModel(),
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    ScreenPlaceholder("消息")
}

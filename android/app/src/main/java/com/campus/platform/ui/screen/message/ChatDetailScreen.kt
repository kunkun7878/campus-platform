package com.campus.platform.ui.screen.message

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.campus.platform.ui.component.ScreenPlaceholder
import com.campus.platform.ui.viewmodel.message.ChatDetailViewModel

/** 聊天详情 */
@Composable
fun ChatDetailScreen(
    viewModel: ChatDetailViewModel = hiltViewModel(),
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    ScreenPlaceholder("聊天详情")
}

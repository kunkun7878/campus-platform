package com.campus.platform.ui.screen.community

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.campus.platform.ui.component.ScreenPlaceholder
import com.campus.platform.ui.viewmodel.community.CommunityViewModel

/** 社区 — Community Tab 根页面 */
@Composable
fun CommunityScreen(
    viewModel: CommunityViewModel = hiltViewModel(),
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    ScreenPlaceholder("社区")
}

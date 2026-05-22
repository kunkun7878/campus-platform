package com.campus.platform.ui.screen.community

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.campus.platform.ui.component.ScreenPlaceholder
import com.campus.platform.ui.viewmodel.community.PostCreateViewModel

/** 发布帖子 */
@Composable
fun PostCreateScreen(
    viewModel: PostCreateViewModel = hiltViewModel(),
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    ScreenPlaceholder("发布帖子")
}

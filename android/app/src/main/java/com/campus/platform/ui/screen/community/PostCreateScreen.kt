package com.campus.platform.ui.screen.community

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.campus.platform.navigation.CampusRoutes
import com.campus.platform.ui.component.MultiImagePicker
import com.campus.platform.ui.component.community.COMMUNITY_CHANNELS
import com.campus.platform.ui.viewmodel.community.PostCreateViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostCreateScreen(
    viewModel: PostCreateViewModel = hiltViewModel(),
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    val title by viewModel.title.collectAsState()
    val content by viewModel.content.collectAsState()
    val selectedSection by viewModel.selectedSection.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val blockReason by viewModel.blockReason.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val publishedPostId by viewModel.publishedPostId.collectAsState()
    val selectedImages by viewModel.selectedImages.collectAsState()
    val isUploadingImages by viewModel.isUploadingImages.collectAsState()

    val context = LocalContext.current
    var sectionExpanded by remember { mutableStateOf(false) }

    // Toast
    LaunchedEffect(toastMessage) {
        toastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    // 发布成功导航
    LaunchedEffect(publishedPostId) {
        publishedPostId?.let { id ->
            if (id.isNotEmpty()) {
                navController.navigate(CampusRoutes.PostDetail.createRoute(id)) {
                    popUpTo(CampusRoutes.Community.route) { inclusive = false }
                }
            } else {
                navController.popBackStack()
            }
            viewModel.onPublishedConsumed()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("发布帖子") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 频道选择
            ExposedDropdownMenuBox(
                expanded = sectionExpanded,
                onExpandedChange = { sectionExpanded = it },
            ) {
                val selectedLabel = COMMUNITY_CHANNELS
                    .find { it.section == selectedSection }?.label ?: "校园墙"
                OutlinedTextField(
                    value = selectedLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("发布到频道") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sectionExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                )
                ExposedDropdownMenu(
                    expanded = sectionExpanded,
                    onDismissRequest = { sectionExpanded = false },
                ) {
                    COMMUNITY_CHANNELS.forEach { channel ->
                        DropdownMenuItem(
                            text = { Text(channel.label) },
                            onClick = {
                                viewModel.setSection(channel.section)
                                sectionExpanded = false
                            },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 标题
            OutlinedTextField(
                value = title,
                onValueChange = { viewModel.setTitle(it) },
                label = { Text("标题") },
                placeholder = { Text("起一个吸引人的标题") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 正文
            OutlinedTextField(
                value = content,
                onValueChange = { viewModel.setContent(it) },
                label = { Text("内容") },
                placeholder = { Text("分享你的想法...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                minLines = 6,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── 图片选择 ──────────────────────────────────────
            MultiImagePicker(
                selectedUris = selectedImages,
                onAddImages = { viewModel.addImages(it) },
                onRemoveImage = { viewModel.removeImage(it) },
                maxCount = 9,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 提交按钮
            Button(
                onClick = { viewModel.submit() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting && !isUploadingImages,
            ) {
                if (isSubmitting || isUploadingImages) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = if (isUploadingImages) "上传图片中..." else "发布中...",
                        fontWeight = FontWeight.SemiBold,
                    )
                } else {
                    Text(
                        text = "提交发布",
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // ── Block 弹框 ──
    if (blockReason != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearBlockReason() },
            title = { Text("内容未通过审核") },
            text = { Text(blockReason ?: "内容不符合社区规范") },
            confirmButton = {
                Button(onClick = { viewModel.clearBlockReason() }) {
                    Text("知道了")
                }
            },
        )
    }
}

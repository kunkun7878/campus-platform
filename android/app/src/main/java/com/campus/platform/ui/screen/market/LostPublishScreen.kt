package com.campus.platform.ui.screen.market

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.campus.platform.navigation.LostDetail
import com.campus.platform.ui.component.FormCard
import com.campus.platform.ui.component.MultiImagePicker
import com.campus.platform.ui.viewmodel.UiState
import com.campus.platform.ui.viewmodel.market.LostPublishViewModel
import com.campus.platform.ui.viewmodel.market.PublishedItemResult

private val CATEGORY_OPTIONS = listOf(
    "电子产品",
    "证件卡片",
    "衣物饰品",
    "书籍文具",
    "钥匙",
    "其他",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LostPublishScreen(
    viewModel: LostPublishViewModel = hiltViewModel(),
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val title by viewModel.title.collectAsStateWithLifecycle()
    val description by viewModel.description.collectAsStateWithLifecycle()
    val type by viewModel.type.collectAsStateWithLifecycle()
    val category by viewModel.category.collectAsStateWithLifecycle()
    val location by viewModel.location.collectAsStateWithLifecycle()
    val lostDate by viewModel.lostDate.collectAsStateWithLifecycle()
    val reward by viewModel.reward.collectAsStateWithLifecycle()
    val contact by viewModel.contact.collectAsStateWithLifecycle()
    val selectedImages by viewModel.selectedImages.collectAsStateWithLifecycle()
    val isUploadingImages by viewModel.isUploadingImages.collectAsStateWithLifecycle()

    var titleError by remember { mutableStateOf<String?>(null) }
    var categoryExpanded by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // ── Handle publish result ──
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is UiState.Error -> {
                val msg = state.message
                when {
                    msg.contains("标题") -> titleError = msg
                    else -> snackbarHostState.showSnackbar(msg)
                }
            }
            is UiState.Success -> {
                titleError = null
                val result = state.data
                if (result != null) {
                    snackbarHostState.showSnackbar("发布成功")
                    navController.navigate(LostDetail(lostId = result.itemId)) {
                        popUpTo("lost-publish") { inclusive = true }
                    }
                }
            }
            is UiState.Loading -> {
                titleError = null
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("发布失物/招领")
                        Text(
                            text = "帮忙找回遗失物品",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
        ) {
            FormCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                // 类型切换：失物 / 招领
                Text(
                    text = "类型",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == "lost",
                        onClick = { viewModel.setType("lost") },
                        label = { Text("失物启事") },
                        shape = RoundedCornerShape(8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                    FilterChip(
                        selected = type == "found",
                        onClick = { viewModel.setType("found") },
                        label = { Text("招领启事") },
                        shape = RoundedCornerShape(8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 标题（必填）
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        viewModel.setTitle(it)
                        if (titleError != null) titleError = null
                    },
                    label = { Text(if (type == "lost") "丢失了什么？" else "捡到了什么？") },
                    placeholder = { Text("请输入物品名称") },
                    isError = titleError != null,
                    supportingText = titleError?.let { err -> { Text(err) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 分类
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it },
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("分类") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                        },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                    )
                    DropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false },
                    ) {
                        CATEGORY_OPTIONS.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    viewModel.setCategory(option)
                                    categoryExpanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 描述
                OutlinedTextField(
                    value = description,
                    onValueChange = { viewModel.setDescription(it) },
                    label = { Text("描述") },
                    placeholder = { Text("物品特征、颜色、品牌、特殊标记等") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 丢失/捡到地点
                OutlinedTextField(
                    value = location,
                    onValueChange = { viewModel.setLocation(it) },
                    label = { Text(if (type == "lost") "丢失地点" else "捡到地点") },
                    placeholder = { Text("如：图书馆二楼、一食堂") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 日期
                OutlinedTextField(
                    value = lostDate,
                    onValueChange = { viewModel.setLostDate(it) },
                    label = { Text("日期") },
                    placeholder = { Text("如：2026-05-24") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 悬赏金额（type = lost 时展示）
                if (type == "lost") {
                    OutlinedTextField(
                        value = reward,
                        onValueChange = { viewModel.setReward(it) },
                        label = { Text("悬赏金额（元）") },
                        placeholder = { Text("0") },
                        prefix = { Text("¥") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // 联系方式
                OutlinedTextField(
                    value = contact,
                    onValueChange = { viewModel.setContact(it) },
                    label = { Text("联系方式") },
                    placeholder = { Text("站内私信联系") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── 图片选择 ──────────────────────────────────────
            MultiImagePicker(
                selectedUris = selectedImages,
                onAddImages = { viewModel.addImages(it) },
                onRemoveImage = { viewModel.removeImage(it) },
                maxCount = 9,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 确认发布按钮
            Button(
                onClick = {
                    if (title.isBlank()) {
                        titleError = "请输入物品名称"
                        return@Button
                    }
                    viewModel.submitPublish()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                enabled = uiState !is UiState.Loading && !isUploadingImages,
            ) {
                if (uiState is UiState.Loading || isUploadingImages) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .height(20.dp)
                            .width(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("发布中...")
                } else {
                    Text("确认发布")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

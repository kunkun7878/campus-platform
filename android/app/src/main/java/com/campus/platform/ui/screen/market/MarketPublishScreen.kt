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
import com.campus.platform.navigation.GoodsDetail
import com.campus.platform.ui.component.FormCard
import com.campus.platform.ui.viewmodel.UiState
import com.campus.platform.ui.viewmodel.market.MarketPublishViewModel

// ── 下拉选项常量 ───────────────────────────────────────────────

private val CATEGORY_OPTIONS = listOf(
    "电子产品",
    "书籍资料",
    "生活用品",
    "数码配件",
    "其他",
)

private val CONDITION_OPTIONS = listOf(
    "全新",
    "九成新",
    "八成新",
    "七成新",
    "六成新及以下",
)

// ── Screen ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketPublishScreen(
    viewModel: MarketPublishViewModel = hiltViewModel(),
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    // ── 表单字段状态 ──────────────────────────────────────────
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val title by viewModel.title.collectAsStateWithLifecycle()
    val description by viewModel.description.collectAsStateWithLifecycle()
    val price by viewModel.price.collectAsStateWithLifecycle()
    val category by viewModel.category.collectAsStateWithLifecycle()
    val condition by viewModel.condition.collectAsStateWithLifecycle()
    val isBargain by viewModel.isBargain.collectAsStateWithLifecycle()
    val meetupLocation by viewModel.meetupLocation.collectAsStateWithLifecycle()
    val contact by viewModel.contact.collectAsStateWithLifecycle()

    // ── 内联验证错误 ──────────────────────────────────────────
    var titleError by remember { mutableStateOf<String?>(null) }
    var priceError by remember { mutableStateOf<String?>(null) }

    // ── 下拉展开状态 ──────────────────────────────────────────
    var categoryExpanded by remember { mutableStateOf(false) }
    var conditionExpanded by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    // ── 响应 ViewModel 验证错误 → 内联展示 ────────────────────
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is UiState.Error -> {
                val msg = state.message
                when {
                    msg.contains("标题") -> {
                        titleError = msg
                        priceError = null
                    }
                    msg.contains("价格") -> {
                        priceError = msg
                        titleError = null
                    }
                    else -> {
                        // 非字段级错误 → Snackbar
                        titleError = null
                        priceError = null
                        snackbarHostState.showSnackbar(msg)
                    }
                }
            }
            is UiState.Success -> {
                // 发布成功 → 清空错误，跳转商品详情
                titleError = null
                priceError = null
                val listing = state.data
                if (listing != null) {
                    snackbarHostState.showSnackbar("发布成功")
                    navController.navigate(GoodsDetail(goodsId = listing.id)) {
                        popUpTo("market-publish") { inclusive = true }
                    }
                }
            }
            is UiState.Loading -> {
                // Loading 时清空之前的错误
                titleError = null
                priceError = null
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("发布二手")
                        Text(
                            text = "同校交易",
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
                // 1. 商品名称（必填）
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        viewModel.setTitle(it)
                        if (titleError != null) titleError = null
                    },
                    label = { Text("商品名称") },
                    placeholder = { Text("请输入商品名称") },
                    isError = titleError != null,
                    supportingText = titleError?.let { err -> { Text(err) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 2. 分类（下拉）
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

                // 3. 价格（数字键盘，¥前缀）
                OutlinedTextField(
                    value = price,
                    onValueChange = {
                        viewModel.setPrice(it)
                        if (priceError != null) priceError = null
                    },
                    label = { Text("价格") },
                    placeholder = { Text("0") },
                    prefix = { Text("¥") },
                    isError = priceError != null,
                    supportingText = priceError?.let { err -> { Text(err) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 4. 成色（下拉）
                ExposedDropdownMenuBox(
                    expanded = conditionExpanded,
                    onExpandedChange = { conditionExpanded = it },
                ) {
                    OutlinedTextField(
                        value = condition,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("成色") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = conditionExpanded)
                        },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                    )
                    DropdownMenu(
                        expanded = conditionExpanded,
                        onDismissRequest = { conditionExpanded = false },
                    ) {
                        CONDITION_OPTIONS.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    viewModel.setCondition(option)
                                    conditionExpanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 5. 描述（多行）
                OutlinedTextField(
                    value = description,
                    onValueChange = { viewModel.setDescription(it) },
                    label = { Text("描述") },
                    placeholder = { Text("描述商品的使用情况、购买渠道等") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 6. 面交地点
                OutlinedTextField(
                    value = meetupLocation,
                    onValueChange = { viewModel.setMeetupLocation(it) },
                    label = { Text("面交地点") },
                    placeholder = { Text("如：图书馆门口、食堂一楼") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 7. 是否可议价 — Chip 行
                Text(
                    text = "议价",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = isBargain,
                        onClick = { viewModel.setIsBargain(true) },
                        label = { Text("可议价") },
                        shape = RoundedCornerShape(8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                    FilterChip(
                        selected = !isBargain,
                        onClick = { viewModel.setIsBargain(false) },
                        label = { Text("不议价") },
                        shape = RoundedCornerShape(8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 8. 联系方式
                OutlinedTextField(
                    value = contact,
                    onValueChange = { viewModel.setContact(it) },
                    label = { Text("联系方式") },
                    placeholder = { Text("站内私信联系") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 9. 确认发布按钮
            Button(
                onClick = {
                    // 先做一次本地快速校验以提供即时反馈
                    if (title.isBlank()) {
                        titleError = "请输入商品标题"
                        return@Button
                    }
                    val priceValue = price.trim().toIntOrNull()
                    if (priceValue == null || priceValue <= 0) {
                        priceError = "请输入有效的价格"
                        return@Button
                    }
                    viewModel.submitPublish()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                enabled = uiState !is UiState.Loading,
            ) {
                if (uiState is UiState.Loading) {
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

            // 底部留白
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

package com.campus.platform.ui.screen.publish

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.campus.platform.data.local.entity.RunnerTaskEntity
import com.campus.platform.ui.viewmodel.publish.PublishUiState
import com.campus.platform.ui.viewmodel.publish.PublishViewModel

/** 跑腿任务发布页面，含类型选择、动态表单、赏金/小费/截止时间 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PublishScreen(
    viewModel: PublishViewModel = hiltViewModel(),
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 成功后 Toast + 返回
    LaunchedEffect(uiState) {
        if (uiState is PublishUiState.Success) {
            Toast.makeText(context, "发布成功", Toast.LENGTH_SHORT).show()
            viewModel.resetUiState()
            navController.popBackStack()
        }
    }

    // 错误 Toast
    LaunchedEffect(uiState) {
        if (uiState is PublishUiState.Error) {
            val err = uiState as PublishUiState.Error
            Toast.makeText(context, err.message, Toast.LENGTH_SHORT).show()
            viewModel.onErrorDismissed()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("发布跑腿") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ── 任务类型选择 ──────────────────────────────────────
            Text(
                text = "任务类型",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(8.dp))

            val taskTypes = listOf(
                RunnerTaskEntity.TYPE_PICKUP to "帮取",
                RunnerTaskEntity.TYPE_DELIVERY to "帮送",
                RunnerTaskEntity.TYPE_PURCHASE to "帮买",
                RunnerTaskEntity.TYPE_UNIVERSAL to "万能帮",
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                taskTypes.forEach { (type, label) ->
                    FilterChip(
                        selected = formState.taskType == type,
                        onClick = { viewModel.onTaskTypeChange(type) },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 标题 ─────────────────────────────────────────────
            OutlinedTextField(
                value = formState.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text("任务标题") },
                placeholder = { Text("例如：帮忙取快递") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── 类型特定字段 ─────────────────────────────────────
            TypeSpecificFields(viewModel, formState)

            Spacer(modifier = Modifier.height(12.dp))

            // ── 赏金 ─────────────────────────────────────────────
            OutlinedTextField(
                value = formState.price,
                onValueChange = viewModel::onPriceChange,
                label = { Text("赏金 (元)") },
                placeholder = { Text("请输入赏金金额") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next,
                ),
                prefix = { Text("¥") },
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── 小费（可选） ─────────────────────────────────────
            OutlinedTextField(
                value = formState.tip,
                onValueChange = viewModel::onTipChange,
                label = { Text("小费 (元，可选)") },
                placeholder = { Text("可选小费") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next,
                ),
                prefix = { Text("¥") },
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── 截止时间（可选） ─────────────────────────────────
            OutlinedTextField(
                value = formState.deadline,
                onValueChange = viewModel::onDeadlineChange,
                label = { Text("截止时间（可选）") },
                placeholder = { Text("如：2026-05-25 18:00") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── 性别限制 ─────────────────────────────────────────
            Text(
                text = "性别限制",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(4.dp))

            val genders = listOf(
                RunnerTaskEntity.GENDER_ANY to "不限",
                RunnerTaskEntity.GENDER_FEMALE_ONLY to "仅女生",
                RunnerTaskEntity.GENDER_MALE_ONLY to "仅男生",
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                genders.forEach { (key, label) ->
                    FilterChip(
                        selected = formState.genderRestriction == key,
                        onClick = { viewModel.onGenderRestrictionChange(key) },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── 错误提示 ─────────────────────────────────────────
            formState.error?.let { errorText ->
                Text(
                    text = errorText,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                )
            }

            // ── 提交按钮 ─────────────────────────────────────────
            Button(
                onClick = { viewModel.publishTask() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !formState.isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                if (formState.isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.height(24.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = "立即发布",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ── 类型特定字段 ────────────────────────────────────────────────────────────

@Composable
private fun TypeSpecificFields(
    viewModel: PublishViewModel,
    formState: com.campus.platform.ui.viewmodel.publish.PublishFormState,
) {
    when (formState.taskType) {
        // 帮取：取货地点 + 送达地点 + 物品描述
        RunnerTaskEntity.TYPE_PICKUP -> {
            OutlinedTextField(
                value = formState.pickupAddr,
                onValueChange = viewModel::onPickupAddrChange,
                label = { Text("取货地点") },
                placeholder = { Text("例如：菜鸟驿站 2 号柜") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = formState.deliveryAddr,
                onValueChange = viewModel::onDeliveryAddrChange,
                label = { Text("送达地点") },
                placeholder = { Text("例如：明德楼 301") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = formState.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text("物品描述") },
                placeholder = { Text("请描述需要取送的物品") },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // 帮送：取件地点 + 送达地点 + 物品描述
        RunnerTaskEntity.TYPE_DELIVERY -> {
            OutlinedTextField(
                value = formState.pickupAddr,
                onValueChange = viewModel::onPickupAddrChange,
                label = { Text("取件地点") },
                placeholder = { Text("例如：图书馆前台") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = formState.deliveryAddr,
                onValueChange = viewModel::onDeliveryAddrChange,
                label = { Text("送达地点") },
                placeholder = { Text("例如：博学楼 502") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = formState.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text("物品描述") },
                placeholder = { Text("请描述需要帮送的物品") },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // 帮买：购买店铺 + 商品名称 + 预估金额
        RunnerTaskEntity.TYPE_PURCHASE -> {
            OutlinedTextField(
                value = formState.storeName,
                onValueChange = viewModel::onStoreNameChange,
                label = { Text("购买店铺") },
                placeholder = { Text("例如：罗森便利店") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = formState.productName,
                onValueChange = viewModel::onProductNameChange,
                label = { Text("商品名称") },
                placeholder = { Text("例如：三明治 + 牛奶") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = formState.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text("备注（可选）") },
                placeholder = { Text("其他要求，如品牌/口味/规格") },
                maxLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // 万能帮：服务描述 + 位置
        RunnerTaskEntity.TYPE_UNIVERSAL -> {
            OutlinedTextField(
                value = formState.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text("服务描述") },
                placeholder = { Text("请详细描述需要帮忙的事项") },
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = formState.pickupAddr,
                onValueChange = viewModel::onPickupAddrChange,
                label = { Text("位置") },
                placeholder = { Text("例如：教学楼 A 区") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

package com.campus.platform.ui.screen.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.campus.platform.ui.viewmodel.profile.RunnerApplyFormState
import com.campus.platform.ui.viewmodel.profile.RunnerApplyUiState
import com.campus.platform.ui.viewmodel.profile.RunnerApplyViewModel

/** 跑腿员认证申请 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunnerApplyScreen(
    viewModel: RunnerApplyViewModel = hiltViewModel(),
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // 提交成功 → 返回上一页
    LaunchedEffect(uiState) {
        if (uiState is RunnerApplyUiState.SubmitSuccess) {
            navController.popBackStack()
        }
    }

    // 表单校验错误 → Snackbar
    LaunchedEffect(uiState) {
        val errorMsg = when (val state = uiState) {
            is RunnerApplyUiState.NewApplication -> state.form.error
            is RunnerApplyUiState.ExistingApplication -> state.form.error
            is RunnerApplyUiState.SubmitError -> state.message
            else -> null
        }
        errorMsg?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("跑腿员认证") },
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
                ),
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        when (val state = uiState) {
            is RunnerApplyUiState.Loading -> {
                // 加载中
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is RunnerApplyUiState.ExistingApplication -> {
                ExistingApplicationContent(
                    state = state,
                    innerPadding = innerPadding,
                    onResubmit = { viewModel.onStartResubmit() },
                )
            }

            is RunnerApplyUiState.NewApplication -> {
                NewApplicationForm(
                    form = state.form,
                    isSubmitting = state.isSubmitting,
                    innerPadding = innerPadding,
                    onRealNameChange = { viewModel.onRealNameChange(it) },
                    onStudentIdChange = { viewModel.onStudentIdChange(it) },
                    onPhoneChange = { viewModel.onPhoneChange(it) },
                    onReasonChange = { viewModel.onReasonChange(it) },
                    onIdCardFrontChange = { viewModel.onIdCardFrontUrlChange(it) },
                    onIdCardBackChange = { viewModel.onIdCardBackUrlChange(it) },
                    onSubmit = { viewModel.submitApplication() },
                )
            }

            is RunnerApplyUiState.SubmitSuccess -> {
                // 短暂显示后再返回 — LaunchedEffect 已处理
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is RunnerApplyUiState.SubmitError -> {
                // 展示错误状态的空表单
                NewApplicationForm(
                    form = RunnerApplyFormState(),
                    isSubmitting = false,
                    innerPadding = innerPadding,
                    onRealNameChange = { viewModel.onRealNameChange(it) },
                    onStudentIdChange = { viewModel.onStudentIdChange(it) },
                    onPhoneChange = { viewModel.onPhoneChange(it) },
                    onReasonChange = { viewModel.onReasonChange(it) },
                    onIdCardFrontChange = { viewModel.onIdCardFrontUrlChange(it) },
                    onIdCardBackChange = { viewModel.onIdCardBackUrlChange(it) },
                    onSubmit = { viewModel.submitApplication() },
                )
            }
        }
    }
}

// ── 已有申请：审核状态卡片 ──────────────────────────────────────────

@Composable
private fun ExistingApplicationContent(
    state: RunnerApplyUiState.ExistingApplication,
    innerPadding: androidx.compose.foundation.layout.PaddingValues,
    onResubmit: () -> Unit,
) {
    val app = state.application
    val (statusText, statusColor, supportingText) = when (app.status) {
        "pending" -> Triple(
            "审核中，请耐心等待",
            Color(0xFFF59E0B), // amber/yellow
            "提交时间：${app.createdAt ?: "未知"}"
        )
        "approved" -> Triple(
            "已通过，可以开始接单了",
            Color(0xFF10B981), // green
            "审核时间：${app.reviewedAt ?: "未知"}"
        )
        "rejected" -> Triple(
            "未通过",
            Color(0xFFEF4444), // red
            app.reviewComment ?: "暂无备注"
        )
        else -> Triple("状态未知", MaterialTheme.colorScheme.onSurfaceVariant, "")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = statusColor.copy(alpha = 0.08f),
            ),
            shape = MaterialTheme.shapes.large,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // 状态指示圆点
                Card(
                    modifier = Modifier.size(64.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(
                        containerColor = statusColor.copy(alpha = 0.15f),
                    ),
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = when (app.status) {
                                "pending" -> "●"
                                "approved" -> "✓"
                                "rejected" -> "✗"
                                else -> "?"
                            },
                            style = MaterialTheme.typography.headlineMedium,
                            color = statusColor,
                        )
                    }
                }

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = statusColor,
                )

                if (supportingText.isNotBlank()) {
                    Text(
                        text = supportingText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // 申请信息摘要
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    InfoRow("姓名", app.realName)
                    InfoRow("学号", app.studentId)
                    InfoRow("手机号", app.phone)
                    if (!app.reason.isNullOrBlank()) {
                        InfoRow("申请理由", app.reason)
                    }
                }
            }
        }

        // 重新申请按钮（仅 rejected 显示）
        if (app.status == "rejected") {
            Button(
                onClick = onResubmit,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("重新申请")
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    ) {
        Text(
            text = "$label：",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(72.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ── 新申请表单 ─────────────────────────────────────────────────────

@Composable
private fun NewApplicationForm(
    form: RunnerApplyFormState,
    isSubmitting: Boolean,
    innerPadding: androidx.compose.foundation.layout.PaddingValues,
    onRealNameChange: (String) -> Unit,
    onStudentIdChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onReasonChange: (String) -> Unit,
    onIdCardFrontChange: (String) -> Unit,
    onIdCardBackChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 必填信息
        OutlinedTextField(
            value = form.realName,
            onValueChange = onRealNameChange,
            label = { Text("真实姓名") },
            placeholder = { Text("请输入真实姓名") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = form.studentId,
            onValueChange = onStudentIdChange,
            label = { Text("学号") },
            placeholder = { Text("请输入学号") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = form.phone,
            onValueChange = onPhoneChange,
            label = { Text("手机号") },
            placeholder = { Text("请输入手机号") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = form.reason,
            onValueChange = onReasonChange,
            label = { Text("申请理由") },
            placeholder = { Text("请简要说明申请跑腿员的原因（选填）") },
            minLines = 3,
            maxLines = 5,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 身份证上传（暂用文本输入）
        Text(
            text = "身份认证",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        OutlinedTextField(
            value = form.idCardFrontUrl,
            onValueChange = onIdCardFrontChange,
            label = { Text("身份证正面") },
            placeholder = { Text("请输入身份证正面图片 URL") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            supportingText = { Text("图片上传即将上线，请先输入图片 URL", style = MaterialTheme.typography.bodySmall) },
        )

        OutlinedTextField(
            value = form.idCardBackUrl,
            onValueChange = onIdCardBackChange,
            label = { Text("身份证反面") },
            placeholder = { Text("请输入身份证反面图片 URL") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            supportingText = { Text("图片上传即将上线，请先输入图片 URL", style = MaterialTheme.typography.bodySmall) },
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 提交按钮
        Button(
            onClick = onSubmit,
            enabled = !isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text("提交申请")
            }
        }
    }
}

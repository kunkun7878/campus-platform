package com.campus.platform.ui.screen.runner

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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.campus.platform.navigation.CampusRoutes
import com.campus.platform.ui.component.MultiImagePicker
import com.campus.platform.ui.viewmodel.runner.AfterSaleApplyUiState
import com.campus.platform.ui.viewmodel.runner.AfterSaleApplyViewModel
import com.campus.platform.ui.viewmodel.runner.OrderSummary

/** 售后申请 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AfterSaleApplyScreen(
    viewModel: AfterSaleApplyViewModel = hiltViewModel(),
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val orderSummary by viewModel.orderSummary.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Error snackbar
    LaunchedEffect(formState.error) {
        formState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onErrorDismissed()
        }
    }

    // Success -> toast + navigate
    LaunchedEffect(uiState) {
        if (uiState is AfterSaleApplyUiState.Success) {
            val afterSaleId = (uiState as AfterSaleApplyUiState.Success).afterSaleId
            snackbarHostState.showSnackbar("售后申请已提交")
            navController.navigate(CampusRoutes.AfterSaleDetail.createRoute(afterSaleId)) {
                popUpTo(CampusRoutes.AfterSaleApply.route) { inclusive = true }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("申请售后") },
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── 1. 订单摘要卡片 ──────────────────────────
            if (formState.isSummaryLoading) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "加载订单信息…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                OrderSummaryCard(orderSummary)
            }

            // ── 2. 售后类型选择 ──────────────────────────
            Text(
                text = "售后类型",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                viewModel.afterSaleTypeOptions.forEach { (type, label) ->
                    val selected = formState.afterSaleType == type
                    if (selected) {
                        FilledTonalButton(
                            onClick = { },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(text = label, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { viewModel.onTypeChange(type) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(text = label)
                        }
                    }
                }
            }

            // ── 3. 申请原因 ──────────────────────────────
            OutlinedTextField(
                value = formState.reason,
                onValueChange = { viewModel.onReasonChange(it) },
                label = { Text("申请原因") },
                placeholder = { Text("请详细描述您遇到的问题…") },
                minLines = 4,
                maxLines = 8,
                modifier = Modifier.fillMaxWidth(),
            )

            // ── 4. 图片上传 ──────────────────────────────
            MultiImagePicker(
                selectedUris = formState.selectedUris,
                onAddImages = { viewModel.onAddImages(it) },
                onRemoveImage = { viewModel.onRemoveImage(it) },
                maxCount = 6,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── 5. 提交按钮 ──────────────────────────────
            Button(
                onClick = { viewModel.submit() },
                enabled = formState.reason.isNotBlank() && !formState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                if (formState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("提交中…")
                } else {
                    Text("提交申请")
                }
            }

            // Bottom spacing for system nav bar
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ── 订单摘要卡片 ────────────────────────────────────────────────

@Composable
private fun OrderSummaryCard(summary: OrderSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "关联订单",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
            )

            SummaryRow(
                label = "订单号",
                value = if (summary.orderId.isNotEmpty()) {
                    "#${summary.orderId.takeLast(8)}"
                } else {
                    "加载中…"
                },
            )
            SummaryRow(label = "类型", value = summary.typeLabel)
            SummaryRow(label = "金额", value = summary.amount)
            SummaryRow(label = "跑腿员", value = summary.runnerName)
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

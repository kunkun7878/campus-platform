package com.campus.platform.ui.screen.runner

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.campus.platform.ui.component.runner.AFTER_SALE_TIMELINE_LABELS
import com.campus.platform.ui.component.runner.OrderStatusTimeline
import com.campus.platform.ui.component.runner.RunnerEmptyState
import com.campus.platform.ui.viewmodel.runner.AfterSaleDetailUiState
import com.campus.platform.ui.viewmodel.runner.AfterSaleDetailViewModel
import com.campus.platform.ui.viewmodel.runner.AfterSaleUserRole

// ── Badge colors ───────────────────────────────────────────

private val BadgeColorRefund = Color(0xFF12B7AE)
private val BadgeColorReturn = Color(0xFFF59E0B)
private val BadgeColorComplaint = Color(0xFFEF4444)

private val BadgeStatusPending = Color(0xFFF59E0B)
private val BadgeStatusReviewing = Color(0xFF2D6BFF)
private val BadgeStatusResolved = Color(0xFF12B7AE)
private val BadgeStatusRejected = Color(0xFFEF4444)

private fun afterSaleTypeBadgeColor(type: String): Color = when (type) {
    "refund" -> BadgeColorRefund
    "return" -> BadgeColorReturn
    "complaint" -> BadgeColorComplaint
    else -> Color.Gray
}

private fun afterSaleStatusBadgeColor(status: String): Color = when (status) {
    "pending" -> BadgeStatusPending
    "processing" -> BadgeStatusReviewing
    "approved" -> BadgeStatusResolved
    "rejected" -> BadgeStatusRejected
    "completed" -> Color.Gray
    else -> Color.Gray
}

// ── Screen ─────────────────────────────────────────────────

/** 售后详情 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AfterSaleDetailScreen(
    viewModel: AfterSaleDetailViewModel = hiltViewModel(),
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("售后详情") },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (val state = uiState) {
                is AfterSaleDetailUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                is AfterSaleDetailUiState.Error -> {
                    RunnerEmptyState(
                        title = "加载失败",
                        subtitle = state.message,
                        actionLabel = "重试",
                        onAction = { viewModel.retry() },
                    )
                }

                is AfterSaleDetailUiState.Empty -> {
                    RunnerEmptyState(
                        title = "售后单不存在",
                        subtitle = "未找到该售后申请，请检查单号是否正确",
                        actionLabel = "返回",
                        onAction = { navController.popBackStack() },
                    )
                }

                is AfterSaleDetailUiState.Success -> {
                    AfterSaleDetailContent(
                        state = state,
                        onSupplement = {
                            Toast.makeText(context, "补充材料功能即将上线", Toast.LENGTH_SHORT).show()
                        },
                        onContactService = {
                            Toast.makeText(context, "请联系客服处理", Toast.LENGTH_SHORT).show()
                        },
                        viewModel = viewModel,
                    )
                }
            }
        }
    }
}

// ── Content ────────────────────────────────────────────────

@Composable
private fun AfterSaleDetailContent(
    state: AfterSaleDetailUiState.Success,
    onSupplement: () -> Unit,
    onContactService: () -> Unit,
    viewModel: AfterSaleDetailViewModel,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        // 1. 售后信息卡片
        AfterSaleInfoCard(
            type = state.afterSale.type,
            typeLabel = viewModel.afterSaleTypeLabel(state.afterSale.type),
            status = state.afterSale.status,
            statusLabel = viewModel.afterSaleStatusLabel(state.afterSale.status),
            reason = state.afterSale.reason,
            createdAt = state.afterSale.createdAt,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 2. 关联订单摘要卡片
        OrderSummaryCard(summary = state.orderSummary)

        Spacer(modifier = Modifier.height(12.dp))

        // 3. 售后进度时间线
        if (state.timelineEvents.isNotEmpty()) {
            TimelineSection(events = state.timelineEvents)

            Spacer(modifier = Modifier.height(16.dp))
        }

        // 4. 操作按钮区
        ActionButtons(
            canOperate = state.canOperate,
            userRole = state.userRole,
            onSupplement = onSupplement,
            onContactService = onContactService,
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ── After-sale info card ───────────────────────────────────

@Composable
private fun AfterSaleInfoCard(
    type: String,
    typeLabel: String,
    status: String,
    statusLabel: String,
    reason: String,
    createdAt: String?,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "售后信息",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Type + Status badges row
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Badge(
                    text = typeLabel,
                    backgroundColor = afterSaleTypeBadgeColor(type),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Badge(
                    text = statusLabel,
                    backgroundColor = afterSaleStatusBadgeColor(status),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            // 申请原因
            InfoRow(label = "申请原因", value = reason.ifBlank { "未填写" })

            // 提交时间
            val timeText = createdAt?.let {
                formatTimestamp(it)
            } ?: "--"
            InfoRow(label = "提交时间", value = timeText)
        }
    }
}

// ── Order summary card ─────────────────────────────────────

@Composable
private fun OrderSummaryCard(summary: com.campus.platform.ui.viewmodel.runner.AfterSaleOrderSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "关联订单",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            InfoRow(label = "订单编号", value = summary.orderId)
            InfoRow(label = "类型", value = summary.typeLabel)
            InfoRow(label = "金额", value = summary.amount)
        }
    }
}

// ── Timeline section ───────────────────────────────────────

@Composable
private fun TimelineSection(events: List<com.campus.platform.ui.component.runner.TimelineEvent>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "售后进度",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            OrderStatusTimeline(
                events = events,
                eventLabelMap = AFTER_SALE_TIMELINE_LABELS,
            )
        }
    }
}

// ── Action buttons ─────────────────────────────────────────

@Composable
private fun ActionButtons(
    canOperate: Boolean,
    userRole: AfterSaleUserRole,
    onSupplement: () -> Unit,
    onContactService: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // requester + pending → 补充说明
        if (canOperate) {
            Button(
                onClick = onSupplement,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text("补充说明")
            }
        }

        // 联系客服（始终可见）
        OutlinedButton(
            onClick = onContactService,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("联系客服")
        }
    }
}

// ── Shared composables ─────────────────────────────────────

/** 信息行：标签 + 值 */
@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(80.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** 彩色标签 Badge */
@Composable
private fun Badge(text: String, backgroundColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = backgroundColor,
        )
    }
}

// ── Helpers ────────────────────────────────────────────────

/** 简单时间戳格式化（ISO 8601 → yyyy-MM-dd HH:mm） */
private fun formatTimestamp(iso: String): String {
    return try {
        val t = iso.replace("T", " ").replace("Z", "")
        // 截取 yyyy-MM-dd HH:mm
        if (t.length >= 16) t.substring(0, 16) else t
    } catch (_: Exception) {
        iso
    }
}

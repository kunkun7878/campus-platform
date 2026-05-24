package com.campus.platform.ui.screen.runner

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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.campus.platform.data.local.mapper.RunnerOrderDto
import com.campus.platform.data.local.mapper.RunnerReviewDto
import com.campus.platform.data.local.mapper.RunnerTaskDto
import com.campus.platform.navigation.CampusRoutes
import com.campus.platform.ui.component.runner.OrderStatusTimeline
import com.campus.platform.ui.component.runner.ORDER_TIMELINE_LABELS
import com.campus.platform.ui.component.runner.RunnerEmptyState
import com.campus.platform.ui.component.runner.RunnerPriceTag
import com.campus.platform.ui.component.runner.TimelineEvent
import com.campus.platform.ui.viewmodel.UiState
import com.campus.platform.ui.viewmodel.runner.AvailableActions
import com.campus.platform.ui.viewmodel.runner.OrderDetailRole
import com.campus.platform.ui.viewmodel.runner.OrderDetailUiData
import com.campus.platform.ui.viewmodel.runner.OrderDetailViewModel

// ── Status color helpers ──────────────────────────────────────

private val StatusColorCompleted = Color(0xFF12B7AE)
private val StatusColorInProgress = Color(0xFF2D6BFF)
private val StatusColorCancelled = Color(0xFFE53935)
private val StatusColorDefault = Color(0xFF757575)

private fun statusBadgeColor(status: String): Color = when (status) {
    "completed" -> StatusColorCompleted
    "delivered" -> StatusColorCompleted
    "delivering" -> StatusColorInProgress
    "accepted" -> StatusColorInProgress
    "cancelled" -> StatusColorCancelled
    "published" -> StatusColorInProgress
    else -> StatusColorDefault
}

private fun statusLabel(status: String): String = when (status) {
    "published" -> "待接单"
    "accepted" -> "待取件"
    "delivering" -> "配送中"
    "delivered" -> "已送达"
    "completed" -> "已完成"
    "cancelled" -> "已取消"
    "after_sale" -> "售后中"
    else -> status
}

private fun runnerTypeLabel(type: String): String = when (type) {
    "pickup" -> "帮取"
    "delivery" -> "帮送"
    "purchase" -> "帮买"
    "universal" -> "万能帮"
    else -> type
}

private fun formatAmount(amount: Int): String = "¥$amount"

private fun formatTime(timestamp: String?): String {
    if (timestamp.isNullOrBlank()) return ""
    return try {
        timestamp.replace("T", " ").take(19)
    } catch (_: Exception) {
        timestamp.take(19)
    }
}

// ── Screen ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    viewModel: OrderDetailViewModel = hiltViewModel(),
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    // ── Unified UI state ──────────────────────────────────────
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // ── One-shot action state ─────────────────────────────────
    val actionInProgress by viewModel.actionInProgress.collectAsStateWithLifecycle()
    val actionError by viewModel.actionError.collectAsStateWithLifecycle()

    // ── Review form state (user input) ────────────────────────
    val rating by viewModel.rating.collectAsStateWithLifecycle()
    val comment by viewModel.comment.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()
    val reviewSubmitted by viewModel.reviewSubmitted.collectAsStateWithLifecycle()

    // Cancel dialog state
    var showCancelDialog by remember { mutableStateOf(false) }
    var cancelReason by remember { mutableStateOf("") }

    // Snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(actionError) {
        actionError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearActionError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("订单详情") },
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
        when (val state = uiState) {
            // ── Loading ─────────────────────────────────────────
            UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            // ── Error ───────────────────────────────────────────
            is UiState.Error -> {
                RunnerEmptyState(
                    title = "加载失败",
                    subtitle = state.message,
                    actionLabel = "重试",
                    onAction = { viewModel.refresh() },
                    modifier = Modifier.padding(paddingValues),
                )
            }

            // ── Success ─────────────────────────────────────────
            is UiState.Success -> {
                val data = state.data

                if (data.isEmpty) {
                    RunnerEmptyState(
                        title = "订单不存在",
                        subtitle = "该订单可能已被删除",
                        modifier = Modifier.padding(paddingValues),
                    )
                } else {
                    OrderDetailContent(
                        data = data,
                        actionInProgress = actionInProgress,
                        rating = rating,
                        comment = comment,
                        isSubmitting = isSubmitting,
                        reviewSubmitted = reviewSubmitted,
                        onAccept = { viewModel.acceptOrder() },
                        onStartDelivery = { viewModel.startDelivery() },
                        onConfirmDelivery = { viewModel.confirmDelivery() },
                        onConfirmReceipt = { viewModel.confirmReceipt() },
                        onCancelClick = { showCancelDialog = true },
                        onApplyAfterSale = {
                            data.order?.let { o ->
                                navController.navigate(CampusRoutes.AfterSaleApply.createRoute(o.id))
                            }
                        },
                        onRatingChange = { viewModel.setRating(it) },
                        onCommentChange = { viewModel.setComment(it) },
                        onSubmitReview = { viewModel.submitReview() },
                        modifier = modifier,
                    )
                }
            }
        }
    }

    // Cancel dialog
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = {
                showCancelDialog = false
                cancelReason = ""
            },
            title = { Text("取消订单") },
            text = {
                Column {
                    Text(
                        text = "确认取消该订单？取消后任务将重新发布。",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = cancelReason,
                        onValueChange = { cancelReason = it },
                        label = { Text("取消原因（选填）") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !actionInProgress,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.cancelOrder(cancelReason)
                        showCancelDialog = false
                        cancelReason = ""
                    },
                    enabled = !actionInProgress,
                ) {
                    Text(
                        text = "确认取消",
                        color = if (actionInProgress)
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else
                            MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCancelDialog = false
                    cancelReason = ""
                }) {
                    Text("返回")
                }
            },
        )
    }
}

// ── OrderDetailContent ────────────────────────────────────────

@Composable
private fun OrderDetailContent(
    data: OrderDetailUiData,
    actionInProgress: Boolean,
    rating: Int,
    comment: String,
    isSubmitting: Boolean,
    reviewSubmitted: Boolean,
    onAccept: () -> Unit,
    onStartDelivery: () -> Unit,
    onConfirmDelivery: () -> Unit,
    onConfirmReceipt: () -> Unit,
    onCancelClick: () -> Unit,
    onApplyAfterSale: () -> Unit,
    onRatingChange: (Int) -> Unit,
    onCommentChange: (String) -> Unit,
    onSubmitReview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Task preview banner (unaccepted task)
        if (data.isTaskPreview) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
            ) {
                Text(
                    text = "该任务暂无跑腿员接单",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                )
            }
        }

        // Task info card
        data.task?.let { t ->
            TaskInfoCard(task = t)
        }

        // Status + Actions card (always show after task info)
        StatusActionsCard(
            order = data.order,
            taskStatus = data.task?.status,
            role = data.role,
            availableActions = data.availableActions,
            actionInProgress = actionInProgress,
            onAccept = onAccept,
            onStartDelivery = onStartDelivery,
            onConfirmDelivery = onConfirmDelivery,
            onConfirmReceipt = onConfirmReceipt,
            onCancelClick = onCancelClick,
            onApplyAfterSale = onApplyAfterSale,
        )

        // Timeline
        if (data.timeline.isNotEmpty()) {
            TimelineCard(events = data.timeline)
        }

        // Review section
        if (data.order?.status == "completed") {
            ReviewSection(
                reviewData = data.review,
                canReview = data.availableActions.canReview,
                reviewSubmitted = reviewSubmitted,
                rating = rating,
                comment = comment,
                isSubmitting = isSubmitting,
                onRatingChange = onRatingChange,
                onCommentChange = onCommentChange,
                onSubmitReview = onSubmitReview,
            )
        }

        // Bottom spacer
        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ── Task Info Card ────────────────────────────────────────────

@Composable
private fun TaskInfoCard(
    task: RunnerTaskDto,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Title + type badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        text = runnerTypeLabel(task.type),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            // Price
            val tipStr = if (task.tip > 0) "+${formatAmount(task.tip)}" else null
            RunnerPriceTag(
                amount = formatAmount(task.price + task.tip),
                tip = tipStr,
            )

            // Pickup address
            if (!task.pickupAddr.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = "取件地址",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = task.pickupAddr,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            // Delivery address
            if (!task.deliveryAddr.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = "送达地址",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = task.deliveryAddr,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            // Description (if any)
            if (!task.description.isNullOrBlank()) {
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Publish time
            if (!task.createdAt.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "发布时间：${formatTime(task.createdAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
            }
        }
    }
}

// ── Status + Actions Card ─────────────────────────────────────

@Composable
private fun StatusActionsCard(
    order: RunnerOrderDto?,
    taskStatus: String? = null,
    role: OrderDetailRole,
    availableActions: AvailableActions,
    actionInProgress: Boolean,
    onAccept: () -> Unit = {},
    onStartDelivery: () -> Unit = {},
    onConfirmDelivery: () -> Unit = {},
    onConfirmReceipt: () -> Unit = {},
    onCancelClick: () -> Unit = {},
    onApplyAfterSale: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // Use order status if available, otherwise fall back to task status
    val effectiveStatus: String = order?.status ?: (taskStatus ?: "")

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Status badge row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = if (order != null) "订单状态" else "任务状态",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (effectiveStatus.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = statusBadgeColor(effectiveStatus).copy(alpha = 0.12f),
                    ) {
                        Text(
                            text = statusLabel(effectiveStatus),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = statusBadgeColor(effectiveStatus),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }

            // Role indicator
            Text(
                text = when (role) {
                    OrderDetailRole.BUYER -> if (order != null) "我的角色：买家" else "我的角色：发布者"
                    OrderDetailRole.RUNNER -> "我的角色：跑腿员"
                    OrderDetailRole.NEITHER -> ""
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Action buttons
            if (actionInProgress) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "处理中...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Accept action (task preview: NEITHER role + published task)
                    if (availableActions.canAccept && role == OrderDetailRole.NEITHER) {
                        Button(
                            onClick = onAccept,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StatusColorInProgress,
                            ),
                        ) {
                            Text("我要接单")
                        }
                    }

                    // Buyer actions
                    if (role == OrderDetailRole.BUYER) {
                        if (availableActions.canConfirmReceipt) {
                            Button(
                                onClick = onConfirmReceipt,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = StatusColorCompleted,
                                ),
                            ) {
                                Text("确认收货")
                            }
                        }
                        if (availableActions.canCancel) {
                            OutlinedButton(
                                onClick = onCancelClick,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = "取消订单",
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                        if (availableActions.canApplyAfterSale) {
                            OutlinedButton(
                                onClick = onApplyAfterSale,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("申请售后")
                            }
                        }
                    }

                    // Runner actions
                    if (role == OrderDetailRole.RUNNER) {
                        if (availableActions.canStartDelivery) {
                            Button(
                                onClick = onStartDelivery,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = StatusColorInProgress,
                                ),
                            ) {
                                Text("确认取件")
                            }
                        }
                        if (availableActions.canConfirmDelivery) {
                            Button(
                                onClick = onConfirmDelivery,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = StatusColorCompleted,
                                ),
                            ) {
                                Text("确认送达")
                            }
                        }
                        if (availableActions.canCancel) {
                            OutlinedButton(
                                onClick = onCancelClick,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = "取消订单",
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }

                    // No actions available
                    if (!availableActions.canAccept &&
                        !availableActions.canStartDelivery &&
                        !availableActions.canConfirmDelivery &&
                        !availableActions.canConfirmReceipt &&
                        !availableActions.canCancel &&
                        !availableActions.canApplyAfterSale
                    ) {
                        Text(
                            text = when (effectiveStatus) {
                                "published" -> "等待跑腿员接单"
                                "accepted" -> "等待跑腿员取件"
                                "delivering" -> "跑腿员配送中，请耐心等待"
                                "delivered" -> "已送达，请及时确认收货"
                                "completed" -> "该订单已完成"
                                "cancelled" -> "该订单已取消"
                                "after_sale" -> "该订单处于售后中"
                                else -> "暂无可用操作"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

// ── Timeline Card ─────────────────────────────────────────────

@Composable
private fun TimelineCard(
    events: List<TimelineEvent>,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "订单动态",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            OrderStatusTimeline(
                events = events,
                eventLabelMap = ORDER_TIMELINE_LABELS,
            )
        }
    }
}

// ── Review Section ────────────────────────────────────────────

@Composable
private fun ReviewSection(
    reviewData: RunnerReviewDto?,
    canReview: Boolean,
    reviewSubmitted: Boolean,
    rating: Int,
    comment: String,
    isSubmitting: Boolean,
    onRatingChange: (Int) -> Unit,
    onCommentChange: (String) -> Unit,
    onSubmitReview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "评价",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            when {
                // Already has review data — read-only display
                reviewData != null -> {
                    ReadOnlyReview(review = reviewData)
                }

                // Submitted — confirmation
                reviewSubmitted -> {
                    Text(
                        text = "感谢你的评价！",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = StatusColorCompleted,
                    )
                }

                // Can review — show form
                canReview -> {
                    ReviewForm(
                        rating = rating,
                        comment = comment,
                        isSubmitting = isSubmitting,
                        onRatingChange = onRatingChange,
                        onCommentChange = onCommentChange,
                        onSubmitReview = onSubmitReview,
                    )
                }

                // Not the right role or already reviewed
                else -> {
                    Text(
                        text = "暂无评价",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ── Read-only Review ──────────────────────────────────────────

@Composable
private fun ReadOnlyReview(
    review: RunnerReviewDto,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Star rating read-only
        Row(verticalAlignment = Alignment.CenterVertically) {
            repeat(5) { index ->
                Icon(
                    imageVector = if (index < review.rating) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (index < review.rating)
                        Color(0xFFFFB800)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${review.rating}/5",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Comment
        if (!review.comment.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ) {
                Text(
                    text = review.comment,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }

        // Time
        if (!review.createdAt.isNullOrBlank()) {
            Text(
                text = "评价时间：${formatTime(review.createdAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        }
    }
}

// ── Review Form ───────────────────────────────────────────────

@Composable
private fun ReviewForm(
    rating: Int,
    comment: String,
    isSubmitting: Boolean,
    onRatingChange: (Int) -> Unit,
    onCommentChange: (String) -> Unit,
    onSubmitReview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Star rating input
        Text(
            text = "评分",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            repeat(5) { index ->
                val starValue = index + 1
                IconButton(
                    onClick = { onRatingChange(starValue) },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = if (starValue <= rating) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = "${starValue}星",
                        modifier = Modifier.size(28.dp),
                        tint = if (starValue <= rating)
                            Color(0xFFFFB800)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (rating > 0) {
                Text(
                    text = "${rating}/5",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFFFB800),
                )
            }
        }

        // Comment input
        OutlinedTextField(
            value = comment,
            onValueChange = onCommentChange,
            label = { Text("评价内容（选填）") },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            maxLines = 4,
            enabled = !isSubmitting,
        )

        // Submit button
        Button(
            onClick = onSubmitReview,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSubmitting && rating > 0,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(if (isSubmitting) "提交中..." else "提交评价")
        }
    }
}

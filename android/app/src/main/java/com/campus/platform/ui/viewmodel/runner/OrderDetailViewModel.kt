package com.campus.platform.ui.viewmodel.runner

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.platform.data.auth.AuthRepository
import com.campus.platform.data.local.mapper.RunnerOrderDto
import com.campus.platform.data.local.mapper.RunnerReviewDto
import com.campus.platform.data.local.mapper.RunnerTaskDto
import com.campus.platform.domain.repository.IOrderTimelineRepository
import com.campus.platform.domain.repository.IRunnerOrderRepository
import com.campus.platform.domain.repository.IRunnerReviewRepository
import com.campus.platform.domain.repository.IRunnerTaskRepository
import com.campus.platform.ui.component.runner.TimelineEvent
import com.campus.platform.ui.viewmodel.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

private const val TAG = "OrderDetailVM"

// ── Role & Actions ────────────────────────────────────────────

enum class OrderDetailRole { BUYER, RUNNER, NEITHER }

data class AvailableActions(
    val canStartDelivery: Boolean = false,
    val canConfirmDelivery: Boolean = false,
    val canConfirmReceipt: Boolean = false,
    val canCancel: Boolean = false,
    val canReview: Boolean = false,
    val canApplyAfterSale: Boolean = false,
    val canAccept: Boolean = false,
)

// ── Unified UI data ───────────────────────────────────────────

data class OrderDetailUiData(
    val order: RunnerOrderDto? = null,
    val task: RunnerTaskDto? = null,
    val timeline: List<TimelineEvent> = emptyList(),
    val review: RunnerReviewDto? = null,
    val isTaskPreview: Boolean = false,
    val availableActions: AvailableActions = AvailableActions(),
    val role: OrderDetailRole = OrderDetailRole.NEITHER,
    val isEmpty: Boolean = false,
)

// ── ViewModel ─────────────────────────────────────────────────

@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    private val taskRepo: IRunnerTaskRepository,
    private val orderRepo: IRunnerOrderRepository,
    private val timelineRepo: IOrderTimelineRepository,
    private val reviewRepo: IRunnerReviewRepository,
    private val authRepository: AuthRepository,
    private val supabase: SupabaseClient,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val orderId: String = savedStateHandle.get<String>("orderId") ?: ""

    // ── Unified UI state ──────────────────────────────────────

    private val _uiState = MutableStateFlow<UiState<OrderDetailUiData>>(UiState.Loading)
    val uiState: StateFlow<UiState<OrderDetailUiData>> = _uiState.asStateFlow()

    // ── One-shot action state (不适合放入 UiState) ─────────────

    private val _actionInProgress = MutableStateFlow(false)
    val actionInProgress: StateFlow<Boolean> = _actionInProgress.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    // ── Review form state (纯用户输入，不适合放入 UiState) ─────

    private val _rating = MutableStateFlow(0)
    val rating: StateFlow<Int> = _rating.asStateFlow()

    private val _comment = MutableStateFlow("")
    val comment: StateFlow<String> = _comment.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _reviewSubmitted = MutableStateFlow(false)
    val reviewSubmitted: StateFlow<Boolean> = _reviewSubmitted.asStateFlow()

    // ── Timeline job for cancellation ─────────────────────────

    private var timelineJob: Job? = null

    // ── Init ──────────────────────────────────────────────────

    init {
        if (orderId.isEmpty()) {
            _uiState.value = UiState.Success(OrderDetailUiData(isEmpty = true))
        } else {
            loadData()
        }
    }

    // ── Data loading ──────────────────────────────────────────

    fun refresh() {
        loadData()
    }

    private fun loadData() {
        // Load initial order + task data
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            try {
                val loadedOrder = orderRepo.getOrderById(orderId)
                if (loadedOrder == null) {
                    // 尝试将其作为 taskId 查找任务（已发布无人接单的回退路径）
                    val taskByOrderId = taskRepo.getTaskById(orderId)
                    if (taskByOrderId != null) {
                        val currentUserId = authRepository.currentUserId()
                        val role = when {
                            currentUserId == taskByOrderId.publisherId -> OrderDetailRole.BUYER
                            else -> OrderDetailRole.NEITHER
                        }
                        _uiState.value = UiState.Success(
                            OrderDetailUiData(
                                task = taskByOrderId,
                                isTaskPreview = true,
                                role = role,
                                availableActions = computeActionsForStatus(
                                    status = taskByOrderId.status,
                                    role = role,
                                    review = null,
                                ),
                            )
                        )
                        return@launch
                    }
                    _uiState.value = UiState.Success(OrderDetailUiData(isEmpty = true))
                    return@launch
                }

                // Order found — load associated task
                val loadedTask = taskRepo.getTaskById(loadedOrder.taskId)
                val currentUserId = authRepository.currentUserId()
                val role = when {
                    currentUserId == loadedOrder.buyerId -> OrderDetailRole.BUYER
                    currentUserId == loadedOrder.runnerId -> OrderDetailRole.RUNNER
                    else -> OrderDetailRole.NEITHER
                }

                _uiState.value = UiState.Success(
                    OrderDetailUiData(
                        order = loadedOrder,
                        task = loadedTask,
                        role = role,
                        availableActions = computeActionsForStatus(
                            status = loadedOrder.status,
                            role = role,
                            review = null,
                        ),
                    )
                )

                // Start observing timeline (separate Flow)
                observeTimeline(orderId)

            } catch (e: Exception) {
                Log.e(TAG, "加载订单详情失败", e)
                _uiState.value = UiState.Error("加载失败，请稍后重试")
            }
        }

        // Review loads via Flow on a separate coroutine
        viewModelScope.launch {
            val currentUserId = authRepository.currentUserId()
            reviewRepo.getReviewsByOrder(orderId).collectLatest { reviews ->
                val review = reviews.firstOrNull { it.reviewerId == currentUserId }
                updateSuccess { current ->
                    val order = current.order
                    if (order != null) {
                        current.copy(
                            review = review,
                            availableActions = computeActionsForStatus(
                                status = order.status,
                                role = current.role,
                                review = review,
                            ),
                        )
                    } else {
                        current.copy(review = review)
                    }
                }
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────

    /**
     * Pure function: compute available actions from status, role, and existing review.
     */
    private fun computeActionsForStatus(
        status: String,
        role: OrderDetailRole,
        review: RunnerReviewDto?,
    ): AvailableActions = when (role) {
        OrderDetailRole.BUYER -> AvailableActions(
            canCancel = status in setOf("accepted", "delivering"),
            canConfirmReceipt = status == "delivered",
            canReview = status == "completed" && review == null,
            canApplyAfterSale = status in setOf("delivered", "completed"),
        )
        OrderDetailRole.RUNNER -> AvailableActions(
            canStartDelivery = status == "accepted",
            canConfirmDelivery = status == "delivering",
            canCancel = status in setOf("accepted", "delivering"),
            canReview = status == "completed" && review == null,
        )
        OrderDetailRole.NEITHER -> AvailableActions(
            canAccept = status == "published",
        )
    }

    /**
     * Update the current UiState.Success data in-place.
     * No-op if the current state is not Success.
     */
    private fun updateSuccess(transform: (OrderDetailUiData) -> OrderDetailUiData) {
        val current = _uiState.value
        if (current is UiState.Success) {
            _uiState.value = UiState.Success(transform(current.data))
        }
    }

    /**
     * Start observing timeline updates from the repository Flow.
     * Cancels any previous timeline job.
     */
    private fun observeTimeline(targetOrderId: String) {
        timelineJob?.cancel()
        timelineJob = viewModelScope.launch {
            timelineRepo.getTimelineByOrderId(targetOrderId).collectLatest { dtos ->
                val events = dtos.map { dto ->
                    TimelineEvent(
                        event = dto.event,
                        description = dto.description,
                        timestamp = dto.createdAt ?: "",
                        isCompleted = true,
                    )
                }
                updateSuccess { it.copy(timeline = events) }
            }
        }
    }

    // ── Status actions (Edge Function) ────────────────────────

    fun startDelivery() {
        callLifecycle("start_delivery")
    }

    fun confirmDelivery() {
        callLifecycle("confirm_delivery")
    }

    fun confirmReceipt() {
        callLifecycle("confirm_receipt")
    }

    fun cancelOrder(reason: String) {
        callLifecycle("cancel", reason = reason)
    }

    fun acceptOrder() {
        val data = (_uiState.value as? UiState.Success)?.data ?: return
        val task = data.task ?: return
        viewModelScope.launch {
            _actionInProgress.value = true
            _actionError.value = null

            try {
                val body = buildJsonObject {
                    put("action", "accept")
                    put("task_id", task.id)
                }

                supabase.functions.invoke("runner-order-lifecycle", body = body)

                // 接单成功后刷新数据
                taskRepo.refreshTask(task.id)
                val updatedTask = taskRepo.getTaskById(task.id)

                orderRepo.refreshOrders(task.schoolId)

                // 查找新创建的订单
                val orders = orderRepo.getOrdersByTaskId(task.id).first()
                val newOrder = orders.firstOrNull()
                if (newOrder != null) {
                    val currentUserId = authRepository.currentUserId()
                    val role = when {
                        currentUserId == newOrder.buyerId -> OrderDetailRole.BUYER
                        currentUserId == newOrder.runnerId -> OrderDetailRole.RUNNER
                        else -> OrderDetailRole.NEITHER
                    }
                    updateSuccess {
                        it.copy(
                            order = newOrder,
                            task = updatedTask,
                            isTaskPreview = false,
                            role = role,
                            availableActions = computeActionsForStatus(
                                status = newOrder.status,
                                role = role,
                                review = null,
                            ),
                        )
                    }

                    // 加载新订单的时间线
                    timelineRepo.refreshTimeline(newOrder.id)
                    observeTimeline(newOrder.id)
                }
            } catch (e: Exception) {
                Log.e(TAG, "接单失败", e)
                _actionError.value = "接单失败，请稍后重试"
            } finally {
                _actionInProgress.value = false
            }
        }
    }

    private fun callLifecycle(action: String, reason: String? = null) {
        viewModelScope.launch {
            _actionInProgress.value = true
            _actionError.value = null

            try {
                val body = buildJsonObject {
                    put("action", action)
                    put("order_id", orderId)
                    if (!reason.isNullOrBlank()) {
                        put("reason", reason)
                        put("cancel_reason", reason)
                    }
                }

                supabase.functions.invoke("runner-order-lifecycle", body = body)
                // invoke() throws on non-2xx; HTTP 200 = success
                refreshOrder()
            } catch (e: Exception) {
                Log.e(TAG, "订单操作失败", e)
                _actionError.value = "操作异常，请稍后重试"
            } finally {
                _actionInProgress.value = false
            }
        }
    }

    // ── Review ────────────────────────────────────────────────

    fun setRating(value: Int) {
        _rating.value = value.coerceIn(1, 5)
    }

    fun setComment(value: String) {
        _comment.value = value
    }

    fun submitReview() {
        val data = (_uiState.value as? UiState.Success)?.data ?: return
        val order = data.order ?: return
        val currentRating = _rating.value
        if (currentRating < 1 || currentRating > 5) {
            _actionError.value = "请选择评分"
            return
        }

        viewModelScope.launch {
            _isSubmitting.value = true
            _actionError.value = null

            try {
                val currentUserId = authRepository.currentUserId() ?: return@launch
                val revieweeId = if (data.role == OrderDetailRole.BUYER) {
                    order.runnerId
                } else {
                    order.buyerId
                }

                val review = RunnerReviewDto(
                    id = UUID.randomUUID().toString(),
                    orderId = orderId,
                    reviewerId = currentUserId,
                    revieweeId = revieweeId,
                    rating = currentRating,
                    comment = _comment.value.ifBlank { null },
                    schoolId = order.schoolId,
                    createdAt = null,
                    updatedAt = null,
                )

                reviewRepo.createReview(review)
                _reviewSubmitted.value = true
            } catch (e: Exception) {
                Log.e(TAG, "提交评价失败", e)
                _actionError.value = "评价提交失败，请稍后重试"
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────

    private suspend fun refreshOrder() {
        // 1) 强制从 Supabase 拉取最新 order + task，刷新 Room 缓存
        orderRepo.refreshOrder(orderId)
        val updatedOrder = orderRepo.getOrderById(orderId)
        if (updatedOrder != null) {
            taskRepo.refreshTask(updatedOrder.taskId)
            val updatedTask = taskRepo.getTaskById(updatedOrder.taskId)
            val role = (_uiState.value as? UiState.Success)?.data?.role
                ?: OrderDetailRole.NEITHER
            val review = (_uiState.value as? UiState.Success)?.data?.review
            updateSuccess {
                it.copy(
                    order = updatedOrder,
                    task = updatedTask,
                    availableActions = computeActionsForStatus(
                        status = updatedOrder.status,
                        role = role,
                        review = review,
                    ),
                )
            }
        }
        // 2) 刷新时间线
        timelineRepo.refreshTimeline(orderId)
    }

    fun clearActionError() {
        _actionError.value = null
    }

    override fun onCleared() {
        super.onCleared()
        timelineJob?.cancel()
    }
}

package com.campus.platform.ui.viewmodel.runner

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.platform.data.auth.AuthRepository
import com.campus.platform.data.local.UserPreferencesDataStore
import com.campus.platform.data.local.mapper.RunnerOrderDto
import com.campus.platform.data.local.mapper.RunnerTaskDto
import com.campus.platform.domain.repository.IRunnerOrderRepository
import com.campus.platform.domain.repository.IRunnerReviewRepository
import com.campus.platform.domain.repository.IRunnerTaskRepository
import com.campus.platform.ui.viewmodel.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "OrderListVM"

// ── Display models ──────────────────────────────────────────

/** 订单列表统一展示项，合并 Task 与 Order 信息。 */
data class OrderListItem(
    /** 导航用 ID：有订单时用 orderId，否则用 taskId */
    val navigateId: String,
    /** 是否存在关联订单（发布任务且暂无接单人时为 false） */
    val hasOrder: Boolean,
    val title: String,
    val taskType: String,
    val typeLabel: String,
    val status: String,
    val statusLabel: String,
    val amount: String,
    val time: String?,
    /** 当前用户是否待评价（订单已完成 且 当前用户尚未提交评价） */
    val isPendingReview: Boolean,
    /** 是否处于售后中 */
    val isAfterSale: Boolean,
)

/** 订单列表 UI 状态数据 */
data class OrderListUiData(
    val orders: List<OrderListItem>,
    val activeTab: Int,       // 0=我发布的, 1=我接的单
    val activeFilter: Int,    // 各 tab 内的筛选索引
    val hasReviewed: Set<String>,
)

// ── Internal combine payload ────────────────────────────────

private data class CombineParams(
    val isLoggedIn: Boolean,
    val tab: String,
    val filter: String,
    val reviewedIds: Set<String>,
)

// ── ViewModel ───────────────────────────────────────────────

@HiltViewModel
class OrderListViewModel @Inject constructor(
    private val taskRepo: IRunnerTaskRepository,
    private val orderRepo: IRunnerOrderRepository,
    private val reviewRepo: IRunnerReviewRepository,
    private val authRepository: AuthRepository,
    private val prefs: UserPreferencesDataStore,
) : ViewModel() {

    // ── Control StateFlows ────────────────────────────────

    private val _activeTab = MutableStateFlow("published")
    private val _activeFilter = MutableStateFlow("all")
    private val _refreshTrigger = MutableStateFlow(0)

    /** 当前用户已评价的订单 ID 集合，用于判断"待评价"pill */
    private val reviewedOrderIds = MutableStateFlow<Set<String>>(emptySet())

    // ── UI State ──────────────────────────────────────────

    private val _uiState =
        MutableStateFlow<UiState<OrderListUiData>>(UiState.Loading)
    val uiState: StateFlow<UiState<OrderListUiData>> = _uiState.asStateFlow()

    init {
        // 加载当前用户已评价的订单 IDs
        viewModelScope.launch {
            authRepository.isLoggedInFlow.collectLatest { isLoggedIn ->
                if (isLoggedIn) {
                    val userId = authRepository.currentUserId() ?: return@collectLatest
                    reviewRepo.getReviewsByReviewer(userId).collectLatest { reviews ->
                        reviewedOrderIds.value = reviews.map { it.orderId }.toSet()
                    }
                } else {
                    reviewedOrderIds.value = emptySet()
                }
            }
        }

        // 5 源 combine 管道：isLoggedIn, tab, filter, refresh, reviewedIds
        viewModelScope.launch {
            combine(
                authRepository.isLoggedInFlow,
                _activeTab,
                _activeFilter,
                _refreshTrigger,
                reviewedOrderIds,
            ) { isLoggedIn, tab, filter, _, reviewedIds ->
                CombineParams(isLoggedIn, tab, filter, reviewedIds)
            }.collectLatest { (isLoggedIn, tab, filter, reviewedIds) ->
                if (!isLoggedIn) {
                    _uiState.value = UiState.Success(buildUiData(emptyList(), reviewedIds))
                    return@collectLatest
                }

                val userId = authRepository.currentUserId() ?: run {
                    _uiState.value = UiState.Success(buildUiData(emptyList(), reviewedIds))
                    return@collectLatest
                }

                _uiState.value = UiState.Loading

                try {
                    when (tab) {
                        "published" -> loadPublishedTasks(userId, filter, reviewedIds)
                        "bought" -> loadBoughtOrders(userId, filter, reviewedIds)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "加载订单列表失败", e)
                    _uiState.value = UiState.Error("加载失败，请稍后重试")
                }
            }
        }
    }

    // ── Actions ────────────────────────────────────────────

    fun setTab(tab: String) {
        _activeTab.value = tab
        _activeFilter.value = "all"
    }

    fun setFilter(filter: String) {
        _activeFilter.value = filter
    }

    fun refresh() {
        _refreshTrigger.value += 1
    }

    // ── Data loading ───────────────────────────────────────

    private suspend fun loadPublishedTasks(
        userId: String,
        filter: String,
        reviewedIds: Set<String>,
    ) {
        val schoolId = prefs.schoolId.first() ?: run {
            _uiState.value = UiState.Success(buildUiData(emptyList(), reviewedIds))
            return
        }
        taskRepo.getTasksByPublisher(userId, schoolId).collect { tasks ->
            val prefiltered = applyTaskFilter(tasks, filter)
            val items = prefiltered.map { task ->
                val order = orderRepo.getOrdersByTaskId(task.id).firstOrNull()?.firstOrNull()
                taskToItem(task, order, reviewedIds)
            }
            val finalItems = when (filter) {
                "after_sale" -> items.filter { it.isAfterSale }
                else -> items
            }
            _uiState.value = UiState.Success(buildUiData(finalItems, reviewedIds))
        }
    }

    private suspend fun loadBoughtOrders(
        userId: String,
        filter: String,
        reviewedIds: Set<String>,
    ) {
        val schoolId = prefs.schoolId.first() ?: run {
            _uiState.value = UiState.Success(buildUiData(emptyList(), reviewedIds))
            return
        }
        orderRepo.getOrdersByRunner(userId, schoolId).collect { orders ->
            val filtered = applyOrderFilter(orders, filter)
            val items = filtered.map { order ->
                val task = taskRepo.getTaskById(order.taskId)
                orderToItem(order, task, reviewedIds)
            }
            _uiState.value = UiState.Success(buildUiData(items, reviewedIds))
        }
    }

    // ── Helpers ────────────────────────────────────────────

    private fun buildUiData(
        orders: List<OrderListItem>,
        reviewedIds: Set<String>,
    ): OrderListUiData = OrderListUiData(
        orders = orders,
        activeTab = tabToIndex(_activeTab.value),
        activeFilter = filterToIndex(_activeFilter.value),
        hasReviewed = reviewedIds,
    )

    private fun tabToIndex(tab: String): Int = when (tab) {
        "bought" -> 1
        else -> 0
    }

    private fun filterToIndex(filter: String): Int = when (filter) {
        "in_progress" -> 1
        "completed" -> 2
        "after_sale" -> 3
        else -> 0
    }

    // ── 筛选逻辑 ───────────────────────────────────────────

    private fun applyTaskFilter(
        tasks: List<RunnerTaskDto>,
        filter: String,
    ): List<RunnerTaskDto> = when (filter) {
        "all" -> tasks
        "in_progress" -> tasks.filter { it.status in IN_PROGRESS_TASK_STATUSES }
        "completed" -> tasks.filter { it.status == "completed" }
        else -> tasks
    }

    private fun applyOrderFilter(
        orders: List<RunnerOrderDto>,
        filter: String,
    ): List<RunnerOrderDto> = when (filter) {
        "all" -> orders
        "in_progress" -> orders.filter { it.status in IN_PROGRESS_ORDER_STATUSES }
        "completed" -> orders.filter { it.status in COMPLETED_ORDER_STATUSES }
        "after_sale" -> orders.filter { it.status == "after_sale" }
        else -> orders
    }

    // ── 映射 ───────────────────────────────────────────────

    private fun taskToItem(
        task: RunnerTaskDto,
        order: RunnerOrderDto?,
        reviewedOrderIds: Set<String>,
    ): OrderListItem {
        val orderId = order?.id
        return OrderListItem(
            navigateId = orderId ?: task.id,
            hasOrder = order != null,
            title = task.title,
            taskType = task.type,
            typeLabel = runnerTypeLabel(task.type),
            status = task.status,
            statusLabel = taskStatusLabel(task.status),
            amount = formatAmount(task.price + task.tip),
            time = task.createdAt,
            isPendingReview = task.status == "completed"
                && orderId != null
                && orderId !in reviewedOrderIds,
            isAfterSale = order?.status == "after_sale",
        )
    }

    private fun orderToItem(
        order: RunnerOrderDto,
        task: RunnerTaskDto?,
        reviewedOrderIds: Set<String>,
    ): OrderListItem {
        return OrderListItem(
            navigateId = order.id,
            hasOrder = true,
            title = task?.title ?: "未知任务",
            taskType = task?.type ?: "universal",
            typeLabel = runnerTypeLabel(task?.type ?: "universal"),
            status = order.status,
            statusLabel = orderStatusLabel(order.status),
            amount = formatAmount((task?.price ?: 0) + (task?.tip ?: 0)),
            time = order.createdAt,
            isPendingReview = order.status == "completed"
                && order.id !in reviewedOrderIds,
            isAfterSale = order.status == "after_sale",
        )
    }

    // ── 静态工具 ───────────────────────────────────────────

    companion object {
        private val IN_PROGRESS_TASK_STATUSES = setOf("assigned", "in_progress")
        private val IN_PROGRESS_ORDER_STATUSES = setOf("accepted", "delivering")
        private val COMPLETED_ORDER_STATUSES = setOf("delivered", "completed")

        fun runnerTypeLabel(type: String): String = when (type) {
            "pickup" -> "帮取"
            "delivery" -> "帮送"
            "purchase" -> "帮买"
            "universal" -> "万能帮"
            else -> type
        }

        fun taskStatusLabel(status: String): String = when (status) {
            "published" -> "待接单"
            "assigned" -> "已接单"
            "in_progress" -> "配送中"
            "completed" -> "已完成"
            "cancelled" -> "已取消"
            else -> status
        }

        fun orderStatusLabel(status: String): String = when (status) {
            "accepted" -> "待配送"
            "delivering" -> "配送中"
            "delivered" -> "已送达"
            "completed" -> "已完成"
            "cancelled" -> "已取消"
            "after_sale" -> "售后中"
            else -> status
        }

        fun formatAmount(amount: Int): String = "¥$amount"
    }
}

package com.campus.platform.ui.viewmodel.runner

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.platform.data.auth.AuthRepository
import com.campus.platform.data.local.mapper.AfterSaleDto
import com.campus.platform.domain.repository.IAfterSaleRepository
import com.campus.platform.domain.repository.IAfterSaleTimelineRepository
import com.campus.platform.domain.repository.IRunnerOrderRepository
import com.campus.platform.domain.repository.IRunnerTaskRepository
import com.campus.platform.ui.component.runner.TimelineEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

private const val TAG = "AfterSaleDetailVM"

// ── User role ──────────────────────────────────────────────

enum class AfterSaleUserRole {
    REQUESTER,
    RUNNER,
    AGENT,
    OTHER,
}

// ── Order summary ──────────────────────────────────────────

data class AfterSaleOrderSummary(
    val orderId: String,
    val typeLabel: String,
    val amount: String,
)

// ── UI state ───────────────────────────────────────────────

sealed interface AfterSaleDetailUiState {
    data object Loading : AfterSaleDetailUiState
    data class Success(
        val afterSale: AfterSaleDto,
        val orderSummary: AfterSaleOrderSummary,
        val timelineEvents: List<TimelineEvent>,
        val userRole: AfterSaleUserRole,
        val canOperate: Boolean,
    ) : AfterSaleDetailUiState
    data class Error(val message: String) : AfterSaleDetailUiState
    data object Empty : AfterSaleDetailUiState
}

// ── After-sale type label ──────────────────────────────────

private val AFTER_SALE_TYPE_LABEL = mapOf(
    "refund" to "退款",
    "return" to "退货",
    "complaint" to "投诉",
)

private val AFTER_SALE_STATUS_LABEL = mapOf(
    "pending" to "待处理",
    "processing" to "处理中",
    "approved" to "已通过",
    "rejected" to "已驳回",
    "completed" to "已完成",
)

/** 终端售后状态，此类状态下所有时间线节点均应标记为已完成。 */
private val TERMINAL_AFTER_SALE_STATUS = setOf("approved", "rejected", "completed")

@HiltViewModel
class AfterSaleDetailViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val afterSaleRepository: IAfterSaleRepository,
    private val timelineRepository: IAfterSaleTimelineRepository,
    private val runnerOrderRepository: IRunnerOrderRepository,
    private val runnerTaskRepository: IRunnerTaskRepository,
    private val authRepository: AuthRepository,
    private val supabase: SupabaseClient,
) : ViewModel() {

    private val saleId: String = savedStateHandle.get<String>("saleId") ?: ""

    private val _uiState = MutableStateFlow<AfterSaleDetailUiState>(AfterSaleDetailUiState.Loading)
    val uiState: StateFlow<AfterSaleDetailUiState> = _uiState.asStateFlow()

    // ── Supplement state ───────────────────────────────────────

    private val _showSupplementSheet = MutableStateFlow(false)
    val showSupplementSheet: StateFlow<Boolean> = _showSupplementSheet.asStateFlow()

    private val _supplementText = MutableStateFlow("")
    val supplementText: StateFlow<String> = _supplementText.asStateFlow()

    private val _isSupplementSending = MutableStateFlow(false)
    val isSupplementSending: StateFlow<Boolean> = _isSupplementSending.asStateFlow()

    private val _supplementMessage = MutableStateFlow<String?>(null)
    val supplementMessage: StateFlow<String?> = _supplementMessage.asStateFlow()

    init {
        if (saleId.isEmpty()) {
            _uiState.value = AfterSaleDetailUiState.Error("参数错误：缺少售后单号")
        } else {
            loadDetail()
        }
    }

    // ── Public actions ──────────────────────────────────────

    fun retry() {
        loadDetail()
    }

    // ── Label helpers ───────────────────────────────────────

    fun afterSaleTypeLabel(type: String): String =
        AFTER_SALE_TYPE_LABEL[type] ?: type

    fun afterSaleStatusLabel(status: String): String =
        AFTER_SALE_STATUS_LABEL[status] ?: status

    // ── Supplement ──────────────────────────────────────────

    fun openSupplementSheet() {
        _showSupplementSheet.value = true
        _supplementText.value = ""
    }

    fun closeSupplementSheet() {
        _showSupplementSheet.value = false
    }

    fun onSupplementTextChange(text: String) {
        _supplementText.value = text
    }

    fun submitSupplement() {
        val text = _supplementText.value.trim()
        if (text.isBlank()) {
            _supplementMessage.value = "请输入补充说明内容"
            return
        }
        viewModelScope.launch {
            _isSupplementSending.value = true
            try {
                val body = buildJsonObject {
                    put("action", "supplement")
                    put("after_sale_id", saleId)
                    put("description", text)
                    put("operator_id", authRepository.currentUserId() ?: "")
                }
                supabase.functions.invoke("runner-after-sale", body = body)
                _showSupplementSheet.value = false
                _supplementMessage.value = "补充说明已提交"
                // Refresh detail
                loadDetail()
            } catch (e: Exception) {
                Log.e(TAG, "提交补充说明失败", e)
                _supplementMessage.value = "提交失败，请稍后重试"
            } finally {
                _isSupplementSending.value = false
            }
        }
    }

    fun clearSupplementMessage() {
        _supplementMessage.value = null
    }

    // ── Private ─────────────────────────────────────────────

    private fun loadDetail() {
        viewModelScope.launch {
            _uiState.value = AfterSaleDetailUiState.Loading
            try {
                // 1. 售后信息
                val afterSale = afterSaleRepository.getAfterSaleById(saleId)
                if (afterSale == null) {
                    _uiState.value = AfterSaleDetailUiState.Empty
                    return@launch
                }

                // 2. 关联订单摘要（order + task）
                val order = runnerOrderRepository.getOrderById(afterSale.orderId)
                val task = order?.let { runnerTaskRepository.getTaskById(it.taskId) }
                val typeLabel = task?.let { OrderListViewModel.runnerTypeLabel(it.type) } ?: "跑腿"
                val amount = task?.let {
                    val total = it.price + it.tip
                    OrderListViewModel.formatAmount(total)
                } ?: "--"
                val orderSummary = AfterSaleOrderSummary(
                    orderId = afterSale.orderId,
                    typeLabel = typeLabel,
                    amount = amount,
                )

                // 3. 售后时间线
                val timelineDtos = timelineRepository.getTimelineByAfterSaleId(saleId).first()
                val isTerminal = afterSale.status in TERMINAL_AFTER_SALE_STATUS
                val timelineEvents = timelineDtos.mapIndexed { index, dto ->
                    val isLast = index == timelineDtos.lastIndex
                    TimelineEvent(
                        event = dto.event,
                        description = dto.description,
                        timestamp = dto.createdAt ?: "--",
                        // 终端状态下全部标记完成；非终端状态下最后一条为「当前进行中」
                        isCompleted = isTerminal || !isLast,
                    )
                }

                // 4. 当前用户角色
                val currentUserId = authRepository.currentUserId()
                val userRole = resolveUserRole(
                    currentUserId = currentUserId,
                    requesterId = afterSale.requesterId,
                    orderRunnerId = order?.runnerId,
                )

                // 5. 是否可操作（requester + pending）
                val canOperate = userRole == AfterSaleUserRole.REQUESTER
                    && afterSale.status == "pending"

                _uiState.value = AfterSaleDetailUiState.Success(
                    afterSale = afterSale,
                    orderSummary = orderSummary,
                    timelineEvents = timelineEvents,
                    userRole = userRole,
                    canOperate = canOperate,
                )
            } catch (e: Exception) {
                Log.e(TAG, "加载售后详情失败", e)
                _uiState.value = AfterSaleDetailUiState.Error("加载失败，请稍后重试")
            }
        }
    }

    private suspend fun resolveUserRole(
        currentUserId: String?,
        requesterId: String,
        orderRunnerId: String?,
    ): AfterSaleUserRole {
        if (currentUserId == null) return AfterSaleUserRole.OTHER
        return when {
            currentUserId == requesterId -> AfterSaleUserRole.REQUESTER
            orderRunnerId != null && currentUserId == orderRunnerId -> AfterSaleUserRole.RUNNER
            else -> {
                val profile = authRepository.getProfile()
                if (profile?.isAgent == true) AfterSaleUserRole.AGENT
                else AfterSaleUserRole.OTHER
            }
        }
    }
}

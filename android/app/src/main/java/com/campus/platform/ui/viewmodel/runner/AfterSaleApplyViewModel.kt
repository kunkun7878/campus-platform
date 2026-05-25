package com.campus.platform.ui.viewmodel.runner

import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.platform.data.auth.AuthRepository
import com.campus.platform.data.local.mapper.AfterSaleDto
import com.campus.platform.domain.repository.IAfterSaleRepository
import com.campus.platform.domain.repository.IImageUploadRepository
import com.campus.platform.domain.repository.IRunnerOrderRepository
import com.campus.platform.domain.repository.IRunnerTaskRepository
import com.campus.platform.domain.repository.IUserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

private const val TAG = "AfterSaleApplyVM"

// ── Form state ─────────────────────────────────────────────

data class AfterSaleApplyFormState(
    val orderId: String = "",
    val afterSaleType: String = "refund",
    val reason: String = "",
    val selectedUris: List<Uri> = emptyList(),
    val uploadedUrls: List<String> = emptyList(),
    val isUploading: Boolean = false,
    val isLoading: Boolean = false,
    val isSummaryLoading: Boolean = false,
    val error: String? = null,
)

// ── Order summary data ─────────────────────────────────────

data class OrderSummary(
    val orderId: String = "",
    val typeLabel: String = "",
    val amount: String = "",
    val runnerName: String = "待派单",
)

// ── UI state ───────────────────────────────────────────────

sealed interface AfterSaleApplyUiState {
    data object Idle : AfterSaleApplyUiState
    data class Success(val afterSaleId: String) : AfterSaleApplyUiState
    data class Error(val message: String) : AfterSaleApplyUiState
}

// ── Type labels ────────────────────────────────────────────

private val TYPE_LABEL_MAP = mapOf(
    "pickup" to "帮取",
    "delivery" to "帮送",
    "purchase" to "帮买",
    "universal" to "万能帮",
)

private val AFTER_SALE_TYPE_LABELS = listOf(
    "refund" to "退款",
    "return" to "退货",
    "complaint" to "投诉",
)

@HiltViewModel
class AfterSaleApplyViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val afterSaleRepository: IAfterSaleRepository,
    private val runnerOrderRepository: IRunnerOrderRepository,
    private val runnerTaskRepository: IRunnerTaskRepository,
    private val userRepository: IUserRepository,
    private val imageUploadRepository: IImageUploadRepository,
    private val supabase: SupabaseClient,
) : ViewModel() {

    private val orderId: String = savedStateHandle.get<String>("orderId") ?: ""

    private val _formState = MutableStateFlow(AfterSaleApplyFormState(orderId = orderId))
    val formState: StateFlow<AfterSaleApplyFormState> = _formState.asStateFlow()

    private val _uiState = MutableStateFlow<AfterSaleApplyUiState>(AfterSaleApplyUiState.Idle)
    val uiState: StateFlow<AfterSaleApplyUiState> = _uiState.asStateFlow()

    private val _orderSummary = MutableStateFlow(OrderSummary())
    val orderSummary: StateFlow<OrderSummary> = _orderSummary.asStateFlow()

    val afterSaleTypeOptions: List<Pair<String, String>> = AFTER_SALE_TYPE_LABELS

    init {
        loadOrderSummary()
    }

    // ── Public actions ──────────────────────────────────────

    fun onTypeChange(type: String) {
        _formState.update { it.copy(afterSaleType = type, error = null) }
    }

    fun onReasonChange(reason: String) {
        _formState.update { it.copy(reason = reason, error = null) }
    }

    fun onErrorDismissed() {
        _formState.update { it.copy(error = null) }
    }

    fun onAddImages(uris: List<Uri>) {
        _formState.update { it.copy(selectedUris = it.selectedUris + uris, error = null) }
    }

    fun onRemoveImage(index: Int) {
        _formState.update { it.copy(selectedUris = it.selectedUris.toMutableList().also { list -> list.removeAt(index) }) }
    }

    fun submit() {
        val state = _formState.value
        if (state.reason.isBlank()) {
            _formState.update { it.copy(error = "请填写申请原因") }
            return
        }

        viewModelScope.launch {
            _formState.update { it.copy(isLoading = true, error = null) }
            try {
                val userId = authRepository.currentUserId() ?: run {
                    _formState.update { it.copy(isLoading = false, error = "请先登录") }
                    return@launch
                }

                // Upload images if any were selected
                val imageUrls = if (state.selectedUris.isNotEmpty()) {
                    _formState.update { it.copy(isUploading = true) }
                    try {
                        imageUploadRepository.uploadImages(
                            uris = state.selectedUris,
                            bucket = "chat-images", // after-sale bucket not yet created, fallback to chat-images
                            resourceIdPrefix = "after_sale_${state.orderId}",
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "图片上传失败", e)
                        _formState.update { it.copy(isUploading = false, isLoading = false, error = "图片上传失败，请稍后重试") }
                        return@launch
                    }
                } else {
                    emptyList()
                }

                val imagesJson = if (imageUrls.isNotEmpty()) {
                    buildJsonArray { imageUrls.forEach { add(JsonPrimitive(it)) } }.toString()
                } else {
                    "[]"
                }

                // 调用 Edge Function runner-after-sale 执行原子化操作：
                //   insert after_sales + update order status + insert timeline
                val body = buildJsonObject {
                    put("action", "create")
                    put("order_id", state.orderId)
                    put("requester_id", userId)
                    put("type", state.afterSaleType)
                    put("reason", state.reason)
                    put("images", imagesJson)
                }

                val httpResponse = supabase.functions.invoke(
                    "runner-after-sale",
                    body = body,
                )
                // 解析 Edge Function 返回的 { after_sale_id }，避免 refresh/get 竞态
                val responseText = httpResponse.bodyAsText()
                val responseObj = Json.parseToJsonElement(responseText) as JsonObject
                val saleId = (responseObj["after_sale_id"] as? JsonPrimitive)?.content
                    ?: state.orderId
                _uiState.value = AfterSaleApplyUiState.Success(saleId)

                // Edge Function 成功后再同步本地 Room 缓存（异步，不阻塞导航）
                viewModelScope.launch {
                    val order = runnerOrderRepository.getOrderById(state.orderId)
                    val schoolId = order?.schoolId ?: ""
                    if (schoolId.isNotEmpty()) {
                        runnerOrderRepository.refreshOrders(schoolId)
                    }
                    afterSaleRepository.refreshAfterSales(userId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "提交售后申请失败", e)
                _formState.update {
                    it.copy(
                        isLoading = false,
                        error = "提交失败，请稍后重试",
                    )
                }
            }
        }
    }

    // ── Private helpers ─────────────────────────────────────

    private fun loadOrderSummary() {
        if (orderId.isEmpty()) return
        viewModelScope.launch {
            _formState.update { it.copy(isSummaryLoading = true) }
            try {
                val order = runnerOrderRepository.getOrderById(orderId) ?: run {
                    _orderSummary.update { it.copy(orderId = orderId) }
                    return@launch
                }

                // Load task info for type + amount
                val task = runnerTaskRepository.getTaskById(order.taskId)
                val typeLabel = task?.let { TYPE_LABEL_MAP[it.type] } ?: "跑腿"
                val amount = task?.let {
                    val total = it.price + it.tip
                    OrderListViewModel.formatAmount(total)
                } ?: "--"

                // Load runner profile for name
                val runnerName = if (order.runnerId.isNotEmpty()) {
                    val profile = userRepository.getProfile(order.runnerId).first()
                    profile?.nickname ?: "跑腿员"
                } else {
                    "待派单"
                }

                _orderSummary.update {
                    OrderSummary(
                        orderId = orderId,
                        typeLabel = typeLabel,
                        amount = amount,
                        runnerName = runnerName,
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "加载订单摘要失败", e)
                _orderSummary.update { it.copy(orderId = orderId) }
            } finally {
                _formState.update { it.copy(isSummaryLoading = false) }
            }
        }
    }
}

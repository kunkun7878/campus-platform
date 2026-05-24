package com.campus.platform.ui.viewmodel.market

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.platform.data.auth.AuthRepository
import com.campus.platform.data.local.entity.MarketListingEntity
import com.campus.platform.data.local.mapper.MarketListingDto
import com.campus.platform.data.local.mapper.MarketOrderDto
import com.campus.platform.domain.repository.IMarketOrderRepository
import com.campus.platform.domain.repository.IMarketRepository
import com.campus.platform.ui.viewmodel.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "MarketOrderDetailVM"

// ── Detail data ────────────────────────────────────────────────

data class OrderDetailData(
    val order: MarketOrderDto,
    val listing: MarketListingDto,
    val isBuyer: Boolean,
    val isSeller: Boolean,
)

// ── Action permissions ─────────────────────────────────────────

data class MarketOrderActions(
    val canAccept: Boolean = false,
    val canCancel: Boolean = false,
    val canConfirmComplete: Boolean = false,
)

// ── ViewModel ──────────────────────────────────────────────────

@HiltViewModel
class MarketOrderDetailViewModel @Inject constructor(
    private val orderRepo: IMarketOrderRepository,
    private val listingRepo: IMarketRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    // ── Data state ───────────────────────────────────────────

    private val _orderId = MutableStateFlow<String?>(null)

    private val _uiState = MutableStateFlow<UiState<OrderDetailData>>(UiState.Loading)
    val uiState: StateFlow<UiState<OrderDetailData>> = _uiState.asStateFlow()

    private val _actions = MutableStateFlow(MarketOrderActions())
    val actions: StateFlow<MarketOrderActions> = _actions.asStateFlow()

    // ── Action state ─────────────────────────────────────────

    private val _actionInProgress = MutableStateFlow(false)
    val actionInProgress: StateFlow<Boolean> = _actionInProgress.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    // ── Load ─────────────────────────────────────────────────

    fun loadOrderDetail(orderId: String) {
        _orderId.value = orderId
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            _actionError.value = null

            try {
                val currentUserId = authRepository.currentUserId()
                if (currentUserId == null) {
                    _uiState.value = UiState.Error("请先登录")
                    return@launch
                }

                // 1. Load order
                val order = orderRepo.getOrderById(orderId)
                if (order == null) {
                    _uiState.value = UiState.Error("订单不存在")
                    return@launch
                }

                // 2. Load listing
                val listing = listingRepo.getListingById(order.listingId)
                if (listing == null) {
                    _uiState.value = UiState.Error("商品信息不存在")
                    return@launch
                }

                // 3. Determine role
                val isBuyer = currentUserId == order.buyerId
                val isSeller = currentUserId == order.sellerId

                val data = OrderDetailData(
                    order = order,
                    listing = listing,
                    isBuyer = isBuyer,
                    isSeller = isSeller,
                )
                _uiState.value = UiState.Success(data)

                // 4. Compute actions
                computeActions(order.status, isBuyer, isSeller)

            } catch (e: Exception) {
                Log.e(TAG, "加载订单详情失败", e)
                _uiState.value = UiState.Error("加载失败，请稍后重试")
            }
        }
    }

    // ── Status actions ───────────────────────────────────────

    /** 卖家确认接单: pending -> accepted */
    fun acceptOrder() {
        val current = _uiState.value
        if (current !is UiState.Success) return
        if (!_actions.value.canAccept) return

        viewModelScope.launch {
            _actionInProgress.value = true
            _actionError.value = null
            try {
                orderRepo.updateOrderStatus(current.data.order.id, "accepted")
                // Reload to refresh state
                current.data.order.id.let { id ->
                    val order = orderRepo.getOrderById(id)
                    if (order != null) {
                        val listing = listingRepo.getListingById(order.listingId)
                        val newData = current.data.copy(order = order, listing = listing ?: current.data.listing)
                        _uiState.value = UiState.Success(newData)
                        computeActions(order.status, newData.isBuyer, newData.isSeller)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "接单失败", e)
                _actionError.value = "操作失败，请稍后重试"
            } finally {
                _actionInProgress.value = false
            }
        }
    }

    /** 取消订单: 更新 order.status='cancelled' + 恢复 listing.status='active' */
    fun cancelOrder(reason: String?) {
        val current = _uiState.value
        if (current !is UiState.Success) return
        if (!_actions.value.canCancel) return

        viewModelScope.launch {
            _actionInProgress.value = true
            _actionError.value = null
            try {
                // Step 1: update order status
                orderRepo.updateOrderStatus(current.data.order.id, "cancelled")
                // Step 2: restore listing status
                listingRepo.updateListing(
                    current.data.listing.id,
                    mapOf("status" to "active")
                )
                // Reload
                val order = orderRepo.getOrderById(current.data.order.id)
                if (order != null) {
                    val listing = listingRepo.getListingById(order.listingId)
                    val newData = current.data.copy(order = order, listing = listing ?: current.data.listing)
                    _uiState.value = UiState.Success(newData)
                    computeActions(order.status, newData.isBuyer, newData.isSeller)
                }
            } catch (e: Exception) {
                Log.e(TAG, "取消订单失败", e)
                _actionError.value = "操作失败，请稍后重试"
            } finally {
                _actionInProgress.value = false
            }
        }
    }

    /** 确认完成: accepted -> completed，同时将 listing 标记为 sold */
    fun confirmComplete() {
        val current = _uiState.value
        if (current !is UiState.Success) return
        if (!_actions.value.canConfirmComplete) return

        viewModelScope.launch {
            _actionInProgress.value = true
            _actionError.value = null
            try {
                orderRepo.updateOrderStatus(current.data.order.id, "completed")
                listingRepo.updateListing(
                    current.data.listing.id,
                    mapOf("status" to MarketListingEntity.STATUS_SOLD)
                )
                val order = orderRepo.getOrderById(current.data.order.id)
                if (order != null) {
                    val listing = listingRepo.getListingById(order.listingId)
                    val newData = current.data.copy(order = order, listing = listing ?: current.data.listing)
                    _uiState.value = UiState.Success(newData)
                    computeActions(order.status, newData.isBuyer, newData.isSeller)
                }
            } catch (e: Exception) {
                Log.e(TAG, "确认完成失败", e)
                _actionError.value = "操作失败，请稍后重试"
            } finally {
                _actionInProgress.value = false
            }
        }
    }

    fun clearActionError() {
        _actionError.value = null
    }

    // ── Helpers ──────────────────────────────────────────────

    /**
     * 计算当前用户可执行的操作。
     *
     * 规则:
     * - pending: 买家无操作/卖家可确认接单+取消
     * - accepted: 买家和卖家都可确认完成，卖家也可取消
     * - completed / cancelled: 无操作
     */
    private fun computeActions(status: String, isBuyer: Boolean, isSeller: Boolean) {
        _actions.value = when {
            isSeller && status == "pending" -> MarketOrderActions(
                canAccept = true,
                canCancel = true,
            )
            isBuyer && status == "pending" -> MarketOrderActions(
                canCancel = true,
            )
            isSeller && status == "accepted" -> MarketOrderActions(
                canCancel = true,
                canConfirmComplete = true,
            )
            isBuyer && status == "accepted" -> MarketOrderActions(
                canConfirmComplete = true,
            )
            else -> MarketOrderActions()
        }
    }
}

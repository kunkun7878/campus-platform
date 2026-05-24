package com.campus.platform.ui.viewmodel.home

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.campus.platform.data.auth.AuthRepository
import com.campus.platform.data.local.mapper.LostFoundClaimDto
import com.campus.platform.data.local.mapper.LostFoundItemDto
import com.campus.platform.domain.repository.ILostFoundRepository
import com.campus.platform.navigation.LostDetail
import com.campus.platform.ui.viewmodel.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "LostDetailVM"

@HiltViewModel
class LostDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val lostFoundRepository: ILostFoundRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val lostId: String = savedStateHandle.toRoute<LostDetail>().lostId

    // ── Item state ────────────────────────────────────────────
    private val _item = MutableStateFlow<LostFoundItemDto?>(null)
    val item: StateFlow<LostFoundItemDto?> = _item.asStateFlow()

    private val _claims = MutableStateFlow<List<LostFoundClaimDto>>(emptyList())
    val claims: StateFlow<List<LostFoundClaimDto>> = _claims.asStateFlow()

    private val _uiState = MutableStateFlow<UiState<Unit>>(UiState.Loading)
    val uiState: StateFlow<UiState<Unit>> = _uiState.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    init {
        loadItem()
    }

    fun loadItem() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val userId = authRepository.currentUserId()
                _currentUserId.value = userId

                // Refresh from server first
                lostFoundRepository.refreshItemById(lostId)
                lostFoundRepository.refreshClaimsByItemId(lostId)

                val loaded = lostFoundRepository.getItemById(lostId)
                if (loaded != null) {
                    _item.value = loaded
                    _uiState.value = UiState.Success(Unit)
                } else {
                    _uiState.value = UiState.Error("物品不存在或已被删除")
                }
            } catch (e: Exception) {
                Log.e(TAG, "加载物品详情失败", e)
                _uiState.value = UiState.Error("加载失败，请稍后重试")
            }
        }
    }

    // ── Start collecting claims flow ──────────────────────────
    fun startObservingClaims() {
        viewModelScope.launch {
            lostFoundRepository.getClaimsByItemId(lostId).collect { list ->
                _claims.value = list
            }
        }
    }

    // ── Actions ───────────────────────────────────────────────

    /** 批准认领 */
    fun approveClaim(claimId: String) {
        viewModelScope.launch {
            _actionError.value = null
            try {
                val body = buildMap<String, Any?> {
                    put("action", "approve_claim")
                    put("claim_id", claimId)
                }
                lostFoundRepository.invokeLostItemLifecycle(body)
                // Refresh local data
                lostFoundRepository.refreshItemById(lostId)
                lostFoundRepository.refreshClaimsByItemId(lostId)
                loadItem() // reload everything
            } catch (e: Exception) {
                Log.e(TAG, "批准认领失败", e)
                _actionError.value = "操作失败，请稍后重试"
            }
        }
    }

    /** 拒绝认领 */
    fun rejectClaim(claimId: String) {
        viewModelScope.launch {
            _actionError.value = null
            try {
                val body = buildMap<String, Any?> {
                    put("action", "reject_claim")
                    put("claim_id", claimId)
                }
                lostFoundRepository.invokeLostItemLifecycle(body)
                lostFoundRepository.refreshItemById(lostId)
                lostFoundRepository.refreshClaimsByItemId(lostId)
                loadItem()
            } catch (e: Exception) {
                Log.e(TAG, "拒绝认领失败", e)
                _actionError.value = "操作失败，请稍后重试"
            }
        }
    }

    /** 确认已归还/已找到 — resolve item */
    fun resolveItem(claimId: String? = null) {
        viewModelScope.launch {
            _actionError.value = null
            try {
                val body = buildMap<String, Any?> {
                    put("action", "resolve_item")
                    put("item_id", lostId)
                    if (claimId != null) put("claim_id", claimId)
                }
                lostFoundRepository.invokeLostItemLifecycle(body)
                lostFoundRepository.refreshItemById(lostId)
                lostFoundRepository.refreshClaimsByItemId(lostId)
                loadItem()
            } catch (e: Exception) {
                Log.e(TAG, "确认解决失败", e)
                _actionError.value = "操作失败，请稍后重试"
            }
        }
    }

    /** 关闭物品 — close item */
    fun closeItem() {
        viewModelScope.launch {
            _actionError.value = null
            try {
                val body = buildMap<String, Any?> {
                    put("action", "close_item")
                    put("item_id", lostId)
                }
                lostFoundRepository.invokeLostItemLifecycle(body)
                lostFoundRepository.refreshItemById(lostId)
                lostFoundRepository.refreshClaimsByItemId(lostId)
                loadItem()
            } catch (e: Exception) {
                Log.e(TAG, "关闭物品失败", e)
                _actionError.value = "操作失败，请稍后重试"
            }
        }
    }

    fun clearActionError() {
        _actionError.value = null
    }
}

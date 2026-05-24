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
import com.campus.platform.domain.repository.IMessageRepository
import com.campus.platform.navigation.LostClaim
import com.campus.platform.ui.viewmodel.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

private const val TAG = "LostClaimVM"

@HiltViewModel
class LostClaimViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val lostFoundRepository: ILostFoundRepository,
    private val messageRepository: IMessageRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val lostId: String = savedStateHandle.toRoute<LostClaim>().lostId

    // ── Item info ─────────────────────────────────────────────
    private val _item = MutableStateFlow<LostFoundItemDto?>(null)
    val item: StateFlow<LostFoundItemDto?> = _item.asStateFlow()

    // ── Form ──────────────────────────────────────────────────
    private val _proofDescription = MutableStateFlow("")
    val proofDescription: StateFlow<String> = _proofDescription.asStateFlow()

    // ── Result ────────────────────────────────────────────────
    private val _uiState = MutableStateFlow<UiState<ClaimResult?>>(UiState.Success(null))
    val uiState: StateFlow<UiState<ClaimResult?>> = _uiState.asStateFlow()

    init {
        loadItem()
    }

    private fun loadItem() {
        viewModelScope.launch {
            try {
                lostFoundRepository.refreshItemById(lostId)
                _item.value = lostFoundRepository.getItemById(lostId)
            } catch (e: Exception) {
                Log.e(TAG, "加载物品信息失败", e)
            }
        }
    }

    fun setProofDescription(value: String) {
        _proofDescription.value = value
    }

    fun submitClaim() {
        viewModelScope.launch {
            val proof = _proofDescription.value.trim()
            if (proof.isBlank()) {
                _uiState.value = UiState.Error("请描述物品特征以证明您是失主")
                return@launch
            }

            _uiState.value = UiState.Loading

            try {
                val userId = authRepository.currentUserId()
                if (userId == null) {
                    _uiState.value = UiState.Error("请先登录")
                    return@launch
                }

                val profile = authRepository.getProfile()
                val schoolId = profile?.schoolId
                if (schoolId == null) {
                    _uiState.value = UiState.Error("请先选择学校")
                    return@launch
                }

                // Get the item to find the publisher
                val item = _item.value
                if (item == null) {
                    _uiState.value = UiState.Error("物品信息加载失败，请返回重试")
                    return@launch
                }

                // Check for existing pending claims before creating a duplicate
                val existingClaims = lostFoundRepository.getClaimsByClaimant(userId)
                val existingPending = existingClaims.firstOrNull()?.filter {
                    it.itemId == lostId && it.status == "pending"
                }
                if (!existingPending.isNullOrEmpty()) {
                    _uiState.value = UiState.Error("您已提交过认领申请，请等待发布者处理")
                    return@launch
                }

                val now = java.time.Instant.now().toString()
                val claimId = UUID.randomUUID().toString()

                // 1. Create claim via repository (direct PostgREST insert)
                val claim = LostFoundClaimDto(
                    id = claimId,
                    itemId = lostId,
                    claimantId = userId,
                    proofDescription = proof.ifBlank { null },
                    status = "pending",
                    schoolId = schoolId,
                    resolvedAt = null,
                    createdAt = now,
                    updatedAt = now,
                )
                lostFoundRepository.createClaim(claim)

                // 2-3. Auto-create conversation and send greeting (best-effort;
                // if conversation setup fails the claim is already submitted).
                try {
                    val conversation = messageRepository.createConversation(item.publisherId)
                    val greeting = buildString {
                        append("您好，我对您发布的「${item.title}」感兴趣。")
                        if (proof.isNotBlank()) {
                            append("\n\n我的描述：$proof")
                        }
                    }
                    messageRepository.sendMessage(conversation.id, greeting)
                    _uiState.value = UiState.Success(
                        ClaimResult(conversationId = conversation.id, itemId = lostId)
                    )
                } catch (convErr: Exception) {
                    Log.e(TAG, "会话创建失败，但认领已提交", convErr)
                    _uiState.value = UiState.Success(
                        ClaimResult(conversationId = "", itemId = lostId)
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "认领提交失败", e)
                _uiState.value = UiState.Error("认领失败，请稍后重试")
            }
        }
    }

    fun resetForm() {
        _proofDescription.value = ""
        _uiState.value = UiState.Success<ClaimResult?>(null)
    }
}

/** 认领提交成功后的结果 */
data class ClaimResult(val conversationId: String, val itemId: String)

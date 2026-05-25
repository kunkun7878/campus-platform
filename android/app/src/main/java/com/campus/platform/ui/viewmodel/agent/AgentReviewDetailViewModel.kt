package com.campus.platform.ui.viewmodel.agent

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.platform.data.local.mapper.CommunityPostDto
import com.campus.platform.domain.repository.ICommunityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "AgentReviewDetailVM"

data class AgentReviewDetailState(
    val post: CommunityPostDto? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val showRejectDialog: Boolean = false,
    val rejectReason: String = "",
    val actionInProgress: Boolean = false,
    val actionCompleted: Boolean = false,
    val actionResult: String? = null,
)

@HiltViewModel
class AgentReviewDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val communityRepo: ICommunityRepository,
) : ViewModel() {

    private val postId: String = savedStateHandle.get<String>("postId") ?: ""

    private val _state = MutableStateFlow(AgentReviewDetailState())
    val state: StateFlow<AgentReviewDetailState> = _state.asStateFlow()

    init {
        loadPost()
    }

    fun loadPost() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val post = communityRepo.getPostById(postId)
                if (post != null) {
                    _state.value = _state.value.copy(post = post, isLoading = false)
                } else {
                    _state.value = _state.value.copy(isLoading = false, error = "帖子不存在")
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadPost failed", e)
                _state.value = _state.value.copy(isLoading = false, error = e.message ?: "加载失败")
            }
        }
    }

    // ── Actions ─────────────────────────────────────────────────

    fun approve() {
        viewModelScope.launch {
            _state.value = _state.value.copy(actionInProgress = true)
            try {
                communityRepo.updatePostStatus(postId, "published")
                _state.value = _state.value.copy(
                    actionInProgress = false,
                    actionCompleted = true,
                    actionResult = "已通过审核",
                )
            } catch (e: Exception) {
                Log.e(TAG, "approve failed", e)
                _state.value = _state.value.copy(
                    actionInProgress = false,
                    error = "操作失败: ${e.message}",
                )
            }
        }
    }

    fun showRejectDialog() {
        _state.value = _state.value.copy(showRejectDialog = true, rejectReason = "")
    }

    fun dismissRejectDialog() {
        _state.value = _state.value.copy(showRejectDialog = false)
    }

    fun updateRejectReason(reason: String) {
        _state.value = _state.value.copy(rejectReason = reason)
    }

    fun confirmReject() {
        viewModelScope.launch {
            _state.value = _state.value.copy(actionInProgress = true, showRejectDialog = false)
            try {
                val reason = _state.value.rejectReason.ifBlank { "内容不符合社区规范" }
                communityRepo.updatePostStatus(postId, "blocked", reason)
                _state.value = _state.value.copy(
                    actionInProgress = false,
                    actionCompleted = true,
                    actionResult = "已拒绝",
                )
            } catch (e: Exception) {
                Log.e(TAG, "reject failed", e)
                _state.value = _state.value.copy(
                    actionInProgress = false,
                    error = "操作失败: ${e.message}",
                )
            }
        }
    }

    fun hide() {
        viewModelScope.launch {
            _state.value = _state.value.copy(actionInProgress = true)
            try {
                communityRepo.updatePostStatus(postId, "hidden")
                _state.value = _state.value.copy(
                    actionInProgress = false,
                    actionCompleted = true,
                    actionResult = "已隐藏",
                )
            } catch (e: Exception) {
                Log.e(TAG, "hide failed", e)
                _state.value = _state.value.copy(
                    actionInProgress = false,
                    error = "操作失败: ${e.message}",
                )
            }
        }
    }

    fun consumeActionResult() {
        _state.value = _state.value.copy(actionCompleted = false, actionResult = null)
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}

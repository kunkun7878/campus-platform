package com.campus.platform.ui.viewmodel.agent

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.platform.data.auth.AuthRepository
import com.campus.platform.data.local.mapper.RunnerApplicationDto
import com.campus.platform.domain.repository.IRunnerApplicationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "AgentRunnerReviewVM"

data class AgentRunnerReviewState(
    val applications: List<RunnerApplicationDto> = emptyList(),
    val expandedId: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val actionInProgress: Boolean = false,
    val showRejectDialog: Boolean = false,
    val rejectTargetId: String? = null,
    val rejectReason: String = "",
    val actionResult: String? = null,
    val schoolId: String = "",
)

@HiltViewModel
class AgentRunnerReviewViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val runnerApplicationRepo: IRunnerApplicationRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AgentRunnerReviewState())
    val state: StateFlow<AgentRunnerReviewState> = _state.asStateFlow()

    init {
        loadApplications()
    }

    fun loadApplications() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val profile = authRepository.getProfile()
                val schoolId = profile?.schoolId ?: ""
                if (schoolId.isBlank()) {
                    _state.value = _state.value.copy(isLoading = false, error = "未绑定学校")
                    return@launch
                }

                val apps = runnerApplicationRepo.getPendingApplications(schoolId)
                _state.value = _state.value.copy(
                    schoolId = schoolId,
                    applications = apps,
                    isLoading = false,
                )
            } catch (e: Exception) {
                Log.e(TAG, "loadApplications failed", e)
                _state.value = _state.value.copy(isLoading = false, error = e.message ?: "加载失败")
            }
        }
    }

    fun toggleExpand(applicationId: String) {
        val current = _state.value.expandedId
        _state.value = _state.value.copy(
            expandedId = if (current == applicationId) null else applicationId
        )
    }

    // ── Approve ─────────────────────────────────────────────────

    fun approve(applicationId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(actionInProgress = true)
            try {
                val profile = authRepository.getProfile()
                val reviewedBy = profile?.id ?: ""
                runnerApplicationRepo.approveApplication(applicationId, reviewedBy)
                _state.value = _state.value.copy(
                    actionInProgress = false,
                    actionResult = "已通过审核",
                    applications = _state.value.applications.filter { it.id != applicationId },
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

    // ── Reject ──────────────────────────────────────────────────

    fun showRejectDialog(applicationId: String) {
        _state.value = _state.value.copy(
            showRejectDialog = true,
            rejectTargetId = applicationId,
            rejectReason = "",
        )
    }

    fun dismissRejectDialog() {
        _state.value = _state.value.copy(showRejectDialog = false, rejectTargetId = null)
    }

    fun updateRejectReason(reason: String) {
        _state.value = _state.value.copy(rejectReason = reason)
    }

    fun confirmReject() {
        val targetId = _state.value.rejectTargetId ?: return
        val reason = _state.value.rejectReason.ifBlank { "资质不符合要求" }
        viewModelScope.launch {
            _state.value = _state.value.copy(
                actionInProgress = true,
                showRejectDialog = false,
                rejectTargetId = null,
            )
            try {
                val profile = authRepository.getProfile()
                val reviewedBy = profile?.id ?: ""
                runnerApplicationRepo.rejectApplication(targetId, reviewedBy, reason)
                _state.value = _state.value.copy(
                    actionInProgress = false,
                    actionResult = "已拒绝",
                    applications = _state.value.applications.filter { it.id != targetId },
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

    fun consumeActionResult() {
        _state.value = _state.value.copy(actionResult = null)
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}

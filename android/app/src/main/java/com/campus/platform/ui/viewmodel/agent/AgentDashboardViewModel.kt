package com.campus.platform.ui.viewmodel.agent

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.platform.data.auth.AuthRepository
import com.campus.platform.domain.repository.ICommunityRepository
import com.campus.platform.domain.repository.IMiscRepository
import com.campus.platform.domain.repository.IRunnerApplicationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "AgentDashboardVM"

data class AgentDashboardState(
    val pendingReviewCount: Int = 0,
    val userCount: Int = 0,
    val announcementCount: Int = 0,
    val pendingRunnerCount: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null,
    val schoolId: String = "",
)

@HiltViewModel
class AgentDashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val communityRepo: ICommunityRepository,
    private val miscRepo: IMiscRepository,
    private val runnerApplicationRepo: IRunnerApplicationRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AgentDashboardState())
    val state: StateFlow<AgentDashboardState> = _state.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val profile = authRepository.getProfile()
                val schoolId = profile?.schoolId ?: ""
                if (schoolId.isBlank()) {
                    _state.value = _state.value.copy(isLoading = false, error = "未绑定学校")
                    return@launch
                }

                // Pending review posts count
                val pendingPosts = try {
                    communityRepo.getPendingReviewPosts(schoolId)
                } catch (e: Exception) { emptyList() }

                // Pending review comments count
                val pendingComments = try {
                    communityRepo.getPendingReviewComments(schoolId)
                } catch (e: Exception) { emptyList() }

                // Pending runner applications
                val pendingRunners = try {
                    runnerApplicationRepo.getPendingApplications(schoolId)
                } catch (e: Exception) { emptyList() }

                _state.value = _state.value.copy(
                    schoolId = schoolId,
                    pendingReviewCount = pendingPosts.size + pendingComments.size,
                    pendingRunnerCount = pendingRunners.size,
                    isLoading = false,
                )
            } catch (e: Exception) {
                Log.e(TAG, "loadDashboard failed", e)
                _state.value = _state.value.copy(isLoading = false, error = e.message ?: "加载失败")
            }
        }
    }
}

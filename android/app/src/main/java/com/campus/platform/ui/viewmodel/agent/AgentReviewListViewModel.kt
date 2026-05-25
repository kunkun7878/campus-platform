package com.campus.platform.ui.viewmodel.agent

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.platform.data.auth.AuthRepository
import com.campus.platform.data.local.mapper.CommunityCommentDto
import com.campus.platform.data.local.mapper.CommunityPostDto
import com.campus.platform.domain.repository.ICommunityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "AgentReviewListVM"

data class AgentReviewListState(
    val selectedTab: Int = 0, // 0 = posts, 1 = comments
    val posts: List<CommunityPostDto> = emptyList(),
    val comments: List<CommunityCommentDto> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val schoolId: String = "",
)

@HiltViewModel
class AgentReviewListViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val communityRepo: ICommunityRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AgentReviewListState())
    val state: StateFlow<AgentReviewListState> = _state.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val profile = authRepository.getProfile()
                val schoolId = profile?.schoolId ?: ""
                if (schoolId.isBlank()) {
                    _state.value = _state.value.copy(isLoading = false, error = "未绑定学校")
                    return@launch
                }

                val posts = try {
                    communityRepo.getPendingReviewPosts(schoolId)
                } catch (e: Exception) { emptyList() }

                val comments = try {
                    communityRepo.getPendingReviewComments(schoolId)
                } catch (e: Exception) { emptyList() }

                _state.value = _state.value.copy(
                    schoolId = schoolId,
                    posts = posts,
                    comments = comments,
                    isLoading = false,
                )
            } catch (e: Exception) {
                Log.e(TAG, "loadData failed", e)
                _state.value = _state.value.copy(isLoading = false, error = e.message ?: "加载失败")
            }
        }
    }

    fun selectTab(tab: Int) {
        _state.value = _state.value.copy(selectedTab = tab)
    }
}

package com.campus.platform.ui.viewmodel.agent

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.platform.data.auth.AuthRepository
import com.campus.platform.data.local.mapper.AnnouncementDto
import com.campus.platform.domain.repository.IMiscRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "AgentAnnouncementListVM"

data class AgentAnnouncementListState(
    val announcements: List<AnnouncementDto> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val schoolId: String = "",
    val deleteInProgress: Boolean = false,
)

@HiltViewModel
class AgentAnnouncementListViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val miscRepo: IMiscRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AgentAnnouncementListState())
    val state: StateFlow<AgentAnnouncementListState> = _state.asStateFlow()

    init {
        loadAnnouncements()
    }

    fun loadAnnouncements() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val profile = authRepository.getProfile()
                val schoolId = profile?.schoolId ?: ""
                if (schoolId.isBlank()) {
                    _state.value = _state.value.copy(isLoading = false, error = "未绑定学校")
                    return@launch
                }

                miscRepo.getAnnouncements(schoolId).collect { items ->
                    _state.value = _state.value.copy(
                        schoolId = schoolId,
                        announcements = items.filter { it.status != "deleted" },
                        isLoading = false,
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadAnnouncements failed", e)
                _state.value = _state.value.copy(isLoading = false, error = e.message ?: "加载失败")
            }
        }
    }

    fun deleteAnnouncement(announcementId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(deleteInProgress = true)
            try {
                miscRepo.deleteAnnouncement(announcementId)
                _state.value = _state.value.copy(deleteInProgress = false)
            } catch (e: Exception) {
                Log.e(TAG, "deleteAnnouncement failed", e)
                _state.value = _state.value.copy(
                    deleteInProgress = false,
                    error = "删除失败: ${e.message}",
                )
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}

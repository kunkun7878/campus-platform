package com.campus.platform.ui.viewmodel.agent

import android.util.Log
import androidx.lifecycle.SavedStateHandle
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
import java.util.UUID
import javax.inject.Inject

private const val TAG = "AgentAnnouncementEditVM"

data class AgentAnnouncementEditState(
    val announcementId: String? = null,
    val title: String = "",
    val content: String = "",
    val priority: String = "normal",
    val isPinned: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isNew: Boolean = true,
    val error: String? = null,
    val saveSuccess: Boolean = false,
)

@HiltViewModel
class AgentAnnouncementEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val miscRepo: IMiscRepository,
) : ViewModel() {

    private val rawId: String = savedStateHandle.get<String>("announcementId") ?: "new"
    private val isNew = rawId == "new"

    private val _state = MutableStateFlow(AgentAnnouncementEditState(announcementId = rawId, isNew = isNew))
    val state: StateFlow<AgentAnnouncementEditState> = _state.asStateFlow()

    init {
        if (!isNew) {
            loadAnnouncement()
        }
    }

    private fun loadAnnouncement() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val announcement = miscRepo.getAnnouncementById(rawId)
                if (announcement != null) {
                    _state.value = _state.value.copy(
                        title = announcement.title,
                        content = announcement.content ?: "",
                        priority = announcement.priority,
                        isPinned = announcement.isPinned,
                        isLoading = false,
                    )
                } else {
                    _state.value = _state.value.copy(isLoading = false, error = "公告不存在")
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadAnnouncement failed", e)
                _state.value = _state.value.copy(isLoading = false, error = e.message ?: "加载失败")
            }
        }
    }

    // ── Form updates ────────────────────────────────────────────

    fun updateTitle(value: String) { _state.value = _state.value.copy(title = value) }
    fun updateContent(value: String) { _state.value = _state.value.copy(content = value) }
    fun updatePriority(value: String) { _state.value = _state.value.copy(priority = value) }
    fun updateIsPinned(value: Boolean) { _state.value = _state.value.copy(isPinned = value) }

    // ── Save ────────────────────────────────────────────────────

    fun save() {
        val s = _state.value
        if (s.title.isBlank()) {
            _state.value = _state.value.copy(error = "标题不能为空")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, error = null)
            try {
                val profile = authRepository.getProfile()
                val schoolId = profile?.schoolId ?: ""
                val now = java.time.Instant.now().toString()

                val dto = if (isNew) {
                    AnnouncementDto(
                        id = UUID.randomUUID().toString(),
                        title = s.title,
                        content = s.content,
                        schoolId = schoolId,
                        publishedBy = profile?.id ?: "",
                        isPinned = s.isPinned,
                        priority = s.priority,
                        status = "published",
                        createdAt = now,
                        updatedAt = now,
                    )
                } else {
                    val existing = miscRepo.getAnnouncementById(rawId)
                    AnnouncementDto(
                        id = rawId,
                        title = s.title,
                        content = s.content,
                        schoolId = existing?.schoolId ?: schoolId,
                        publishedBy = existing?.publishedBy ?: (profile?.id ?: ""),
                        isPinned = s.isPinned,
                        priority = s.priority,
                        status = existing?.status ?: "published",
                        createdAt = existing?.createdAt ?: now,
                        updatedAt = now,
                    )
                }

                miscRepo.upsertAnnouncement(dto)
                _state.value = _state.value.copy(isSaving = false, saveSuccess = true)
            } catch (e: Exception) {
                Log.e(TAG, "save failed", e)
                _state.value = _state.value.copy(isSaving = false, error = "保存失败: ${e.message}")
            }
        }
    }

    fun consumeSaveSuccess() {
        _state.value = _state.value.copy(saveSuccess = false)
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}

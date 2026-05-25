package com.campus.platform.ui.viewmodel.home

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.platform.data.local.mapper.AnnouncementDto
import com.campus.platform.domain.repository.IMiscRepository
import com.campus.platform.navigation.AnnouncementDetail
import com.campus.platform.ui.viewmodel.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnnouncementDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val miscRepository: IMiscRepository,
) : ViewModel() {

    private val announcementId: String = savedStateHandle.toRoute<AnnouncementDetail>().announcementId

    private val _uiState = MutableStateFlow<UiState<AnnouncementDto>>(UiState.Loading)
    val uiState: StateFlow<UiState<AnnouncementDto>> = _uiState.asStateFlow()

    init {
        loadAnnouncement()
    }

    private fun loadAnnouncement() {
        if (announcementId.isBlank()) {
            _uiState.value = UiState.Error("公告不存在")
            return
        }
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val announcement = miscRepository.getAnnouncementById(announcementId)
                if (announcement != null) {
                    _uiState.value = UiState.Success(announcement)
                } else {
                    _uiState.value = UiState.Error("公告不存在")
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "加载失败")
            }
        }
    }
}

package com.campus.platform.ui.viewmodel.global

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.platform.data.auth.AuthRepository
import com.campus.platform.data.model.Campus
import com.campus.platform.data.model.School
import com.campus.platform.data.school.SchoolRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SchoolSelectState(
    val schools: List<School> = emptyList(),
    val allCampuses: Map<String, List<Campus>> = emptyMap(),
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val selectedSchool: School? = null,
    val selectedCampus: Campus? = null,
)

@HiltViewModel
class SchoolSelectViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val schoolRepository: SchoolRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SchoolSelectState())
    val state: StateFlow<SchoolSelectState> = _state.asStateFlow()

    init {
        loadSchools()
    }

    fun loadSchools() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val schoolList = schoolRepository.getSchools()
                val campusMap = mutableMapOf<String, List<Campus>>()
                for (s in schoolList) {
                    campusMap[s.id] = schoolRepository.getCampuses(s.id)
                }
                _state.update {
                    it.copy(
                        schools = schoolList,
                        allCampuses = campusMap,
                        isLoading = false,
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        error = e.message ?: "加载学校列表失败",
                        isLoading = false,
                    )
                }
            }
        }
    }

    fun selectSchool(school: School) {
        _state.update { it.copy(selectedSchool = school) }
        // 如果该学校只有一个校区，自动选中
        val campuses = _state.value.allCampuses[school.id] ?: emptyList()
        if (campuses.size == 1) {
            _state.update { it.copy(selectedCampus = campuses.first()) }
        } else {
            _state.update { it.copy(selectedCampus = null) }
        }
    }

    fun selectCampus(campus: Campus) {
        _state.update { it.copy(selectedCampus = campus) }
    }

    fun clearSchoolSelection() {
        _state.update { it.copy(selectedSchool = null, selectedCampus = null) }
    }

    fun confirmSelection(onSuccess: () -> Unit) {
        val state = _state.value
        val school = state.selectedSchool ?: return
        val campus = state.selectedCampus ?: return

        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true) }
            try {
                val uid = authRepository.currentUserId() ?: return@launch
                authRepository.selectSchool(uid, school.id, campus.id)
                onSuccess()
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        error = e.message ?: "选校失败，请重试",
                        isSubmitting = false,
                    )
                }
            }
        }
    }

    fun onErrorDismissed() {
        _state.update { it.copy(error = null) }
    }
}

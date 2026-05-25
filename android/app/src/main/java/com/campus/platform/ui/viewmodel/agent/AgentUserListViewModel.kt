package com.campus.platform.ui.viewmodel.agent

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.platform.data.auth.AuthRepository
import com.campus.platform.data.model.Profile
import com.campus.platform.domain.repository.IUserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "AgentUserListVM"

data class AgentUserListState(
    val users: List<Profile> = emptyList(),
    val filteredUsers: List<Profile> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val error: String? = null,
    val schoolId: String = "",
)

@HiltViewModel
class AgentUserListViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: IUserRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AgentUserListState())
    val state: StateFlow<AgentUserListState> = _state.asStateFlow()

    init {
        loadUsers()
    }

    fun loadUsers() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val profile = authRepository.getProfile()
                val schoolId = profile?.schoolId ?: ""
                if (schoolId.isBlank()) {
                    _state.value = _state.value.copy(isLoading = false, error = "未绑定学校")
                    return@launch
                }

                val users = try {
                    userRepository.getUsersBySchool(schoolId)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "loadUsers failed", e)
                    emptyList()
                }

                _state.value = _state.value.copy(
                    schoolId = schoolId,
                    users = users,
                    filteredUsers = users,
                    isLoading = false,
                )
            } catch (e: Exception) {
                Log.e(TAG, "loadUsers failed", e)
                _state.value = _state.value.copy(isLoading = false, error = e.message ?: "加载失败")
            }
        }
    }

    fun search(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        if (query.isBlank()) {
            _state.value = _state.value.copy(filteredUsers = _state.value.users)
        } else {
            val lower = query.lowercase()
            _state.value = _state.value.copy(
                filteredUsers = _state.value.users.filter {
                    (it.nickname?.lowercase()?.contains(lower) == true) ||
                    (it.phone?.contains(query) == true) ||
                    (it.id.lowercase().contains(lower))
                }
            )
        }
    }
}

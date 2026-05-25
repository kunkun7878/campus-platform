package com.campus.platform.ui.viewmodel.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.platform.data.local.entity.FeedbackEntity
import com.campus.platform.data.local.mapper.FeedbackDto
import com.campus.platform.domain.repository.IMiscRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class FeedbackViewModel @Inject constructor(
    private val miscRepository: IMiscRepository,
) : ViewModel() {

    private val _formState = MutableStateFlow(FeedbackFormState())
    val formState: StateFlow<FeedbackFormState> = _formState.asStateFlow()

    private val _submitState = MutableStateFlow<FeedbackSubmitState>(FeedbackSubmitState.Idle)
    val submitState: StateFlow<FeedbackSubmitState> = _submitState.asStateFlow()

    fun onTypeChange(type: String) {
        _formState.value = _formState.value.copy(type = type)
    }

    fun onContentChange(content: String) {
        _formState.value = _formState.value.copy(content = content)
    }

    fun onContactChange(contact: String) {
        _formState.value = _formState.value.copy(contact = contact)
    }

    fun submit(userId: String, schoolId: String) {
        val state = _formState.value
        if (state.type.isBlank() || state.content.isBlank()) return

        viewModelScope.launch {
            _submitState.value = FeedbackSubmitState.Loading
            try {
                val feedback = FeedbackDto(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    type = state.type,
                    content = state.content,
                    contact = state.contact.ifBlank { null },
                    images = "[]",
                    status = FeedbackEntity.STATUS_PENDING,
                    reply = null,
                    schoolId = schoolId,
                    createdAt = null,
                    updatedAt = null,
                )
                miscRepository.submitFeedback(feedback)
                _submitState.value = FeedbackSubmitState.Success
                _formState.value = FeedbackFormState()
            } catch (e: Exception) {
                _submitState.value = FeedbackSubmitState.Error(e.message ?: "提交失败")
            }
        }
    }

    fun resetSubmitState() {
        _submitState.value = FeedbackSubmitState.Idle
    }
}

data class FeedbackFormState(
    val type: String = FeedbackEntity.TYPE_BUG,
    val content: String = "",
    val contact: String = "",
)

sealed class FeedbackSubmitState {
    data object Idle : FeedbackSubmitState()
    data object Loading : FeedbackSubmitState()
    data object Success : FeedbackSubmitState()
    data class Error(val message: String) : FeedbackSubmitState()
}

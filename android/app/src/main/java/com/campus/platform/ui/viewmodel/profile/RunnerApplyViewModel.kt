package com.campus.platform.ui.viewmodel.profile

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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

// ── Form state ──────────────────────────────────────────────────

data class RunnerApplyFormState(
    val realName: String = "",
    val studentId: String = "",
    val phone: String = "",
    val reason: String = "",
    val idCardFrontUrl: String = "",
    val idCardBackUrl: String = "",
    val error: String? = null,
)

// ── UI state ────────────────────────────────────────────────────

sealed interface RunnerApplyUiState {
    /** 正在加载已有申请 */
    data object Loading : RunnerApplyUiState

    /** 无已有申请或重新申请 — 展示表单 */
    data class NewApplication(val form: RunnerApplyFormState = RunnerApplyFormState(), val isSubmitting: Boolean = false) : RunnerApplyUiState

    /** 已有申请且状态为 pending/approved/rejected — 展示审核状态卡片 */
    data class ExistingApplication(
        val application: RunnerApplicationDto,
        val isResubmitting: Boolean = false,
        val form: RunnerApplyFormState = RunnerApplyFormState(),
    ) : RunnerApplyUiState

    /** 提交成功 */
    data object SubmitSuccess : RunnerApplyUiState

    /** 提交失败 */
    data class SubmitError(val message: String) : RunnerApplyUiState
}

// ── ViewModel ───────────────────────────────────────────────────

private const val TAG = "RunnerApplyVM"

@HiltViewModel
class RunnerApplyViewModel @Inject constructor(
    private val repository: IRunnerApplicationRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<RunnerApplyUiState>(RunnerApplyUiState.Loading)
    val uiState: StateFlow<RunnerApplyUiState> = _uiState.asStateFlow()

    private var currentUserId: String = ""
    private var currentSchoolId: String = ""

    init {
        loadExistingApplication()
    }

    // ── Public actions ────────────────────────────────────────

    fun onRealNameChange(value: String) {
        updateForm { it.copy(realName = value, error = null) }
    }

    fun onStudentIdChange(value: String) {
        updateForm { it.copy(studentId = value, error = null) }
    }

    fun onPhoneChange(value: String) {
        updateForm { it.copy(phone = value, error = null) }
    }

    fun onReasonChange(value: String) {
        updateForm { it.copy(reason = value, error = null) }
    }

    fun onIdCardFrontUrlChange(value: String) {
        updateForm { it.copy(idCardFrontUrl = value, error = null) }
    }

    fun onIdCardBackUrlChange(value: String) {
        updateForm { it.copy(idCardBackUrl = value, error = null) }
    }

    /** 从 rejected 状态触发重新申请，切换回表单模式 */
    fun onStartResubmit() {
        val current = _uiState.value
        if (current is RunnerApplyUiState.ExistingApplication) {
            _uiState.value = RunnerApplyUiState.NewApplication(
                form = RunnerApplyFormState(
                    realName = current.application.realName,
                    studentId = current.application.studentId,
                    phone = current.application.phone,
                    reason = current.application.reason ?: "",
                    idCardFrontUrl = current.application.idCardFront ?: "",
                    idCardBackUrl = current.application.idCardBack ?: "",
                ),
            )
        }
    }

    /**
     * 提交跑腿员认证申请（CREATE 操作，非状态转换）。
     *
     * 安全分析：本方法直接调用 Repository → Supabase INSERT，不经过 Edge Function。
     * 这是安全的，原因如下：
     * 1. 这是 INSERT 新建记录（status=pending），是对申请的创建，不是已有记录的状态变更。
     * 2. RLS insert policy (runner_applications_insert_policy) 强约束：
     *    - user_id = auth.uid() — 只能为自己提交申请
     *    - school_id = get_user_school_id() — 只能在自己学校提交
     * 3. UNIQUE 约束 uq_runner_applications_user_school 保证每人每校仅一条申请，
     *    防止重复提交滥用。
     * 4. UPDATE 仅 Agent 可执行（runner_applications_update_policy），
     *    防止用户自行修改审核状态。
     */
    fun submitApplication() {
        val form = when (val state = _uiState.value) {
            is RunnerApplyUiState.NewApplication -> state.form
            is RunnerApplyUiState.ExistingApplication -> state.form
            else -> return
        }

        // 校验
        val errors = buildList {
            if (form.realName.isBlank()) add("请输入真实姓名")
            if (form.studentId.isBlank()) add("请输入学号")
            if (form.phone.isBlank()) add("请输入手机号")
        }
        if (errors.isNotEmpty()) {
            updateForm { it.copy(error = errors.first()) }
            return
        }

        viewModelScope.launch {
            // 标记提交中
            setSubmitting(true)
            try {
                val dto = RunnerApplicationDto(
                    id = UUID.randomUUID().toString(),
                    userId = currentUserId,
                    realName = form.realName.trim(),
                    studentId = form.studentId.trim(),
                    phone = form.phone.trim(),
                    reason = form.reason.trim().ifBlank { null },
                    idCardFront = form.idCardFrontUrl.trim().ifBlank { null },
                    idCardBack = form.idCardBackUrl.trim().ifBlank { null },
                    status = "pending",
                    reviewComment = null,
                    reviewedBy = null,
                    reviewedAt = null,
                    schoolId = currentSchoolId,
                    createdAt = null,
                    updatedAt = null,
                )
                repository.submitApplication(dto)
                _uiState.value = RunnerApplyUiState.SubmitSuccess
            } catch (e: Exception) {
                Log.e(TAG, "提交申请失败", e)
                _uiState.value = RunnerApplyUiState.SubmitError(
                    "提交失败，请稍后重试"
                )
            }
        }
    }

    fun onErrorDismissed() {
        val current = _uiState.value
        if (current is RunnerApplyUiState.SubmitError) {
            // 回到表单状态
            _uiState.value = RunnerApplyUiState.NewApplication()
        }
    }

    // ── Internal ──────────────────────────────────────────────

    private fun loadExistingApplication() {
        viewModelScope.launch {
            _uiState.value = RunnerApplyUiState.Loading
            try {
                val userId = authRepository.currentUserId() ?: run {
                    _uiState.value = RunnerApplyUiState.SubmitError("未登录，请先登录")
                    return@launch
                }
                currentUserId = userId

                val profile = authRepository.getProfile()
                currentSchoolId = profile?.schoolId ?: ""

                val existing = repository.getMyApplication(userId, currentSchoolId)
                _uiState.value = if (existing != null) {
                    RunnerApplyUiState.ExistingApplication(application = existing)
                } else {
                    RunnerApplyUiState.NewApplication()
                }
            } catch (e: Exception) {
                Log.e(TAG, "加载已有申请失败", e)
                _uiState.value = RunnerApplyUiState.SubmitError(
                    "加载失败，请稍后重试"
                )
            }
        }
    }

    private fun updateForm(transform: (RunnerApplyFormState) -> RunnerApplyFormState) {
        _uiState.update { current ->
            when (current) {
                is RunnerApplyUiState.NewApplication -> current.copy(
                    form = transform(current.form)
                )
                is RunnerApplyUiState.ExistingApplication -> current.copy(
                    form = transform(current.form)
                )
                else -> current
            }
        }
    }

    private fun setSubmitting(submitting: Boolean) {
        _uiState.update { current ->
            when (current) {
                is RunnerApplyUiState.NewApplication -> current.copy(isSubmitting = submitting)
                is RunnerApplyUiState.ExistingApplication -> current.copy(isResubmitting = submitting)
                else -> current
            }
        }
    }
}

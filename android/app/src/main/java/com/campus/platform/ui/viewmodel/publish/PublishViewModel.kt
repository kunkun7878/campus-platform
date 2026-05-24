package com.campus.platform.ui.viewmodel.publish

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.platform.data.local.UserPreferencesDataStore
import com.campus.platform.data.local.entity.RunnerTaskEntity
import com.campus.platform.data.local.mapper.RunnerTaskDto
import com.campus.platform.domain.repository.IRunnerTaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

// ── 表单状态 ────────────────────────────────────────────────────────────────

data class PublishFormState(
    val taskType: String = RunnerTaskEntity.TYPE_PICKUP,
    val title: String = "",
    val description: String = "",
    val pickupAddr: String = "",
    val deliveryAddr: String = "",
    val storeName: String = "",
    val productName: String = "",
    val price: String = "",
    val tip: String = "",
    val deadline: String = "",
    val genderRestriction: String = RunnerTaskEntity.GENDER_ANY,
    val isLoading: Boolean = false,
    val error: String? = null,
)

// ── UI 状态 ────────────────────────────────────────────────────────────────

sealed interface PublishUiState {
    data object Idle : PublishUiState
    data object Loading : PublishUiState
    data object Success : PublishUiState
    data class Error(val message: String) : PublishUiState
}

// ── ViewModel ──────────────────────────────────────────────────────────────

private const val TAG = "PublishVM"

@HiltViewModel
class PublishViewModel @Inject constructor(
    private val runnerTaskRepository: IRunnerTaskRepository,
    private val prefs: UserPreferencesDataStore,
) : ViewModel() {

    private val _formState = MutableStateFlow(PublishFormState())
    val formState: StateFlow<PublishFormState> = _formState.asStateFlow()

    private val _uiState = MutableStateFlow<PublishUiState>(PublishUiState.Idle)
    val uiState: StateFlow<PublishUiState> = _uiState.asStateFlow()

    // ── 类型选择 ────────────────────────────────────────────────────────────

    fun onTaskTypeChange(type: String) {
        _formState.update { it.copy(taskType = type, error = null) }
    }

    // ── 字段变更 ────────────────────────────────────────────────────────────

    fun onTitleChange(value: String) {
        _formState.update { it.copy(title = value, error = null) }
    }

    fun onDescriptionChange(value: String) {
        _formState.update { it.copy(description = value, error = null) }
    }

    fun onPickupAddrChange(value: String) {
        _formState.update { it.copy(pickupAddr = value, error = null) }
    }

    fun onDeliveryAddrChange(value: String) {
        _formState.update { it.copy(deliveryAddr = value, error = null) }
    }

    fun onStoreNameChange(value: String) {
        _formState.update { it.copy(storeName = value, error = null) }
    }

    fun onProductNameChange(value: String) {
        _formState.update { it.copy(productName = value, error = null) }
    }

    fun onPriceChange(value: String) {
        _formState.update { it.copy(price = value, error = null) }
    }

    fun onTipChange(value: String) {
        _formState.update { it.copy(tip = value, error = null) }
    }

    fun onDeadlineChange(value: String) {
        _formState.update { it.copy(deadline = value, error = null) }
    }

    fun onGenderRestrictionChange(value: String) {
        _formState.update { it.copy(genderRestriction = value, error = null) }
    }

    fun onErrorDismissed() {
        _formState.update { it.copy(error = null) }
        _uiState.value = PublishUiState.Idle
    }

    fun resetUiState() {
        _uiState.value = PublishUiState.Idle
    }

    // ── 表单校验 ────────────────────────────────────────────────────────────

    private fun validate(): String? {
        val state = _formState.value
        val type = state.taskType

        if (state.title.isBlank()) {
            return "请输入任务标题"
        }

        when (type) {
            RunnerTaskEntity.TYPE_PICKUP -> {
                if (state.pickupAddr.isBlank()) return "请输入取货地点"
                if (state.deliveryAddr.isBlank()) return "请输入送达地点"
            }
            RunnerTaskEntity.TYPE_DELIVERY -> {
                if (state.pickupAddr.isBlank()) return "请输入取件地点"
                if (state.deliveryAddr.isBlank()) return "请输入送达地点"
            }
            RunnerTaskEntity.TYPE_PURCHASE -> {
                if (state.storeName.isBlank()) return "请输入购买店铺"
                if (state.productName.isBlank()) return "请输入商品名称"
            }
            RunnerTaskEntity.TYPE_UNIVERSAL -> {
                if (state.description.isBlank()) return "请输入服务描述"
            }
        }

        val priceNum = state.price.toIntOrNull()
        if (priceNum == null || priceNum <= 0) {
            return "请输入有效的赏金金额（正整数）"
        }

        return null
    }

    // ── 发布 ────────────────────────────────────────────────────────────────

    /**
     * 发布跑腿任务（CREATE 操作，非状态转换）。
     *
     * 安全分析：本方法直接调用 Repository → Supabase INSERT，不经过 Edge Function。
     * 这是安全的，原因如下：
     * 1. 这是 INSERT 新建记录（status=published），不是已有任务的状态转换。
     * 2. RLS insert policy (runner_tasks_insert_policy) 强约束：
     *    - publisher_id = auth.uid() — 只能以自己身份发布
     *    - school_id = get_user_school_id() — 只能在自己学校发布
     *    任何伪造 publisher_id 或越校发布的 INSERT 都会被 RLS 拒绝。
     * 3. Edge Function 仅需覆盖状态转换操作（accept / start_delivery / confirm 等），
     *    创建操作不需要 Edge Function 额外校验。
     */
    fun publishTask() {
        val error = validate()
        if (error != null) {
            _formState.update { it.copy(error = error) }
            return
        }

        viewModelScope.launch {
            _formState.update { it.copy(isLoading = true) }
            _uiState.value = PublishUiState.Loading

            try {
                val state = _formState.value
                val userId = prefs.userId.first() ?: throw Exception("未登录，请先登录")
                val schoolId = prefs.schoolId.first() ?: throw Exception("未选择学校")

                val dto = RunnerTaskDto(
                    id = UUID.randomUUID().toString(),
                    publisherId = userId,
                    runnerId = null,
                    type = state.taskType,
                    title = state.title.trim(),
                    description = buildDescription(state),
                    pickupAddr = buildPickupAddr(state),
                    deliveryAddr = if (state.taskType == RunnerTaskEntity.TYPE_PICKUP ||
                        state.taskType == RunnerTaskEntity.TYPE_DELIVERY
                    ) state.deliveryAddr.trim() else null,
                    price = state.price.toInt(),
                    tip = state.tip.toIntOrNull() ?: 0,
                    status = RunnerTaskEntity.STATUS_PUBLISHED,
                    deadline = state.deadline.ifBlank { null },
                    schoolId = schoolId,
                    images = "[]",
                    genderRestriction = state.genderRestriction,
                    autoCancelMinutes = 20,
                    createdAt = null,
                    updatedAt = null,
                )

                runnerTaskRepository.publishTask(dto)
                _uiState.value = PublishUiState.Success
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "发布任务失败", e)
                _formState.update { it.copy(error = "发布失败，请重试") }
                _uiState.value = PublishUiState.Error("发布失败")
            } finally {
                _formState.update { it.copy(isLoading = false) }
            }
        }
    }

    // ── DTO 字段映射 ────────────────────────────────────────────────────────

    /**
     * 根据任务类型拼接描述字段。
     * - 帮买：店铺 + 商品名称拼接
     * - 万能帮/帮取/帮送：直接使用描述
     */
    private fun buildDescription(state: PublishFormState): String {
        return when (state.taskType) {
            RunnerTaskEntity.TYPE_PURCHASE -> {
                if (state.description.isNotBlank()) {
                    "店铺: ${state.storeName.trim()}; 商品: ${state.productName.trim()}; 备注: ${state.description.trim()}"
                } else {
                    "店铺: ${state.storeName.trim()}; 商品: ${state.productName.trim()}"
                }
            }
            else -> state.description.trim()
        }
    }

    /**
     * 根据任务类型映射取件地址字段。
     * - 帮买：取件地址存购买店铺
     * - 万能帮：取件地址存服务位置
     */
    private fun buildPickupAddr(state: PublishFormState): String? {
        return when (state.taskType) {
            RunnerTaskEntity.TYPE_PURCHASE -> state.storeName.trim()
            RunnerTaskEntity.TYPE_UNIVERSAL -> state.pickupAddr.trim().ifBlank { null }
            else -> state.pickupAddr.trim()
        }
    }
}

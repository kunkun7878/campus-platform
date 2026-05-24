package com.campus.platform.ui.viewmodel.market

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.platform.data.auth.AuthRepository
import com.campus.platform.data.local.mapper.MarketListingDto
import com.campus.platform.domain.repository.IMarketRepository
import com.campus.platform.ui.component.MarketUiMapper
import com.campus.platform.ui.viewmodel.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "MarketPublishVM"

@HiltViewModel
class MarketPublishViewModel @Inject constructor(
    private val marketRepository: IMarketRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    // ── 发布结果状态 ──────────────────────────────────────────
    // 初始 Success(null) 表示表单就绪，尚未发布
    // 发布成功后 Success(listing) 持有新创建的商品数据

    private val _uiState = MutableStateFlow<UiState<MarketListingDto?>>(UiState.Success(null))
    val uiState: StateFlow<UiState<MarketListingDto?>> = _uiState.asStateFlow()

    // ── 表单字段 ──────────────────────────────────────────────

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _price = MutableStateFlow("")
    val price: StateFlow<String> = _price.asStateFlow()

    private val _originalPrice = MutableStateFlow("")
    val originalPrice: StateFlow<String> = _originalPrice.asStateFlow()

    private val _category = MutableStateFlow("电子产品")
    val category: StateFlow<String> = _category.asStateFlow()

    private val _condition = MutableStateFlow("全新")
    val condition: StateFlow<String> = _condition.asStateFlow()

    private val _isBargain = MutableStateFlow(true)
    val isBargain: StateFlow<Boolean> = _isBargain.asStateFlow()

    private val _meetupLocation = MutableStateFlow("")
    val meetupLocation: StateFlow<String> = _meetupLocation.asStateFlow()

    private val _contact = MutableStateFlow("站内私信联系")
    val contact: StateFlow<String> = _contact.asStateFlow()

    // ── 表单字段 setter ───────────────────────────────────────

    fun setTitle(value: String) {
        _title.value = value
    }

    fun setDescription(value: String) {
        _description.value = value
    }

    fun setPrice(value: String) {
        _price.value = value
    }

    fun setOriginalPrice(value: String) {
        _originalPrice.value = value
    }

    fun setCategory(value: String) {
        _category.value = value
    }

    fun setCondition(value: String) {
        _condition.value = value
    }

    fun setIsBargain(value: Boolean) {
        _isBargain.value = value
    }

    fun setMeetupLocation(value: String) {
        _meetupLocation.value = value
    }

    fun setContact(value: String) {
        _contact.value = value
    }

    // ── 发布 ──────────────────────────────────────────────────

    /** 提交发布：验证 → 构造 DTO → 调用 Repository → 更新 uiState */
    fun submitPublish() {
        viewModelScope.launch {
            // 1. 验证必填字段
            val titleText = _title.value.trim()
            if (titleText.isBlank()) {
                _uiState.value = UiState.Error("请输入商品标题")
                return@launch
            }

            val priceText = _price.value.trim()
            val priceInt = priceText.toIntOrNull()
            if (priceInt == null || priceInt <= 0) {
                _uiState.value = UiState.Error("请输入有效的价格")
                return@launch
            }

            _uiState.value = UiState.Loading

            try {
                // 2. 获取用户与学校信息
                val userId = authRepository.currentUserId()
                if (userId == null) {
                    _uiState.value = UiState.Error("请先登录")
                    return@launch
                }

                val profile = authRepository.getProfile()
                val schoolId = profile?.schoolId
                if (schoolId == null) {
                    _uiState.value = UiState.Error("请先选择学校")
                    return@launch
                }

                val now = java.time.Instant.now().toString()
                val originalPriceInt = _originalPrice.value.trim().toIntOrNull()
                if (originalPriceInt != null && originalPriceInt < priceInt) {
                    _uiState.value = UiState.Error("原价不能低于售价")
                    return@launch
                }

                // 3. 将中文成色映射为英文数据库值
                val englishCondition = MarketUiMapper.toEnglishCondition(_condition.value)

                // 4. 构造 DTO 并发布
                val listing = MarketListingDto(
                    id = UUID.randomUUID().toString(),
                    sellerId = userId,
                    title = titleText,
                    description = _description.value.trim().ifBlank { null },
                    price = priceInt,
                    originalPrice = originalPriceInt,
                    images = "",                                          // 图片功能待后续实现
                    category = _category.value,
                    condition = englishCondition,
                    status = "active",
                    schoolId = schoolId,
                    isBargain = _isBargain.value,
                    contact = _contact.value,
                    meetupLocation = _meetupLocation.value.trim().ifBlank { null },
                    createdAt = now,
                    updatedAt = now,
                )

                marketRepository.publishListing(listing)
                _uiState.value = UiState.Success(listing)
            } catch (e: Exception) {
                Log.e(TAG, "发布商品失败", e)
                _uiState.value = UiState.Error("发布失败，请稍后重试")
            }
        }
    }

    // ── 表单重置 ──────────────────────────────────────────────

    /** 重置所有表单字段和发布状态，用于发布成功后重新发布 */
    fun resetForm() {
        _title.value = ""
        _description.value = ""
        _price.value = ""
        _originalPrice.value = ""
        _category.value = "电子产品"
        _condition.value = "全新"
        _isBargain.value = true
        _meetupLocation.value = ""
        _contact.value = "站内私信联系"
        _uiState.value = UiState.Success(null)
    }
}

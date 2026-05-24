package com.campus.platform.ui.viewmodel.market

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.platform.data.auth.AuthRepository
import com.campus.platform.domain.repository.IImageUploadRepository
import com.campus.platform.domain.repository.ILostFoundRepository
import com.campus.platform.ui.viewmodel.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID

private const val TAG = "LostPublishVM"

private val CATEGORY_OPTIONS = listOf(
    "电子产品",
    "证件卡片",
    "衣物饰品",
    "书籍文具",
    "钥匙",
    "其他",
)

@HiltViewModel
class LostPublishViewModel @Inject constructor(
    private val lostFoundRepository: ILostFoundRepository,
    private val authRepository: AuthRepository,
    private val imageUploadRepository: IImageUploadRepository,
) : ViewModel() {

    // ── 发布结果状态 ──────────────────────────────────────────
    private val _uiState = MutableStateFlow<UiState<PublishedItemResult?>>(UiState.Success(null))
    val uiState: StateFlow<UiState<PublishedItemResult?>> = _uiState.asStateFlow()

    // ── 表单字段 ──────────────────────────────────────────────
    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _type = MutableStateFlow("lost")
    val type: StateFlow<String> = _type.asStateFlow()

    private val _category = MutableStateFlow("其他")
    val category: StateFlow<String> = _category.asStateFlow()

    private val _location = MutableStateFlow("")
    val location: StateFlow<String> = _location.asStateFlow()

    private val _lostDate = MutableStateFlow("")
    val lostDate: StateFlow<String> = _lostDate.asStateFlow()

    private val _reward = MutableStateFlow("")
    val reward: StateFlow<String> = _reward.asStateFlow()

    private val _contact = MutableStateFlow("站内私信联系")
    val contact: StateFlow<String> = _contact.asStateFlow()

    // ── 图片选择 ──────────────────────────────────────────────
    private val _selectedImages = MutableStateFlow<List<Uri>>(emptyList())
    val selectedImages: StateFlow<List<Uri>> = _selectedImages.asStateFlow()

    private val _uploadedImageUrls = MutableStateFlow<List<String>>(emptyList())
    private val _isUploadingImages = MutableStateFlow(false)
    val isUploadingImages: StateFlow<Boolean> = _isUploadingImages.asStateFlow()

    // ── Setters ───────────────────────────────────────────────
    fun setTitle(value: String) { _title.value = value }
    fun setDescription(value: String) { _description.value = value }
    fun setType(value: String) { _type.value = value }
    fun setCategory(value: String) { _category.value = value }
    fun setLocation(value: String) { _location.value = value }
    fun setLostDate(value: String) { _lostDate.value = value }
    fun setReward(value: String) { _reward.value = value }
    fun setContact(value: String) { _contact.value = value }

    fun addImages(uris: List<Uri>) {
        val current = _selectedImages.value.toMutableList()
        current.addAll(uris)
        _selectedImages.value = current.take(9)
    }

    fun removeImage(index: Int) {
        val current = _selectedImages.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _selectedImages.value = current
        }
    }

    fun clearImages() {
        _selectedImages.value = emptyList()
        _uploadedImageUrls.value = emptyList()
    }

    // ── Publish ───────────────────────────────────────────────
    fun submitPublish() {
        viewModelScope.launch {
            val titleText = _title.value.trim()
            if (titleText.isBlank()) {
                _uiState.value = UiState.Error("请输入物品标题")
                return@launch
            }
            if (_type.value != "lost" && _type.value != "found") {
                _uiState.value = UiState.Error("请选择类型")
                return@launch
            }

            _uiState.value = UiState.Loading

            try {
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

                // Upload images if any selected
                val imageUrls = mutableListOf<String>()
                val selectedUris = _selectedImages.value
                if (selectedUris.isNotEmpty()) {
                    _isUploadingImages.value = true
                    val itemId = UUID.randomUUID().toString()
                    for ((index, uri) in selectedUris.withIndex()) {
                        try {
                            val url = imageUploadRepository.uploadImage(
                                uri = uri,
                                bucket = "lost-found-images",
                                resourceId = "${itemId}_$index",
                            )
                            imageUrls.add(url)
                        } catch (e: Exception) {
                            Log.e(TAG, "Upload image $index failed", e)
                            // Continue with images that succeeded
                        }
                    }
                    _uploadedImageUrls.value = imageUrls
                    _isUploadingImages.value = false
                }

                val rewardInt = _reward.value.trim().toIntOrNull() ?: 0

                val body = buildMap<String, Any?> {
                    put("action", "publish_item")
                    put("title", titleText)
                    put("type", _type.value)
                    put("description", _description.value.trim().ifBlank { null })
                    put("images", encodeImageUrls(imageUrls))
                    put("location", _location.value.trim().ifBlank { null })
                    put("lost_date", _lostDate.value.trim().ifBlank { null })
                    put("category", _category.value)
                    put("reward", rewardInt)
                    put("contact", _contact.value.trim().ifBlank { "站内私信联系" })
                    put("school_id", schoolId)
                }

                val responseText = lostFoundRepository.invokeLostItemLifecycle(body)

                // Parse JSON response to get item_id
                val json = Json.parseToJsonElement(responseText) as JsonObject
                val itemId = json["item_id"]?.jsonPrimitive?.content
                    ?: throw IllegalStateException("EdgeFn 返回数据缺少 item_id")

                // Refresh local cache
                lostFoundRepository.refreshItemById(itemId)

                // Clear images after successful publish
                _selectedImages.value = emptyList()
                _uploadedImageUrls.value = emptyList()

                _uiState.value = UiState.Success(PublishedItemResult(itemId = itemId))
            } catch (e: Exception) {
                Log.e(TAG, "发布失物失败", e)
                _uiState.value = UiState.Error("发布失败，请稍后重试")
            } finally {
                _isUploadingImages.value = false
            }
        }
    }

    fun resetForm() {
        _title.value = ""
        _description.value = ""
        _type.value = "lost"
        _category.value = "其他"
        _location.value = ""
        _lostDate.value = ""
        _reward.value = ""
        _contact.value = "站内私信联系"
        _selectedImages.value = emptyList()
        _uploadedImageUrls.value = emptyList()
        _uiState.value = UiState.Success<PublishedItemResult?>(null)
    }
}

/** 将图片 URL 列表编码为 JSON 数组字符串 */
private fun encodeImageUrls(urls: List<String>): String {
    if (urls.isEmpty()) return "[]"
    return urls.joinToString(prefix = "[", postfix = "]", separator = ",") { "\"$it\"" }
}

/** 发布成功后返回的数据 */
data class PublishedItemResult(val itemId: String)

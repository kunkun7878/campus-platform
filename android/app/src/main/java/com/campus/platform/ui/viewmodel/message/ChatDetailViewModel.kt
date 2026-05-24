package com.campus.platform.ui.viewmodel.message

import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.platform.data.auth.AuthRepository
import com.campus.platform.data.local.entity.MessageEntity
import com.campus.platform.data.local.mapper.ConversationDto
import com.campus.platform.data.local.mapper.MessageDto
import com.campus.platform.data.model.Profile
import com.campus.platform.domain.repository.IImageUploadRepository
import com.campus.platform.domain.repository.IMessageRepository
import com.campus.platform.domain.repository.IUserRepository
import com.campus.platform.ui.viewmodel.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

private const val TAG = "ChatDetailVM"

/** 聊天详情页 UI 数据 */
data class ChatDetailUiData(
    val messages: List<MessageDto> = emptyList(),
    val conversation: ConversationDto? = null,
    val otherUser: Profile? = null,
    val currentUserId: String = "",
)

@HiltViewModel
class ChatDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val messageRepo: IMessageRepository,
    private val userRepo: IUserRepository,
    private val authRepository: AuthRepository,
    private val imageUploadRepository: IImageUploadRepository,
) : ViewModel() {

    /** 会话 ID，从导航参数获取 */
    private val conversationId: String = savedStateHandle["chatId"] ?: ""

    // ── 页面状态 ────────────────────────────────────────────

    private val _uiState = MutableStateFlow<UiState<ChatDetailUiData>>(UiState.Loading)
    val uiState: StateFlow<UiState<ChatDetailUiData>> = _uiState.asStateFlow()

    // ── 输入框 ──────────────────────────────────────────────

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    // ── 正在发送 ────────────────────────────────────────────

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _isUploadingImage = MutableStateFlow(false)
    val isUploadingImage: StateFlow<Boolean> = _isUploadingImage.asStateFlow()

    // ── 当前用户 ID ────────────────────────────────────────

    private var currentUserId: String? = null

    init {
        if (conversationId.isEmpty()) {
            _uiState.value = UiState.Error("未找到会话")
        } else {
            loadChat()
        }
    }

    // ── 加载聊天数据 ────────────────────────────────────────

    private fun loadChat() {
        viewModelScope.launch {
            try {
                currentUserId = authRepository.currentUserId()
                if (currentUserId == null) {
                    _uiState.value = UiState.Error("请先登录")
                    return@launch
                }

                // 加载会话信息
                val conv = messageRepo.getConversationById(conversationId)
                val otherId = if (conv != null) {
                    if (conv.user1Id == currentUserId) conv.user2Id else conv.user1Id
                } else null
                val otherProfile = if (otherId != null) {
                    try { userRepo.getProfile(otherId).first() } catch (_: Exception) { null }
                } else null

                // 观察消息流（自动刷新）
                launch {
                    messageRepo.getMessages(conversationId)
                        .catch { e ->
                            Log.e(TAG, "消息流异常", e)
                            val current = _uiState.value
                            if (current is UiState.Loading) {
                                _uiState.value = UiState.Error("加载消息失败")
                            }
                            emit(emptyList())
                        }
                        .collectLatest { messages ->
                            _uiState.value = UiState.Success(
                                ChatDetailUiData(
                                    messages = messages,
                                    conversation = conv,
                                    otherUser = otherProfile,
                                    currentUserId = currentUserId ?: "",
                                )
                            )
                        }
                }

                // 进入聊天详情自动标记已读
                markAsRead()
            } catch (e: Exception) {
                Log.e(TAG, "加载聊天失败", e)
                _uiState.value = UiState.Error("加载失败，请稍后重试")
            }
        }
    }

    // ── 标记已读 ────────────────────────────────────────────

    private fun markAsRead() {
        viewModelScope.launch {
            try {
                messageRepo.markAsRead(conversationId)
            } catch (e: Exception) {
                Log.e(TAG, "标记已读失败", e)
            }
        }
    }

    // ── 输入 ────────────────────────────────────────────────

    fun onInputChange(text: String) {
        _inputText.value = text
    }

    // ── 发送消息 ────────────────────────────────────────────

    fun sendMessage() {
        val content = _inputText.value.trim()
        if (content.isEmpty()) return

        viewModelScope.launch {
            _isSending.value = true
            _inputText.value = ""

            try {
                messageRepo.sendMessage(conversationId, content)
                // 乐观 UI 由 Repository 层处理：
                // 1) 立即写入 SENDING 状态的消息到 Room
                // 2) 后台发送到 Supabase
                // 3) 成功 → 更新为 SENT
                // 4) 失败 → 更新为 FAILED
                // UI 层通过 collectLatest 自动收到更新
            } catch (e: Exception) {
                Log.e(TAG, "发送消息失败", e)
                // 恢复输入框内容
                _inputText.value = content
            } finally {
                _isSending.value = false
            }
        }
    }

    // ── 重发失败消息 ────────────────────────────────────────

    fun resendMessage(messageId: String) {
        viewModelScope.launch {
            try {
                val data = (_uiState.value as? UiState.Success)?.data ?: return@launch
                val failedMsg = data.messages.find { it.id == messageId } ?: return@launch
                if (failedMsg.localStatus != MessageEntity.LOCAL_STATUS_FAILED) return@launch

                // 先删除失败的本地记录
                messageRepo.deleteMessage(messageId)
                // 重新发送（Repository 层会创建新记录）
                messageRepo.sendMessage(conversationId, failedMsg.content)
            } catch (e: Exception) {
                Log.e(TAG, "重发失败", e)
            }
        }
    }

    // ── 发送图片消息 ────────────────────────────────────────

    fun sendImage(uri: Uri) {
        viewModelScope.launch {
            _isUploadingImage.value = true
            try {
                val msgId = UUID.randomUUID().toString()
                val imageUrl = imageUploadRepository.uploadImage(
                    uri = uri,
                    bucket = "chat-images",
                    resourceId = msgId,
                )
                // Send the signed URL as message content
                messageRepo.sendMessage(conversationId, imageUrl)
            } catch (e: Exception) {
                Log.e(TAG, "发送图片失败", e)
                // The UI can show a toast via the error state
            } finally {
                _isUploadingImage.value = false
            }
        }
    }
}

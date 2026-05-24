package com.campus.platform.ui.viewmodel.community

import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.platform.data.auth.AuthRepository
import com.campus.platform.data.local.mapper.GroupMemberDto
import com.campus.platform.data.local.mapper.GroupMessageDto
import com.campus.platform.data.local.mapper.OfficialGroupDto
import com.campus.platform.data.model.Profile
import com.campus.platform.data.repository.ImageUploadRepository
import com.campus.platform.domain.repository.IGroupRepository
import com.campus.platform.domain.repository.IMessageRepository
import com.campus.platform.domain.repository.IUserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

private const val TAG = "GroupChatVM"

/**
 * UI model for a single group-chat message — fully resolved with sender info.
 * e.message is never passed through to UI; all error translation happens here.
 */
data class GroupMessageUi(
    val id: String,
    val senderId: String,
    val senderNickname: String,
    val senderAvatarUrl: String?,
    val content: String,
    val isSystemMessage: Boolean,
    val isMine: Boolean,
    val createdAt: String?,
)

@HiltViewModel
class GroupChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val groupRepo: IGroupRepository,
    private val messageRepo: IMessageRepository,
    private val userRepo: IUserRepository,
    private val authRepository: AuthRepository,
    private val imageUploadRepository: ImageUploadRepository,
) : ViewModel() {

    private val groupId: String = savedStateHandle["chatId"] ?: ""

    // ── User cache for mapping sender ID to profile ──────────

    private val userCache = mutableMapOf<String, Profile?>()

    // ── Current user ID ──────────────────────────────────────

    private val currentUserId = MutableStateFlow<String?>(null)

    // ── Group info ───────────────────────────────────────────

    private val _group = MutableStateFlow<OfficialGroupDto?>(null)
    val group: StateFlow<OfficialGroupDto?> = _group.asStateFlow()

    // ── Member status ────────────────────────────────────────

    private val _isMember = MutableStateFlow(false)
    val isMember: StateFlow<Boolean> = _isMember.asStateFlow()

    // ── Members list ─────────────────────────────────────────

    val members: StateFlow<List<GroupMemberDto>> = groupRepo
        .getMembersByGroupId(groupId)
        .catch { e ->
            Log.e(TAG, "Failed to load members", e)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    // ── Messages ─────────────────────────────────────────────

    val messages: StateFlow<List<GroupMessageUi>>

    // ── Online count ────────────────────────────────────────

    private val _onlineCount = MutableStateFlow(0)
    val onlineCount: StateFlow<Int> = _onlineCount.asStateFlow()

    // ── Input text ───────────────────────────────────────────

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _isUploadingImage = MutableStateFlow(false)
    val isUploadingImage: StateFlow<Boolean> = _isUploadingImage.asStateFlow()

    // ── Toast / status ───────────────────────────────────────

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _isJoining = MutableStateFlow(false)
    val isJoining: StateFlow<Boolean> = _isJoining.asStateFlow()

    private val _isLeaving = MutableStateFlow(false)
    val isLeaving: StateFlow<Boolean> = _isLeaving.asStateFlow()

    init {
        // Resolve current user and group info
        viewModelScope.launch {
            try {
                val uid = authRepository.currentUserId()
                currentUserId.value = uid

                val g = groupRepo.getGroupById(groupId)
                _group.value = g

                if (uid != null) {
                    _isMember.value = groupRepo.isMember(groupId, uid)
                    // Seed own profile into user cache
                    try {
                        userRepo.refreshProfile(uid)
                        val profile = userRepo.getProfile(uid).first()
                        userCache[uid] = profile
                    } catch (_: Exception) { /* best-effort */ }
                }

                // Online count approximates member count initially
                _onlineCount.value = g?.memberCount ?: 0
            } catch (e: Exception) {
                Log.e(TAG, "Init failed", e)
                _toastMessage.value = "加载群信息失败"
            }
        }

        // Messages: observe + resolve sender info
        messages = messageRepo
            .observeGroupMessages(groupId)
            .catch { e ->
                Log.e(TAG, "Failed to observe messages", e)
                emit(emptyList())
            }
            .map { dtos -> resolveMessages(dtos) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )
    }

    // ── Public actions ────────────────────────────────────────

    fun updateInputText(text: String) {
        _inputText.value = text
    }

    fun sendMessage() {
        val text = _inputText.value.trim()
        if (text.isEmpty() || _isSending.value) return
        if (!_isMember.value) {
            _toastMessage.value = "请先加入群聊"
            return
        }

        _isSending.value = true
        _inputText.value = ""
        viewModelScope.launch {
            try {
                messageRepo.sendGroupMessage(groupId, text)
            } catch (e: Exception) {
                Log.e(TAG, "sendMessage failed", e)
                _toastMessage.value = "发送失败，请重试"
                _inputText.value = text // Restore text on failure
            } finally {
                _isSending.value = false
            }
        }
    }

    fun joinGroup() {
        viewModelScope.launch {
            _isJoining.value = true
            try {
                val userId = authRepository.currentUserId()
                if (userId == null) {
                    _toastMessage.value = "请先登录"
                    return@launch
                }
                groupRepo.joinGroup(groupId, userId)
                _isMember.value = true
                _toastMessage.value = "已加入群聊"
            } catch (e: Exception) {
                Log.e(TAG, "joinGroup failed", e)
                _toastMessage.value = "加入失败，请重试"
            } finally {
                _isJoining.value = false
            }
        }
    }

    fun leaveGroup() {
        viewModelScope.launch {
            _isLeaving.value = true
            try {
                val userId = authRepository.currentUserId() ?: return@launch
                groupRepo.leaveGroup(groupId, userId)
                _isMember.value = false
                _toastMessage.value = "已退出群聊"
            } catch (e: Exception) {
                Log.e(TAG, "leaveGroup failed", e)
                _toastMessage.value = "退出失败，请重试"
            } finally {
                _isLeaving.value = false
            }
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    // ── Image message ─────────────────────────────────────────

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
                messageRepo.sendGroupMessage(groupId, imageUrl)
            } catch (e: Exception) {
                Log.e(TAG, "sendImage failed", e)
                _toastMessage.value = "图片发送失败，请重试"
            } finally {
                _isUploadingImage.value = false
            }
        }
    }

    // ── Internal helpers ──────────────────────────────────────

    /**
     * Map [GroupMessageDto] to [GroupMessageUi], resolving sender info from
     * the user cache and fetching any missing profiles.
     */
    private suspend fun resolveMessages(dtos: List<GroupMessageDto>): List<GroupMessageUi> {
        val mine = currentUserId.value
        val unseenSenders = dtos
            .filter { it.messageType == GroupMessageDto.TYPE_USER && it.senderId !in userCache }
            .map { it.senderId }
            .distinct()

        // Fetch missing profiles
        for (senderId in unseenSenders) {
            try {
                userRepo.refreshProfile(senderId)
                val profile = userRepo.getProfile(senderId).first()
                userCache[senderId] = profile
            } catch (_: Exception) { /* best-effort; show fallback nickname */ }
        }

        return dtos.map { dto ->
            if (dto.messageType == GroupMessageDto.TYPE_SYSTEM) {
                GroupMessageUi(
                    id = dto.id,
                    senderId = dto.senderId,
                    senderNickname = "",
                    senderAvatarUrl = null,
                    content = dto.content,
                    isSystemMessage = true,
                    isMine = false,
                    createdAt = dto.createdAt,
                )
            } else {
                val profile = userCache[dto.senderId]
                val isMine = mine != null && dto.senderId == mine
                GroupMessageUi(
                    id = dto.id,
                    senderId = dto.senderId,
                    senderNickname = profile?.nickname ?: dto.senderId.take(8),
                    senderAvatarUrl = profile?.avatarUrl ?: profile?.nickname?.let {
                        // fallback: no avatar from profile
                        null
                    },
                    content = dto.content,
                    isSystemMessage = false,
                    isMine = isMine,
                    createdAt = dto.createdAt,
                )
            }
        }
    }
}

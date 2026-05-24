package com.campus.platform.ui.viewmodel.community

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.platform.data.auth.AuthRepository
import com.campus.platform.data.local.UserPreferencesDataStore
import com.campus.platform.data.local.mapper.CommunityCommentDto
import com.campus.platform.data.local.mapper.CommunityPostDto
import com.campus.platform.domain.repository.ICommunityRepository
import com.campus.platform.domain.repository.ModerationResult
import com.campus.platform.ui.viewmodel.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

private const val TAG = "PostDetailVM"

@HiltViewModel
class PostDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val communityRepo: ICommunityRepository,
    private val authRepository: AuthRepository,
    private val prefs: UserPreferencesDataStore,
) : ViewModel() {

    private val postId: String = savedStateHandle["postId"] ?: ""

    // ── 帖子数据 ──

    private val _post = MutableStateFlow<CommunityPostDto?>(null)
    val post: StateFlow<CommunityPostDto?> = _post.asStateFlow()

    // ── 点赞状态（乐观更新） ──

    private val _isLikedLocal = MutableStateFlow(false)
    val isLiked: StateFlow<Boolean> = _isLikedLocal.asStateFlow()

    private val _likeCountLocal = MutableStateFlow(0)
    val likeCount: StateFlow<Int> = _likeCountLocal.asStateFlow()

    // ── 评论列表 ──

    val comments: StateFlow<UiState<List<CommunityCommentDto>>> = communityRepo
        .getCommentsByPostId(postId)
        .map { list -> UiState.Success(list) as UiState<List<CommunityCommentDto>> }
        .catch { e ->
            Log.e(TAG, "加载评论失败", e)
            emit(UiState.Error("加载评论失败"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState.Loading,
        )

    // ── 评论输入 ──

    private val _commentText = MutableStateFlow("")
    val commentText: StateFlow<String> = _commentText.asStateFlow()

    private val _isSendingComment = MutableStateFlow(false)
    val isSendingComment: StateFlow<Boolean> = _isSendingComment.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    // 导航回退事件（删除成功后触发）
    private val _navBack = MutableStateFlow(false)
    val navBack: StateFlow<Boolean> = _navBack.asStateFlow()

    // ── 当前用户信息 ──

    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    init {
        // 加载帖子详情
        viewModelScope.launch {
            val userId = authRepository.currentUserId()
            _currentUserId.value = userId

            val p = communityRepo.getPostById(postId)
            if (p != null) {
                _post.value = p
                _likeCountLocal.value = p.likeCount
                if (userId != null) {
                    _isLikedLocal.value = communityRepo.isLiked(postId, userId)
                }
            }
        }
    }

    // ── 点赞（乐观更新） ──

    fun toggleLike() {
        viewModelScope.launch {
            val userId = _currentUserId.value ?: return@launch

            // 乐观更新
            val wasLiked = _isLikedLocal.value
            _isLikedLocal.value = !wasLiked
            _likeCountLocal.value = if (wasLiked) (_likeCountLocal.value - 1).coerceAtLeast(0)
                else _likeCountLocal.value + 1

            try {
                val newState = communityRepo.toggleLike(postId, userId)
                // 用真实结果修正
                _isLikedLocal.value = newState
                // 重新从 DB 获取准确计数
                val updated = communityRepo.getPostById(postId)
                if (updated != null) {
                    _likeCountLocal.value = updated.likeCount
                }
            } catch (e: Exception) {
                Log.e(TAG, "点赞操作失败", e)
                // 回滚
                _isLikedLocal.value = wasLiked
                _likeCountLocal.value = if (wasLiked) (_likeCountLocal.value + 1)
                    else (_likeCountLocal.value - 1).coerceAtLeast(0)
                _actionError.value = "操作失败，请重试"
            }
        }
    }

    // ── 发送评论 ──

    fun setCommentText(text: String) {
        _commentText.value = text
    }

    fun submitComment() {
        val text = _commentText.value.trim()
        if (text.isBlank()) return

        viewModelScope.launch {
            _isSendingComment.value = true
            try {
                val userId = _currentUserId.value ?: return@launch
                val schoolId = prefs.schoolId.first() ?: return@launch
                val comment = CommunityCommentDto(
                    id = UUID.randomUUID().toString(),
                    postId = postId,
                    authorId = userId,
                    parentId = null,
                    content = text,
                    likeCount = 0,
                    status = "published",
                    schoolId = schoolId,
                    createdAt = java.time.Instant.now().toString(),
                    updatedAt = null,
                )
                when (val result = communityRepo.publishCommentViaModeration(comment)) {
                    is ModerationResult.Blocked -> {
                        _actionError.value = "评论被拒绝：${result.reason}"
                    }
                    is ModerationResult.Reviewing -> {
                        _commentText.value = ""
                        communityRepo.refreshComments(postId)
                        _actionError.value = "评论已提交审核"
                    }
                    is ModerationResult.Passed -> {
                        _commentText.value = ""
                        communityRepo.refreshComments(postId)
                    }
                    is ModerationResult.Error -> {
                        _actionError.value = result.message
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "发送评论失败", e)
                _actionError.value = "发送失败，请重试"
            } finally {
                _isSendingComment.value = false
            }
        }
    }

    // ── 编辑帖子 ──

    fun updatePost(title: String, content: String) {
        val currentPost = _post.value ?: return
        viewModelScope.launch {
            try {
                val updatedPost = currentPost.copy(
                    title = title,
                    content = content,
                    updatedAt = java.time.Instant.now().toString(),
                )
                when (val result = communityRepo.updatePostViaModeration(updatedPost)) {
                    is ModerationResult.Blocked -> {
                        _actionError.value = "编辑被拒绝：${result.reason}"
                    }
                    is ModerationResult.Reviewing -> {
                        _actionError.value = "编辑内容已提交审核"
                        val updated = communityRepo.getPostById(postId)
                        if (updated != null) _post.value = updated
                    }
                    is ModerationResult.Passed -> {
                        val updated = communityRepo.getPostById(postId)
                        if (updated != null) _post.value = updated
                    }
                    is ModerationResult.Error -> {
                        _actionError.value = result.message
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "编辑帖子失败", e)
                _actionError.value = "编辑失败，请重试"
            }
        }
    }

    // ── 删除帖子 ──

    fun deletePost() {
        viewModelScope.launch {
            try {
                communityRepo.updatePost(postId, mapOf("status" to "deleted"))
                // 也移除本地
                _navBack.value = true
            } catch (e: Exception) {
                Log.e(TAG, "删除帖子失败", e)
                _actionError.value = "删除失败，请重试"
            }
        }
    }

    // ── 删除评论 ──

    fun deleteComment(commentId: String) {
        viewModelScope.launch {
            try {
                communityRepo.deleteComment(commentId)
                communityRepo.refreshComments(postId)
            } catch (e: Exception) {
                Log.e(TAG, "删除评论失败", e)
                _actionError.value = "删除失败，请重试"
            }
        }
    }

    fun onNavBackConsumed() {
        _navBack.value = false
    }

    fun clearActionError() {
        _actionError.value = null
    }
}

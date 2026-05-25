package com.campus.platform.data.auth

import android.util.Log
import com.campus.platform.data.model.Profile
import com.campus.platform.push.FcmTokenManager
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Supabase Auth 封装。
 *
 * Phase 2 主认证方式：手机号 + 密码
 * 手机号 + SMS OTP 登录：通过反射获取 OtpType.Phone 枚举值以绕过 Kotlin-Java 互操作问题。
 */
@Singleton
class AuthRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val fcmTokenManager: FcmTokenManager,
) {

    companion object {
        private const val TAG = "AuthRepository"
    }

    // ── Session ────────────────────────────────────────────

    /** 响应式 session 状态流 */
    val sessionFlow: Flow<UserSession?> = supabase.auth.sessionStatus
        .filterIsInstance<SessionStatus.Authenticated>()
        .map { it.session }

    /** 当前是否有有效 session（Flow） */
    val isLoggedInFlow: Flow<Boolean> = supabase.auth.sessionStatus
        .map { it is SessionStatus.Authenticated }

    /** 同步获取当前 session（可能为 null） */
    suspend fun getSession(): UserSession? = supabase.auth.currentSessionOrNull()

    /** 当前登录用户 ID（可能为 null） */
    suspend fun currentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    // ── 手机号 + SMS OTP ──────────────────────────────────

    /**
     * 发送 SMS 验证码到指定手机号。
     * 发送前应由 UI 层完成 CAPTCHA 验证。
     */
    suspend fun signInWithOtp(phone: String) {
        supabase.auth.signInWith(OTP) {
            this.phone = "+86$phone"
        }
    }

    /**
     * 验证 SMS OTP 令牌并完成登录。
     *
     * OtpType.Phone 是 Java 内部枚举，Kotlin 无法直接作为表达式使用
     * （SDK 3.1.2 的 Kotlin-Java 互操作问题）。通过反射获取枚举值作为替代方案。
     * SDK 升级到 3.2+ 后可简化为：
     *   supabase.auth.verifyPhoneOtp(type = OtpType.Phone, phone = "+86$phone", token = token)
     */
    suspend fun verifyOtp(phone: String, token: String) {
        @Suppress("UNCHECKED_CAST")
        val phoneType = OtpType.Phone::class.java.enumConstants!!.first()
        supabase.auth.verifyPhoneOtp(type = phoneType, phone = "+86$phone", token = token)
    }

    // ── 邮箱 + 密码 登录/注册 ─────────────────────────────

    private suspend fun signInWithEmail(email: String, password: String) {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    private suspend fun signUpWithEmail(email: String, password: String) {
        supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
    }

    // ── 手机号 + 密码 登录/注册 ───────────────────────────

    /** 手机号 + 密码登录（内部使用虚拟邮箱） */
    suspend fun signInWithPhoneAndPassword(phone: String, password: String) {
        signInWithEmail(phoneToEmail(phone), password)
    }

    /** 手机号 + 密码注册（内部使用虚拟邮箱） */
    suspend fun signUpWithPhoneAndPassword(phone: String, password: String) {
        signUpWithEmail(phoneToEmail(phone), password)
        val uid = currentUserId()
        if (uid != null) {
            try {
                supabase.postgrest.from("profiles").update(mapOf("phone" to phone)) {
                    filter { eq("id", uid) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update phone", e)
            }
        }
    }

    // ── 密码管理 ───────────────────────────────────────────

    /** 更新当前用户密码 */
    suspend fun updatePassword(newPassword: String) {
        supabase.auth.updateUser {
            password = newPassword
        }
    }

    /** 发送密码重置 OTP（预留，当前与 signInWithOtp 共用流程） */
    suspend fun resetPassword(phone: String) {
        signInWithOtp(phone)
    }

    // ── 用户资料 ───────────────────────────────────────────

    /** 获取当前用户的 profile */
    suspend fun getProfile(): Profile? {
        val uid = currentUserId() ?: return null
        return try {
            supabase.postgrest
                .from("profiles")
                .select { filter { eq("id", uid) } }
                .decodeSingleOrNull<Profile>()
        } catch (e: Exception) {
            null
        }
    }

    /** 获取当前用户的 profile（Flow 响应式） */
    fun getProfileFlow(): Flow<Profile?> = isLoggedInFlow.map { loggedIn ->
        if (!loggedIn) return@map null
        try {
            supabase.postgrest
                .from("profiles")
                .select {
                    filter {
                        eq("id", supabase.auth.currentUserOrNull()?.id ?: "")
                    }
                }
                .decodeSingleOrNull<Profile>()
        } catch (e: Exception) {
            null
        }
    }

    /** 更新当前用户的 profile */
    suspend fun updateProfile(
        nickname: String? = null,
        avatarUrl: String? = null,
        email: String? = null,
    ) {
        val uid = currentUserId() ?: return
        val updates = buildMap<String, String?> {
            nickname?.let { put("nickname", it) }
            avatarUrl?.let { put("avatar_url", it) }
            email?.let { put("email", it) }
        }
        supabase.postgrest
            .from("profiles")
            .update(updates) {
                filter { eq("id", uid) }
            }
    }

    // ── 学校和校区选择 ─────────────────────────────────────

    /** 选择学校（仅在首次选校时调用，后续不可更改） */
    suspend fun selectSchool(uid: String, schoolId: String, campusId: String) {
        supabase.postgrest
            .from("profiles")
            .update(
                mapOf(
                    "school_id" to schoolId,
                    "campus_id" to campusId,
                )
            ) {
                filter { eq("id", uid) }
            }
    }

    /** 切换校区（仅限同一学校内） */
    suspend fun switchCampus(uid: String, campusId: String) {
        supabase.postgrest
            .from("profiles")
            .update(mapOf("campus_id" to campusId)) {
                filter { eq("id", uid) }
            }
    }

    // ── 账号管理 ───────────────────────────────────────────

    /** 软删除账号 */
    suspend fun deleteAccount(reason: String? = null) {
        val uid = currentUserId() ?: return
        // Deactivate all FCM tokens before sign-out so push notifications
        // stop targeting this user's old devices.
        fcmTokenManager.deactivateAllTokens(uid)
        supabase.postgrest
            .from("profiles")
            .update(
                mapOf(
                    "status" to 2,
                    "deleted_at" to java.time.Instant.now().toString(),
                )
            ) {
                filter { eq("id", uid) }
            }
        supabase.auth.signOut()
    }

    /** 退出登录 */
    suspend fun signOut() {
        // Deactivate all FCM tokens before sign-out so push notifications
        // stop targeting this user's old devices.
        currentUserId()?.let { fcmTokenManager.deactivateAllTokens(it) }
        supabase.auth.signOut()
    }

    /** 刷新当前 session */
    suspend fun refreshSession() {
        supabase.auth.refreshCurrentSession()
    }

    // ── Helper ─────────────────────────────────────────────

    private fun phoneToEmail(phone: String) = "$phone@campus.local"
}

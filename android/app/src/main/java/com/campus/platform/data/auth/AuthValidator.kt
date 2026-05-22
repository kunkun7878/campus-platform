package com.campus.platform.data.auth

/**
 * 认证相关校验工具。
 *
 * 提供手机号格式校验、密码强度评估、短信验证码格式校验。
 */
object AuthValidator {

    /** 手机号正则：中国大陆 1 开头的 11 位数字 */
    private val PHONE_REGEX = Regex("^1[3-9]\\d{9}$")

    /** 验证码正则：6 位数字 */
    private val CODE_REGEX = Regex("^\\d{6}$")

    /** 密码强度等级 */
    enum class PasswordStrength(val label: String, val description: String) {
        Weak("弱", "密码强度不足，建议增加字母和数字组合"),
        Medium("中", "密码强度一般，建议添加特殊字符"),
        Strong("强", "密码强度良好"),
    }

    /**
     * 校验手机号格式。
     *
     * @return null 表示校验通过，非 null 为错误信息。
     */
    fun validatePhone(phone: String): String? {
        if (phone.isBlank()) return "请输入手机号"
        if (!PHONE_REGEX.matches(phone)) return "请输入正确的 11 位手机号"
        return null
    }

    /**
     * 评估密码强度。
     *
     * 检查条件：长度 >= 8、包含字母、包含数字、包含特殊字符。
     * 满足条件数 <= 2 → Weak, == 3 → Medium, == 4 → Strong。
     */
    fun evaluatePasswordStrength(password: String): PasswordStrength {
        if (password.length < 8) return PasswordStrength.Weak

        var score = 0
        if (password.any { it.isDigit() }) score++
        if (password.any { it.isLetter() }) score++
        if (password.any { !it.isLetterOrDigit() }) score++
        if (password.length >= 12) score++

        return when {
            score <= 2 -> PasswordStrength.Weak
            score == 3 -> PasswordStrength.Medium
            else -> PasswordStrength.Strong
        }
    }

    /**
     * 校验密码格式。
     *
     * @return null 表示校验通过，非 null 为错误信息。
     */
    fun validatePassword(password: String): String? {
        if (password.length < 8) return "密码长度至少 8 位"
        if (!password.any { it.isDigit() }) return "密码需要包含数字"
        if (!password.any { it.isLetter() }) return "密码需要包含字母"
        return null
    }

    /**
     * 校验短信验证码格式。
     *
     * @return null 表示校验通过，非 null 为错误信息。
     */
    fun validateVerificationCode(code: String): String? {
        if (code.isBlank()) return "请输入验证码"
        if (!CODE_REGEX.matches(code)) return "验证码为 6 位数字"
        return null
    }
}

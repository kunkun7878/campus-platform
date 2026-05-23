package com.campus.platform.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String,
    val phone: String? = null,
    val email: String? = null,
    @SerialName("email_verified_at")
    val emailVerifiedAt: String? = null,
    val nickname: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    @SerialName("school_id")
    val schoolId: String? = null,
    @SerialName("campus_id")
    val campusId: String? = null,
    @SerialName("is_agent")
    val isAgent: Boolean = false,
    val status: Int = 0,
    @SerialName("runner_status")
    val runnerStatus: String = "none",
    @SerialName("invite_code")
    val inviteCode: String? = null,
    @SerialName("referrer_id")
    val referrerId: String? = null,
    @SerialName("deleted_at")
    val deletedAt: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
)

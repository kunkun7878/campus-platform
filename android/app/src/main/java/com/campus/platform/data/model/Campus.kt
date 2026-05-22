package com.campus.platform.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Campus(
    val id: String,
    @SerialName("school_id")
    val schoolId: String,
    val name: String,
    val address: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
)

package com.campus.platform.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class School(
    val id: String,
    val name: String,
    val abbreviation: String? = null,
    val city: String? = null,
    val province: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
)

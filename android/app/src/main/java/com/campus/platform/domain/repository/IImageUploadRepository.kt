package com.campus.platform.domain.repository

import android.net.Uri

interface IImageUploadRepository {
    suspend fun uploadImage(uri: Uri, bucket: String, resourceId: String): String
    suspend fun uploadImages(uris: List<Uri>, bucket: String, resourceIdPrefix: String): List<String>
}

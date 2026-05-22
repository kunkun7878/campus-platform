package com.campus.platform.domain.repository

import com.campus.platform.data.local.mapper.RunnerApplicationDto
import kotlinx.coroutines.flow.Flow

interface IRunnerApplicationRepository {

    suspend fun getMyApplication(userId: String, schoolId: String): RunnerApplicationDto?

    suspend fun submitApplication(application: RunnerApplicationDto)

    suspend fun getApplicationStatus(userId: String, schoolId: String): String?
}

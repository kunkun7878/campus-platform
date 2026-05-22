package com.campus.platform.domain.repository

import com.campus.platform.data.local.mapper.RunnerTaskDto
import kotlinx.coroutines.flow.Flow

interface IRunnerTaskRepository {

    fun getTasksBySchool(schoolId: String): Flow<List<RunnerTaskDto>>

    fun getTasksByStatus(schoolId: String, status: String): Flow<List<RunnerTaskDto>>

    suspend fun getTaskById(id: String): RunnerTaskDto?

    fun getTasksByPublisher(userId: String): Flow<List<RunnerTaskDto>>

    fun getTasksByRunner(userId: String): Flow<List<RunnerTaskDto>>

    suspend fun publishTask(task: RunnerTaskDto)

    suspend fun updateTask(id: String, updates: Map<String, Any?>)

    suspend fun refreshTasks(schoolId: String)
}

package com.campus.platform.domain.repository

import com.campus.platform.data.local.mapper.RunnerTaskDto
import kotlinx.coroutines.flow.Flow

interface IRunnerTaskRepository {

    fun getTasksBySchool(schoolId: String): Flow<List<RunnerTaskDto>>

    fun getTasksByStatus(schoolId: String, status: String): Flow<List<RunnerTaskDto>>

    suspend fun getTaskById(id: String): RunnerTaskDto?

    fun getTasksByPublisher(userId: String, schoolId: String): Flow<List<RunnerTaskDto>>

    fun getTasksByRunner(userId: String, schoolId: String): Flow<List<RunnerTaskDto>>

    suspend fun publishTask(task: RunnerTaskDto)

    suspend fun updateTask(id: String, updates: Map<String, Any?>)

    suspend fun refreshTasks(schoolId: String)

    /** 强制从 Supabase 重新拉取单条任务并 upsert Room */
    suspend fun refreshTask(taskId: String)
}

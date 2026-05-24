package com.campus.platform.domain.repository

import com.campus.platform.data.local.mapper.RunnerOrderDto
import kotlinx.coroutines.flow.Flow

interface IRunnerOrderRepository {

    fun getOrdersByBuyer(userId: String): Flow<List<RunnerOrderDto>>

    fun getOrdersByRunner(userId: String, schoolId: String): Flow<List<RunnerOrderDto>>

    fun getOrdersByTaskId(taskId: String): Flow<List<RunnerOrderDto>>

    suspend fun getOrderById(id: String): RunnerOrderDto?

    suspend fun createOrder(order: RunnerOrderDto)

    suspend fun updateOrderStatus(id: String, status: String)

    suspend fun refreshOrders(schoolId: String)

    /** 强制从 Supabase 重新拉取单条订单并 upsert Room */
    suspend fun refreshOrder(orderId: String)
}

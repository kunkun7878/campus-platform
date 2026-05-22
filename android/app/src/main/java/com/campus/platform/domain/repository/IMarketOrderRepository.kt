package com.campus.platform.domain.repository

import com.campus.platform.data.local.mapper.MarketOrderDto
import kotlinx.coroutines.flow.Flow

interface IMarketOrderRepository {

    fun getOrdersByBuyer(userId: String): Flow<List<MarketOrderDto>>

    fun getOrdersBySeller(userId: String): Flow<List<MarketOrderDto>>

    suspend fun getOrderById(id: String): MarketOrderDto?

    suspend fun createOrder(order: MarketOrderDto)

    suspend fun updateOrderStatus(id: String, status: String)

    suspend fun refreshOrders(schoolId: String)
}

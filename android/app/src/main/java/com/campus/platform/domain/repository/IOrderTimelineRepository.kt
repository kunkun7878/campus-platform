package com.campus.platform.domain.repository

import com.campus.platform.data.local.mapper.OrderTimelineDto
import kotlinx.coroutines.flow.Flow

interface IOrderTimelineRepository {

    fun getTimelineByOrderId(orderId: String): Flow<List<OrderTimelineDto>>

    suspend fun refreshTimeline(orderId: String)
}

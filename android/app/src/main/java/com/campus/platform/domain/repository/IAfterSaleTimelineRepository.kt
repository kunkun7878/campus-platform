package com.campus.platform.domain.repository

import com.campus.platform.data.local.mapper.AfterSaleTimelineDto
import kotlinx.coroutines.flow.Flow

interface IAfterSaleTimelineRepository {

    fun getTimelineByAfterSaleId(afterSaleId: String): Flow<List<AfterSaleTimelineDto>>

    suspend fun refreshTimeline(afterSaleId: String)
}

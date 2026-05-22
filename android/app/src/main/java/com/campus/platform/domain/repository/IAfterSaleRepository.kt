package com.campus.platform.domain.repository

import com.campus.platform.data.local.mapper.AfterSaleDto
import kotlinx.coroutines.flow.Flow

interface IAfterSaleRepository {

    fun getAfterSalesByRequester(userId: String): Flow<List<AfterSaleDto>>

    fun getAfterSalesByOrderId(orderId: String): Flow<List<AfterSaleDto>>

    suspend fun getAfterSaleById(id: String): AfterSaleDto?

    suspend fun createAfterSale(afterSale: AfterSaleDto)

    suspend fun updateAfterSaleStatus(id: String, status: String, resultComment: String? = null)

    suspend fun refreshAfterSales(userId: String)
}

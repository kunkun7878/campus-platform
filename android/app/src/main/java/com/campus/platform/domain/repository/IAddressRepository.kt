package com.campus.platform.domain.repository

import com.campus.platform.data.local.mapper.UserAddressDto
import kotlinx.coroutines.flow.Flow

interface IAddressRepository {

    fun getAddresses(userId: String): Flow<List<UserAddressDto>>

    suspend fun addAddress(address: UserAddressDto)

    suspend fun updateAddress(address: UserAddressDto)

    suspend fun deleteAddress(id: String)

    suspend fun refreshAddresses(userId: String)

    suspend fun getAddressById(id: String): UserAddressDto?
}

package com.campus.platform.data.repository

import com.campus.platform.data.local.dao.UserDao
import com.campus.platform.data.local.mapper.UserAddressDto
import com.campus.platform.data.local.mapper.toDto
import com.campus.platform.data.local.mapper.toEntity
import com.campus.platform.domain.repository.IAddressRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class AddressApiDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    val label: String,
    @SerialName("contact_name") val contactName: String,
    @SerialName("contact_phone") val contactPhone: String,
    val address: String,
    @SerialName("is_default") val isDefault: Boolean = false,
    @SerialName("school_id") val schoolId: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

private fun AddressApiDto.toMapperDto() = UserAddressDto(
    id = id, userId = userId, label = label,
    contactName = contactName, contactPhone = contactPhone,
    address = address, isDefault = isDefault, schoolId = schoolId,
    createdAt = createdAt, updatedAt = updatedAt,
)

private fun UserAddressDto.toApiDto() = AddressApiDto(
    id = id, userId = userId, label = label,
    contactName = contactName, contactPhone = contactPhone,
    address = address, isDefault = isDefault, schoolId = schoolId,
    createdAt = createdAt, updatedAt = updatedAt,
)

@Singleton
class AddressRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val userDao: UserDao,
) : IAddressRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun getAddresses(userId: String): Flow<List<UserAddressDto>> {
        scope.launch { refreshAddresses(userId) }
        return userDao.getAddressesByUserId(userId).map { entities ->
            entities.map { it.toDto() }
        }
    }

    override suspend fun addAddress(address: UserAddressDto) {
        val result = supabase.postgrest
            .from("user_addresses")
            .insert(address.toApiDto()) { select() }
            .decodeSingle<AddressApiDto>()
        userDao.upsertAddress(result.toMapperDto().toEntity())
    }

    override suspend fun updateAddress(address: UserAddressDto) {
        supabase.postgrest
            .from("user_addresses")
            .update(address.toApiDto()) { filter { eq("id", address.id) } }
        userDao.upsertAddress(address.toEntity())
    }

    override suspend fun deleteAddress(id: String) {
        supabase.postgrest
            .from("user_addresses")
            .delete { filter { eq("id", id) } }
        userDao.deleteAddressById(id)
    }

    override suspend fun refreshAddresses(userId: String) {
        try {
            val result = supabase.postgrest
                .from("user_addresses")
                .select { filter { eq("user_id", userId) } }
                .decodeList<AddressApiDto>()
            userDao.upsertAllAddresses(result.map { it.toMapperDto().toEntity() })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, "Refresh error", e)
        }
    }

    override suspend fun getAddressById(id: String): UserAddressDto? {
        return userDao.getAddressById(id)?.toDto()
    }
}

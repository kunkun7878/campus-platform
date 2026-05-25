package com.campus.platform.ui.viewmodel.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.platform.data.local.mapper.UserAddressDto
import com.campus.platform.domain.repository.IAddressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AddressManageViewModel @Inject constructor(
    private val addressRepository: IAddressRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddressManageUiState())
    val uiState: StateFlow<AddressManageUiState> = _uiState.asStateFlow()

    private val _formState = MutableStateFlow(AddressFormState())
    val formState: StateFlow<AddressFormState> = _formState.asStateFlow()

    fun loadAddresses(userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            addressRepository.getAddresses(userId).collect { addresses ->
                _uiState.value = _uiState.value.copy(
                    addresses = addresses,
                    isLoading = false,
                )
            }
        }
    }

    fun showAddDialog() {
        _formState.value = AddressFormState()
        _uiState.value = _uiState.value.copy(showDialog = true, editingAddress = null)
    }

    fun showEditDialog(address: UserAddressDto) {
        _formState.value = AddressFormState(
            label = address.label,
            contactName = address.contactName,
            contactPhone = address.contactPhone,
            addressText = address.address,
            isDefault = address.isDefault,
        )
        _uiState.value = _uiState.value.copy(showDialog = true, editingAddress = address)
    }

    fun dismissDialog() {
        _uiState.value = _uiState.value.copy(showDialog = false, editingAddress = null)
    }

    fun onLabelChange(value: String) {
        _formState.value = _formState.value.copy(label = value)
    }

    fun onContactNameChange(value: String) {
        _formState.value = _formState.value.copy(contactName = value)
    }

    fun onContactPhoneChange(value: String) {
        _formState.value = _formState.value.copy(contactPhone = value)
    }

    fun onAddressChange(value: String) {
        _formState.value = _formState.value.copy(addressText = value)
    }

    fun onDefaultChange(value: Boolean) {
        _formState.value = _formState.value.copy(isDefault = value)
    }

    fun saveAddress(userId: String) {
        val form = _formState.value
        if (form.label.isBlank() || form.contactName.isBlank() || form.contactPhone.isBlank() || form.addressText.isBlank()) {
            _formState.value = _formState.value.copy(error = "请填写完整信息")
            return
        }

        viewModelScope.launch {
            try {
                val editing = _uiState.value.editingAddress
                if (editing != null) {
                    addressRepository.updateAddress(
                        editing.copy(
                            label = form.label,
                            contactName = form.contactName,
                            contactPhone = form.contactPhone,
                            address = form.addressText,
                            isDefault = form.isDefault,
                        )
                    )
                } else {
                    val newAddress = UserAddressDto(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        label = form.label,
                        contactName = form.contactName,
                        contactPhone = form.contactPhone,
                        address = form.addressText,
                        isDefault = form.isDefault,
                        schoolId = null,
                        createdAt = null,
                        updatedAt = null,
                    )
                    addressRepository.addAddress(newAddress)
                }
                dismissDialog()
            } catch (e: Exception) {
                _formState.value = _formState.value.copy(error = e.message ?: "保存失败")
            }
        }
    }

    fun deleteAddress(id: String) {
        viewModelScope.launch {
            try {
                addressRepository.deleteAddress(id)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "删除失败")
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
        _formState.value = _formState.value.copy(error = null)
    }
}

data class AddressManageUiState(
    val addresses: List<UserAddressDto> = emptyList(),
    val isLoading: Boolean = false,
    val showDialog: Boolean = false,
    val editingAddress: UserAddressDto? = null,
    val error: String? = null,
)

data class AddressFormState(
    val label: String = "",
    val contactName: String = "",
    val contactPhone: String = "",
    val addressText: String = "",
    val isDefault: Boolean = false,
    val error: String? = null,
)

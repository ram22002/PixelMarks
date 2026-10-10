package com.pixelmarks.feature.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pixelmarks.core.database.entity.ConnectedDeviceEntity
import com.pixelmarks.data.repository.ConnectedDeviceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ConnectedDevicesViewModel(
    private val repository: ConnectedDeviceRepository
) : ViewModel() {

    val devices: StateFlow<List<ConnectedDeviceEntity>> = repository.observeAllDevices()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun revokeDevice(deviceId: Long) {
        viewModelScope.launch {
            repository.revokeDevice(deviceId)
        }
    }

    fun revokeAllDevices() {
        viewModelScope.launch {
            repository.revokeAllDevices()
        }
    }

    fun deleteDevice(deviceId: Long) {
        viewModelScope.launch {
            repository.deleteDevice(deviceId)
        }
    }

    companion object {
        fun provideFactory(
            repository: ConnectedDeviceRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ConnectedDevicesViewModel(repository) as T
            }
        }
    }
}

package com.pixelmarks.feature.connectpc

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pixelmarks.core.pcserver.LocalNetworkUtil
import com.pixelmarks.core.pcserver.LocalServerManager
import com.pixelmarks.core.pcserver.PairingManager
import com.pixelmarks.core.pcserver.QrCodeGenerator
import com.pixelmarks.core.pcserver.model.PairingState
import com.pixelmarks.core.pcserver.model.PcServerState
import com.pixelmarks.data.repository.ConnectedDeviceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ConnectPcUiState(
    val isWifiConnected: Boolean = true,
    val serverState: PcServerState = PcServerState.Stopped,
    val pairingState: PairingState = PairingState.Idle,
    val qrBitmap: Bitmap? = null,
    val activeDevicesCount: Int = 0,
    val pendingConfirmation: PairingState.PendingConfirmation? = null,
    val errorMessage: String? = null
)

class ConnectPcViewModel(
    private val context: Context,
    private val serverManager: LocalServerManager,
    private val pairingManager: PairingManager,
    private val deviceRepository: ConnectedDeviceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConnectPcUiState())
    val uiState: StateFlow<ConnectPcUiState> = _uiState.asStateFlow()

    init {
        checkNetworkStatus()

        viewModelScope.launch {
            combine(
                serverManager.serverState,
                pairingManager.pairingState,
                deviceRepository.observeAllDevices()
            ) { server, pairing, devices ->
                val activeCount = devices.count { it.revokedAt == null && !it.isBlocked }
                val pending = pairing as? PairingState.PendingConfirmation

                val qr = if (pairing is PairingState.Active && pairing.qrContent.isNotBlank()) {
                    withContext(Dispatchers.Default) {
                        QrCodeGenerator.generateQrBitmap(pairing.qrContent, size = 480)
                    }
                } else null

                _uiState.update { current ->
                    current.copy(
                        serverState = server,
                        pairingState = pairing,
                        qrBitmap = qr,
                        activeDevicesCount = activeCount,
                        pendingConfirmation = pending
                    )
                }
            }.collect {}
        }
    }

    fun checkNetworkStatus() {
        val connected = LocalNetworkUtil.isLocalNetworkConnected(context)
        _uiState.update { it.copy(isWifiConnected = connected) }
    }

    fun toggleServer() {
        val currentState = _uiState.value.serverState
        if (currentState is PcServerState.Running || currentState is PcServerState.Starting) {
            stopServer()
        } else {
            startServer()
        }
    }

    fun startServer() {
        checkNetworkStatus()
        val ip = LocalNetworkUtil.getLocalIpAddress()
        if (ip == null && !_uiState.value.isWifiConnected) {
            _uiState.update {
                it.copy(errorMessage = "Please connect to a local Wi-Fi network before starting the server.")
            }
            return
        }

        _uiState.update { it.copy(errorMessage = null) }
        val success = serverManager.startServer()
        if (success) {
            generatePairingCode()
        }
    }

    fun stopServer() {
        serverManager.stopServer()
        _uiState.update { it.copy(qrBitmap = null) }
    }

    fun generatePairingCode() {
        val server = serverManager.serverState.value
        val baseUrl = if (server is PcServerState.Running) {
            server.serverUrl
        } else {
            val ip = LocalNetworkUtil.getLocalIpAddress() ?: "127.0.0.1"
            "http://$ip:8080"
        }

        try {
            pairingManager.generateNewCode(baseUrl)
        } catch (e: Exception) {
            _uiState.update { it.copy(errorMessage = e.message) }
        }
    }

    fun approvePairing(requestId: String) {
        pairingManager.approvePairing(requestId, "")
    }

    fun denyPairing(requestId: String) {
        pairingManager.denyPairing(requestId)
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    companion object {
        fun provideFactory(
            context: Context,
            serverManager: LocalServerManager,
            pairingManager: PairingManager,
            deviceRepository: ConnectedDeviceRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ConnectPcViewModel(context, serverManager, pairingManager, deviceRepository) as T
            }
        }
    }
}

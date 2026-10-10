package com.pixelmarks.core.pcserver.model

/**
 * State representing the embedded local HTTP server lifecycle.
 */
sealed interface PcServerState {
    object Stopped : PcServerState
    object Starting : PcServerState
    data class Running(
        val ipAddress: String,
        val port: Int,
        val serverUrl: String = "http://$ipAddress:$port"
    ) : PcServerState
    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : PcServerState
}

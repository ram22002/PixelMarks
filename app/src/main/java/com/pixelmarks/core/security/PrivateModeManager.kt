package com.pixelmarks.core.security

import com.pixelmarks.core.security.model.PrivacyMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Application-scoped runtime manager for Private Mode (Secure Folder).
 * Private mode is strictly runtime-only and is never persisted to disk.
 * When the app goes to background or restarts, it automatically resets to PUBLIC.
 */
interface PrivateModeManager {
    val mode: StateFlow<PrivacyMode>
    fun enterPrivateMode()
    fun exitPrivateMode()
    fun onAppBackgrounded()
    fun isPrivateActive(): Boolean = mode.value == PrivacyMode.PRIVATE
}

class PrivateModeManagerImpl : PrivateModeManager {

    private val _mode = MutableStateFlow(PrivacyMode.PUBLIC)
    override val mode: StateFlow<PrivacyMode> = _mode.asStateFlow()

    override fun enterPrivateMode() {
        _mode.value = PrivacyMode.PRIVATE
    }

    override fun exitPrivateMode() {
        _mode.value = PrivacyMode.PUBLIC
    }

    override fun onAppBackgrounded() {
        // Leaving the app or backgrounding always drops back to PUBLIC
        exitPrivateMode()
    }
}

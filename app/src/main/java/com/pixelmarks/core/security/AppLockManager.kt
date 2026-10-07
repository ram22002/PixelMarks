package com.pixelmarks.core.security

import com.pixelmarks.core.security.model.AppLockTimeout
import com.pixelmarks.data.repository.SecurityPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Lifecycle-aware coordinator for App Lock, private collections, and session unlock state.
 */
class AppLockManager(
    private val securityPreferencesRepository: SecurityPreferencesRepository,
    private val applicationScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {

    private var lastBackgroundTimestamp: Long = 0L

    val isAppLockEnabled: StateFlow<Boolean> = securityPreferencesRepository.observeAppLockEnabled()
        .stateIn(applicationScope, SharingStarted.Eagerly, false)

    val appLockTimeout: StateFlow<AppLockTimeout> = securityPreferencesRepository.observeLockTimeout()
        .stateIn(applicationScope, SharingStarted.Eagerly, AppLockTimeout.IMMEDIATELY)

    private val _isAppLocked = MutableStateFlow(false)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    private val _isPrivateSessionUnlocked = MutableStateFlow(false)
    val isPrivateSessionUnlocked: StateFlow<Boolean> = _isPrivateSessionUnlocked.asStateFlow()

    private val _unlockedCollectionIds = MutableStateFlow<Set<Long>>(emptySet())
    val unlockedCollectionIds: StateFlow<Set<Long>> = _unlockedCollectionIds.asStateFlow()

    init {
        // Cold start check: if app lock is enabled, start in locked state
        applicationScope.launch {
            if (securityPreferencesRepository.isAppLockEnabled()) {
                _isAppLocked.value = true
            }
        }
    }

    /**
     * Called when the app moves to the background (user leaves app).
     */
    fun onAppBackgrounded() {
        lastBackgroundTimestamp = System.currentTimeMillis()
    }

    /**
     * Called when the app returns to the foreground.
     */
    fun onAppForegrounded() {
        if (!isAppLockEnabled.value) {
            _isAppLocked.value = false
            return
        }

        // If already locked, remain locked
        if (_isAppLocked.value) return

        val timeout = appLockTimeout.value
        if (timeout == AppLockTimeout.IMMEDIATELY) {
            _isAppLocked.value = true
            _isPrivateSessionUnlocked.value = false
            _unlockedCollectionIds.value = emptySet()
        } else {
            val elapsed = System.currentTimeMillis() - lastBackgroundTimestamp
            if (elapsed >= timeout.durationMillis && lastBackgroundTimestamp > 0L) {
                _isAppLocked.value = true
                _isPrivateSessionUnlocked.value = false
                _unlockedCollectionIds.value = emptySet()
            }
        }
    }

    fun unlockApp() {
        _isAppLocked.value = false
        lastBackgroundTimestamp = System.currentTimeMillis()
    }

    fun lockApp() {
        _isAppLocked.value = true
        _isPrivateSessionUnlocked.value = false
        _unlockedCollectionIds.value = emptySet()
    }

    fun unlockPrivateSession() {
        _isPrivateSessionUnlocked.value = true
    }

    fun lockPrivateSession() {
        _isPrivateSessionUnlocked.value = false
        _unlockedCollectionIds.value = emptySet()
    }

    fun unlockCollection(collectionId: Long) {
        _unlockedCollectionIds.value = _unlockedCollectionIds.value + collectionId
    }

    fun isCollectionUnlocked(collectionId: Long): Boolean {
        return _isPrivateSessionUnlocked.value || _unlockedCollectionIds.value.contains(collectionId)
    }
}

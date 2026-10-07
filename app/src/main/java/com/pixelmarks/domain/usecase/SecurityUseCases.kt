package com.pixelmarks.domain.usecase

import com.pixelmarks.core.security.AppLockManager
import com.pixelmarks.core.security.biometric.BiometricAuthManager
import com.pixelmarks.core.security.model.AppLockTimeout
import com.pixelmarks.core.security.model.BiometricAuthCapability
import com.pixelmarks.data.repository.SecurityPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

data class AppLockSettings(
    val isEnabled: Boolean,
    val timeout: AppLockTimeout
)

class ObserveAppLockSettingsUseCase(
    private val securityPreferencesRepository: SecurityPreferencesRepository
) {
    operator fun invoke(): Flow<Boolean> = securityPreferencesRepository.observeAppLockEnabled()
    fun observeTimeout(): Flow<AppLockTimeout> = securityPreferencesRepository.observeLockTimeout()
}

class SetAppLockEnabledUseCase(
    private val securityPreferencesRepository: SecurityPreferencesRepository,
    private val appLockManager: AppLockManager
) {
    suspend operator fun invoke(enabled: Boolean) {
        securityPreferencesRepository.setAppLockEnabled(enabled)
        if (!enabled) {
            appLockManager.unlockApp()
        }
    }
}

class SetAppLockTimeoutUseCase(
    private val securityPreferencesRepository: SecurityPreferencesRepository
) {
    suspend operator fun invoke(timeout: AppLockTimeout) {
        securityPreferencesRepository.setLockTimeout(timeout)
    }
}

class CheckBiometricCapabilityUseCase(
    private val biometricAuthManager: BiometricAuthManager
) {
    operator fun invoke(): BiometricAuthCapability = biometricAuthManager.checkCapability()
}

class ManageAppLockUseCase(
    private val appLockManager: AppLockManager
) {
    val isAppLocked: StateFlow<Boolean> = appLockManager.isAppLocked
    val isPrivateSessionUnlocked: StateFlow<Boolean> = appLockManager.isPrivateSessionUnlocked
    val unlockedCollectionIds: StateFlow<Set<Long>> = appLockManager.unlockedCollectionIds

    fun observePrivateSessionUnlocked(): Flow<Boolean> = appLockManager.isPrivateSessionUnlocked
    fun unlockApp() = appLockManager.unlockApp()
    fun lockApp() = appLockManager.lockApp()
    fun unlockPrivateSession() = appLockManager.unlockPrivateSession()
    fun lockPrivateSession() = appLockManager.lockPrivateSession()
    fun unlockCollection(collectionId: Long) = appLockManager.unlockCollection(collectionId)
    fun isCollectionUnlocked(collectionId: Long): Boolean = appLockManager.isCollectionUnlocked(collectionId)
}

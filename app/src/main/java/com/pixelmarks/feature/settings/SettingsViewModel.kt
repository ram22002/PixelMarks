package com.pixelmarks.feature.settings

import android.content.Intent
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pixelmarks.core.browser.BrowserPreference
import com.pixelmarks.core.security.biometric.BiometricAuthManager
import com.pixelmarks.core.security.model.AppLockTimeout
import com.pixelmarks.core.security.model.BiometricAuthCapability
import com.pixelmarks.core.security.model.PrivateAuthMethod
import com.pixelmarks.data.repository.PrivatePreferencesRepository
import com.pixelmarks.domain.usecase.CheckBiometricCapabilityUseCase
import com.pixelmarks.domain.usecase.ObserveAppLockSettingsUseCase
import com.pixelmarks.domain.usecase.ObserveBrowserPreferenceUseCase
import com.pixelmarks.domain.usecase.SetAppLockEnabledUseCase
import com.pixelmarks.domain.usecase.SetAppLockTimeoutUseCase
import com.pixelmarks.domain.usecase.SetBrowserPreferenceUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val browserPreference: BrowserPreference = BrowserPreference.DEFAULT,
    val isAppLockEnabled: Boolean = false,
    val appLockTimeout: AppLockTimeout = AppLockTimeout.IMMEDIATELY,
    val biometricCapability: BiometricAuthCapability = BiometricAuthCapability.AVAILABLE
)

class SettingsViewModel(
    private val observeBrowserPreferenceUseCase: ObserveBrowserPreferenceUseCase,
    private val setBrowserPreferenceUseCase: SetBrowserPreferenceUseCase,
    private val observeAppLockSettingsUseCase: ObserveAppLockSettingsUseCase,
    private val setAppLockEnabledUseCase: SetAppLockEnabledUseCase,
    private val setAppLockTimeoutUseCase: SetAppLockTimeoutUseCase,
    private val checkBiometricCapabilityUseCase: CheckBiometricCapabilityUseCase,
    private val biometricAuthManager: BiometricAuthManager,
    private val privatePreferencesRepository: PrivatePreferencesRepository
) : ViewModel() {

    val browserPreference: StateFlow<BrowserPreference> =
        observeBrowserPreferenceUseCase()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = BrowserPreference.DEFAULT
            )

    val isAppLockEnabled: StateFlow<Boolean> =
        observeAppLockSettingsUseCase()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = false
            )

    val appLockTimeout: StateFlow<AppLockTimeout> =
        observeAppLockSettingsUseCase.observeTimeout()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = AppLockTimeout.IMMEDIATELY
            )

    val privateAuthMethod: StateFlow<PrivateAuthMethod> =
        privatePreferencesRepository.authMethod
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = PrivateAuthMethod.BIOMETRIC
            )

    val hasCustomPassword: StateFlow<Boolean> =
        privatePreferencesRepository.hasCustomPassword
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = false
            )

    val passwordHint: StateFlow<String?> =
        privatePreferencesRepository.passwordHint
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = null
            )

    val isRecoveryEnabled: StateFlow<Boolean> =
        privatePreferencesRepository.isRecoveryEnabled
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = true
            )

    private val _biometricCapability = MutableStateFlow(checkBiometricCapabilityUseCase())
    val biometricCapability: StateFlow<BiometricAuthCapability> = _biometricCapability.asStateFlow()

    fun refreshBiometricCapability() {
        _biometricCapability.value = checkBiometricCapabilityUseCase()
    }

    fun onBrowserPreferenceSelected(preference: BrowserPreference) {
        viewModelScope.launch {
            setBrowserPreferenceUseCase(preference)
        }
    }

    fun setLockTimeout(timeout: AppLockTimeout) {
        viewModelScope.launch {
            setAppLockTimeoutUseCase(timeout)
        }
    }

    fun setPrivateAuthMethod(method: PrivateAuthMethod) {
        viewModelScope.launch {
            privatePreferencesRepository.setAuthMethod(method)
        }
    }

    fun setRecoveryEnabled(enabled: Boolean) {
        viewModelScope.launch {
            privatePreferencesRepository.setRecoveryEnabled(enabled)
        }
    }

    fun requestToggleAppLock(
        enabled: Boolean,
        activity: FragmentActivity,
        onResult: (Boolean, String?) -> Unit
    ) {
        val capability = checkBiometricCapabilityUseCase()
        _biometricCapability.value = capability

        if (enabled && !capability.canLock) {
            onResult(false, capability.userMessage)
            return
        }

        val promptTitle = if (enabled) "Enable App Lock" else "Disable App Lock"
        biometricAuthManager.authenticate(
            activity = activity,
            title = promptTitle,
            subtitle = "Verify your credentials to update security settings",
            onSuccess = {
                viewModelScope.launch {
                    setAppLockEnabledUseCase(enabled)
                    onResult(true, null)
                }
            },
            onError = { _, errString ->
                onResult(false, errString.toString())
            }
        )
    }

    fun createEnrollIntent(): Intent = biometricAuthManager.createEnrollIntent()

    companion object {
        fun provideFactory(
            observeBrowserPreferenceUseCase: ObserveBrowserPreferenceUseCase,
            setBrowserPreferenceUseCase: SetBrowserPreferenceUseCase,
            observeAppLockSettingsUseCase: ObserveAppLockSettingsUseCase,
            setAppLockEnabledUseCase: SetAppLockEnabledUseCase,
            setAppLockTimeoutUseCase: SetAppLockTimeoutUseCase,
            checkBiometricCapabilityUseCase: CheckBiometricCapabilityUseCase,
            biometricAuthManager: BiometricAuthManager,
            privatePreferencesRepository: PrivatePreferencesRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(
                    observeBrowserPreferenceUseCase,
                    setBrowserPreferenceUseCase,
                    observeAppLockSettingsUseCase,
                    setAppLockEnabledUseCase,
                    setAppLockTimeoutUseCase,
                    checkBiometricCapabilityUseCase,
                    biometricAuthManager,
                    privatePreferencesRepository
                ) as T
            }
        }
    }
}

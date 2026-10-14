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

import com.pixelmarks.data.repository.AppColorScheme
import com.pixelmarks.data.repository.AppPreferencesRepository
import com.pixelmarks.data.repository.CollectionStyle
import com.pixelmarks.data.repository.DisplayFont
import com.pixelmarks.data.repository.StartupPage
import com.pixelmarks.data.repository.ThemeMode

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
    private val privatePreferencesRepository: PrivatePreferencesRepository,
    private val appPreferencesRepository: AppPreferencesRepository? = null
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> =
        appPreferencesRepository?.observeThemeMode()
            ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.DARK)
            ?: MutableStateFlow(ThemeMode.DARK)

    val dynamicThemeEnabled: StateFlow<Boolean> =
        appPreferencesRepository?.observeDynamicTheme()
            ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
            ?: MutableStateFlow(false)

    val colorScheme: StateFlow<AppColorScheme> =
        appPreferencesRepository?.observeColorScheme()
            ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppColorScheme.ARCHIVAL_GOLD)
            ?: MutableStateFlow(AppColorScheme.ARCHIVAL_GOLD)

    val displayFont: StateFlow<DisplayFont> =
        appPreferencesRepository?.observeDisplayFont()
            ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DisplayFont.EDITORIAL_SERIF)
            ?: MutableStateFlow(DisplayFont.EDITORIAL_SERIF)

    val language: StateFlow<String> =
        appPreferencesRepository?.observeLanguage()
            ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "English")
            ?: MutableStateFlow("English")

    val startupPage: StateFlow<StartupPage> =
        appPreferencesRepository?.observeStartupPage()
            ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StartupPage.HOME)
            ?: MutableStateFlow(StartupPage.HOME)

    val collectionStyle: StateFlow<CollectionStyle> =
        appPreferencesRepository?.observeCollectionStyle()
            ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CollectionStyle.FOLIO)
            ?: MutableStateFlow(CollectionStyle.FOLIO)

    val bookmarkShowFavicon: StateFlow<Boolean> =
        appPreferencesRepository?.observeShowFavicon()
            ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
            ?: MutableStateFlow(true)

    val bookmarkShowDomain: StateFlow<Boolean> =
        appPreferencesRepository?.observeShowDomain()
            ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
            ?: MutableStateFlow(true)

    val bookmarkShowNotes: StateFlow<Boolean> =
        appPreferencesRepository?.observeShowNotes()
            ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
            ?: MutableStateFlow(true)

    val bookmarkCompactMode: StateFlow<Boolean> =
        appPreferencesRepository?.observeCompactMode()
            ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
            ?: MutableStateFlow(false)

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
            setBrowserPreference(preference)
        }
    }

    suspend fun setBrowserPreference(preference: BrowserPreference) {
        setBrowserPreferenceUseCase(preference)
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

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { updateThemeMode(mode) }
    }

    suspend fun updateThemeMode(mode: ThemeMode) {
        appPreferencesRepository?.setThemeMode(mode)
    }

    fun setDynamicTheme(enabled: Boolean) {
        viewModelScope.launch { updateDynamicTheme(enabled) }
    }

    suspend fun updateDynamicTheme(enabled: Boolean) {
        appPreferencesRepository?.setDynamicTheme(enabled)
    }

    fun setColorScheme(scheme: AppColorScheme) {
        viewModelScope.launch { updateColorScheme(scheme) }
    }

    suspend fun updateColorScheme(scheme: AppColorScheme) {
        appPreferencesRepository?.setColorScheme(scheme)
    }

    fun setDisplayFont(font: DisplayFont) {
        viewModelScope.launch { updateDisplayFont(font) }
    }

    suspend fun updateDisplayFont(font: DisplayFont) {
        appPreferencesRepository?.setDisplayFont(font)
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch { updateLanguage(lang) }
    }

    suspend fun updateLanguage(lang: String) {
        appPreferencesRepository?.setLanguage(lang)
    }

    fun setStartupPage(page: StartupPage) {
        viewModelScope.launch { updateStartupPage(page) }
    }

    suspend fun updateStartupPage(page: StartupPage) {
        appPreferencesRepository?.setStartupPage(page)
    }

    fun setCollectionStyle(style: CollectionStyle) {
        viewModelScope.launch { updateCollectionStyle(style) }
    }

    suspend fun updateCollectionStyle(style: CollectionStyle) {
        appPreferencesRepository?.setCollectionStyle(style)
    }

    fun setBookmarkShowFavicon(show: Boolean) {
        viewModelScope.launch { updateBookmarkShowFavicon(show) }
    }

    suspend fun updateBookmarkShowFavicon(show: Boolean) {
        appPreferencesRepository?.setShowFavicon(show)
    }

    fun setBookmarkShowDomain(show: Boolean) {
        viewModelScope.launch { updateBookmarkShowDomain(show) }
    }

    suspend fun updateBookmarkShowDomain(show: Boolean) {
        appPreferencesRepository?.setShowDomain(show)
    }

    fun setBookmarkShowNotes(show: Boolean) {
        viewModelScope.launch { updateBookmarkShowNotes(show) }
    }

    suspend fun updateBookmarkShowNotes(show: Boolean) {
        appPreferencesRepository?.setShowNotes(show)
    }

    fun setBookmarkCompactMode(compact: Boolean) {
        viewModelScope.launch { updateBookmarkCompactMode(compact) }
    }

    suspend fun updateBookmarkCompactMode(compact: Boolean) {
        appPreferencesRepository?.setCompactMode(compact)
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
            privatePreferencesRepository: PrivatePreferencesRepository,
            appPreferencesRepository: AppPreferencesRepository? = null
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
                    privatePreferencesRepository,
                    appPreferencesRepository
                ) as T
            }
        }
    }
}

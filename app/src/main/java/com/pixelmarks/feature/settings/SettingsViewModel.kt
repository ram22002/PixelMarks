package com.pixelmarks.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pixelmarks.core.browser.BrowserPreference
import com.pixelmarks.domain.usecase.ObserveBrowserPreferenceUseCase
import com.pixelmarks.domain.usecase.SetBrowserPreferenceUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val browserPreference: BrowserPreference = BrowserPreference.DEFAULT
)

class SettingsViewModel(
    private val observeBrowserPreferenceUseCase: ObserveBrowserPreferenceUseCase,
    private val setBrowserPreferenceUseCase: SetBrowserPreferenceUseCase
) : ViewModel() {

    val browserPreference: StateFlow<BrowserPreference> =
        observeBrowserPreferenceUseCase()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = BrowserPreference.DEFAULT
            )

    fun onBrowserPreferenceSelected(preference: BrowserPreference) {
        viewModelScope.launch {
            setBrowserPreferenceUseCase(preference)
        }
    }

    companion object {
        fun provideFactory(
            observeBrowserPreferenceUseCase: ObserveBrowserPreferenceUseCase,
            setBrowserPreferenceUseCase: SetBrowserPreferenceUseCase
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(
                    observeBrowserPreferenceUseCase,
                    setBrowserPreferenceUseCase
                ) as T
            }
        }
    }
}

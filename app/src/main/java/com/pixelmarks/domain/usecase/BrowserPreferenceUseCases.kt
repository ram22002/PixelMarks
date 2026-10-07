package com.pixelmarks.domain.usecase

import com.pixelmarks.core.browser.BrowserPreference
import com.pixelmarks.data.repository.BrowserPreferenceRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case to observe the current [BrowserPreference] as a stream.
 */
class ObserveBrowserPreferenceUseCase(
    private val browserPreferenceRepository: BrowserPreferenceRepository
) {
    operator fun invoke(): Flow<BrowserPreference> =
        browserPreferenceRepository.observePreference()
}

/**
 * Use case to persist a new [BrowserPreference].
 */
class SetBrowserPreferenceUseCase(
    private val browserPreferenceRepository: BrowserPreferenceRepository
) {
    suspend operator fun invoke(preference: BrowserPreference) =
        browserPreferenceRepository.setPreference(preference)
}

/**
 * Use case to read the current [BrowserPreference] in a one-shot query.
 */
class GetBrowserPreferenceUseCase(
    private val browserPreferenceRepository: BrowserPreferenceRepository
) {
    suspend operator fun invoke(): BrowserPreference =
        browserPreferenceRepository.getPreference()
}

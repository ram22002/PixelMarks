package com.pixelmarks.feature.changelog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pixelmarks.core.update.GitHubReleaseRepository
import com.pixelmarks.core.update.model.GitHubRelease
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

/** UI state for the Changelog screen. */
sealed interface ChangelogUiState {
    data object Loading : ChangelogUiState
    data class Success(val releases: List<GitHubRelease>, val isOfflineFallback: Boolean = false) : ChangelogUiState
    data object Offline : ChangelogUiState
    data class Error(val message: String) : ChangelogUiState
}

class ChangelogViewModel(
    private val releaseRepository: GitHubReleaseRepository
) : ViewModel() {

    private val _state = MutableStateFlow<ChangelogUiState>(ChangelogUiState.Loading)
    val state: StateFlow<ChangelogUiState> = _state.asStateFlow()

    fun loadChangelog() {
        if (_state.value is ChangelogUiState.Success) return // Already loaded
        fetchReleases(forceRefresh = false)
    }

    fun refresh() {
        releaseRepository.invalidateCache()
        fetchReleases(forceRefresh = true)
    }

    private fun fetchReleases(forceRefresh: Boolean) {
        viewModelScope.launch {
            if (_state.value !is ChangelogUiState.Success) {
                _state.value = ChangelogUiState.Loading
            }
            val result = releaseRepository.getReleases(forceRefresh = forceRefresh)
            _state.value = result.fold(
                onSuccess = { releases ->
                    val filtered = releases.filter { !it.isDraft }
                    ChangelogUiState.Success(filtered, isOfflineFallback = false)
                },
                onFailure = { e ->
                    // If network fails but cache exists, display cached releases with fallback indicator
                    val cached = releaseRepository.getCachedReleases()?.filter { !it.isDraft }
                    if (!cached.isNullOrEmpty()) {
                        ChangelogUiState.Success(cached, isOfflineFallback = true)
                    } else {
                        val msg = e.message ?: "Unknown error"
                        when {
                            msg.contains("internet", ignoreCase = true) ||
                                msg.contains("No internet", ignoreCase = true) ||
                                e is IOException && (msg.contains("UnknownHost") || msg.contains("connect")) ->
                                ChangelogUiState.Offline
                            else -> ChangelogUiState.Error(msg)
                        }
                    }
                }
            )
        }
    }

    companion object {
        fun provideFactory(repo: GitHubReleaseRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ChangelogViewModel(repo) as T
            }
    }
}

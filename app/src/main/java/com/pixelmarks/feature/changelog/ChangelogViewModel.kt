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
    data class Success(val releases: List<GitHubRelease>) : ChangelogUiState
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
        fetchReleases()
    }

    fun refresh() {
        releaseRepository.invalidateCache()
        _state.value = ChangelogUiState.Loading
        fetchReleases()
    }

    private fun fetchReleases() {
        viewModelScope.launch {
            _state.value = ChangelogUiState.Loading
            val result = releaseRepository.getReleases()
            _state.value = result.fold(
                onSuccess = { releases ->
                    ChangelogUiState.Success(releases.filter { !it.isDraft })
                },
                onFailure = { e ->
                    val msg = e.message ?: "Unknown error"
                    when {
                        msg.contains("internet", ignoreCase = true) ||
                        msg.contains("No internet", ignoreCase = true) ||
                        e is IOException && (msg.contains("UnknownHost") || msg.contains("connect")) ->
                            ChangelogUiState.Offline
                        else -> ChangelogUiState.Error(msg)
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


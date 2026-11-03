package com.pixelmarks.feature.update

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pixelmarks.app.BuildConfig
import com.pixelmarks.core.update.ApkDownloadManager
import com.pixelmarks.core.update.ApkInstaller
import com.pixelmarks.core.update.DownloadState
import com.pixelmarks.core.update.GitHubReleaseRepository
import com.pixelmarks.core.update.UpdateChecker
import com.pixelmarks.core.update.model.GitHubRelease
import com.pixelmarks.core.update.model.ReleaseChannel
import com.pixelmarks.core.update.model.UpdateCheckResult
import com.pixelmarks.data.datastore.UpdatePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Shared ViewModel for update checking, APK download and installation.
 * Used by SettingsScreen, AboutScreen and UpdateBanner.
 */
class UpdateViewModel(
    private val releaseRepository: GitHubReleaseRepository,
    private val updatePreferences: UpdatePreferences,
    private val downloadManager: ApkDownloadManager,
    private val apkInstaller: ApkInstaller,
    private val installedVersionName: String = BuildConfig.VERSION_NAME,
    private val installedVersionCode: Int = BuildConfig.VERSION_CODE
) : ViewModel() {

    private val _updateCheckResult = MutableStateFlow<UpdateCheckResult>(UpdateCheckResult.UpToDate)
    val updateCheckResult: StateFlow<UpdateCheckResult> = _updateCheckResult.asStateFlow()

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private val defaultChannel: ReleaseChannel
        get() {
            val defaultName = UpdatePreferences.getDefaultChannel(installedVersionName)
            return runCatching { ReleaseChannel.valueOf(defaultName) }.getOrDefault(ReleaseChannel.STABLE)
        }

    private val _selectedChannel = MutableStateFlow(ReleaseChannel.STABLE)
    val selectedChannel: StateFlow<ReleaseChannel> = _selectedChannel.asStateFlow()

    private val _canInstallPackages = MutableStateFlow(false)
    val canInstallPackages: StateFlow<Boolean> = _canInstallPackages.asStateFlow()

    init {
        _selectedChannel.value = defaultChannel
        viewModelScope.launch {
            // Restore persisted channel
            val channelName = updatePreferences.releaseChannelName.first()
            _selectedChannel.value = runCatching {
                ReleaseChannel.valueOf(channelName)
            }.getOrDefault(defaultChannel)

            _canInstallPackages.value = apkInstaller.canInstallPackages()
        }
    }

    /** Manual "Check for Updates" action. Always fires a fresh check. */
    fun checkForUpdates() {
        viewModelScope.launch {
            _updateCheckResult.value = UpdateCheckResult.Loading
            releaseRepository.invalidateCache()
            performCheck()
        }
    }

    /**
     * Startup check — skipped if last check was less than [UpdatePreferences.MIN_CHECK_INTERVAL_MS] ago.
     */
    fun checkForUpdatesOnStartup() {
        viewModelScope.launch {
            val lastCheck = updatePreferences.lastCheckTimestamp.first()
            val elapsed = System.currentTimeMillis() - lastCheck
            if (elapsed < UpdatePreferences.MIN_CHECK_INTERVAL_MS) return@launch
            _updateCheckResult.value = UpdateCheckResult.Loading
            performCheck()
        }
    }

    private suspend fun performCheck() {
        val checker = UpdateChecker(releaseRepository, installedVersionName, installedVersionCode)
        val dismissed = updatePreferences.dismissedVersion.first()
        val result = checker.checkForUpdate(_selectedChannel.value, dismissedVersionName = dismissed)
        _updateCheckResult.value = result
        updatePreferences.recordCheckTimestamp()
    }

    /** User dismissed the update banner — persist to avoid re-showing. */
    fun dismissUpdate(versionName: String) {
        viewModelScope.launch {
            updatePreferences.setDismissedVersion(versionName)
            _updateCheckResult.value = UpdateCheckResult.UpToDate
        }
    }

    /** Start downloading the APK for [release]. */
    fun startDownload(release: GitHubRelease) {
        val asset = release.apkAsset ?: return
        viewModelScope.launch {
            downloadManager.downloadApk(asset, release.versionName).collect { state ->
                _downloadState.value = state
            }
        }
    }

    /** Cancel an in-progress download. */
    fun cancelDownload() {
        val state = _downloadState.value
        if (state is DownloadState.Downloading) {
            downloadManager.cancelDownload(state.downloadId)
        }
        _downloadState.value = DownloadState.Cancelled
    }

    /** Reset download state back to Idle. */
    fun resetDownloadState() {
        _downloadState.value = DownloadState.Idle
    }

    /** Trigger system package installer with the downloaded APK. */
    fun installApk(apkUri: Uri) {
        _canInstallPackages.value = apkInstaller.canInstallPackages()
        if (_canInstallPackages.value) {
            apkInstaller.install(apkUri)
        }
    }

    /** Open the install-unknown-apps Settings page. */
    fun requestInstallPermission() {
        apkInstaller.openInstallPermissionSettings()
        // Re-check after user returns (caller triggers refresh via LaunchedEffect)
        _canInstallPackages.value = apkInstaller.canInstallPackages()
    }

    /** Refresh install permission status (call from onResume / LaunchedEffect). */
    fun refreshInstallPermission() {
        _canInstallPackages.value = apkInstaller.canInstallPackages()
    }

    /** Open GitHub releases page in browser as fallback. */
    fun openReleasePage(htmlUrl: String) {
        apkInstaller.openReleasePage(htmlUrl)
    }

    fun setReleaseChannel(channel: ReleaseChannel) {
        _selectedChannel.value = channel
        viewModelScope.launch {
            updatePreferences.setReleaseChannel(channel.name)
            // Immediately perform a fresh check for the newly selected channel
            _updateCheckResult.value = UpdateCheckResult.Loading
            releaseRepository.invalidateCache()
            performCheck()
        }
    }

    companion object {
        fun provideFactory(
            releaseRepository: GitHubReleaseRepository,
            updatePreferences: UpdatePreferences,
            context: Context
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return UpdateViewModel(
                    releaseRepository = releaseRepository,
                    updatePreferences = updatePreferences,
                    downloadManager = ApkDownloadManager(context),
                    apkInstaller = ApkInstaller(context)
                ) as T
            }
        }
    }
}


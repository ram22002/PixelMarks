package com.pixelmarks.feature.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pixelmarks.app.BuildConfig
import com.pixelmarks.app.PixelMarksApplication
import com.pixelmarks.core.browser.BrowserPreference
import com.pixelmarks.core.update.DownloadState
import com.pixelmarks.core.update.model.UpdateCheckResult
import com.pixelmarks.feature.update.UpdateBanner
import com.pixelmarks.feature.update.UpdateProgressDialog
import com.pixelmarks.feature.update.UpdateViewModel
import com.pixelmarks.ui.theme.ArchivalBackground
import com.pixelmarks.ui.theme.ArchivalPrimary
import com.pixelmarks.ui.theme.ArchivalSurfaceContainer
import com.pixelmarks.ui.theme.ArchivalSurfaceContainerHigh
import com.pixelmarks.ui.theme.ArchivalSurfaceContainerLow
import com.pixelmarks.ui.theme.ArchivalTextPrimary
import com.pixelmarks.ui.theme.ArchivalTextSecondary
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onNavigateToConnectPc: () -> Unit = {},
    onNavigateToConnectedDevices: () -> Unit = {},
    onNavigateToBackup: () -> Unit = {},
    onNavigateToHtmlImportExport: () -> Unit = {},
    onNavigateToManageTags: () -> Unit = {},
    onNavigateToBrowserPreference: () -> Unit = {},
    onNavigateToAppLock: () -> Unit = {},
    onNavigateToSecureFolder: () -> Unit = {},
    onNavigateToSecurity: () -> Unit = onNavigateToAppLock,
    onNavigateToCustomizeBookmark: () -> Unit = {},
    onNavigateToThemeAndColors: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToChangelog: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel(
        factory = run {
            val app = LocalContext.current.applicationContext as PixelMarksApplication
            SettingsViewModel.provideFactory(
                observeBrowserPreferenceUseCase = app.container.observeBrowserPreferenceUseCase,
                setBrowserPreferenceUseCase = app.container.setBrowserPreferenceUseCase,
                observeAppLockSettingsUseCase = app.container.observeAppLockSettingsUseCase,
                setAppLockEnabledUseCase = app.container.setAppLockEnabledUseCase,
                setAppLockTimeoutUseCase = app.container.setAppLockTimeoutUseCase,
                checkBiometricCapabilityUseCase = app.container.checkBiometricCapabilityUseCase,
                biometricAuthManager = app.container.biometricAuthManager,
                privatePreferencesRepository = app.container.privatePreferencesRepository,
                appPreferencesRepository = app.container.appPreferencesRepository
            )
        }
    )
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val themeMode by viewModel.themeMode.collectAsState()
    val dynamicTheme by viewModel.dynamicThemeEnabled.collectAsState()
    val colorScheme by viewModel.colorScheme.collectAsState()
    val displayFont by viewModel.displayFont.collectAsState()
    val language by viewModel.language.collectAsState()

    val startupPage by viewModel.startupPage.collectAsState()
    val collectionStyle by viewModel.collectionStyle.collectAsState()
    val browserPreference by viewModel.browserPreference.collectAsState()

    val isAppLockEnabled by viewModel.isAppLockEnabled.collectAsState()
    val appLockTimeout by viewModel.appLockTimeout.collectAsState()
    val privateAuthMethod by viewModel.privateAuthMethod.collectAsState()

    val updateViewModel: UpdateViewModel = viewModel(
        factory = run {
            val app = LocalContext.current.applicationContext as PixelMarksApplication
            UpdateViewModel.provideFactory(
                releaseRepository = app.container.releaseRepository,
                updatePreferences = app.container.updatePreferences,
                context = LocalContext.current.applicationContext
            )
        }
    )
    val updateCheckResult by updateViewModel.updateCheckResult.collectAsState()
    val downloadState by updateViewModel.downloadState.collectAsState()
    val canInstallPackages by updateViewModel.canInstallPackages.collectAsState()

    var hasManuallyCheckedUpdates by remember { mutableStateOf(false) }

    LaunchedEffect(updateCheckResult) {
        if (hasManuallyCheckedUpdates) {
            when (val res = updateCheckResult) {
                is UpdateCheckResult.UpToDate -> {
                    snackbarHostState.showSnackbar("PixelMarks is up to date (v${BuildConfig.VERSION_NAME})")
                    hasManuallyCheckedUpdates = false
                }
                is UpdateCheckResult.Offline -> {
                    snackbarHostState.showSnackbar("No internet connection to check for updates")
                    hasManuallyCheckedUpdates = false
                }
                is UpdateCheckResult.Error -> {
                    snackbarHostState.showSnackbar("Update check failed: ${res.message}")
                    hasManuallyCheckedUpdates = false
                }
                else -> Unit
            }
        }
    }

    // Dialog state
    var showThemeModeDialog by remember { mutableStateOf(false) }
    var showColorSchemeDialog by remember { mutableStateOf(false) }
    var showDisplayFontDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showStartupPageDialog by remember { mutableStateOf(false) }
    var showCollectionStyleDialog by remember { mutableStateOf(false) }
    var showDeveloperMessageDialog by remember { mutableStateOf(false) }
    var showLicensesDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = ArchivalBackground,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = ArchivalTextPrimary,
                fontFamily = FontFamily.Serif,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )

            // Update banner if available
            UpdateBanner(
                checkResult = updateCheckResult,
                onWhatsNew = { onNavigateToChangelog() },
                onUpdate = { release -> updateViewModel.startDownload(release) },
                onDismiss = { version -> updateViewModel.dismissUpdate(version) }
            )

            // Category 1: Theme
            SettingsCategorySection(title = "Theme") {
                SettingsActionRow(
                    title = "Theme Mode",
                    subtitle = themeMode.label,
                    icon = Icons.Default.NightsStay,
                    onClick = onNavigateToThemeAndColors
                )

                HorizontalDivider(color = ArchivalSurfaceContainerHigh)

                SettingsSwitchRow(
                    title = "Use dynamic themes",
                    subtitle = "Enable dynamic theme changes for the app",
                    icon = Icons.Default.AutoAwesome,
                    checked = dynamicTheme,
                    onCheckedChange = { viewModel.setDynamicTheme(it) }
                )

                HorizontalDivider(color = ArchivalSurfaceContainerHigh)

                SettingsActionRow(
                    title = "Pick color scheme",
                    subtitle = colorScheme.label,
                    icon = Icons.Default.Palette,
                    onClick = onNavigateToThemeAndColors
                )

                HorizontalDivider(color = ArchivalSurfaceContainerHigh)

                SettingsActionRow(
                    title = "Display font",
                    subtitle = displayFont.label,
                    icon = Icons.Default.TextFields,
                    onClick = { showDisplayFontDialog = true }
                )

                HorizontalDivider(color = ArchivalSurfaceContainerHigh)

                SettingsActionRow(
                    title = "Language",
                    subtitle = language,
                    icon = Icons.Default.Language,
                    onClick = { showLanguageDialog = true }
                )
            }

            // Category 2: Data Management (Manage Tags removed - already in bottom nav)
            SettingsCategorySection(title = "Data Management") {
                SettingsActionRow(
                    title = "Backup & Restore",
                    subtitle = "Manual file backup and restore (JSON / Encrypted)",
                    icon = Icons.Default.Restore,
                    onClick = onNavigateToBackup
                )

                HorizontalDivider(color = ArchivalSurfaceContainerHigh)

                SettingsActionRow(
                    title = "Browser Import & Export",
                    subtitle = "Transfer bookmarks with Chrome, Safari, and other apps",
                    icon = Icons.Default.SwapHoriz,
                    onClick = onNavigateToHtmlImportExport
                )

                HorizontalDivider(color = ArchivalSurfaceContainerHigh)

                SettingsActionRow(
                    title = "Connect to PC",
                    subtitle = "Open and manage your bookmarks on your computer",
                    icon = Icons.Default.Laptop,
                    onClick = onNavigateToConnectPc
                )

                HorizontalDivider(color = ArchivalSurfaceContainerHigh)

                SettingsActionRow(
                    title = "Connected PC Devices",
                    subtitle = "Manage paired workstations & tokens",
                    icon = Icons.Default.Devices,
                    onClick = onNavigateToConnectedDevices
                )
            }

            // Category 3: Appearance & Behavior
            SettingsCategorySection(title = "Appearance & Behavior") {
                SettingsActionRow(
                    title = "Customize Bookmark",
                    subtitle = "Customize the appearance of the bookmark",
                    icon = Icons.Default.Tune,
                    onClick = onNavigateToCustomizeBookmark
                )

                HorizontalDivider(color = ArchivalSurfaceContainerHigh)

                SettingsActionRow(
                    title = "Collection Style",
                    subtitle = collectionStyle.label,
                    icon = Icons.Default.Folder,
                    onClick = { showCollectionStyleDialog = true }
                )

                HorizontalDivider(color = ArchivalSurfaceContainerHigh)

                // Concise single row showing current selection, opens dedicated screen
                SettingsActionRow(
                    title = "Browser Preference",
                    subtitle = when (browserPreference) {
                        BrowserPreference.IN_APP -> "In-App Browser"
                        BrowserPreference.SYSTEM_PREFERRED -> "System Preferred"
                        BrowserPreference.DEFAULT_BROWSER -> "Default Browser"
                        BrowserPreference.ASK_EVERY_TIME -> "Ask Every Time"
                    },
                    icon = Icons.Default.Public,
                    onClick = onNavigateToBrowserPreference
                )

                HorizontalDivider(color = ArchivalSurfaceContainerHigh)

                SettingsActionRow(
                    title = "Startup Page",
                    subtitle = startupPage.label,
                    icon = Icons.Default.Home,
                    onClick = { showStartupPageDialog = true }
                )
            }

            // Category 4: Security & Privacy (Divided into dedicated App Lock and Secure Folder screens)
            SettingsCategorySection(title = "Security & Privacy") {
                SettingsActionRow(
                    title = "App Lock",
                    subtitle = if (isAppLockEnabled) "Enabled (${appLockTimeout.label})" else "Require credentials on app open",
                    icon = Icons.Default.Lock,
                    onClick = onNavigateToAppLock
                )

                HorizontalDivider(color = ArchivalSurfaceContainerHigh)

                SettingsActionRow(
                    title = "Secure Folder (Archival Vault)",
                    subtitle = "Vault authentication & long-press shortcut",
                    icon = Icons.Default.Security,
                    onClick = onNavigateToSecureFolder
                )
            }

            // Category 5: About
            SettingsCategorySection(title = "About") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Image(
                        painter = painterResource(id = com.pixelmarks.app.R.drawable.ic_splash_logo),
                        contentDescription = "PixelMarks Logo",
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Column {
                        Text(
                            text = "PixelMarks",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ArchivalTextPrimary,
                            fontFamily = FontFamily.Serif
                        )
                        Text(
                            text = "v${BuildConfig.VERSION_NAME} • Curatorial Bookmark Catalogue",
                            style = MaterialTheme.typography.bodySmall,
                            color = ArchivalTextSecondary
                        )
                    }
                }

                HorizontalDivider(color = ArchivalSurfaceContainerHigh)

                SettingsActionRow(
                    title = "About PixelMarks",
                    subtitle = "Developer, contributors & project info",
                    icon = Icons.Default.Info,
                    onClick = onNavigateToAbout
                )

                HorizontalDivider(color = ArchivalSurfaceContainerHigh)

                SettingsActionRow(
                    title = "Changelog",
                    subtitle = "Release history and notes",
                    icon = Icons.Default.ChatBubbleOutline,
                    onClick = onNavigateToChangelog
                )

                HorizontalDivider(color = ArchivalSurfaceContainerHigh)

                val updateSubtitle = when (val res = updateCheckResult) {
                    is UpdateCheckResult.Loading -> "Checking for updates…"
                    is UpdateCheckResult.UpdateAvailable -> "New version v${res.release.versionName} available"
                    is UpdateCheckResult.Offline -> "Offline — tap to retry"
                    is UpdateCheckResult.Error -> "Check failed — tap to retry"
                    is UpdateCheckResult.UpToDate -> "v${BuildConfig.VERSION_NAME} is up to date"
                }

                SettingsActionRow(
                    title = "Check for Updates",
                    subtitle = updateSubtitle,
                    icon = Icons.Default.SystemUpdateAlt,
                    onClick = {
                        hasManuallyCheckedUpdates = true
                        updateViewModel.checkForUpdates()
                    }
                )

                HorizontalDivider(color = ArchivalSurfaceContainerHigh)

                SettingsActionRow(
                    title = "Licenses",
                    subtitle = "View open source licenses",
                    icon = Icons.Default.Security,
                    onClick = { showLicensesDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Download & Install progress dialog
    if (downloadState !is DownloadState.Idle && downloadState !is DownloadState.Cancelled) {
        val releaseUrl = (updateCheckResult as? UpdateCheckResult.UpdateAvailable)?.release?.htmlUrl
            ?: "https://github.com/ram22002/PixelMarks/releases"
        UpdateProgressDialog(
            downloadState = downloadState,
            canInstallPackages = canInstallPackages,
            onCancel = { updateViewModel.cancelDownload() },
            onInstall = { uri -> updateViewModel.installApk(uri) },
            onRequestPermission = { updateViewModel.requestInstallPermission() },
            onDismiss = { updateViewModel.resetDownloadState() },
            onOpenReleasePage = { updateViewModel.openReleasePage(releaseUrl) }
        )
    }

    // Modal dialogs
    if (showThemeModeDialog) {
        ThemeModeDialog(
            currentMode = themeMode,
            onDismiss = { showThemeModeDialog = false },
            onSelectMode = { viewModel.setThemeMode(it) }
        )
    }

    if (showColorSchemeDialog) {
        ColorSchemeDialog(
            currentScheme = colorScheme,
            onDismiss = { showColorSchemeDialog = false },
            onSelectScheme = { viewModel.setColorScheme(it) }
        )
    }

    if (showDisplayFontDialog) {
        DisplayFontDialog(
            currentFont = displayFont,
            onDismiss = { showDisplayFontDialog = false },
            onSelectFont = { viewModel.setDisplayFont(it) }
        )
    }

    if (showLanguageDialog) {
        LanguageDialog(
            currentLanguage = language,
            onDismiss = { showLanguageDialog = false },
            onSelectLanguage = { viewModel.setLanguage(it) }
        )
    }

    if (showStartupPageDialog) {
        StartupPageDialog(
            currentPage = startupPage,
            onDismiss = { showStartupPageDialog = false },
            onSelectPage = { viewModel.setStartupPage(it) }
        )
    }

    if (showCollectionStyleDialog) {
        CollectionStyleDialog(
            currentStyle = collectionStyle,
            onDismiss = { showCollectionStyleDialog = false },
            onSelectStyle = { viewModel.setCollectionStyle(it) }
        )
    }

    if (showDeveloperMessageDialog) {
        DeveloperMessageDialog(onDismiss = { showDeveloperMessageDialog = false })
    }

    if (showLicensesDialog) {
        LicensesDialog(onDismiss = { showLicensesDialog = false })
    }
}

@Composable
private fun SettingsCategorySection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = ArchivalTextSecondary.copy(alpha = 0.85f),
            modifier = Modifier.padding(start = 4.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ArchivalSurfaceContainer),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badgeText: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ArchivalTextSecondary,
            modifier = Modifier.size(22.dp)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp, end = 8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = ArchivalTextPrimary,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = ArchivalTextSecondary,
                fontSize = 12.sp,
                maxLines = 1
            )
        }
        if (badgeText != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(ArchivalPrimary.copy(alpha = 0.18f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = ArchivalPrimary,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ArchivalTextSecondary,
            modifier = Modifier.size(22.dp)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp, end = 8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = ArchivalTextPrimary,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = ArchivalTextSecondary,
                fontSize = 12.sp,
                maxLines = 1
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ArchivalBackground,
                checkedTrackColor = ArchivalPrimary,
                uncheckedTrackColor = ArchivalSurfaceContainerHigh
            )
        )
    }
}

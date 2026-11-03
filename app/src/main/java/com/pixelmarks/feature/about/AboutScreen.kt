package com.pixelmarks.feature.about

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pixelmarks.app.BuildConfig
import com.pixelmarks.app.PixelMarksApplication
import com.pixelmarks.app.R
import com.pixelmarks.core.update.DownloadState
import com.pixelmarks.core.update.model.ReleaseChannel
import com.pixelmarks.core.update.model.UpdateCheckResult
import com.pixelmarks.feature.update.ReleaseChannelDialog
import com.pixelmarks.feature.update.UpdateBanner
import com.pixelmarks.feature.update.UpdateProgressDialog
import com.pixelmarks.feature.update.UpdateViewModel
import com.pixelmarks.ui.theme.ArchivalBackground
import com.pixelmarks.ui.theme.ArchivalPrimary
import com.pixelmarks.ui.theme.ArchivalSurfaceContainer
import com.pixelmarks.ui.theme.ArchivalSurfaceContainerHigh
import com.pixelmarks.ui.theme.ArchivalTextPrimary
import com.pixelmarks.ui.theme.ArchivalTextSecondary
import kotlinx.coroutines.delay

// ---- Static project metadata (no invented data) ----
private const val GITHUB_REPO_URL = "https://github.com/ram22002/PixelMarks"
private const val GITHUB_RELEASES_URL = "https://github.com/ram22002/PixelMarks/releases"
private const val DEVELOPER_NAME = "ram22002"
private const val DEVELOPER_ROLE = "Developer"
private const val DEVELOPER_GITHUB_URL = "https://github.com/ram22002"

/** Known contributors. Add real entries as the project grows. */
private data class ContributorInfo(
    val name: String,
    val role: String,
    val githubUrl: String
)

private val CONTRIBUTORS = listOf(
    ContributorInfo(
        name = "ram22002",
        role = "Developer & Maintainer",
        githubUrl = "https://github.com/ram22002"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit,
    onNavigateToChangelog: () -> Unit,
    onCheckForUpdates: () -> Unit,
    updateViewModel: UpdateViewModel = viewModel(
        factory = run {
            val app = LocalContext.current.applicationContext as PixelMarksApplication
            UpdateViewModel.provideFactory(
                releaseRepository = app.container.releaseRepository,
                updatePreferences = app.container.updatePreferences,
                context = LocalContext.current.applicationContext
            )
        }
    )
) {
    val context = LocalContext.current
    val updateCheckResult by updateViewModel.updateCheckResult.collectAsState()
    val downloadState by updateViewModel.downloadState.collectAsState()
    val canInstallPackages by updateViewModel.canInstallPackages.collectAsState()
    val selectedChannel by updateViewModel.selectedChannel.collectAsState()

    var showChannelDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        updateViewModel.refreshInstallPermission()
    }

    // Staggered entrance animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(80)
        visible = true
    }

    fun openUrl(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    Scaffold(
        containerColor = ArchivalBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "About",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = ArchivalTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ArchivalTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ArchivalBackground)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Update banner (animated)
            AnimatedVisibility(
                visible = visible && updateCheckResult is UpdateCheckResult.UpdateAvailable,
                enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { -it / 2 }
            ) {
                UpdateBanner(
                    checkResult = updateCheckResult,
                    onWhatsNew = { onNavigateToChangelog() },
                    onUpdate = { release -> updateViewModel.startDownload(release) },
                    onDismiss = { version -> updateViewModel.dismissUpdate(version) }
                )
            }

            // ── App identity card ──────────────────────────────────────
            AnimatedSection(visible = visible, delayMs = 0) {
                AboutCard {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_splash_logo),
                            contentDescription = "PixelMarks icon",
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(16.dp))
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "PixelMarks",
                                style = MaterialTheme.typography.headlineSmall,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                color = ArchivalTextPrimary
                            )
                            Text(
                                text = "Curatorial Bookmark Catalogue",
                                style = MaterialTheme.typography.bodySmall,
                                color = ArchivalTextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            VersionChip(
                                versionName = BuildConfig.VERSION_NAME,
                                versionCode = BuildConfig.VERSION_CODE,
                                buildType = BuildConfig.BUILD_TYPE
                            )
                        }
                    }
                }
            }

            // ── Developer card ─────────────────────────────────────────
            AnimatedSection(visible = visible, delayMs = 60) {
                AboutSectionLabel("Developer")
                AboutCard {
                    ContributorRow(
                        name = DEVELOPER_NAME,
                        role = DEVELOPER_ROLE,
                        onClick = { openUrl(DEVELOPER_GITHUB_URL) }
                    )
                }
            }

            // ── Contributors ───────────────────────────────────────────
            if (CONTRIBUTORS.size > 1) {
                AnimatedSection(visible = visible, delayMs = 100) {
                    AboutSectionLabel("Contributors")
                    AboutCard {
                        CONTRIBUTORS.forEachIndexed { index, contributor ->
                            ContributorRow(
                                name = contributor.name,
                                role = contributor.role,
                                onClick = { openUrl(contributor.githubUrl) }
                            )
                            if (index < CONTRIBUTORS.lastIndex) {
                                HorizontalDivider(
                                    color = ArchivalSurfaceContainerHigh,
                                    modifier = Modifier.padding(start = 56.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ── Links section ──────────────────────────────────────────
            AnimatedSection(visible = visible, delayMs = 140) {
                AboutSectionLabel("Project")
                AboutCard {
                    AboutLinkRow(
                        icon = Icons.Default.Code,
                        title = "GitHub Repository",
                        subtitle = "ram22002/PixelMarks",
                        onClick = { openUrl(GITHUB_REPO_URL) }
                    )
                    HorizontalDivider(color = ArchivalSurfaceContainerHigh)
                    AboutLinkRow(
                        icon = Icons.Default.SystemUpdateAlt,
                        title = "Releases",
                        subtitle = "View all published releases",
                        onClick = { openUrl(GITHUB_RELEASES_URL) }
                    )
                    HorizontalDivider(color = ArchivalSurfaceContainerHigh)
                    AboutLinkRow(
                        icon = Icons.Default.Description,
                        title = "Changelog",
                        subtitle = "Release history and notes",
                        onClick = onNavigateToChangelog
                    )
                    HorizontalDivider(color = ArchivalSurfaceContainerHigh)
                    val updateSubtitle = when (val res = updateCheckResult) {
                        is UpdateCheckResult.Loading -> "Checking for updates…"
                        is UpdateCheckResult.UpdateAvailable -> "New version v${res.release.versionName} available"
                        is UpdateCheckResult.UpToDate -> "Currently v${BuildConfig.VERSION_NAME} (latest)"
                        is UpdateCheckResult.Offline -> "Offline — tap to retry"
                        is UpdateCheckResult.Error -> "Check failed — tap to retry"
                    }
                    AboutLinkRow(
                        icon = Icons.Default.SystemUpdateAlt,
                        title = "Check for Updates",
                        subtitle = updateSubtitle,
                        onClick = { updateViewModel.checkForUpdates() },
                        trailingIcon = Icons.Default.SystemUpdateAlt
                    )
                    HorizontalDivider(color = ArchivalSurfaceContainerHigh)
                    AboutLinkRow(
                        icon = Icons.Default.Build,
                        title = "Release Channel",
                        subtitle = selectedChannel.label,
                        onClick = { showChannelDialog = true }
                    )
                }
            }

            // ── App info section ───────────────────────────────────────
            AnimatedSection(visible = visible, delayMs = 180) {
                AboutSectionLabel("App Info")
                AboutCard {
                    AboutInfoRow(icon = Icons.Default.Info, label = "Version", value = BuildConfig.VERSION_NAME)
                    HorizontalDivider(color = ArchivalSurfaceContainerHigh)
                    AboutInfoRow(icon = Icons.Default.Build, label = "Build type", value = BuildConfig.BUILD_TYPE.replaceFirstChar { it.uppercaseChar() })
                    HorizontalDivider(color = ArchivalSurfaceContainerHigh)
                    AboutInfoRow(icon = Icons.Default.Group, label = "Package", value = "com.pixelmarks.app")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Download & Installation progress dialog
        if (downloadState !is DownloadState.Idle && downloadState !is DownloadState.Cancelled) {
            val releaseUrl = (updateCheckResult as? UpdateCheckResult.UpdateAvailable)?.release?.htmlUrl
                ?: GITHUB_RELEASES_URL
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

        // Release channel selection dialog
        if (showChannelDialog) {
            ReleaseChannelDialog(
                currentChannel = selectedChannel,
                onSelectChannel = { updateViewModel.setReleaseChannel(it) },
                onDismiss = { showChannelDialog = false }
            )
        }
    }
}

// ── Private composable helpers ─────────────────────────────────────────────

@Composable
private fun AnimatedSection(
    visible: Boolean,
    delayMs: Int,
    content: @Composable () -> Unit
) {
    var sectionVisible by remember { mutableStateOf(false) }
    LaunchedEffect(visible) {
        if (visible) {
            delay(delayMs.toLong())
            sectionVisible = true
        }
    }
    AnimatedVisibility(
        visible = sectionVisible,
        enter = fadeIn(tween(280)) + slideInVertically(tween(280)) { it / 3 }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            content()
        }
    }
}

@Composable
private fun AboutSectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Medium,
        color = ArchivalTextSecondary.copy(alpha = 0.75f),
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun AboutCard(content: @Composable () -> Unit) {
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

@Composable
private fun VersionChip(versionName: String, versionCode: Int, buildType: String) {
    val channel = when {
        versionName.contains("nightly", ignoreCase = true) || buildType.contains("nightly", ignoreCase = true) -> "Nightly"
        versionName.contains("beta", ignoreCase = true) || buildType.contains("beta", ignoreCase = true) -> "Beta"
        else -> "Stable"
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(ArchivalPrimary.copy(alpha = 0.15f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = "v$versionName",
                style = MaterialTheme.typography.labelSmall,
                color = ArchivalPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(ArchivalSurfaceContainerHigh)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = channel,
                style = MaterialTheme.typography.labelSmall,
                color = ArchivalTextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun ContributorRow(name: String, role: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar placeholder — initials
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(ArchivalPrimary.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.take(1).uppercase(),
                color = ArchivalPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = ArchivalTextPrimary
            )
            Text(
                text = role,
                style = MaterialTheme.typography.bodySmall,
                color = ArchivalTextSecondary,
                fontSize = 12.sp
            )
        }
        Icon(
            imageVector = Icons.Default.OpenInBrowser,
            contentDescription = "Open GitHub",
            tint = ArchivalTextSecondary,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun AboutLinkRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailingIcon: ImageVector? = Icons.Default.OpenInBrowser
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = ArchivalTextSecondary, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = ArchivalTextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = ArchivalTextSecondary, fontSize = 12.sp)
        }
        if (trailingIcon != null) {
            Icon(imageVector = trailingIcon, contentDescription = null, tint = ArchivalTextSecondary, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun AboutInfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = ArchivalTextSecondary, modifier = Modifier.size(20.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = ArchivalTextPrimary, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall, color = ArchivalTextSecondary, fontSize = 12.sp)
    }
}


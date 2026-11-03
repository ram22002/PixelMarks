package com.pixelmarks.feature.changelog

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pixelmarks.app.BuildConfig
import com.pixelmarks.app.PixelMarksApplication
import com.pixelmarks.core.update.model.GitHubRelease
import com.pixelmarks.core.update.model.ReleaseChannel
import com.pixelmarks.ui.theme.ArchivalBackground
import com.pixelmarks.ui.theme.ArchivalPrimary
import com.pixelmarks.ui.theme.ArchivalSurfaceContainer
import com.pixelmarks.ui.theme.ArchivalSurfaceContainerHigh
import com.pixelmarks.ui.theme.ArchivalTextPrimary
import com.pixelmarks.ui.theme.ArchivalTextSecondary
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogScreen(
    onNavigateBack: () -> Unit,
    viewModel: ChangelogViewModel = viewModel(
        factory = run {
            val app = LocalContext.current.applicationContext as PixelMarksApplication
            ChangelogViewModel.provideFactory(app.container.releaseRepository)
        }
    )
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadChangelog()
    }

    Scaffold(
        containerColor = ArchivalBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Changelog",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = ArchivalTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ArchivalTextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = ArchivalTextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ArchivalBackground)
            )
        }
    ) { paddingValues ->
        AnimatedContent(
            targetState = state,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) { currentState ->
            when (currentState) {
                is ChangelogUiState.Loading -> ChangelogLoadingState()
                is ChangelogUiState.Offline -> ChangelogOfflineState(onRetry = { viewModel.refresh() })
                is ChangelogUiState.Error -> ChangelogErrorState(
                    message = currentState.message,
                    onRetry = { viewModel.refresh() }
                )
                is ChangelogUiState.Success -> ChangelogSuccessContent(
                    releases = currentState.releases,
                    installedVersion = BuildConfig.VERSION_NAME
                )
            }
        }
    }
}

@Composable
private fun ChangelogSuccessContent(
    releases: List<GitHubRelease>,
    installedVersion: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Installed version indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(ArchivalPrimary.copy(alpha = 0.10f))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Installed version",
                style = MaterialTheme.typography.bodyMedium,
                color = ArchivalTextSecondary
            )
            Text(
                text = "v$installedVersion",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = ArchivalPrimary
            )
        }

        if (releases.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No releases found.\nCreate a release on GitHub to populate this list.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ArchivalTextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            releases.forEach { release ->
                ReleaseCard(
                    release = release,
                    isInstalled = release.versionName == installedVersion
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun ReleaseCard(release: GitHubRelease, isInstalled: Boolean) {
    var expanded by remember { mutableStateOf(isInstalled) } // Auto-expand the installed version

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ArchivalSurfaceContainer)
    ) {
        // Header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = release.name.ifBlank { release.tagName },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = ArchivalTextPrimary,
                        fontFamily = FontFamily.Serif
                    )
                    ChannelBadge(channel = release.channel)
                    if (isInstalled) InstalledBadge()
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formatDate(release.publishedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = ArchivalTextSecondary,
                    fontSize = 11.sp
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = ArchivalTextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }

        // Expandable release notes
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(200)) + fadeIn(tween(200)),
            exit = shrinkVertically(tween(180)) + fadeOut(tween(180))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 14.dp)
            ) {
                androidx.compose.material3.HorizontalDivider(
                    color = ArchivalSurfaceContainerHigh,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                if (release.body.isBlank()) {
                    Text(
                        text = "No release notes provided.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ArchivalTextSecondary
                    )
                } else {
                    // Render Markdown as styled plain text (sections by "##" headers)
                    MarkdownStyled(markdown = release.body)
                }
            }
        }
    }
}

/** Very lightweight Markdown renderer — headings, bullets. No library needed. */
@Composable
private fun MarkdownStyled(markdown: String) {
    val lines = markdown.lines()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        lines.forEach { line ->
            when {
                line.startsWith("### ") -> Text(
                    text = line.removePrefix("### "),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = ArchivalPrimary,
                    fontSize = 12.sp
                )
                line.startsWith("## ") -> Text(
                    text = line.removePrefix("## "),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = ArchivalTextPrimary,
                    fontSize = 13.sp
                )
                line.startsWith("# ") -> Text(
                    text = line.removePrefix("# "),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = ArchivalTextPrimary,
                    fontFamily = FontFamily.Serif
                )
                line.startsWith("- ") || line.startsWith("* ") -> Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("•", color = ArchivalPrimary, fontSize = 12.sp)
                    Text(
                        text = line.removePrefix("- ").removePrefix("* ").trim(),
                        style = MaterialTheme.typography.bodySmall,
                        color = ArchivalTextSecondary,
                        fontSize = 12.sp
                    )
                }
                line.isBlank() -> Spacer(modifier = Modifier.height(4.dp))
                else -> Text(
                    text = line,
                    style = MaterialTheme.typography.bodySmall,
                    color = ArchivalTextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun ChannelBadge(channel: ReleaseChannel) {
    if (channel == ReleaseChannel.STABLE) return
    val color = when (channel) {
        ReleaseChannel.BETA -> ArchivalPrimary.copy(alpha = 0.7f)
        ReleaseChannel.NIGHTLY -> ArchivalTextSecondary.copy(alpha = 0.6f)
        else -> return
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 5.dp, vertical = 1.dp)
    ) {
        Text(text = channel.label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun InstalledBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(ArchivalPrimary.copy(alpha = 0.15f))
            .padding(horizontal = 5.dp, vertical = 1.dp)
    ) {
        Text(text = "Installed", color = ArchivalPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ChangelogLoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator(color = ArchivalPrimary, modifier = Modifier.size(36.dp))
            Text("Loading releases…", color = ArchivalTextSecondary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ChangelogOfflineState(onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.WifiOff, contentDescription = null, tint = ArchivalTextSecondary, modifier = Modifier.size(48.dp))
            Text("No internet connection", color = ArchivalTextPrimary, fontWeight = FontWeight.SemiBold)
            Text("Connect to view the changelog", color = ArchivalTextSecondary, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onRetry) { Text("Retry", color = ArchivalPrimary) }
        }
    }
}

@Composable
private fun ChangelogErrorState(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(24.dp)) {
            Text("Failed to load changelog", color = ArchivalTextPrimary, fontWeight = FontWeight.SemiBold)
            Text(message, color = ArchivalTextSecondary, style = MaterialTheme.typography.bodySmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            TextButton(onClick = onRetry) { Text("Retry", color = ArchivalPrimary) }
        }
    }
}

/** Format ISO-8601 date string to readable form. */
private fun formatDate(isoDate: String): String {
    return try {
        // Parse "2024-03-15T12:00:00Z" → "15 Mar 2024"
        val parts = isoDate.substringBefore('T').split("-")
        val months = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
        val month = months.getOrNull((parts.getOrNull(1)?.toIntOrNull() ?: 1) - 1) ?: parts[1]
        "${parts[2]} $month ${parts[0]}"
    } catch (_: Exception) {
        isoDate.substringBefore('T')
    }
}


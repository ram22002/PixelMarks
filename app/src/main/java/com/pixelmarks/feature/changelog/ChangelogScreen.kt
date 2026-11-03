package com.pixelmarks.feature.changelog

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pixelmarks.app.BuildConfig
import com.pixelmarks.app.PixelMarksApplication
import com.pixelmarks.core.update.model.GitHubRelease
import com.pixelmarks.core.update.model.ReleaseChannel
import com.pixelmarks.core.update.model.SemVer
import com.pixelmarks.ui.theme.ArchivalBackground
import com.pixelmarks.ui.theme.ArchivalPrimary
import com.pixelmarks.ui.theme.ArchivalSurfaceContainer
import com.pixelmarks.ui.theme.ArchivalSurfaceContainerHigh
import com.pixelmarks.ui.theme.ArchivalTextPrimary
import com.pixelmarks.ui.theme.ArchivalTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.loadChangelog()
    }

    val openBrowser = { url: String ->
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
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
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ArchivalTextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = ArchivalTextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ArchivalBackground)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val s = state) {
                is ChangelogUiState.Loading -> ChangelogLoadingState()
                is ChangelogUiState.Offline -> ChangelogOfflineState(onRetry = { viewModel.refresh() })
                is ChangelogUiState.Error -> ChangelogErrorState(
                    message = s.message,
                    onRetry = { viewModel.refresh() }
                )
                is ChangelogUiState.Success -> ChangelogContent(
                    releases = s.releases,
                    installedVersionName = BuildConfig.VERSION_NAME,
                    isOfflineFallback = s.isOfflineFallback,
                    onOpenReleaseUrl = { url -> openBrowser(url) }
                )
            }
        }
    }
}

@Composable
private fun ChangelogContent(
    releases: List<GitHubRelease>,
    installedVersionName: String,
    isOfflineFallback: Boolean,
    onOpenReleaseUrl: (String) -> Unit
) {
    var selectedChannelFilter by remember { mutableStateOf<ReleaseChannel?>(null) }
    val installedSemVer = remember(installedVersionName) { SemVer.parse(installedVersionName) }

    val filtered = remember(releases, selectedChannelFilter) {
        if (selectedChannelFilter == null) releases
        else releases.filter { it.channel == selectedChannelFilter }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (isOfflineFallback) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(ArchivalSurfaceContainerHigh)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = null,
                    tint = ArchivalTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Offline — displaying cached release history",
                    style = MaterialTheme.typography.bodySmall,
                    color = ArchivalTextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        // Channel filter chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChipItem(
                label = "All",
                selected = selectedChannelFilter == null,
                onClick = { selectedChannelFilter = null }
            )
            ReleaseChannel.entries.forEach { channel ->
                FilterChipItem(
                    label = channel.label,
                    selected = selectedChannelFilter == channel,
                    onClick = { selectedChannelFilter = channel }
                )
            }
        }

        // Installed version banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
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
                text = "v$installedVersionName",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = ArchivalPrimary
            )
        }

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No releases found for this filter.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ArchivalTextSecondary
                )
            }
        } else {
            filtered.forEach { release ->
                val isInstalled = release.semVer == installedSemVer
                ReleaseCard(
                    release = release,
                    isInstalled = isInstalled,
                    onOpenReleaseUrl = { onOpenReleaseUrl(release.htmlUrl) }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun ReleaseCard(
    release: GitHubRelease,
    isInstalled: Boolean,
    onOpenReleaseUrl: () -> Unit
) {
    var expanded by remember { mutableStateOf(isInstalled) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ArchivalSurfaceContainer)
    ) {
        // Header
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
                        text = release.displayTitle,
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
                    text = "v${release.versionName} • ${formatDate(release.publishedAt)}",
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

        // Expandable release notes body
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
                HorizontalDivider(
                    color = ArchivalSurfaceContainerHigh,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                val cleanedBody = sanitizeReleaseBody(release.body)

                if (cleanedBody.isBlank()) {
                    Text(
                        text = "No release notes provided.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ArchivalTextSecondary,
                        fontStyle = FontStyle.Italic
                    )
                } else {
                    MarkdownStyled(markdown = cleanedBody)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Actions row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (release.apkAsset != null) {
                        Text(
                            text = "APK: ${formatFileSize(release.apkAsset!!.sizeBytes)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ArchivalTextSecondary,
                            fontSize = 10.sp
                        )
                    } else {
                        Text(
                            text = "APK not attached",
                            style = MaterialTheme.typography.labelSmall,
                            color = ArchivalTextSecondary.copy(alpha = 0.6f),
                            fontSize = 10.sp
                        )
                    }

                    OutlinedButton(
                        onClick = onOpenReleaseUrl,
                        modifier = Modifier.height(32.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = ArchivalPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "View on GitHub",
                            color = ArchivalPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Filter out raw GitHub metadata artifacts (like SVG file names, raw commit archives, JSON dumps).
 */
internal fun sanitizeReleaseBody(body: String): String {
    if (body.isBlank()) return ""
    val lines = body.lines().filter { line ->
        val trimmed = line.trim()
        val lower = trimmed.lowercase()
        // Filter out lines mentioning SVG files, zip/tar archives, or raw generic commit metadata
        !lower.endsWith(".svg") &&
            !lower.endsWith(".zip") &&
            !lower.endsWith(".tar.gz") &&
            !lower.startsWith("download:") &&
            !lower.startsWith("assets:") &&
            !lower.startsWith("{\"")
    }
    return lines.joinToString("\n").trim()
}

/**
 * Clean, styled Markdown renderer supporting headings, sections, lists, and inline styles.
 */
@Composable
private fun MarkdownStyled(markdown: String) {
    val lines = markdown.lines()
    val primary = ArchivalPrimary
    val textPrimary = ArchivalTextPrimary
    val containerHigh = ArchivalSurfaceContainerHigh

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        lines.forEach { line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("#### ") -> {
                    Text(
                        text = buildAnnotatedMarkdown(trimmed.removePrefix("#### "), primary, textPrimary, containerHigh),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = ArchivalTextPrimary,
                        fontSize = 12.sp
                    )
                }
                trimmed.startsWith("### ") -> {
                    val heading = trimmed.removePrefix("### ")
                    val headingColor = when {
                        heading.contains("feature", ignoreCase = true) -> ArchivalPrimary
                        heading.contains("fix", ignoreCase = true) -> Color(0xFFD97706)
                        heading.contains("break", ignoreCase = true) -> Color(0xFFDC2626)
                        else -> ArchivalTextPrimary
                    }
                    Text(
                        text = heading,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = headingColor,
                        fontSize = 13.sp
                    )
                }
                trimmed.startsWith("## ") -> {
                    Text(
                        text = trimmed.removePrefix("## "),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = ArchivalTextPrimary,
                        fontFamily = FontFamily.Serif,
                        fontSize = 14.sp
                    )
                }
                trimmed.startsWith("# ") -> {
                    Text(
                        text = trimmed.removePrefix("# "),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ArchivalTextPrimary,
                        fontFamily = FontFamily.Serif
                    )
                }
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Text("•", color = ArchivalPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = buildAnnotatedMarkdown(
                                trimmed.removePrefix("- ").removePrefix("* ").trim(),
                                primary,
                                textPrimary,
                                containerHigh
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = ArchivalTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
                trimmed.firstOrNull()?.isDigit() == true && trimmed.contains(". ") && trimmed.indexOf(". ") < 4 -> {
                    val number = trimmed.substringBefore(". ")
                    val content = trimmed.substringAfter(". ")
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Text(
                            text = "$number.",
                            color = ArchivalPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = buildAnnotatedMarkdown(content, primary, textPrimary, containerHigh),
                            style = MaterialTheme.typography.bodySmall,
                            color = ArchivalTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
                trimmed.isBlank() -> {
                    Spacer(modifier = Modifier.height(2.dp))
                }
                else -> {
                    Text(
                        text = buildAnnotatedMarkdown(trimmed, primary, textPrimary, containerHigh),
                        style = MaterialTheme.typography.bodySmall,
                        color = ArchivalTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

/**
 * Parses inline markdown: **bold**, *italic*, `code`, and [links](url).
 */
internal fun buildAnnotatedMarkdown(
    raw: String,
    primaryColor: Color = Color(0xFF6B4226),
    textPrimaryColor: Color = Color(0xFF2C241E),
    containerHighColor: Color = Color(0xFFEBE0D2)
): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < raw.length) {
            when {
                // Inline bold: **text**
                raw.startsWith("**", i) -> {
                    val end = raw.indexOf("**", i + 2)
                    if (end != -1) {
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = textPrimaryColor))
                        append(raw.substring(i + 2, end))
                        pop()
                        i = end + 2
                    } else {
                        append(raw[i])
                        i++
                    }
                }
                // Inline code: `code`
                raw[i] == '`' -> {
                    val end = raw.indexOf('`', i + 1)
                    if (end != -1) {
                        pushStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = containerHighColor,
                                color = primaryColor
                            )
                        )
                        append(" ${raw.substring(i + 1, end)} ")
                        pop()
                        i = end + 1
                    } else {
                        append(raw[i])
                        i++
                    }
                }
                // Markdown link: [text](url)
                raw[i] == '[' -> {
                    val closeBracket = raw.indexOf(']', i)
                    val openParen = if (closeBracket != -1) raw.indexOf('(', closeBracket) else -1
                    val closeParen = if (openParen != -1) raw.indexOf(')', openParen) else -1
                    if (closeBracket != -1 && openParen == closeBracket + 1 && closeParen != -1) {
                        val text = raw.substring(i + 1, closeBracket)
                        pushStyle(
                            SpanStyle(
                                color = primaryColor,
                                textDecoration = TextDecoration.Underline,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        append(text)
                        pop()
                        i = closeParen + 1
                    } else {
                        append(raw[i])
                        i++
                    }
                }
                // Inline italic: *text* or _text_
                (raw[i] == '*' || raw[i] == '_') && i + 1 < raw.length && raw[i + 1] != raw[i] -> {
                    val marker = raw[i]
                    val end = raw.indexOf(marker, i + 1)
                    if (end != -1 && end > i + 1) {
                        pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                        append(raw.substring(i + 1, end))
                        pop()
                        i = end + 1
                    } else {
                        append(raw[i])
                        i++
                    }
                }
                else -> {
                    append(raw[i])
                    i++
                }
            }
        }
    }
}

@Composable
private fun FilterChipItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) ArchivalPrimary else ArchivalSurfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (selected) ArchivalSurfaceContainer else ArchivalTextSecondary,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun ChannelBadge(channel: ReleaseChannel) {
    if (channel == ReleaseChannel.STABLE) return
    val color = when (channel) {
        ReleaseChannel.BETA -> ArchivalPrimary.copy(alpha = 0.85f)
        ReleaseChannel.NIGHTLY -> ArchivalTextSecondary.copy(alpha = 0.7f)
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(color = ArchivalPrimary, modifier = Modifier.size(36.dp))
            Text("Loading release notes…", color = ArchivalTextSecondary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ChangelogOfflineState(onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(24.dp)
        ) {
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Text("Failed to load releases", color = ArchivalTextPrimary, fontWeight = FontWeight.SemiBold)
            Text(message, color = ArchivalTextSecondary, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onRetry) { Text("Retry", color = ArchivalPrimary) }
        }
    }
}

private fun formatDate(isoDate: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        val date = parser.parse(isoDate) ?: return isoDate.take(10)
        val formatter = SimpleDateFormat("MMM d, yyyy", Locale.US)
        formatter.format(date)
    } catch (_: Exception) {
        isoDate.take(10)
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (1024f * 1024f))
        bytes >= 1024 -> String.format(Locale.US, "%.0f KB", bytes / 1024f)
        else -> "$bytes B"
    }
}

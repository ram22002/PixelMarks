package com.pixelmarks.feature.update

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelmarks.core.update.DownloadState
import com.pixelmarks.core.update.model.GitHubRelease
import com.pixelmarks.core.update.model.ReleaseChannel
import com.pixelmarks.core.update.model.UpdateCheckResult
import com.pixelmarks.ui.theme.ArchivalPrimary
import com.pixelmarks.ui.theme.ArchivalSurfaceContainer
import com.pixelmarks.ui.theme.ArchivalSurfaceContainerHigh
import com.pixelmarks.ui.theme.ArchivalTextPrimary
import com.pixelmarks.ui.theme.ArchivalTextSecondary

/**
 * Dialog shown when an update is available.
 * Displays version, release notes (What's new), and actions:
 * - View Changelog
 * - Update Now
 * - Later
 */
@Composable
fun UpdateAvailableDialog(
    release: GitHubRelease,
    onViewChangelog: () -> Unit,
    onUpdateNow: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ArchivalSurfaceContainer,
        shape = RoundedCornerShape(20.dp),
        icon = {
            Icon(
                imageVector = Icons.Default.SystemUpdateAlt,
                contentDescription = null,
                tint = ArchivalPrimary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Update available",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = ArchivalTextPrimary,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Version: v${release.versionName}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = ArchivalPrimary,
                    fontSize = 13.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "What's new:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = ArchivalTextPrimary
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 160.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ArchivalSurfaceContainerHigh.copy(alpha = 0.5f))
                        .padding(10.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    val notes = release.body.ifBlank { "No release notes provided." }
                    Text(
                        text = notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = ArchivalTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onUpdateNow,
                colors = ButtonDefaults.buttonColors(containerColor = ArchivalPrimary)
            ) {
                Text(
                    text = "Update Now",
                    color = ArchivalSurfaceContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onViewChangelog) {
                    Text("View Changelog", color = ArchivalPrimary)
                }
                Spacer(modifier = Modifier.width(4.dp))
                TextButton(onClick = onDismiss) {
                    Text("Later", color = ArchivalTextSecondary)
                }
            }
        }
    )
}

/**
 * Informational feedback dialog shown when user manually taps "Check for Updates"
 * for states other than [UpdateCheckResult.UpdateAvailable].
 */
@Composable
fun UpdateStatusFeedbackDialog(
    result: UpdateCheckResult,
    installedVersion: String,
    channel: ReleaseChannel,
    onViewOnGitHub: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    when (result) {
        is UpdateCheckResult.UpToDate -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                containerColor = ArchivalSurfaceContainer,
                shape = RoundedCornerShape(20.dp),
                icon = {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = ArchivalPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        text = "You're up to date",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = ArchivalTextPrimary
                    )
                },
                text = {
                    Text(
                        text = "PixelMarks v$installedVersion is the newest version available on the ${channel.label} channel.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ArchivalTextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = ArchivalPrimary)
                    ) {
                        Text("OK", color = ArchivalSurfaceContainer)
                    }
                }
            )
        }

        is UpdateCheckResult.ApkAssetMissing -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                containerColor = ArchivalSurfaceContainer,
                shape = RoundedCornerShape(20.dp),
                icon = {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = ArchivalPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        text = "APK Not Available",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = ArchivalTextPrimary
                    )
                },
                text = {
                    Text(
                        text = "Version v${result.release.versionName} was found on GitHub, but an APK has not been attached yet. You can view the release in your browser.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ArchivalTextSecondary
                    )
                },
                confirmButton = {
                    if (onViewOnGitHub != null) {
                        Button(
                            onClick = onViewOnGitHub,
                            colors = ButtonDefaults.buttonColors(containerColor = ArchivalPrimary)
                        ) {
                            Text("View on GitHub", color = ArchivalSurfaceContainer)
                        }
                    } else {
                        Button(onClick = onDismiss) { Text("OK") }
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text("Close", color = ArchivalTextSecondary)
                    }
                }
            )
        }

        is UpdateCheckResult.RateLimited -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                containerColor = ArchivalSurfaceContainer,
                shape = RoundedCornerShape(20.dp),
                icon = {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        text = "API Rate Limited",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = ArchivalTextPrimary
                    )
                },
                text = {
                    Text(
                        text = "GitHub API rate limit exceeded. Resets at ${result.resetTime}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ArchivalTextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = ArchivalPrimary)
                    ) {
                        Text("OK", color = ArchivalSurfaceContainer)
                    }
                }
            )
        }

        is UpdateCheckResult.Offline -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                containerColor = ArchivalSurfaceContainer,
                shape = RoundedCornerShape(20.dp),
                icon = {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = ArchivalTextSecondary,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        text = "Offline",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = ArchivalTextPrimary
                    )
                },
                text = {
                    Text(
                        text = "Unable to connect to GitHub. Please check your internet connection.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ArchivalTextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = ArchivalPrimary)
                    ) {
                        Text("OK", color = ArchivalSurfaceContainer)
                    }
                }
            )
        }

        is UpdateCheckResult.NoReleasesFound -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                containerColor = ArchivalSurfaceContainer,
                shape = RoundedCornerShape(20.dp),
                title = {
                    Text(
                        text = "No Releases Found",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = ArchivalTextPrimary
                    )
                },
                text = {
                    Text(
                        text = "No published releases were found for the ${result.channel.label} channel.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ArchivalTextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = ArchivalPrimary)
                    ) {
                        Text("OK", color = ArchivalSurfaceContainer)
                    }
                }
            )
        }

        is UpdateCheckResult.ReleaseUnavailable -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                containerColor = ArchivalSurfaceContainer,
                shape = RoundedCornerShape(20.dp),
                title = {
                    Text(
                        text = "Release Unavailable",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = ArchivalTextPrimary
                    )
                },
                text = {
                    Text(
                        text = result.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ArchivalTextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = ArchivalPrimary)
                    ) {
                        Text("OK", color = ArchivalSurfaceContainer)
                    }
                }
            )
        }

        is UpdateCheckResult.InvalidMetadata -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                containerColor = ArchivalSurfaceContainer,
                shape = RoundedCornerShape(20.dp),
                title = {
                    Text(
                        text = "Invalid Metadata",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = ArchivalTextPrimary
                    )
                },
                text = {
                    Text(
                        text = "Received invalid release metadata from GitHub: ${result.message}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ArchivalTextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = ArchivalPrimary)
                    ) {
                        Text("OK", color = ArchivalSurfaceContainer)
                    }
                }
            )
        }

        is UpdateCheckResult.Error -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                containerColor = ArchivalSurfaceContainer,
                shape = RoundedCornerShape(20.dp),
                title = {
                    Text(
                        text = "Check Failed",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = ArchivalTextPrimary
                    )
                },
                text = {
                    Text(
                        text = result.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ArchivalTextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = ArchivalPrimary)
                    ) {
                        Text("OK", color = ArchivalSurfaceContainer)
                    }
                }
            )
        }

        else -> Unit
    }
}

/**
 * Dialog shown during APK download, download completion, or download failure.
 */
@Composable
fun UpdateProgressDialog(
    downloadState: DownloadState,
    canInstallPackages: Boolean,
    onCancel: () -> Unit,
    onInstall: (Uri) -> Unit,
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit,
    onOpenReleasePage: (() -> Unit)? = null
) {
    when (downloadState) {
        is DownloadState.Downloading -> {
            AlertDialog(
                onDismissRequest = { /* Prevent accidental dismiss during download */ },
                containerColor = ArchivalSurfaceContainer,
                shape = RoundedCornerShape(20.dp),
                title = {
                    Text(
                        text = "Downloading Update",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = ArchivalTextPrimary
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (downloadState.progressPercent > 0) {
                                "${downloadState.progressPercent}% downloaded"
                            } else {
                                "Connecting to GitHub…"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = ArchivalTextSecondary
                        )

                        if (downloadState.progressPercent > 0) {
                            LinearProgressIndicator(
                                progress = { downloadState.progressPercent / 100f },
                                modifier = Modifier.fillMaxWidth(),
                                color = ArchivalPrimary,
                                trackColor = ArchivalSurfaceContainerHigh
                            )
                        } else {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                color = ArchivalPrimary,
                                trackColor = ArchivalSurfaceContainerHigh
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = onCancel) {
                        Text("Cancel", color = ArchivalTextSecondary)
                    }
                }
            )
        }

        is DownloadState.Completed -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                containerColor = ArchivalSurfaceContainer,
                shape = RoundedCornerShape(20.dp),
                icon = {
                    Icon(
                        imageVector = if (canInstallPackages) Icons.Default.CheckCircle else Icons.Default.Security,
                        contentDescription = null,
                        tint = ArchivalPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        text = if (canInstallPackages) "Update Ready to Install" else "Permission Required",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = ArchivalTextPrimary
                    )
                },
                text = {
                    Text(
                        text = if (canInstallPackages) {
                            "The update has been downloaded and verified. Would you like to install it now?"
                        } else {
                            "To install the update, please enable \"Install unknown apps\" for PixelMarks in system settings."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = ArchivalTextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (canInstallPackages) {
                                onInstall(downloadState.localUri)
                            } else {
                                onRequestPermission()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ArchivalPrimary)
                    ) {
                        Text(
                            text = if (canInstallPackages) "Install Now" else "Open Settings",
                            color = ArchivalSurfaceContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text("Later", color = ArchivalTextSecondary)
                    }
                }
            )
        }

        is DownloadState.Failed -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                containerColor = ArchivalSurfaceContainer,
                shape = RoundedCornerShape(20.dp),
                icon = {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        text = "Download Failed",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = ArchivalTextPrimary
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = downloadState.reason,
                            style = MaterialTheme.typography.bodySmall,
                            color = ArchivalTextSecondary
                        )
                        if (onOpenReleasePage != null) {
                            Text(
                                text = "You can download the APK directly from GitHub Releases in your browser.",
                                style = MaterialTheme.typography.bodySmall,
                                color = ArchivalTextPrimary
                            )
                        }
                    }
                },
                confirmButton = {
                    if (onOpenReleasePage != null) {
                        Button(
                            onClick = onOpenReleasePage,
                            colors = ButtonDefaults.buttonColors(containerColor = ArchivalPrimary)
                        ) {
                            Text("Open in Browser", color = ArchivalSurfaceContainer, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = ArchivalPrimary)
                        ) {
                            Text("OK", color = ArchivalSurfaceContainer)
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text("Close", color = ArchivalTextSecondary)
                    }
                }
            )
        }

        else -> Unit
    }
}

/**
 * Dialog to select update channel (Stable, Beta, Nightly).
 */
@Composable
fun ReleaseChannelDialog(
    currentChannel: ReleaseChannel,
    onSelectChannel: (ReleaseChannel) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ArchivalSurfaceContainer,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Release Channel",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = ArchivalTextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ReleaseChannel.entries.forEach { channel ->
                    val isSelected = channel == currentChannel
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = channel.label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) ArchivalPrimary else ArchivalTextPrimary
                            )
                            Text(
                                text = when (channel) {
                                    ReleaseChannel.STABLE -> "Official tested releases (recommended)"
                                    ReleaseChannel.BETA -> "Preview upcoming features; may have bugs"
                                    ReleaseChannel.NIGHTLY -> "Bleeding-edge builds with latest changes"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = ArchivalTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                onSelectChannel(channel)
                                onDismiss()
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = ArchivalPrimary,
                                unselectedColor = ArchivalTextSecondary
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = ArchivalTextSecondary)
            }
        }
    )
}

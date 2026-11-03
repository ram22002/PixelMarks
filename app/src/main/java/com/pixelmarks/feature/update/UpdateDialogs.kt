package com.pixelmarks.feature.update

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelmarks.core.update.DownloadState
import com.pixelmarks.core.update.model.GitHubRelease
import com.pixelmarks.core.update.model.ReleaseChannel
import com.pixelmarks.ui.theme.ArchivalPrimary
import com.pixelmarks.ui.theme.ArchivalSurfaceContainer
import com.pixelmarks.ui.theme.ArchivalSurfaceContainerHigh
import com.pixelmarks.ui.theme.ArchivalTextPrimary
import com.pixelmarks.ui.theme.ArchivalTextSecondary

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
                        androidx.compose.material3.RadioButton(
                            selected = isSelected,
                            onClick = {
                                onSelectChannel(channel)
                                onDismiss()
                            },
                            colors = androidx.compose.material3.RadioButtonDefaults.colors(
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


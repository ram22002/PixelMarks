package com.pixelmarks.feature.update

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelmarks.core.update.model.GitHubRelease
import com.pixelmarks.core.update.model.UpdateCheckResult
import com.pixelmarks.ui.theme.ArchivalPrimary
import com.pixelmarks.ui.theme.ArchivalSurfaceContainer
import com.pixelmarks.ui.theme.ArchivalSurfaceContainerHigh
import com.pixelmarks.ui.theme.ArchivalTextPrimary
import com.pixelmarks.ui.theme.ArchivalTextSecondary

/**
 * Unobtrusive update availability banner.
 * Shown in Settings and optionally on the Home screen.
 * The user can dismiss it (persisted), or navigate to install the update.
 */
@Composable
fun UpdateBanner(
    checkResult: UpdateCheckResult,
    onWhatsNew: (GitHubRelease) -> Unit,
    onUpdate: (GitHubRelease) -> Unit,
    onDismiss: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isVisible = checkResult is UpdateCheckResult.UpdateAvailable
    AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier
    ) {
        val release = (checkResult as? UpdateCheckResult.UpdateAvailable)?.release ?: return@AnimatedVisibility

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(ArchivalSurfaceContainer)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SystemUpdateAlt,
                contentDescription = null,
                tint = ArchivalPrimary,
                modifier = Modifier.size(22.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Update available — v${release.versionName}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = ArchivalTextPrimary,
                    fontSize = 13.sp
                )
                Text(
                    text = release.channel.label + " release",
                    style = MaterialTheme.typography.labelSmall,
                    color = ArchivalTextSecondary,
                    fontSize = 11.sp
                )
            }

            TextButton(onClick = { onWhatsNew(release) }) {
                Text(
                    text = "What's new",
                    color = ArchivalPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.width(2.dp))

            TextButton(onClick = { onUpdate(release) }) {
                Text(
                    text = "Update",
                    color = ArchivalPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(
                onClick = { onDismiss(release.versionName) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss update",
                    tint = ArchivalTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}


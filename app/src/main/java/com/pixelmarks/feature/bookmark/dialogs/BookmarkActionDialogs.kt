package com.pixelmarks.feature.bookmark.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.pixelmarks.core.image.ImageUrlValidator
import com.pixelmarks.ui.theme.Spacing
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun SetReminderDialog(
    currentReminderAt: Long?,
    onDismiss: () -> Unit,
    onSetReminder: (Long?) -> Unit
) {
    val now = System.currentTimeMillis()

    // Calculate tomorrow 9:00 AM
    val tomorrowMorning = remember {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        cal.timeInMillis
    }

    val threeHoursLater = now + (3 * 3600 * 1000L)
    val threeDaysLater = now + (3 * 24 * 3600 * 1000L)
    val oneWeekLater = now + (7 * 24 * 3600 * 1000L)

    val dateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                Icon(
                    imageVector = Icons.Default.Alarm,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("Set Read-Later Reminder")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                if (currentReminderAt != null) {
                    Text(
                        text = "Current: ${dateFormat.format(Date(currentReminderAt))}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                ReminderPresetItem(
                    title = "Later Today (+3 hours)",
                    subtitle = dateFormat.format(Date(threeHoursLater)),
                    icon = Icons.Default.Alarm,
                    onClick = {
                        onSetReminder(threeHoursLater)
                        onDismiss()
                    }
                )

                ReminderPresetItem(
                    title = "Tomorrow Morning",
                    subtitle = dateFormat.format(Date(tomorrowMorning)),
                    icon = Icons.Default.CalendarMonth,
                    onClick = {
                        onSetReminder(tomorrowMorning)
                        onDismiss()
                    }
                )

                ReminderPresetItem(
                    title = "In 3 Days",
                    subtitle = dateFormat.format(Date(threeDaysLater)),
                    icon = Icons.Default.CalendarMonth,
                    onClick = {
                        onSetReminder(threeDaysLater)
                        onDismiss()
                    }
                )

                ReminderPresetItem(
                    title = "Next Week",
                    subtitle = dateFormat.format(Date(oneWeekLater)),
                    icon = Icons.Default.CalendarMonth,
                    onClick = {
                        onSetReminder(oneWeekLater)
                        onDismiss()
                    }
                )
            }
        },
        confirmButton = {
            if (currentReminderAt != null) {
                TextButton(
                    onClick = {
                        onSetReminder(null)
                        onDismiss()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.AlarmOff,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("Clear Reminder")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ReminderPresetItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.medium, vertical = Spacing.small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun SetImageFromUrlDialog(
    initialUrl: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var urlText by remember { mutableStateOf(initialUrl ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("Set Image From URL")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                Text(
                    text = "Enter a direct image link (HTTP or HTTPS):",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = urlText,
                    onValueChange = {
                        urlText = it
                        errorMessage = null
                    },
                    placeholder = { Text("https://example.com/cover.jpg") },
                    trailingIcon = {
                        if (urlText.isNotEmpty()) {
                            IconButton(onClick = { urlText = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    isError = errorMessage != null,
                    supportingText = {
                        if (errorMessage != null) {
                            Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = urlText.trim()
                    if (trimmed.isEmpty()) {
                        errorMessage = "Image URL cannot be empty"
                        return@Button
                    }
                    if (!ImageUrlValidator.isValidImageUrl(trimmed)) {
                        errorMessage = "Only http:// and https:// image URLs are allowed"
                        return@Button
                    }
                    onConfirm(trimmed)
                    onDismiss()
                }
            ) {
                Text("Set Image")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SetExpirationDialog(
    currentNotes: String,
    onDismiss: () -> Unit,
    onSetExpirationNote: (String) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val now = System.currentTimeMillis()

    fun calculateExpiryNote(days: Int): String {
        val target = now + (days * 24 * 3600 * 1000L)
        val dateStr = dateFormat.format(Date(target))
        val tag = "[Expires: $dateStr]"
        // Replace existing expiry tag if present or append
        val cleaned = currentNotes.replace(Regex("\\[Expires: [^\\]]+\\]"), "").trim()
        return if (cleaned.isEmpty()) tag else "$cleaned\n$tag"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                Icon(
                    imageVector = Icons.Default.HourglassBottom,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("Set Expiration")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                Text(
                    text = "Mark when this temporary bookmark should expire:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))

                ReminderPresetItem(
                    title = "7 Days",
                    subtitle = "Expires in 1 week",
                    icon = Icons.Default.HourglassBottom,
                    onClick = {
                        onSetExpirationNote(calculateExpiryNote(7))
                        onDismiss()
                    }
                )

                ReminderPresetItem(
                    title = "30 Days",
                    subtitle = "Expires in 1 month",
                    icon = Icons.Default.HourglassBottom,
                    onClick = {
                        onSetExpirationNote(calculateExpiryNote(30))
                        onDismiss()
                    }
                )

                ReminderPresetItem(
                    title = "90 Days",
                    subtitle = "Expires in 3 months",
                    icon = Icons.Default.HourglassBottom,
                    onClick = {
                        onSetExpirationNote(calculateExpiryNote(90))
                        onDismiss()
                    }
                )
            }
        },
        confirmButton = {
            if (currentNotes.contains("[Expires:")) {
                TextButton(
                    onClick = {
                        val cleaned = currentNotes.replace(Regex("\\[Expires: [^\\]]+\\]"), "").trim()
                        onSetExpirationNote(cleaned)
                        onDismiss()
                    }
                ) {
                    Text("Clear Expiration")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

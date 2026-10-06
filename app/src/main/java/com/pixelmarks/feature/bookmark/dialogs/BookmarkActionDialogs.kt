package com.pixelmarks.feature.bookmark.dialogs

import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pixelmarks.core.image.ImageUrlValidator
import com.pixelmarks.core.reminder.ReminderFormatter
import com.pixelmarks.core.reminder.ReminderTimeHelper
import com.pixelmarks.ui.theme.Spacing
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class CustomReminderStep {
    NONE,
    DATE_PICKER,
    TIME_PICKER,
    CONFIRMATION
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetReminderDialog(
    currentReminderAt: Long?,
    onDismiss: () -> Unit,
    onSetReminder: (Long?) -> Unit
) {
    var customStep by remember { mutableStateOf(CustomReminderStep.NONE) }
    var selectedDateUtcMillis by remember { mutableStateOf<Long?>(null) }
    var selectedHour by remember { mutableIntStateOf(12) }
    var selectedMinute by remember { mutableIntStateOf(0) }
    var customTargetTimestamp by remember { mutableLongStateOf(0L) }

    when (customStep) {
        CustomReminderStep.NONE -> {
            PresetReminderDialog(
                currentReminderAt = currentReminderAt,
                onDismiss = onDismiss,
                onSelectPreset = { timestamp ->
                    onSetReminder(timestamp)
                    onDismiss()
                },
                onClearReminder = {
                    onSetReminder(null)
                    onDismiss()
                },
                onOpenCustom = {
                    customStep = CustomReminderStep.DATE_PICKER
                }
            )
        }

        CustomReminderStep.DATE_PICKER -> {
            CustomDatePickerDialog(
                initialDateMillis = selectedDateUtcMillis,
                onDateSelected = { dateUtc ->
                    selectedDateUtcMillis = dateUtc
                    customStep = CustomReminderStep.TIME_PICKER
                },
                onDismiss = {
                    customStep = CustomReminderStep.NONE
                }
            )
        }

        CustomReminderStep.TIME_PICKER -> {
            CustomTimePickerDialog(
                selectedDateUtcMillis = selectedDateUtcMillis ?: System.currentTimeMillis(),
                initialHour = selectedHour,
                initialMinute = selectedMinute,
                onTimeSelected = { hour, minute ->
                    selectedHour = hour
                    selectedMinute = minute
                    val combined = ReminderTimeHelper.combineDateAndTime(
                        dateUtcMidnightMillis = selectedDateUtcMillis ?: System.currentTimeMillis(),
                        hourOfDay = hour,
                        minute = minute
                    )
                    customTargetTimestamp = combined
                    customStep = CustomReminderStep.CONFIRMATION
                },
                onBack = {
                    customStep = CustomReminderStep.DATE_PICKER
                },
                onDismiss = {
                    customStep = CustomReminderStep.NONE
                }
            )
        }

        CustomReminderStep.CONFIRMATION -> {
            CustomReminderConfirmDialog(
                timestamp = customTargetTimestamp,
                onConfirm = { confirmedTimestamp ->
                    onSetReminder(confirmedTimestamp)
                    customStep = CustomReminderStep.NONE
                    onDismiss()
                },
                onBack = {
                    customStep = CustomReminderStep.TIME_PICKER
                },
                onDismiss = {
                    customStep = CustomReminderStep.NONE
                }
            )
        }
    }
}

@Composable
private fun PresetReminderDialog(
    currentReminderAt: Long?,
    onDismiss: () -> Unit,
    onSelectPreset: (Long) -> Unit,
    onClearReminder: () -> Unit,
    onOpenCustom: () -> Unit
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

    // Calculate tomorrow 8:00 PM
    val tomorrowEvening = remember {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 20)
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
                Text(if (currentReminderAt != null) "Edit Reminder" else "Set Read-Later Reminder")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                if (currentReminderAt != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.medium, vertical = Spacing.small),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.small)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Current reminder",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = ReminderFormatter.format(currentReminderAt),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            TextButton(onClick = onClearReminder) {
                                Text(
                                    text = "Remove",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(Spacing.extraSmall))
                }

                ReminderPresetItem(
                    title = "Later Today (+3 hours)",
                    subtitle = dateFormat.format(Date(threeHoursLater)),
                    icon = Icons.Default.Alarm,
                    onClick = { onSelectPreset(threeHoursLater) }
                )

                ReminderPresetItem(
                    title = "Tomorrow Morning",
                    subtitle = dateFormat.format(Date(tomorrowMorning)),
                    icon = Icons.Default.CalendarMonth,
                    onClick = { onSelectPreset(tomorrowMorning) }
                )

                ReminderPresetItem(
                    title = "Tomorrow Evening",
                    subtitle = dateFormat.format(Date(tomorrowEvening)),
                    icon = Icons.Default.CalendarMonth,
                    onClick = { onSelectPreset(tomorrowEvening) }
                )

                ReminderPresetItem(
                    title = "In 3 Days",
                    subtitle = dateFormat.format(Date(threeDaysLater)),
                    icon = Icons.Default.CalendarMonth,
                    onClick = { onSelectPreset(threeDaysLater) }
                )

                ReminderPresetItem(
                    title = "Next Week",
                    subtitle = dateFormat.format(Date(oneWeekLater)),
                    icon = Icons.Default.CalendarMonth,
                    onClick = { onSelectPreset(oneWeekLater) }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                ReminderPresetItem(
                    title = "Custom date & time",
                    subtitle = "Choose specific date & time",
                    icon = Icons.Default.EditCalendar,
                    onClick = onOpenCustom
                )
            }
        },
        confirmButton = {
            if (currentReminderAt != null) {
                TextButton(onClick = onClearReminder) {
                    Icon(
                        imageVector = Icons.Default.AlarmOff,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("Remove Reminder")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomDatePickerDialog(
    initialDateMillis: Long?,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDateMillis ?: System.currentTimeMillis(),
        selectableDates = remember {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return ReminderTimeHelper.isDateSelectable(utcTimeMillis)
                }

                override fun isSelectableYear(year: Int): Boolean {
                    return year >= Calendar.getInstance().get(Calendar.YEAR)
                }
            }
        }
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { onDateSelected(it) }
                },
                enabled = datePickerState.selectedDateMillis != null
            ) {
                Text("Next")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomTimePickerDialog(
    selectedDateUtcMillis: Long,
    initialHour: Int,
    initialMinute: Int,
    onTimeSelected: (hour: Int, minute: Int) -> Unit,
    onBack: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val is24Hour = remember { DateFormat.is24HourFormat(context) }
    val now = remember { System.currentTimeMillis() }
    val isToday = remember(selectedDateUtcMillis) {
        ReminderTimeHelper.isDateToday(selectedDateUtcMillis, now)
    }

    val defaultHour = remember(selectedDateUtcMillis) {
        if (isToday) {
            val nextHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) + 1
            if (nextHour >= 24) 23 else nextHour
        } else {
            9
        }
    }
    val defaultMinute = remember(selectedDateUtcMillis) { 0 }

    val timePickerState = rememberTimePickerState(
        initialHour = if (initialHour != 12) initialHour else defaultHour,
        initialMinute = if (initialMinute != 0) initialMinute else defaultMinute,
        is24Hour = is24Hour
    )

    var timeErrorMessage by remember { mutableStateOf<String?>(null) }

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
                Text("Select Time")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                TimePicker(state = timePickerState)

                if (timeErrorMessage != null) {
                    Text(
                        text = timeErrorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val combined = ReminderTimeHelper.combineDateAndTime(
                        dateUtcMidnightMillis = selectedDateUtcMillis,
                        hourOfDay = timePickerState.hour,
                        minute = timePickerState.minute
                    )
                    val validation = ReminderTimeHelper.validateReminderTime(combined)
                    if (validation is ReminderTimeHelper.ValidationResult.Invalid) {
                        timeErrorMessage = validation.message
                    } else {
                        timeErrorMessage = null
                        onTimeSelected(timePickerState.hour, timePickerState.minute)
                    }
                }
            ) {
                Text("Next")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onBack) {
                    Text("Back")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
private fun CustomReminderConfirmDialog(
    timestamp: Long,
    onConfirm: (Long) -> Unit,
    onBack: () -> Unit,
    onDismiss: () -> Unit
) {
    var confirmError by remember { mutableStateOf<String?>(null) }
    val fullDateFormat = remember { SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()) }
    val fullTimeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val dateStr = remember(timestamp) { fullDateFormat.format(Date(timestamp)) }
    val timeStr = remember(timestamp) { fullTimeFormat.format(Date(timestamp)) }

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
                Text("Custom Reminder")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.medium),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = dateStr,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = timeStr,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (confirmError != null) {
                    Text(
                        text = confirmError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val validation = ReminderTimeHelper.validateReminderTime(timestamp)
                    if (validation is ReminderTimeHelper.ValidationResult.Invalid) {
                        confirmError = validation.message
                    } else {
                        onConfirm(timestamp)
                    }
                }
            ) {
                Text("Set Reminder")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onBack) {
                    Text("Back")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
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

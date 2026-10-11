package com.pixelmarks.feature.tags

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pixelmarks.core.model.Tag
import com.pixelmarks.ui.theme.ArchivalError
import com.pixelmarks.ui.theme.ArchivalSurfaceContainerHigh
import com.pixelmarks.ui.theme.ArchivalSurfaceContainerHighest
import com.pixelmarks.ui.theme.ArchivalSurfaceContainerLowest
import com.pixelmarks.ui.theme.ArchivalTextPrimary
import com.pixelmarks.ui.theme.ArchivalTextSecondary
import com.pixelmarks.ui.theme.BrassGold
import com.pixelmarks.ui.theme.Spacing
import com.pixelmarks.ui.theme.TerracottaAccent

@Composable
fun CreateEditTagDialog(
    initialTag: Tag? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, color: String?) -> Unit
) {
    var name by remember { mutableStateOf(initialTag?.name ?: "") }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ArchivalSurfaceContainerHigh,
        titleContentColor = ArchivalTextPrimary,
        textContentColor = ArchivalTextSecondary,
        shape = RoundedCornerShape(18.dp),
        title = {
            Text(
                text = if (initialTag == null) "Create Tag" else "Edit Tag",
                style = MaterialTheme.typography.titleLarge,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                color = ArchivalTextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.medium)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        validationError = null
                    },
                    label = { Text("Tag Name", color = ArchivalTextSecondary) },
                    placeholder = { Text("e.g. Android, Design, Recipe", color = ArchivalTextSecondary.copy(alpha = 0.5f)) },
                    isError = validationError != null,
                    supportingText = validationError?.let { { Text(it, color = ArchivalError) } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ArchivalTextPrimary,
                        unfocusedTextColor = ArchivalTextPrimary,
                        focusedBorderColor = BrassGold,
                        unfocusedBorderColor = ArchivalSurfaceContainerHighest,
                        focusedLabelColor = BrassGold,
                        unfocusedLabelColor = ArchivalTextSecondary,
                        cursorColor = BrassGold
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isEmpty()) {
                        validationError = "Tag name cannot be empty"
                    } else {
                        onSave(trimmed, initialTag?.color)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = TerracottaAccent,
                    contentColor = ArchivalSurfaceContainerLowest
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = ArchivalTextSecondary
                )
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DeleteTagConfirmDialog(
    tagName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ArchivalSurfaceContainerHigh,
        titleContentColor = ArchivalTextPrimary,
        textContentColor = ArchivalTextSecondary,
        shape = RoundedCornerShape(18.dp),
        title = {
            Text(
                text = "Delete Tag?",
                style = MaterialTheme.typography.titleLarge,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                color = ArchivalTextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
                Text("Are you sure you want to delete \"$tagName\"?", color = ArchivalTextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "• Bookmarks using this tag will NOT be deleted.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ArchivalTextSecondary
                )
                Text(
                    text = "• The tag will only be unassigned from any bookmarks.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ArchivalTextSecondary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Delete", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = ArchivalTextSecondary
                )
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SelectTagsDialog(
    currentTagIds: Set<Long>,
    availableTags: List<Tag>,
    onDismiss: () -> Unit,
    onConfirm: (Set<Long>) -> Unit,
    onCreateNewTagRequested: () -> Unit
) {
    var selectedIds by remember { mutableStateOf(currentTagIds) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ArchivalSurfaceContainerHigh,
        titleContentColor = ArchivalTextPrimary,
        textContentColor = ArchivalTextSecondary,
        shape = RoundedCornerShape(18.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select Tags",
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    color = ArchivalTextPrimary
                )
                OutlinedButton(
                    onClick = onCreateNewTagRequested,
                    modifier = Modifier.height(36.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = BrassGold
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("New Tag", color = BrassGold)
                }
            }
        },
        text = {
            if (availableTags.isEmpty()) {
                Text("No tags created yet.", color = ArchivalTextSecondary)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(availableTags, key = { it.id }) { tag ->
                        val isChecked = selectedIds.contains(tag.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedIds = if (isChecked) {
                                        selectedIds - tag.id
                                    } else {
                                        selectedIds + tag.id
                                    }
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    selectedIds = if (checked) {
                                        selectedIds + tag.id
                                    } else {
                                        selectedIds - tag.id
                                    }
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = BrassGold,
                                    uncheckedColor = ArchivalTextSecondary,
                                    checkmarkColor = ArchivalSurfaceContainerLowest
                                )
                            )
                            Text(
                                text = "#${tag.name}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ArchivalTextPrimary,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedIds) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = TerracottaAccent,
                    contentColor = ArchivalSurfaceContainerLowest
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = ArchivalTextSecondary
                )
            ) {
                Text("Cancel")
            }
        }
    )
}

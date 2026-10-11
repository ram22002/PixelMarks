package com.pixelmarks.feature.bookmark

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pixelmarks.app.PixelMarksApplication
import com.pixelmarks.core.model.Collection
import com.pixelmarks.core.util.UrlUtils
import com.pixelmarks.domain.usecase.AddBookmarkResult
import com.pixelmarks.ui.theme.ArchivalError
import com.pixelmarks.ui.theme.ArchivalSurfaceContainerHigh
import com.pixelmarks.ui.theme.ArchivalSurfaceContainerHighest
import com.pixelmarks.ui.theme.ArchivalSurfaceContainerLowest
import com.pixelmarks.ui.theme.ArchivalTextPrimary
import com.pixelmarks.ui.theme.ArchivalTextSecondary
import com.pixelmarks.ui.theme.BrassGold
import com.pixelmarks.ui.theme.Spacing
import com.pixelmarks.ui.theme.TerracottaAccent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddBookmarkDialog(
    isPrivateInitial: Boolean = false,
    initialCollectionId: Long? = null,
    onDismiss: () -> Unit,
    onOpenFullAddScreen: (() -> Unit)? = null,
    onBookmarkSaved: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as PixelMarksApplication
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    var url by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var selectedCollectionId by remember { mutableStateOf(initialCollectionId) }
    var isPrivate by remember { mutableStateOf(isPrivateInitial) }
    var availableCollections by remember { mutableStateOf<List<Collection>>(emptyList()) }
    var isCollectionDropdownExpanded by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var duplicateExistingId by remember { mutableStateOf<Long?>(null) }

    // Load available collections for current privacy scope
    LaunchedEffect(isPrivate) {
        app.container.observeCollectionsUseCase?.let { useCase ->
            useCase(isPrivate = isPrivate).collect { cols ->
                availableCollections = cols
            }
        }
    }

    // Auto-populate URL from clipboard if clipboard contains a valid URL
    LaunchedEffect(Unit) {
        val clipText = clipboardManager.getText()?.text?.trim()
        if (!clipText.isNullOrBlank() && UrlUtils.isValidUrl(clipText)) {
            url = clipText
        }
    }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = ArchivalTextPrimary,
        unfocusedTextColor = ArchivalTextPrimary,
        focusedBorderColor = BrassGold,
        unfocusedBorderColor = ArchivalSurfaceContainerHighest,
        focusedLabelColor = BrassGold,
        unfocusedLabelColor = ArchivalTextSecondary,
        cursorColor = BrassGold
    )

    fun handleSave(forceSave: Boolean = false) {
        val trimmedUrl = url.trim()
        if (trimmedUrl.isEmpty()) {
            errorMessage = "URL cannot be empty"
            return
        }

        isSaving = true
        errorMessage = null
        coroutineScope.launch {
            val result = app.container.addBookmarkUseCase(
                rawUrl = trimmedUrl,
                customTitle = title.trim().ifEmpty { null },
                collectionId = selectedCollectionId,
                isPrivate = isPrivate,
                forceSave = forceSave
            )

            isSaving = false
            when (result) {
                is AddBookmarkResult.Success -> {
                    onBookmarkSaved(result.bookmark.id)
                }
                is AddBookmarkResult.Duplicate -> {
                    duplicateExistingId = result.existing.id
                    errorMessage = "This bookmark is already saved. Tap Save again to keep both."
                }
                is AddBookmarkResult.InvalidUrl -> {
                    errorMessage = result.message
                }
                is AddBookmarkResult.Error -> {
                    errorMessage = result.message
                }
            }
        }
    }

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
                    text = "Add Bookmark",
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    color = ArchivalTextPrimary
                )
                if (onOpenFullAddScreen != null) {
                    TextButton(
                        onClick = onOpenFullAddScreen,
                        colors = ButtonDefaults.textButtonColors(contentColor = BrassGold)
                    ) {
                        Text("More Details", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.medium)
            ) {
                // URL input with Paste / Clear quick action
                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        errorMessage = null
                        duplicateExistingId = null
                    },
                    label = { Text("Web URL", color = ArchivalTextSecondary) },
                    placeholder = { Text("https://example.com", color = ArchivalTextSecondary.copy(alpha = 0.5f)) },
                    leadingIcon = {
                        Icon(Icons.Default.Link, contentDescription = null, tint = BrassGold)
                    },
                    trailingIcon = {
                        if (url.isNotEmpty()) {
                            IconButton(onClick = { url = ""; errorMessage = null }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = ArchivalTextSecondary)
                            }
                        } else {
                            IconButton(onClick = {
                                val clip = clipboardManager.getText()?.text?.trim()
                                if (!clip.isNullOrBlank()) {
                                    url = clip
                                    errorMessage = null
                                }
                            }) {
                                Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = BrassGold)
                            }
                        }
                    },
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { { Text(it, color = ArchivalError) } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = textFieldColors,
                    shape = RoundedCornerShape(10.dp)
                )

                // Title input (optional)
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title (Optional)", color = ArchivalTextSecondary) },
                    placeholder = { Text("Auto-derived from webpage if empty", color = ArchivalTextSecondary.copy(alpha = 0.5f)) },
                    leadingIcon = {
                        Icon(Icons.Default.Title, contentDescription = null, tint = ArchivalTextSecondary)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = textFieldColors,
                    shape = RoundedCornerShape(10.dp)
                )

                // Collection selector dropdown
                ExposedDropdownMenuBox(
                    expanded = isCollectionDropdownExpanded,
                    onExpandedChange = { isCollectionDropdownExpanded = it }
                ) {
                    val currentCollectionName = availableCollections.firstOrNull { it.id == selectedCollectionId }?.name
                        ?: "No Collection (Unsorted)"
                    OutlinedTextField(
                        value = currentCollectionName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Collection", color = ArchivalTextSecondary) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCollectionDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        colors = textFieldColors,
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = isCollectionDropdownExpanded,
                        onDismissRequest = { isCollectionDropdownExpanded = false },
                        modifier = Modifier.background(ArchivalSurfaceContainerHigh)
                    ) {
                        DropdownMenuItem(
                            text = { Text("No Collection (Unsorted)", color = ArchivalTextPrimary) },
                            onClick = {
                                selectedCollectionId = null
                                isCollectionDropdownExpanded = false
                            }
                        )
                        availableCollections.forEach { col ->
                            DropdownMenuItem(
                                text = { Text(col.name, color = ArchivalTextPrimary) },
                                onClick = {
                                    selectedCollectionId = col.id
                                    isCollectionDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Private toggle row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (isPrivate) BrassGold else ArchivalTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                    Text(
                        text = "Save in Secure Folder",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ArchivalTextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = isPrivate,
                        onCheckedChange = { isPrivate = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ArchivalSurfaceContainerLowest,
                            checkedTrackColor = BrassGold,
                            uncheckedThumbColor = ArchivalTextSecondary,
                            uncheckedTrackColor = ArchivalSurfaceContainerHighest
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (duplicateExistingId != null) {
                        handleSave(forceSave = true)
                    } else {
                        handleSave(forceSave = false)
                    }
                },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = TerracottaAccent,
                    contentColor = ArchivalSurfaceContainerLowest
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = ArchivalSurfaceContainerLowest
                    )
                } else {
                    Text(
                        text = if (duplicateExistingId != null) "Save Anyway" else "Save",
                        fontWeight = FontWeight.Bold
                    )
                }
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

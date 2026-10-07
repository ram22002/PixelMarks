package com.pixelmarks.feature.bookmark

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pixelmarks.app.PixelMarksApplication
import com.pixelmarks.core.image.BookmarkThumbnail
import com.pixelmarks.core.image.ImageUrlValidator
import com.pixelmarks.core.reminder.ReminderFormatter
import com.pixelmarks.feature.bookmark.dialogs.SetExpirationDialog
import com.pixelmarks.feature.bookmark.dialogs.SetImageFromUrlDialog
import com.pixelmarks.feature.bookmark.dialogs.SetReminderDialog
import com.pixelmarks.feature.collection.SelectCollectionDialog
import com.pixelmarks.feature.collection.getIconVector
import com.pixelmarks.feature.collection.parseHexColor
import com.pixelmarks.feature.tags.CreateEditTagDialog
import com.pixelmarks.feature.tags.SelectTagsDialog
import com.pixelmarks.ui.components.PixelMarksScreenScaffold
import com.pixelmarks.ui.theme.Spacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditBookmarkScreen(
    bookmarkId: Long,
    onNavigateBack: () -> Unit,
    viewModel: EditBookmarkViewModel = viewModel(
        factory = run {
            val app = LocalContext.current.applicationContext as PixelMarksApplication
            EditBookmarkViewModel.provideFactory(
                bookmarkId = bookmarkId,
                bookmarkRepository = app.container.bookmarkRepository,
                tagRepository = app.container.tagRepository,
                collectionRepository = app.container.collectionRepository,
                reminderRepository = app.container.reminderRepository,
                metadataRepository = app.container.metadataRepository,
                createTagUseCase = app.container.createTagUseCase,
                privateModeManager = app.container.privateModeManager
            )
        }
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showCollectionPicker by remember { mutableStateOf(false) }
    var showSelectTagsDialog by remember { mutableStateOf(false) }
    var showCreateTagDialog by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var showExpirationDialog by remember { mutableStateOf(false) }
    var showSetImageUrlDialog by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.onImageReplaced(uri.toString())
        }
    }

    LaunchedEffect(uiState.isSavedSuccessfully) {
        if (uiState.isSavedSuccessfully) {
            onNavigateBack()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.dismissError()
        }
    }

    if (showCollectionPicker) {
        SelectCollectionDialog(
            title = "Choose Collection",
            confirmButtonText = "Select",
            currentCollectionId = uiState.selectedCollectionId,
            availableCollections = uiState.availableCollections,
            onDismiss = { showCollectionPicker = false },
            onSelectCollection = { colId ->
                viewModel.onCollectionSelected(colId)
                showCollectionPicker = false
            }
        )
    }

    if (showSelectTagsDialog) {
        SelectTagsDialog(
            currentTagIds = uiState.selectedTagIds,
            availableTags = uiState.availableTags,
            onDismiss = { showSelectTagsDialog = false },
            onConfirm = { selectedIds ->
                viewModel.onTagsSelected(selectedIds)
                showSelectTagsDialog = false
            },
            onCreateNewTagRequested = {
                showCreateTagDialog = true
            }
        )
    }

    if (showCreateTagDialog) {
        CreateEditTagDialog(
            onDismiss = { showCreateTagDialog = false },
            onSave = { name, _ ->
                viewModel.createTag(name) { newTag ->
                    showCreateTagDialog = false
                    viewModel.onTagsSelected(uiState.selectedTagIds + newTag.id)
                }
            }
        )
    }

    if (showReminderDialog) {
        SetReminderDialog(
            currentReminderAt = uiState.reminderAt,
            onDismiss = { showReminderDialog = false },
            onSetReminder = { timestamp ->
                viewModel.onReminderChanged(timestamp)
            }
        )
    }

    if (showExpirationDialog) {
        SetExpirationDialog(
            currentNotes = uiState.notes,
            onDismiss = { showExpirationDialog = false },
            onSetExpirationNote = { updatedNotes ->
                viewModel.onNotesChanged(updatedNotes)
            }
        )
    }

    if (showSetImageUrlDialog) {
        SetImageFromUrlDialog(
            initialUrl = uiState.thumbnailUrl,
            onDismiss = { showSetImageUrlDialog = false },
            onConfirm = { url ->
                viewModel.onImageReplaced(url)
            }
        )
    }

    var showPrivacyConfirmDialog by remember { mutableStateOf(false) }

    if (showPrivacyConfirmDialog) {
        val targetPrivate = !uiState.isPrivate
        val selectedColName = uiState.availableCollections.firstOrNull { it.id == uiState.selectedCollectionId }?.name
        val selectedTagNames = uiState.availableTags.filter { it.id in uiState.selectedTagIds }.map { it.name }
        val associationsSummary = buildString {
            if (selectedColName != null) {
                append("collection \"$selectedColName\"")
            }
            if (selectedTagNames.isNotEmpty()) {
                if (selectedColName != null) append(" and ")
                append("tag(s) ${selectedTagNames.joinToString { "\"$it\"" }}")
            }
        }

        val dialogTitle = if (targetPrivate) "Move to Secure Folder?" else "Make Bookmark Public?"
        val dialogMsg = if (targetPrivate) {
            if (associationsSummary.isNotBlank()) {
                "Moving this bookmark to Secure Folder will isolate it from normal mode. Currently assigned to $associationsSummary. These public associations will be unlinked."
            } else {
                "Moving this bookmark to Secure Folder will isolate it from normal mode."
            }
        } else {
            if (associationsSummary.isNotBlank()) {
                "Making this bookmark public will make it visible in normal mode. Currently assigned to private $associationsSummary. These private associations will be removed to prevent privacy leaks."
            } else {
                "Making this bookmark public will make it visible in normal mode."
            }
        }

        AlertDialog(
            onDismissRequest = { showPrivacyConfirmDialog = false },
            title = { Text(dialogTitle, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
            text = { Text(dialogMsg) },
            confirmButton = {
                Button(
                    onClick = {
                        showPrivacyConfirmDialog = false
                        viewModel.requestTogglePrivacy(targetPrivate)
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPrivacyConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    PixelMarksScreenScaffold(
        title = if (uiState.isPrivate) "Edit Private Bookmark 🔒" else "Edit Bookmark",
        onNavigateBack = onNavigateBack
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Spacing.medium)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(Spacing.medium)
                ) {
                    // Image Section & Actions
                    Text(
                        text = "Bookmark Image",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (ImageUrlValidator.isValidImageUrl(uiState.thumbnailUrl)) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            BookmarkThumbnail(
                                thumbnailUrl = uiState.thumbnailUrl,
                                modifier = Modifier.fillMaxWidth(),
                                size = 150.dp
                            )
                            IconButton(
                                onClick = viewModel::onImageRemoved,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                    .size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Remove Image",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.small)
                    ) {
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("Pick Photo", style = MaterialTheme.typography.labelMedium)
                        }

                        OutlinedButton(
                            onClick = { showSetImageUrlDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("Image URL", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    // URL Field
                    OutlinedTextField(
                        value = uiState.url,
                        onValueChange = viewModel::onUrlChanged,
                        label = { Text("Web URL") },
                        leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                        trailingIcon = {
                            if (uiState.url.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onUrlChanged("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Title Field
                    OutlinedTextField(
                        value = uiState.title,
                        onValueChange = viewModel::onTitleChanged,
                        label = { Text("Title") },
                        leadingIcon = { Icon(Icons.Default.Title, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Description Field
                    OutlinedTextField(
                        value = uiState.description,
                        onValueChange = viewModel::onDescriptionChanged,
                        label = { Text("Description (Optional)") },
                        leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Notes Field
                    OutlinedTextField(
                        value = uiState.notes,
                        onValueChange = viewModel::onNotesChanged,
                        label = { Text("Optional Note") },
                        placeholder = { Text("Watch this later because...") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null) },
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Privacy Toggle Card
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = if (uiState.isPrivate) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.medium, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (uiState.isPrivate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(end = Spacing.medium)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Private Bookmark",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (uiState.isPrivate) "Stored in Secure Folder" else "Accessible in normal mode",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = uiState.isPrivate,
                                onCheckedChange = { showPrivacyConfirmDialog = true }
                            )
                        }
                    }

                    // Collection Selector
                    val selectedCol = uiState.availableCollections.firstOrNull { it.id == uiState.selectedCollectionId }
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCollectionPicker = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.medium, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
                        ) {
                            if (selectedCol != null) {
                                val colColor = parseHexColor(selectedCol.color)
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(colColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getIconVector(selectedCol.icon),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Collection", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                    Text(selectedCol.name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                                }
                                IconButton(
                                    onClick = { viewModel.onCollectionSelected(null) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear collection", tint = MaterialTheme.colorScheme.outline)
                                }
                            } else {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Collection", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                    Text("None (Unsorted)", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    // Tags Section
                    val selectedTags = uiState.availableTags.filter { uiState.selectedTagIds.contains(it.id) }
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(Spacing.small)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tags (${selectedTags.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            OutlinedButton(
                                onClick = { showSelectTagsDialog = true },
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.size(4.dp))
                                Text(if (selectedTags.isEmpty()) "Add Tags" else "Edit Tags", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        if (selectedTags.isNotEmpty()) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                selectedTags.forEach { tag ->
                                    InputChip(
                                        selected = true,
                                        onClick = { viewModel.onRemoveTag(tag.id) },
                                        label = { Text(tag.name) },
                                        trailingIcon = {
                                            Icon(Icons.Default.Clear, contentDescription = "Remove tag", modifier = Modifier.size(16.dp))
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Reminder and Expiration Section
                    if (uiState.reminderAt != null) {
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = Spacing.medium, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Alarm,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Reminder",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = ReminderFormatter.format(uiState.reminderAt!!),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TextButton(onClick = { showReminderDialog = true }) {
                                        Text("Edit")
                                    }
                                    TextButton(onClick = { viewModel.onReminderChanged(null) }) {
                                        Text("Remove")
                                    }
                                }
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = { showReminderDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("Set Reminder")
                        }
                    }

                    val hasExpiry = uiState.notes.contains("[Expires:")
                    OutlinedButton(
                        onClick = { showExpirationDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.HourglassBottom, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(if (hasExpiry) "Expiration Set" else "Set Expiration")
                    }

                    Spacer(modifier = Modifier.height(Spacing.medium))

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.weight(1f),
                            enabled = !uiState.isSaving
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = viewModel::saveBookmark,
                            modifier = Modifier.weight(1f),
                            enabled = !uiState.isSaving && uiState.url.isNotBlank()
                        ) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text("Save Changes")
                            }
                        }
                    }
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(Spacing.medium)
            )
        }
    }
}

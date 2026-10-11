
package com.pixelmarks.feature.bookmark

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
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontFamily
import com.pixelmarks.ui.theme.ArchivalError
import com.pixelmarks.ui.theme.ArchivalSurfaceContainer
import com.pixelmarks.ui.theme.ArchivalSurfaceContainerHigh
import com.pixelmarks.ui.theme.ArchivalSurfaceContainerHighest
import com.pixelmarks.ui.theme.ArchivalSurfaceContainerLowest
import com.pixelmarks.ui.theme.ArchivalTextPrimary
import com.pixelmarks.ui.theme.ArchivalTextSecondary
import com.pixelmarks.ui.theme.BrassGold
import com.pixelmarks.ui.theme.TerracottaAccent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pixelmarks.app.PixelMarksApplication
import com.pixelmarks.core.image.BookmarkFavicon
import com.pixelmarks.core.image.BookmarkThumbnail
import com.pixelmarks.core.image.ImageUrlValidator
import com.pixelmarks.core.reminder.ReminderFormatter
import com.pixelmarks.feature.bookmark.dialogs.SetExpirationDialog
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
fun AddBookmarkScreen(
    initialUrl: String? = null,
    initialCollectionId: Long? = null,
    onNavigateBack: () -> Unit,
    onNavigateToBookmarkDetail: (Long) -> Unit = {},
    onNavigateToEditBookmark: (Long) -> Unit = {},
    viewModel: AddBookmarkViewModel = viewModel(
        factory = run {
            val app = LocalContext.current.applicationContext as PixelMarksApplication
            AddBookmarkViewModel.provideFactory(
                addBookmarkUseCase = app.container.addBookmarkUseCase,
                metadataRepository = app.container.metadataRepository,
                observeCollectionsUseCase = app.container.observeCollectionsUseCase,
                observeTagsUseCase = app.container.observeTagsUseCase,
                createTagUseCase = app.container.createTagUseCase,
                privateModeManager = app.container.privateModeManager,
                initialUrl = initialUrl,
                initialCollectionId = initialCollectionId
            )
        }
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCollectionPicker by remember { mutableStateOf(false) }
    var showSelectTagsDialog by remember { mutableStateOf(false) }
    var showCreateTagDialog by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var showExpirationDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val app = context.applicationContext as PixelMarksApplication
    val activity = context as? androidx.fragment.app.FragmentActivity
    val coroutineScope = rememberCoroutineScope()
    var showPasswordPrompt by remember { mutableStateOf(false) }

    fun requestEnablePrivateMode() {
        if (app.container.privateModeManager.isPrivateActive()) {
            viewModel.onPrivacyChanged(true)
            return
        }
        coroutineScope.launch {
            val method = app.container.privatePreferencesRepository.authMethod.first()
            if (method == com.pixelmarks.core.security.model.PrivateAuthMethod.CUSTOM_PASSWORD) {
                showPasswordPrompt = true
            } else if (activity != null) {
                app.container.biometricAuthManager.authenticate(
                    activity = activity,
                    title = "Secure Folder",
                    subtitle = "Authenticate to save bookmark in Secure Folder",
                    onSuccess = {
                        app.container.privateModeManager.enterPrivateMode()
                        viewModel.onPrivacyChanged(true)
                    },
                    onError = { _, _ -> }
                )
            }
        }
    }

    if (showPasswordPrompt) {
        com.pixelmarks.feature.security.SecureFolderPasswordDialog(
            onDismiss = { showPasswordPrompt = false },
            onSuccess = {
                showPasswordPrompt = false
                app.container.privateModeManager.enterPrivateMode()
                viewModel.onPrivacyChanged(true)
            },
            onFallbackAuth = if (activity != null) {
                {
                    showPasswordPrompt = false
                    app.container.biometricAuthManager.authenticate(
                        activity = activity,
                        title = "Secure Folder Recovery",
                        subtitle = "Authenticate to enter Private Mode",
                        onSuccess = {
                            app.container.privateModeManager.enterPrivateMode()
                            viewModel.onPrivacyChanged(true)
                        },
                        onError = { _, _ -> }
                    )
                }
            } else null
        )
    }

    // When save succeeds, return to previous screen
    LaunchedEffect(uiState.isSavedSuccessfully) {
        if (uiState.isSavedSuccessfully) {
            onNavigateBack()
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

    // Duplicate alert dialog with full options
    uiState.duplicateBookmark?.let { existing ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissDuplicateDialog() },
            title = {
                Text(
                    text = "This bookmark already exists",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
                    Text(
                        text = "A bookmark with this URL is already saved in your library:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = existing.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = existing.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.small)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.dismissDuplicateDialog()
                                onNavigateToBookmarkDetail(existing.id)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Open Existing", style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(
                            onClick = {
                                viewModel.dismissDuplicateDialog()
                                onNavigateToEditBookmark(existing.id)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Edit Existing", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Button(
                        onClick = { viewModel.saveAnyway() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Anyway")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissDuplicateDialog() }
                ) {
                    Text("Cancel")
                }
            }
        )
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

    PixelMarksScreenScaffold(
        title = if (uiState.isPrivate) "Save Private Bookmark 🔒" else "Save Bookmark",
        onNavigateBack = onNavigateBack
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(Spacing.medium)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            // URL Input (First so it never shifts down when typing)
            OutlinedTextField(
                value = uiState.url,
                onValueChange = viewModel::onUrlChanged,
                label = { Text("Web URL", color = ArchivalTextSecondary) },
                placeholder = { Text("https://example.com or example.com", color = ArchivalTextSecondary.copy(alpha = 0.5f)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        tint = BrassGold
                    )
                },
                trailingIcon = {
                    if (uiState.url.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onUrlChanged("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear URL",
                                tint = ArchivalTextSecondary
                            )
                        }
                    }
                },
                isError = uiState.errorMessage != null,
                supportingText = {
                    if (uiState.errorMessage != null) {
                        Text(
                            text = uiState.errorMessage ?: "",
                            color = ArchivalError
                        )
                    } else {
                        Text("Protocol defaults to https:// if omitted", color = ArchivalTextSecondary.copy(alpha = 0.7f))
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next
                ),
                colors = textFieldColors,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Live Metadata Preview Card (Placed below URL input to avoid layout shifts)
            if (uiState.url.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = ArchivalSurfaceContainer
                    )
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (uiState.isLoadingMetadata) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                color = BrassGold,
                                trackColor = ArchivalSurfaceContainerHighest
                            )
                        }

                        if (ImageUrlValidator.isValidImageUrl(uiState.previewThumbnailUrl)) {
                            BookmarkThumbnail(
                                thumbnailUrl = uiState.previewThumbnailUrl,
                                modifier = Modifier.fillMaxWidth(),
                                size = 140.dp
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.medium),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                BookmarkFavicon(faviconUrl = uiState.previewFaviconUrl)
                                Text(
                                    text = uiState.domain.ifBlank { "Preview" },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = BrassGold
                                )
                                if (uiState.isLoadingMetadata) {
                                    Spacer(modifier = Modifier.size(6.dp))
                                    Text(
                                        text = "Loading preview...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ArchivalTextSecondary
                                    )
                                }
                            }

                            val displayTitle = uiState.title.ifBlank { uiState.domain.ifBlank { uiState.url } }
                            Text(
                                text = displayTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = ArchivalTextPrimary
                            )

                            if (uiState.description.isNotBlank()) {
                                Text(
                                    text = uiState.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ArchivalTextSecondary,
                                    maxLines = 3
                                )
                            }
                        }
                    }
                }
            }

            // Title Input
            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::onTitleChanged,
                label = { Text("Title (Optional)", color = ArchivalTextSecondary) },
                placeholder = { Text("Auto-derived from webpage if left blank", color = ArchivalTextSecondary.copy(alpha = 0.5f)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Title,
                        contentDescription = null,
                        tint = ArchivalTextSecondary
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next
                ),
                colors = textFieldColors,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Description Input
            OutlinedTextField(
                value = uiState.description,
                onValueChange = viewModel::onDescriptionChanged,
                label = { Text("Description (Optional)", color = ArchivalTextSecondary) },
                placeholder = { Text("Webpage summary or brief excerpt", color = ArchivalTextSecondary.copy(alpha = 0.5f)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = ArchivalTextSecondary
                    )
                },
                maxLines = 3,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next
                ),
                colors = textFieldColors,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Optional Note Input
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = viewModel::onNotesChanged,
                label = { Text("Optional Note", color = ArchivalTextSecondary) },
                placeholder = { Text("Watch this later because...", color = ArchivalTextSecondary.copy(alpha = 0.5f)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Notes,
                        contentDescription = null,
                        tint = ArchivalTextSecondary
                    )
                },
                maxLines = 4,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { viewModel.saveBookmark() }
                ),
                colors = textFieldColors,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Privacy Toggle Card
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = ArchivalSurfaceContainer
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
                        tint = if (uiState.isPrivate) BrassGold else ArchivalTextSecondary,
                        modifier = Modifier.padding(end = Spacing.medium)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Private Bookmark",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = ArchivalTextPrimary
                        )
                        Text(
                            text = if (uiState.isPrivate) "Stored in Secure Folder" else "Accessible in normal mode",
                            style = MaterialTheme.typography.bodySmall,
                            color = ArchivalTextSecondary
                        )
                    }
                    Switch(
                        checked = uiState.isPrivate,
                        onCheckedChange = { checked ->
                            if (checked) {
                                requestEnablePrivateMode()
                            } else {
                                viewModel.onPrivacyChanged(false)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ArchivalSurfaceContainerLowest,
                            checkedTrackColor = BrassGold,
                            uncheckedThumbColor = ArchivalTextSecondary,
                            uncheckedTrackColor = ArchivalSurfaceContainerHighest
                        )
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
                colors = CardDefaults.outlinedCardColors(
                    containerColor = ArchivalSurfaceContainer
                )
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
                            Text(
                                text = "Collection",
                                style = MaterialTheme.typography.labelSmall,
                                color = ArchivalTextSecondary
                            )
                            Text(
                                text = selectedCol.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = ArchivalTextPrimary
                            )
                        }
                        IconButton(
                            onClick = { viewModel.onCollectionSelected(null) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear collection selection",
                                tint = ArchivalTextSecondary
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = BrassGold,
                            modifier = Modifier.size(28.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Collection (Optional)",
                                style = MaterialTheme.typography.labelSmall,
                                color = ArchivalTextSecondary
                            )
                            Text(
                                text = "None (Unsorted)",
                                style = MaterialTheme.typography.bodyLarge,
                                color = ArchivalTextSecondary
                            )
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
                        color = ArchivalTextPrimary
                    )
                    OutlinedButton(
                        onClick = { showSelectTagsDialog = true },
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = BrassGold
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(
                            text = if (selectedTags.isEmpty()) "Add Tags" else "Edit Tags",
                            style = MaterialTheme.typography.labelMedium,
                            color = BrassGold
                        )
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
                                label = { Text("#${tag.name}", color = ArchivalTextPrimary) },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Remove tag",
                                        modifier = Modifier.size(16.dp),
                                        tint = ArchivalTextSecondary
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // Reminder & Expiration Management
            if (uiState.reminderAt != null) {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = ArchivalSurfaceContainer
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
                                tint = BrassGold,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Reminder set",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BrassGold
                                )
                                Text(
                                    text = ReminderFormatter.format(uiState.reminderAt!!),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ArchivalTextPrimary
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = { showReminderDialog = true }) {
                                Text("Edit", color = BrassGold)
                            }
                            TextButton(onClick = { viewModel.onReminderChanged(null) }) {
                                Text("Remove", color = ArchivalError)
                            }
                        }
                    }
                }
            } else {
                OutlinedButton(
                    onClick = { showReminderDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(16.dp), tint = BrassGold)
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Set Reminder", color = BrassGold)
                }
            }

            val hasExpiry = uiState.notes.contains("[Expires:")
            OutlinedButton(
                onClick = { showExpirationDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.HourglassBottom, contentDescription = null, modifier = Modifier.size(16.dp), tint = BrassGold)
                Spacer(modifier = Modifier.size(6.dp))
                Text(if (hasExpiry) "Expiration Set" else "Set Expiration", color = BrassGold)
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
                    enabled = !uiState.isLoading,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel", color = ArchivalTextSecondary)
                }

                Button(
                    onClick = viewModel::saveBookmark,
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isLoading && uiState.url.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TerracottaAccent,
                        contentColor = ArchivalSurfaceContainerLowest
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = ArchivalSurfaceContainerLowest
                        )
                    } else {
                        Text("Save Bookmark", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

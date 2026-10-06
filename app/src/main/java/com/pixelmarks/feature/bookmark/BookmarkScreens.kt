package com.pixelmarks.feature.bookmark

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pixelmarks.app.PixelMarksApplication
import com.pixelmarks.core.image.BookmarkFavicon
import com.pixelmarks.core.image.BookmarkThumbnail
import com.pixelmarks.core.image.ImageUrlValidator
import com.pixelmarks.feature.bookmark.dialogs.SetExpirationDialog
import com.pixelmarks.feature.bookmark.dialogs.SetReminderDialog
import com.pixelmarks.feature.collection.SelectCollectionDialog
import com.pixelmarks.feature.collection.getIconVector
import com.pixelmarks.feature.collection.parseHexColor
import com.pixelmarks.feature.tags.CreateEditTagDialog
import com.pixelmarks.feature.tags.SelectTagsDialog
import com.pixelmarks.ui.components.PixelMarksScreenScaffold
import com.pixelmarks.ui.theme.Spacing
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BookmarkDetailScreen(
    bookmarkId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToEditBookmark: (Long) -> Unit = {},
    onNavigateToTagDetail: (Long) -> Unit = {},
    viewModel: BookmarkDetailViewModel = viewModel(
        factory = run {
            val app = LocalContext.current.applicationContext as PixelMarksApplication
            BookmarkDetailViewModel.provideFactory(
                bookmarkId = bookmarkId,
                getBookmarkUseCase = app.container.getBookmarkUseCase,
                observeBookmarkByIdUseCase = app.container.observeBookmarkByIdUseCase,
                observeBookmarkTagsUseCase = app.container.observeBookmarkTagsUseCase,
                observeTagsUseCase = app.container.observeTagsUseCase,
                replaceBookmarkTagsUseCase = app.container.replaceBookmarkTagsUseCase,
                createTagUseCase = app.container.createTagUseCase,
                toggleBookmarkFavoriteUseCase = app.container.toggleBookmarkFavoriteUseCase,
                toggleBookmarkPinUseCase = app.container.toggleBookmarkPinUseCase,
                archiveBookmarkUseCase = app.container.archiveBookmarkUseCase,
                unarchiveBookmarkUseCase = app.container.unarchiveBookmarkUseCase,
                trashBookmarkUseCase = app.container.trashBookmarkUseCase,
                restoreBookmarkUseCase = app.container.restoreBookmarkUseCase,
                permanentlyDeleteBookmarkUseCase = app.container.permanentlyDeleteBookmarkUseCase,
                updateBookmarkUseCase = app.container.updateBookmarkUseCase,
                moveBookmarkUseCase = app.container.moveBookmarkUseCase,
                observeCollectionsUseCase = app.container.observeCollectionsUseCase,
                setBookmarkReminderUseCase = app.container.setBookmarkReminderUseCase,
                cancelBookmarkReminderUseCase = app.container.cancelBookmarkReminderUseCase
            )
        }
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var showSelectTagsDialog by remember { mutableStateOf(false) }
    var showCreateTagDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showCollectionPicker by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var showExpirationDialog by remember { mutableStateOf(false) }
    var showEditNoteDialog by remember { mutableStateOf(false) }
    var editingNoteText by remember { mutableStateOf("") }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    if (showSelectTagsDialog) {
        SelectTagsDialog(
            currentTagIds = uiState.assignedTags.map { it.id }.toSet(),
            availableTags = uiState.allTags,
            onDismiss = { showSelectTagsDialog = false },
            onConfirm = { selectedIds ->
                viewModel.updateTags(selectedIds)
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
                    val currentIds = uiState.assignedTags.map { it.id }.toSet()
                    viewModel.updateTags(currentIds + newTag.id)
                }
            }
        )
    }

    if (showCollectionPicker) {
        SelectCollectionDialog(
            title = "Move to Collection",
            confirmButtonText = "Move",
            currentCollectionId = uiState.bookmark?.collectionId,
            availableCollections = uiState.availableCollections,
            onDismiss = { showCollectionPicker = false },
            onSelectCollection = { colId ->
                viewModel.moveToCollection(colId)
                showCollectionPicker = false
            }
        )
    }

    if (showReminderDialog) {
        SetReminderDialog(
            currentReminderAt = uiState.bookmark?.reminderAt,
            onDismiss = { showReminderDialog = false },
            onSetReminder = { timestamp ->
                viewModel.setReminder(timestamp)
            }
        )
    }

    if (showExpirationDialog) {
        SetExpirationDialog(
            currentNotes = uiState.bookmark?.notes ?: "",
            onDismiss = { showExpirationDialog = false },
            onSetExpirationNote = { updatedNotes ->
                viewModel.updateNotes(updatedNotes)
            }
        )
    }

    if (showEditNoteDialog) {
        AlertDialog(
            onDismissRequest = { showEditNoteDialog = false },
            title = { Text("Edit Note") },
            text = {
                OutlinedTextField(
                    value = editingNoteText,
                    onValueChange = { editingNoteText = it },
                    label = { Text("Note") },
                    placeholder = { Text("Add personal context, highlights, or thoughts...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 5
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateNotes(editingNoteText)
                        showEditNoteDialog = false
                    }
                ) {
                    Text("Save Note")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNoteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete permanently?") },
            text = {
                Text("This bookmark and its saved data will be permanently deleted. This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.deletePermanently { onNavigateBack() }
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    val current = uiState.bookmark

    fun copyLinkToClipboard() {
        if (current == null) return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Bookmark URL", current.url)
        clipboard.setPrimaryClip(clip)
        coroutineScope.launch {
            snackbarHostState.showSnackbar("Link copied")
        }
    }

    fun shareBookmark() {
        if (current == null) return
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "${current.title}\n\n${current.url}")
        }
        val chooser = Intent.createChooser(sendIntent, "Share bookmark")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(chooser)
        } catch (_: Exception) {}
    }

    fun openInBrowser() {
        if (current == null) return
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(current.url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)
        } catch (_: Exception) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Unable to open browser for this link")
            }
        }
    }

    PixelMarksScreenScaffold(
        title = "Bookmark Detail",
        onNavigateBack = onNavigateBack,
        actions = {
            if (current != null) {
                if (current.isTrashed) {
                    IconButton(onClick = { viewModel.restore { onNavigateBack() } }) {
                        Icon(
                            imageVector = Icons.Default.RestoreFromTrash,
                            contentDescription = "Restore bookmark",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { showDeleteConfirmDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = "Delete permanently",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    IconButton(onClick = ::copyLinkToClipboard) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Link",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = ::shareBookmark) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onNavigateToEditBookmark(bookmarkId) }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Bookmark",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { viewModel.moveToTrash { onNavigateBack() } }) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Move to Trash",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Spacing.medium)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.medium)
            ) {
                if (current != null) {
                    if (current.isTrashed) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.medium),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "This bookmark is in Trash",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                TextButton(onClick = { viewModel.restore { onNavigateBack() } }) {
                                    Text("Restore", color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }

                    if (ImageUrlValidator.isValidImageUrl(current.thumbnailUrl)) {
                        BookmarkThumbnail(
                            thumbnailUrl = current.thumbnailUrl,
                            modifier = Modifier.fillMaxWidth(),
                            size = 150.dp
                        )
                    }

                    // Title with Status Badges
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = current.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (current.isPinned) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Pinned",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        if (current.isFavorite) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Favorite",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Domain & Favicon
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        BookmarkFavicon(faviconUrl = current.faviconUrl)
                        Text(
                            text = current.domain,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Description
                    if (!current.description.isNullOrBlank()) {
                        Text(
                            text = current.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // URL & Open in Browser Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = current.url,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = ::openInBrowser) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Open in Browser",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Lifecycle Status Chips (Favorite, Pin, Archive)
                    if (!current.isTrashed) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AssistChip(
                                onClick = { viewModel.toggleFavorite() },
                                label = { Text(if (current.isFavorite) "Favorited" else "Favorite") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (current.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = null,
                                        tint = if (current.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )

                            AssistChip(
                                onClick = { viewModel.togglePin() },
                                label = { Text(if (current.isPinned) "Pinned" else "Pin") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (current.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                                        contentDescription = null,
                                        tint = if (current.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )

                            AssistChip(
                                onClick = { viewModel.toggleArchive() },
                                label = { Text(if (current.isArchived) "Archived" else "Archive") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (current.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }

                    // Collection Section
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { if (!current.isTrashed) showCollectionPicker = true },
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
                            val col = uiState.assignedCollection
                            if (col != null) {
                                val colColor = parseHexColor(col.color)
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(colColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getIconVector(col.icon),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Collection", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                    Text(col.name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                                }
                            } else {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Collection", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                    Text("Unsorted (No Collection)", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            if (!current.isTrashed) {
                                TextButton(onClick = { showCollectionPicker = true }) {
                                    Text("Change")
                                }
                            }
                        }
                    }

                    // Optional Notes Section
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.medium),
                            verticalArrangement = Arrangement.spacedBy(Spacing.small)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Text("Note", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                                }

                                if (!current.isTrashed) {
                                    IconButton(
                                        onClick = {
                                            editingNoteText = current.notes ?: ""
                                            showEditNoteDialog = true
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit note", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            if (!current.notes.isNullOrBlank()) {
                                Text(
                                    text = current.notes,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    text = "No notes added yet. Tap edit to add a note.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }

                    // Reminder & Expiration Management
                    if (!current.isTrashed) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.small)
                        ) {
                            val reminderFormatted = current.reminderAt?.let {
                                SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(it))
                            }

                            OutlinedButton(
                                onClick = { showReminderDialog = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.size(4.dp))
                                Text(
                                    text = if (reminderFormatted != null) reminderFormatted else "Reminder",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }

                            val hasExpiry = current.notes?.contains("[Expires:") == true
                            OutlinedButton(
                                onClick = { showExpirationDialog = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.HourglassBottom, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.size(4.dp))
                                Text(
                                    text = if (hasExpiry) "Expires Set" else "Expiration",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }

                    // Tags Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tags (${uiState.assignedTags.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (!current.isTrashed) {
                            IconButton(onClick = { showSelectTagsDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit tags",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    if (uiState.assignedTags.isEmpty()) {
                        Text(
                            text = "No tags assigned. Tap edit to assign tags.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            uiState.assignedTags.forEach { tag ->
                                AssistChip(
                                    onClick = { onNavigateToTagDetail(tag.id) },
                                    label = { Text(tag.name) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Label,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Bookmark #$bookmarkId",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Loading bookmark details...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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

package com.pixelmarks.feature.bookmark

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.pixelmarks.core.browser.BrowserLauncher
import com.pixelmarks.core.browser.BrowserPreference
import com.pixelmarks.feature.browser.OpenLinkBottomSheet
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
import androidx.compose.foundation.layout.width
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.ui.text.font.FontFamily
import com.pixelmarks.ui.theme.ArchivalBackground
import com.pixelmarks.ui.theme.ArchivalSurfaceContainerLowest
import com.pixelmarks.ui.theme.ArchivalSurfaceContainerLow
import com.pixelmarks.ui.theme.ArchivalSurfaceContainer
import com.pixelmarks.ui.theme.ArchivalSurfaceContainerHigh
import com.pixelmarks.ui.theme.ArchivalSurfaceContainerHighest
import com.pixelmarks.ui.theme.ArchivalTextPrimary
import com.pixelmarks.ui.theme.ArchivalTextSecondary
import com.pixelmarks.ui.theme.ArchivalError
import com.pixelmarks.ui.theme.BrassGold
import com.pixelmarks.ui.theme.TerracottaAccent
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
import androidx.compose.material3.Surface
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
    onNavigateToBrowser: (String) -> Unit = {},
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
                cancelBookmarkReminderUseCase = app.container.cancelBookmarkReminderUseCase,
                observeBookmarkReminderUseCase = app.container.observeBookmarkReminderUseCase,
                recordBookmarkVisitUseCase = app.container.recordBookmarkVisitUseCase,
                getBrowserPreferenceUseCase = app.container.getBrowserPreferenceUseCase,
                privateModeManager = app.container.privateModeManager
            )
        }
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val app = context.applicationContext as PixelMarksApplication
    val activity = context as? androidx.fragment.app.FragmentActivity
    val unlockedCollectionIds by app.container.appLockManager.unlockedCollectionIds.collectAsState()
    val isPrivateSessionUnlocked by app.container.appLockManager.isPrivateSessionUnlocked.collectAsState()

    val currentCollection = uiState.assignedCollection
    val isProtected = currentCollection?.isLocked == true || currentCollection?.isHidden == true
    val isCollectionUnlocked = currentCollection == null || isPrivateSessionUnlocked || unlockedCollectionIds.contains(currentCollection.id)

    fun triggerBookmarkUnlock() {
        if (activity != null && currentCollection != null) {
            app.container.biometricAuthManager.authenticate(
                activity = activity,
                title = "Unlock Bookmark",
                subtitle = "Authenticate to view protected bookmark",
                onSuccess = {
                    app.container.appLockManager.unlockCollection(currentCollection.id)
                },
                onError = { _: Int, errString: CharSequence ->
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(errString.toString())
                    }
                }
            )
        }
    }

    LaunchedEffect(isProtected, isCollectionUnlocked) {
        if (isProtected && !isCollectionUnlocked) {
            triggerBookmarkUnlock()
        }
    }

    var showSelectTagsDialog by remember { mutableStateOf(false) }
    var showCreateTagDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showCollectionPicker by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var showExpirationDialog by remember { mutableStateOf(false) }
    var showEditNoteDialog by remember { mutableStateOf(false) }
    var editingNoteText by remember { mutableStateOf("") }
    var showAskEveryTimeSheet by remember { mutableStateOf(false) }
    var askEveryTimeUrl by remember { mutableStateOf("") }
    val current = uiState.bookmark

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
        val isTrashed = current?.isTrashed == true
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = ArchivalSurfaceContainerHigh,
            titleContentColor = ArchivalTextPrimary,
            textContentColor = ArchivalTextSecondary,
            shape = RoundedCornerShape(18.dp),
            title = {
                Text(
                    text = if (isTrashed) "Delete Permanently?" else "Move to Trash?",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    color = ArchivalTextPrimary
                )
            },
            text = {
                Text(
                    text = if (isTrashed)
                        "This bookmark and its saved data will be permanently deleted. This action cannot be undone."
                    else
                        "Move \"${current?.title ?: "this bookmark"}\" to Trash? You can restore it later from Trash.",
                    color = ArchivalTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        if (isTrashed) {
                            viewModel.deletePermanently { onNavigateBack() }
                        } else {
                            viewModel.moveToTrash { onNavigateBack() }
                        }
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF8B2525),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmDialog = false },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = ArchivalTextSecondary
                    )
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAskEveryTimeSheet) {
        OpenLinkBottomSheet(
            url = askEveryTimeUrl,
            onDismiss = { showAskEveryTimeSheet = false },
            onSelectMode = { mode ->
                when (mode) {
                    BrowserPreference.IN_APP -> onNavigateToBrowser(askEveryTimeUrl)
                    BrowserPreference.SYSTEM_PREFERRED -> BrowserLauncher.launchSystemPreferred(context, askEveryTimeUrl)
                    BrowserPreference.DEFAULT_BROWSER -> BrowserLauncher.launchDefaultBrowser(context, askEveryTimeUrl)
                    else -> BrowserLauncher.launchSystemPreferred(context, askEveryTimeUrl)
                }
            }
        )
    }

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
        viewModel.recordVisit()
        coroutineScope.launch {
            val pref = viewModel.getBrowserPreference()
            BrowserLauncher.openUrl(
                context = context,
                url = current.url,
                preference = pref,
                onNavigateToInApp = onNavigateToBrowser,
                onShowAskEveryTimeSheet = { targetUrl ->
                    askEveryTimeUrl = targetUrl
                    showAskEveryTimeSheet = true
                }
            )
        }
    }

    val dynamicThemeEnabled by app.container.appPreferencesRepository.observeDynamicTheme().collectAsState(initial = false)
    val currentThemeMode by app.container.appPreferencesRepository.observeThemeMode().collectAsState(initial = com.pixelmarks.data.repository.ThemeMode.DARK)

    val dynamicPalette = com.pixelmarks.ui.theme.rememberBookmarkDynamicPalette(
        imageUrl = current?.thumbnailUrl ?: current?.faviconUrl,
        domain = current?.domain.orEmpty(),
        themeMode = currentThemeMode,
        enabled = dynamicThemeEnabled
    )

    androidx.compose.runtime.CompositionLocalProvider(
        com.pixelmarks.ui.theme.LocalArchivalColors provides dynamicPalette
    ) {
        PixelMarksScreenScaffold(
            title = "Bookmark Detail",
            onNavigateBack = onNavigateBack,
        actions = {
            if (current != null && (!isProtected || isCollectionUnlocked)) {
                if (current.isTrashed) {
                    IconButton(onClick = { viewModel.restore { onNavigateBack() } }) {
                        Icon(
                            imageVector = Icons.Default.RestoreFromTrash,
                            contentDescription = "Restore bookmark",
                            tint = BrassGold
                        )
                    }
                    IconButton(onClick = { showDeleteConfirmDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = "Delete permanently",
                            tint = ArchivalError
                        )
                    }
                } else {
                    IconButton(onClick = { viewModel.toggleFavorite() }) {
                        Icon(
                            imageVector = if (current.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favorite",
                            tint = if (current.isFavorite) BrassGold else ArchivalTextSecondary
                        )
                    }
                    IconButton(onClick = { viewModel.togglePin() }) {
                        Icon(
                            imageVector = if (current.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                            contentDescription = "Pin",
                            tint = if (current.isPinned) TerracottaAccent else ArchivalTextSecondary
                        )
                    }
                    IconButton(onClick = { viewModel.toggleArchive() }) {
                        Icon(
                            imageVector = if (current.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                            contentDescription = "Archive",
                            tint = ArchivalTextSecondary
                        )
                    }
                }
            }
        },
        bottomBar = {
            if (current != null && (!isProtected || isCollectionUnlocked)) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = ArchivalBackground,
                    shadowElevation = 8.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ArchivalSurfaceContainerHighest.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Button(
                            onClick = ::openInBrowser,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            elevation = androidx.compose.material3.ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Text(
                                text = "OPEN ORIGINAL URL",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
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
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                if (current != null) {
                    if (isProtected && !isCollectionUnlocked) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(ArchivalSurfaceContainerHigh),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = BrassGold,
                                    modifier = Modifier.size(40.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Bookmark is Protected",
                                style = MaterialTheme.typography.titleLarge,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                color = ArchivalTextPrimary
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "This bookmark belongs to a locked or private collection. Authenticate to view details.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ArchivalTextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { triggerBookmarkUnlock() },
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = BrassGold,
                                    contentColor = ArchivalSurfaceContainerLowest
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Unlock Bookmark", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        if (current.isTrashed) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = Color(0xFF4A1010),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "This bookmark is in Trash",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFFFFDAD6)
                                    )
                                    TextButton(onClick = { viewModel.restore { onNavigateBack() } }) {
                                        Text("Restore", color = BrassGold, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Top Timestamp Strip (Right Aligned)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = null,
                                tint = TerracottaAccent,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            val relativeTime = remember(current.createdAt) {
                                val diff = System.currentTimeMillis() - current.createdAt
                                val hours = diff / (1000 * 60 * 60)
                                if (hours < 1) "ADDED JUST NOW"
                                else if (hours < 24) "ADDED ${hours} HRS AGO"
                                else "ADDED ${hours / 24} DAYS AGO"
                            }
                            Text(
                                text = relativeTime.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = TerracottaAccent,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        // Main Title
                        Text(
                            text = current.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontFamily = FontFamily.Serif,
                            color = ArchivalTextPrimary,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 30.sp
                        )

                        // 16:9 Clean Preview Graphic / Thumbnail
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(ArchivalSurfaceContainerLowest),
                            contentAlignment = Alignment.Center
                        ) {
                            if (ImageUrlValidator.isValidImageUrl(current.thumbnailUrl)) {
                                BookmarkThumbnail(
                                    thumbnailUrl = current.thumbnailUrl,
                                    modifier = Modifier.fillMaxSize(),
                                    size = 190.dp
                                )
                            } else {
                                BookmarkFavicon(faviconUrl = current.faviconUrl, size = 48.dp)
                            }
                        }

                        // DOMAIN Card (Favicon + Domain + Copy/Share)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = ArchivalSurfaceContainerLow,
                            border = androidx.compose.foundation.BorderStroke(1.dp, ArchivalSurfaceContainerHighest),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(ArchivalSurfaceContainerLowest),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        BookmarkFavicon(
                                            faviconUrl = current.faviconUrl,
                                            size = 20.dp
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "DOMAIN",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ArchivalTextSecondary,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                        Text(
                                            text = current.domain.ifBlank { current.url },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = ArchivalTextPrimary,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = ::copyLinkToClipboard,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                            containerColor = ArchivalSurfaceContainerHigh,
                                            contentColor = ArchivalTextPrimary
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = null,
                                            tint = TerracottaAccent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("COPY", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                    }

                                    Button(
                                        onClick = ::shareBookmark,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                            containerColor = ArchivalSurfaceContainerHigh,
                                            contentColor = ArchivalTextPrimary
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = null,
                                            tint = TerracottaAccent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("SHARE", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                    }
                                }
                            }
                        }

                        // Schedule & Retention Settings Card
                        val activeReminderTime = uiState.reminder?.reminderAt ?: current.reminderAt
                        val hasExpiry = current.notes?.contains("[Expires:") == true

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = ArchivalSurfaceContainerLow,
                            border = androidx.compose.foundation.BorderStroke(1.dp, ArchivalSurfaceContainerHighest),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Alarm,
                                        contentDescription = null,
                                        tint = BrassGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "SCHEDULE & RETENTION",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = BrassGold,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }

                                // Reminder Item
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ArchivalSurfaceContainerLowest)
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Alarm,
                                            contentDescription = null,
                                            tint = ArchivalTextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "Set Reminder",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = ArchivalTextPrimary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = if (activeReminderTime != null) ReminderFormatter.format(activeReminderTime) else "Not scheduled",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ArchivalTextSecondary
                                            )
                                        }
                                    }
                                    Button(
                                        onClick = { showReminderDialog = true },
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.height(30.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                            containerColor = ArchivalSurfaceContainerHigh,
                                            contentColor = BrassGold
                                        )
                                    ) {
                                        Text(
                                            text = if (activeReminderTime != null) "Edit" else "+ Add Reminder",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                // Expiration Item
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ArchivalSurfaceContainerLowest)
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.HourglassBottom,
                                            contentDescription = null,
                                            tint = ArchivalTextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "Set Expiration",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = ArchivalTextPrimary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = if (hasExpiry) "Expiration Set" else "Never expires",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ArchivalTextSecondary
                                            )
                                        }
                                    }
                                    Button(
                                        onClick = { showExpirationDialog = true },
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.height(30.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                            containerColor = ArchivalSurfaceContainerHigh,
                                            contentColor = TerracottaAccent
                                        )
                                    ) {
                                        Text(
                                            text = if (hasExpiry) "Edit" else "Set Date →",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        // Website Description Card
                        if (!current.description.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = ArchivalSurfaceContainerLow,
                                border = androidx.compose.foundation.BorderStroke(1.dp, ArchivalSurfaceContainerHighest),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = BrassGold,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "WEBSITE DESCRIPTION",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = BrassGold,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                    Text(
                                        text = current.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = ArchivalTextSecondary,
                                        lineHeight = 22.sp
                                    )
                                }
                            }
                        }

                        // Personal Margin Notes
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = ArchivalSurfaceContainerLow,
                            border = androidx.compose.foundation.BorderStroke(1.dp, ArchivalSurfaceContainerHighest),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = null,
                                            tint = TerracottaAccent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "PERSONAL MARGIN NOTES",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = TerracottaAccent,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .clickable {
                                                editingNoteText = current.notes ?: ""
                                                showEditNoteDialog = true
                                            }
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = null,
                                            tint = BrassGold,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "EDIT",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = BrassGold,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ArchivalSurfaceContainerLowest)
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(3.dp)
                                                .height(44.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(TerracottaAccent)
                                        )
                                        Text(
                                            text = if (!current.notes.isNullOrBlank()) "“  ${current.notes}  ”"
                                            else "No personal margin notes recorded for this document.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = ArchivalTextPrimary,
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                            lineHeight = 20.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Taxonomy & Collection Card
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = ArchivalSurfaceContainerLow,
                            border = androidx.compose.foundation.BorderStroke(1.dp, ArchivalSurfaceContainerHighest),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = BrassGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "TAXONOMY & COLLECTION",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = BrassGold,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }

                                // Tags Cluster
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    uiState.assignedTags.forEach { tag ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(ArchivalSurfaceContainerLowest)
                                                .clickable { onNavigateToTagDetail(tag.id) }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = "#${tag.name}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ArchivalTextPrimary,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(ArchivalSurfaceContainerHigh)
                                            .clickable { showSelectTagsDialog = true }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "+ Add Tag",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = BrassGold,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                // Collection Navigator Card
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ArchivalSurfaceContainerLowest)
                                        .clickable { showCollectionPicker = true }
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val col = uiState.assignedCollection
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    if (col != null) parseHexColor(col.color)
                                                    else Color(0xFF9B783E)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (col != null) getIconVector(col.icon) else Icons.Default.Folder,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = "COLLECTION",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ArchivalTextSecondary,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.sp
                                            )
                                            Text(
                                                text = col?.name ?: "Unsorted (No Collection)",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = ArchivalTextPrimary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = ArchivalTextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Row with Edit Bookmark & Delete buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // EDIT BOOKMARK
                            Button(
                                onClick = { onNavigateToEditBookmark(bookmarkId) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = ArchivalSurfaceContainerHigh,
                                    contentColor = ArchivalTextPrimary
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ArchivalSurfaceContainerHighest)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = ArchivalTextPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "EDIT BOOKMARK",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }

                            // DELETE
                            Button(
                                onClick = { showDeleteConfirmDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF6B1414),
                                    contentColor = Color(0xFFFFDAD6)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = null,
                                    tint = Color(0xFFFFDAD6),
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "DELETE",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Loading bookmark details...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ArchivalTextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
}

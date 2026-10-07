package com.pixelmarks.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pixelmarks.app.PixelMarksApplication
import com.pixelmarks.core.database.model.MetadataStatus
import com.pixelmarks.core.image.BookmarkFavicon
import com.pixelmarks.core.image.BookmarkThumbnail
import com.pixelmarks.core.image.ImageUrlValidator
import com.pixelmarks.core.model.Bookmark
import com.pixelmarks.ui.components.BookmarkActionMenu
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.pixelmarks.core.browser.BrowserLauncher
import com.pixelmarks.core.browser.BrowserPreference
import com.pixelmarks.feature.browser.OpenLinkBottomSheet
import kotlinx.coroutines.launch
import com.pixelmarks.ui.components.PixelMarksScreenScaffold
import com.pixelmarks.ui.theme.PixelMarksTheme
import com.pixelmarks.ui.theme.Spacing

@Composable
fun HomeScreen(
    onNavigateToSearch: () -> Unit,
    onNavigateToAddBookmark: () -> Unit,
    onNavigateToBookmarkDetail: (Long) -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToBrowser: (String) -> Unit = {},
    onNavigateToConnectPc: () -> Unit,
    onNavigateToTrash: () -> Unit = {},
    viewModel: HomeViewModel = viewModel(
        factory = run {
            val app = LocalContext.current.applicationContext as PixelMarksApplication
            HomeViewModel.provideFactory(
                observeBookmarksUseCase = app.container.observeBookmarksUseCase,
                deleteBookmarkUseCase = app.container.deleteBookmarkUseCase,
                toggleBookmarkFavoriteUseCase = app.container.toggleBookmarkFavoriteUseCase,
                toggleBookmarkPinUseCase = app.container.toggleBookmarkPinUseCase,
                archiveBookmarkUseCase = app.container.archiveBookmarkUseCase,
                trashBookmarkUseCase = app.container.trashBookmarkUseCase,
                restoreBookmarkUseCase = app.container.restoreBookmarkUseCase,
                privateModeManager = app.container.privateModeManager
            )
        }
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showAskEveryTimeSheet by remember { mutableStateOf(false) }
    var askEveryTimeUrl by remember { mutableStateOf("") }

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

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.ShowTrashUndo -> {
                    val result = snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = "UNDO",
                        duration = androidx.compose.material3.SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.restoreBookmark(event.bookmarkId)
                    }
                }
            }
        }
    }

    PixelMarksScreenScaffold(
        title = if (uiState.isPrivateMode) "🔒 Secure Folder" else "PixelMarks",
        snackbarHost = { SnackbarHost(snackbarHostState) },
        actions = {
            IconButton(onClick = onNavigateToSearch) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search bookmarks"
                )
            }
            IconButton(onClick = onNavigateToTrash) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Trash"
                )
            }
            IconButton(onClick = onNavigateToAddBookmark) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add new bookmark"
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            item {
                Spacer(modifier = Modifier.height(Spacing.small))
                // Foundation Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.medium),
                        verticalArrangement = Arrangement.spacedBy(Spacing.small)
                    ) {
                        Text(
                            text = "Local-First Dashboard",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Bookmarks stored in local Room SQLite database.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Bookmarks Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Saved Bookmarks (${uiState.bookmarks.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            // Loading state
            if (uiState.isLoading) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.large),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (uiState.bookmarks.isEmpty()) {
                // Empty State
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.large),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Spacing.small)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = Spacing.small)
                            )
                            Text(
                                text = "No Bookmarks Yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Add your first link using the button below.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(Spacing.small))
                            Button(onClick = onNavigateToAddBookmark) {
                                Text("Add Your First Bookmark")
                            }
                        }
                    }
                }
            } else {
                // Bookmarks List
                items(
                    items = uiState.bookmarks,
                    key = { it.id }
                ) { bookmark ->
                    BookmarkListItem(
                        bookmark = bookmark,
                        onClick = { onNavigateToBookmarkDetail(bookmark.id) },
                        onEdit = { onNavigateToBookmarkDetail(bookmark.id) },
                        onOpen = {
                            coroutineScope.launch {
                                val app = context.applicationContext as PixelMarksApplication
                                app.container.recordBookmarkVisitUseCase(bookmark.id)
                                val pref = app.container.getBrowserPreferenceUseCase()
                                BrowserLauncher.openUrl(
                                    context = context,
                                    url = bookmark.url,
                                    preference = pref,
                                    onNavigateToInApp = onNavigateToBrowser,
                                    onShowAskEveryTimeSheet = { targetUrl ->
                                        askEveryTimeUrl = targetUrl
                                        showAskEveryTimeSheet = true
                                    }
                                )
                            }
                        },
                        onDelete = { viewModel.moveToTrash(bookmark) },
                        onToggleFavorite = { viewModel.toggleFavorite(bookmark) },
                        onTogglePin = { viewModel.togglePin(bookmark) },
                        onArchive = { viewModel.archiveBookmark(bookmark) }
                    )
                }
            }

            // Feature Placeholders Section (Preserved from Task 1)
            item {
                Spacer(modifier = Modifier.height(Spacing.medium))
                Text(
                    text = "Feature Placeholders",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small)
                ) {
                    Button(
                        onClick = onNavigateToAddBookmark,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Add Bookmark")
                    }
                    OutlinedButton(
                        onClick = { onNavigateToBookmarkDetail(1L) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Detail Sample")
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small)
                ) {
                    OutlinedButton(
                        onClick = onNavigateToReminders,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reminders")
                    }
                    OutlinedButton(
                        onClick = { onNavigateToBrowser("") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Browser")
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = onNavigateToConnectPc,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Connect to PC")
                }
                Spacer(modifier = Modifier.height(Spacing.large))
            }
        }
    }
}

@Composable
fun BookmarkListItem(
    bookmark: Bookmark,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onOpen: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onToggleFavorite: (() -> Unit)? = null,
    onTogglePin: (() -> Unit)? = null,
    onArchive: (() -> Unit)? = null,
    onUnarchive: (() -> Unit)? = null,
    onMove: (() -> Unit)? = null,
    onRemoveFromCollection: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium),
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (ImageUrlValidator.isValidImageUrl(bookmark.thumbnailUrl)) {
                BookmarkThumbnail(
                    thumbnailUrl = bookmark.thumbnailUrl,
                    size = 72.dp
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = bookmark.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (bookmark.isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    if (bookmark.isFavorite) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Favorite",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BookmarkFavicon(faviconUrl = bookmark.faviconUrl)
                    Text(
                        text = bookmark.domain,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (bookmark.metadataStatus == MetadataStatus.FETCHING) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(start = 2.dp)
                                .size(12.dp),
                            strokeWidth = 1.5.dp
                        )
                    }
                }
                if (!bookmark.description.isNullOrBlank()) {
                    Text(
                        text = bookmark.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = bookmark.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            BookmarkActionMenu(
                bookmark = bookmark,
                onOpen = onOpen,
                onEdit = onEdit,
                onToggleFavorite = onToggleFavorite,
                onTogglePin = onTogglePin,
                onArchive = onArchive,
                onUnarchive = onUnarchive,
                onMoveToTrash = onDelete,
                onMoveToCollection = onMove,
                onRemoveFromCollection = onRemoveFromCollection
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    PixelMarksTheme {
        HomeScreen(
            onNavigateToSearch = {},
            onNavigateToAddBookmark = {},
            onNavigateToBookmarkDetail = {},
            onNavigateToReminders = {},
            onNavigateToBrowser = {},
            onNavigateToConnectPc = {},
            onNavigateToTrash = {}
        )
    }
}

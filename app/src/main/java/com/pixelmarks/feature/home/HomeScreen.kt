package com.pixelmarks.feature.home

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pixelmarks.app.PixelMarksApplication
import com.pixelmarks.core.browser.BrowserLauncher
import com.pixelmarks.core.browser.BrowserPreference
import com.pixelmarks.core.image.BookmarkFavicon
import com.pixelmarks.core.image.BookmarkThumbnail
import com.pixelmarks.core.image.ImageUrlValidator
import com.pixelmarks.core.model.Bookmark
import com.pixelmarks.feature.bookmark.QuickAddBookmarkDialog
import com.pixelmarks.feature.browser.OpenLinkBottomSheet
import com.pixelmarks.ui.components.ArchivalSubHeader
import com.pixelmarks.ui.components.PixelMarksTopAppBar
import com.pixelmarks.ui.theme.ArchivalBackground
import com.pixelmarks.ui.theme.ArchivalPrimary
import com.pixelmarks.ui.theme.ArchivalSecondary
import com.pixelmarks.ui.theme.ArchivalSurface
import com.pixelmarks.ui.theme.ArchivalSurfaceContainer
import com.pixelmarks.ui.theme.ArchivalSurfaceContainerHigh
import com.pixelmarks.ui.theme.ArchivalSurfaceContainerHighest
import com.pixelmarks.ui.theme.ArchivalSurfaceContainerLow
import com.pixelmarks.ui.theme.ArchivalSurfaceContainerLowest
import com.pixelmarks.ui.theme.ArchivalTextPrimary
import com.pixelmarks.ui.theme.ArchivalTextSecondary
import com.pixelmarks.ui.theme.BrassGold
import com.pixelmarks.ui.theme.BrassGoldLight
import com.pixelmarks.ui.theme.Spacing
import com.pixelmarks.ui.theme.TerracottaAccent
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private enum class HomeFilter {
    ALL, FAVORITES, PINNED, NOTES
}

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
    var isGridView by remember { mutableStateOf(false) }
    var activeFilter by remember { mutableStateOf(HomeFilter.ALL) }
    var showQuickAddDialog by remember { mutableStateOf(false) }

    if (showQuickAddDialog) {
        QuickAddBookmarkDialog(
            isPrivateInitial = uiState.isPrivateMode,
            onDismiss = { showQuickAddDialog = false },
            onOpenFullAddScreen = {
                showQuickAddDialog = false
                onNavigateToAddBookmark()
            },
            onBookmarkSaved = {
                showQuickAddDialog = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Bookmark saved")
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

    fun openBookmark(bookmark: Bookmark) {
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
    }

    fun shareBookmark(bookmark: Bookmark) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "${bookmark.title}\n${bookmark.url}")
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Bookmark")
        context.startActivity(shareIntent)
    }

    val filteredBookmarks = remember(uiState.bookmarks, activeFilter) {
        when (activeFilter) {
            HomeFilter.ALL -> uiState.bookmarks
            HomeFilter.FAVORITES -> uiState.bookmarks.filter { it.isFavorite }
            HomeFilter.PINNED -> uiState.bookmarks.filter { it.isPinned }
            HomeFilter.NOTES -> uiState.bookmarks.filter { !it.notes.isNullOrBlank() }
        }
    }

    val pinnedBookmarks = remember(uiState.bookmarks) {
        uiState.bookmarks.filter { it.isPinned }
    }

    val recentBookmarks = remember(uiState.bookmarks) {
        uiState.bookmarks.filter { !it.isPinned }
    }

    Scaffold(
        containerColor = ArchivalBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            PixelMarksTopAppBar(
                isPrivateMode = uiState.isPrivateMode,
                onNavigateToConnectPc = onNavigateToConnectPc,
                onNavigateToSearch = onNavigateToSearch,
                onNavigateToReminders = onNavigateToReminders,
                hasUnreadReminders = true
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showQuickAddDialog = true },
                containerColor = TerracottaAccent,
                contentColor = ArchivalSurfaceContainerLowest,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Save Bookmark",
                    modifier = Modifier.size(26.dp)
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
            // Editorial Salutation & Date Header
            item {
                Spacer(modifier = Modifier.height(4.dp))
                ArchivalSubHeader(
                    isGridView = isGridView,
                    onToggleGridView = { isGridView = it }
                )
            }

            // Curatorial Filter Pills (Horizontal Scrolling)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterPill(
                        label = "All Entries",
                        count = uiState.bookmarks.size,
                        isSelected = activeFilter == HomeFilter.ALL,
                        onClick = { activeFilter = HomeFilter.ALL }
                    )
                    FilterPill(
                        label = "Favorites",
                        count = uiState.bookmarks.count { it.isFavorite },
                        isSelected = activeFilter == HomeFilter.FAVORITES,
                        onClick = { activeFilter = HomeFilter.FAVORITES }
                    )
                    FilterPill(
                        label = "Pinned",
                        count = pinnedBookmarks.size,
                        isSelected = activeFilter == HomeFilter.PINNED,
                        onClick = { activeFilter = HomeFilter.PINNED }
                    )
                    FilterPill(
                        label = "With Notes",
                        count = uiState.bookmarks.count { !it.notes.isNullOrBlank() },
                        isSelected = activeFilter == HomeFilter.NOTES,
                        onClick = { activeFilter = HomeFilter.NOTES }
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
                        CircularProgressIndicator(color = BrassGold)
                    }
                }
            } else if (filteredBookmarks.isEmpty()) {
                // Empty State
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = ArchivalSurfaceContainerLow,
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ArchivalSurfaceContainerHighest)
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
                                tint = BrassGold,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "No Bookmarks in Catalogue",
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = FontFamily.Serif,
                                color = ArchivalTextPrimary
                            )
                            Text(
                                text = "Add your first link using the button below.",
                                style = MaterialTheme.typography.bodySmall,
                                color = ArchivalTextSecondary
                            )
                            Spacer(modifier = Modifier.height(Spacing.extraSmall))
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = ArchivalPrimary,
                                modifier = Modifier.clickable(onClick = onNavigateToAddBookmark)
                            ) {
                                Text(
                                    text = "Add Bookmark",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ArchivalSurfaceContainerLowest,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }
            } else if (activeFilter == HomeFilter.ALL) {
                // ALL ENTRIES: Pinned Bookmarks Shelf (if any) + Recent Bookmarks Stream
                if (pinnedBookmarks.isNotEmpty()) {
                    item {
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
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = null,
                                    tint = TerracottaAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "PINNED BOOKMARKS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ArchivalTextPrimary,
                                    letterSpacing = 1.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = ArchivalSurfaceContainerHighest
                            ) {
                                Text(
                                    text = "${pinnedBookmarks.size} pinned",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TerracottaAccent,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Pinned Grid (2 columns)
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            pinnedBookmarks.chunked(2).forEach { rowPair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowPair.forEach { bookmark ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            PinnedBookmarkCard(
                                                bookmark = bookmark,
                                                onClick = { onNavigateToBookmarkDetail(bookmark.id) },
                                                onOpen = { openBookmark(bookmark) },
                                                onToggleFavorite = { viewModel.toggleFavorite(bookmark) },
                                                onShare = { shareBookmark(bookmark) }
                                            )
                                        }
                                    }
                                    if (rowPair.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }

                // Recent Bookmarks Stream
                if (recentBookmarks.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
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
                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    tint = BrassGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "RECENT BOOKMARKS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ArchivalTextPrimary,
                                    letterSpacing = 1.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = "${recentBookmarks.size} entries",
                                style = MaterialTheme.typography.labelSmall,
                                color = ArchivalTextSecondary
                            )
                        }
                    }

                    // List or Grid items
                    if (isGridView) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                recentBookmarks.chunked(2).forEach { rowPair ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowPair.forEach { bookmark ->
                                            Box(modifier = Modifier.weight(1f)) {
                                                GridBookmarkCard(
                                                    bookmark = bookmark,
                                                    onClick = { onNavigateToBookmarkDetail(bookmark.id) },
                                                    onOpen = { openBookmark(bookmark) },
                                                    onToggleFavorite = { viewModel.toggleFavorite(bookmark) },
                                                    onShare = { shareBookmark(bookmark) }
                                                )
                                            }
                                        }
                                        if (rowPair.size == 1) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        items(recentBookmarks, key = { it.id }) { bookmark ->
                            ListBookmarkCard(
                                bookmark = bookmark,
                                onClick = { onNavigateToBookmarkDetail(bookmark.id) },
                                onOpen = { openBookmark(bookmark) },
                                onToggleFavorite = { viewModel.toggleFavorite(bookmark) },
                                onShare = { shareBookmark(bookmark) },
                                onDelete = { viewModel.moveToTrash(bookmark) }
                            )
                        }
                    }
                }
            } else {
                // FILTERED STREAM: Favorites, Pinned, or With Notes
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val (icon, titleText) = when (activeFilter) {
                                HomeFilter.FAVORITES -> Icons.Default.Star to "FAVORITE BOOKMARKS"
                                HomeFilter.PINNED -> Icons.Default.PushPin to "PINNED BOOKMARKS"
                                HomeFilter.NOTES -> Icons.Default.EditNote to "BOOKMARKS WITH NOTES"
                                else -> Icons.AutoMirrored.Filled.MenuBook to "BOOKMARKS"
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (activeFilter == HomeFilter.PINNED) TerracottaAccent else BrassGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = titleText,
                                style = MaterialTheme.typography.labelSmall,
                                color = ArchivalTextPrimary,
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = "${filteredBookmarks.size} entries",
                            style = MaterialTheme.typography.labelSmall,
                            color = ArchivalTextSecondary
                        )
                    }
                }

                // List or Grid items
                if (isGridView) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            filteredBookmarks.chunked(2).forEach { rowPair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowPair.forEach { bookmark ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            GridBookmarkCard(
                                                bookmark = bookmark,
                                                onClick = { onNavigateToBookmarkDetail(bookmark.id) },
                                                onOpen = { openBookmark(bookmark) },
                                                onToggleFavorite = { viewModel.toggleFavorite(bookmark) },
                                                onShare = { shareBookmark(bookmark) }
                                            )
                                        }
                                    }
                                    if (rowPair.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(filteredBookmarks, key = { it.id }) { bookmark ->
                            ListBookmarkCard(
                                bookmark = bookmark,
                                onClick = { onNavigateToBookmarkDetail(bookmark.id) },
                                onOpen = { openBookmark(bookmark) },
                                onToggleFavorite = { viewModel.toggleFavorite(bookmark) },
                                onShare = { shareBookmark(bookmark) },
                                onDelete = { viewModel.moveToTrash(bookmark) }
                            )
                        }
                    }
                }

            item {
                Spacer(modifier = Modifier.height(Spacing.large))
            }
        }
    }
}

@Composable
private fun FilterPill(
    label: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) TerracottaAccent else ArchivalSurfaceContainer,
        contentColor = if (isSelected) ArchivalSurfaceContainerLowest else ArchivalTextSecondary,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
            )
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (isSelected) ArchivalSurfaceContainerLowest.copy(alpha = 0.2f) else ArchivalSurfaceContainerLowest
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) ArchivalSurfaceContainerLowest else ArchivalTextSecondary,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                )
            }
        }
    }
}

@Composable
private fun PinnedBookmarkCard(
    bookmark: Bookmark,
    onClick: () -> Unit,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = ArchivalSurfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(1.dp, ArchivalSurfaceContainerHighest),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.small),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Thumbnail with Pin overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ArchivalSurfaceContainerLowest)
            ) {
                if (ImageUrlValidator.isValidImageUrl(bookmark.thumbnailUrl)) {
                    BookmarkThumbnail(
                        thumbnailUrl = bookmark.thumbnailUrl,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        BookmarkFavicon(faviconUrl = bookmark.faviconUrl, size = 32.dp)
                    }
                }
                Box(
                    modifier = Modifier
                        .padding(6.dp)
                        .size(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(TerracottaAccent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = null,
                        tint = ArchivalSurfaceContainerLowest,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            // Domain & relative time
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                BookmarkFavicon(faviconUrl = bookmark.faviconUrl, size = 12.dp)
                Text(
                    text = "${bookmark.domain} • ${formatRelativeTime(bookmark.createdAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ArchivalTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Title
            Text(
                text = bookmark.title,
                style = MaterialTheme.typography.titleSmall,
                fontFamily = FontFamily.Serif,
                color = ArchivalTextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            if (!bookmark.description.isNullOrBlank()) {
                Text(
                    text = bookmark.description!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = ArchivalTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }

            if (bookmark.tags.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    bookmark.tags.take(3).forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = ArchivalSurfaceContainerHighest
                        ) {
                            Text(
                                text = "#${tag.name}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ArchivalTextSecondary,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            // Margin note snippet
            if (!bookmark.notes.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = ArchivalSurfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EditNote,
                            contentDescription = null,
                            tint = BrassGold,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = bookmark.notes!!,
                            style = MaterialTheme.typography.labelSmall,
                            color = ArchivalTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Bottom Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (bookmark.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (bookmark.isFavorite) BrassGold else ArchivalTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onShare,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = ArchivalTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BookmarkGridPlate(
    bookmark: Bookmark,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GridBookmarkCard(
        bookmark = bookmark,
        onClick = onClick
    )
}

@Composable
fun ArchivalBookmarkCard(
    bookmark: Bookmark,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ListBookmarkCard(
        bookmark = bookmark,
        onClick = onClick
    )
}

@Composable
fun GridBookmarkCard(
    bookmark: Bookmark,
    onClick: () -> Unit,
    onOpen: () -> Unit = {},
    onToggleFavorite: () -> Unit = {},
    onShare: () -> Unit = {}
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = ArchivalSurfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(1.dp, ArchivalSurfaceContainerHighest),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.small),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ArchivalSurfaceContainerLowest)
            ) {
                if (ImageUrlValidator.isValidImageUrl(bookmark.thumbnailUrl)) {
                    BookmarkThumbnail(
                        thumbnailUrl = bookmark.thumbnailUrl,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        BookmarkFavicon(faviconUrl = bookmark.faviconUrl, size = 32.dp)
                    }
                }
                if (bookmark.isPinned) {
                    Box(
                        modifier = Modifier
                            .padding(6.dp)
                            .size(20.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(TerracottaAccent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = null,
                            tint = ArchivalSurfaceContainerLowest,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                BookmarkFavicon(faviconUrl = bookmark.faviconUrl, size = 12.dp)
                Text(
                    text = "${bookmark.domain} • ${formatRelativeTime(bookmark.createdAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ArchivalTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = bookmark.title,
                style = MaterialTheme.typography.titleSmall,
                fontFamily = FontFamily.Serif,
                color = ArchivalTextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            if (!bookmark.description.isNullOrBlank()) {
                Text(
                    text = bookmark.description!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = ArchivalTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }

            if (bookmark.tags.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    bookmark.tags.take(3).forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = ArchivalSurfaceContainerLowest
                        ) {
                            Text(
                                text = "#${tag.name}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ArchivalTextSecondary,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (bookmark.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (bookmark.isFavorite) BrassGold else ArchivalTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onShare,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = ArchivalTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ListBookmarkCard(
    bookmark: Bookmark,
    onClick: () -> Unit,
    onOpen: () -> Unit = {},
    onToggleFavorite: () -> Unit = {},
    onShare: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = ArchivalSurfaceContainerLowest,
        border = androidx.compose.foundation.BorderStroke(1.dp, ArchivalSurfaceContainerHigh.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left square thumbnail
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ArchivalSurfaceContainerLow),
                contentAlignment = Alignment.Center
            ) {
                if (ImageUrlValidator.isValidImageUrl(bookmark.thumbnailUrl)) {
                    BookmarkThumbnail(
                        thumbnailUrl = bookmark.thumbnailUrl,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    BookmarkFavicon(faviconUrl = bookmark.faviconUrl, size = 28.dp)
                }
            }

            // Center details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    BookmarkFavicon(faviconUrl = bookmark.faviconUrl, size = 12.dp)
                    Text(
                        text = "${bookmark.domain} • ${formatRelativeTime(bookmark.createdAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ArchivalTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = bookmark.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Serif,
                    color = ArchivalTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )

                if (!bookmark.description.isNullOrBlank()) {
                    Text(
                        text = bookmark.description!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = ArchivalTextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 16.sp
                    )
                }

                if (bookmark.tags.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        bookmark.tags.forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = ArchivalSurfaceContainerLow
                            ) {
                                Text(
                                    text = "#${tag.name}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ArchivalTextSecondary,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                if (!bookmark.notes.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = ArchivalSurfaceContainerLow
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = null,
                                tint = TerracottaAccent,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "✎ Note: ${bookmark.notes}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ArchivalTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            // Right side actions
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (bookmark.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (bookmark.isFavorite) BrassGold else ArchivalTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onShare,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = ArchivalTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}


private fun formatRelativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    return when {
        days > 0 -> "${days}d"
        hours > 0 -> "${hours}h"
        minutes > 0 -> "${minutes}m"
        else -> "just now"
    }
}

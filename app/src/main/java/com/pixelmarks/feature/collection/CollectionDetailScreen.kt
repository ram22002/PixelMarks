package com.pixelmarks.feature.collection

import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.GridView
import com.pixelmarks.feature.home.BookmarkGridPlate
import com.pixelmarks.feature.home.ListBookmarkCard
import com.pixelmarks.feature.home.rememberBookmarkCardConfig
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import com.pixelmarks.core.image.BookmarkFavicon
import com.pixelmarks.core.image.BookmarkThumbnail
import com.pixelmarks.core.image.ImageUrlValidator
import com.pixelmarks.core.model.Bookmark
import com.pixelmarks.core.model.Collection
import com.pixelmarks.ui.theme.ArchivalBackground
import com.pixelmarks.ui.theme.ArchivalPrimary
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

@Composable
fun CollectionDetailScreen(
    collectionId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToCollectionDetail: (Long) -> Unit,
    onNavigateToBookmarkDetail: (Long) -> Unit,
    onNavigateToAddBookmark: (Long) -> Unit,
    onNavigateToBrowser: (String) -> Unit = {},
    viewModel: CollectionDetailViewModel = viewModel(
        factory = run {
            val app = LocalContext.current.applicationContext as PixelMarksApplication
            CollectionDetailViewModel.provideFactory(
                collectionId = collectionId,
                getCollectionUseCase = app.container.getCollectionUseCase,
                observeCollectionsUseCase = app.container.observeCollectionsUseCase,
                bookmarkRepository = app.container.bookmarkRepository,
                createCollectionUseCase = app.container.createCollectionUseCase,
                updateCollectionUseCase = app.container.updateCollectionUseCase,
                deleteCollectionUseCase = app.container.deleteCollectionUseCase,
                moveBookmarkUseCase = app.container.moveBookmarkUseCase,
                deleteBookmarkUseCase = app.container.deleteBookmarkUseCase,
                privateModeManager = app.container.privateModeManager
            )
        }
    )
) {
    val context = LocalContext.current
    val app = context.applicationContext as PixelMarksApplication
    val activity = context as? androidx.fragment.app.FragmentActivity
    val coroutineScope = rememberCoroutineScope()

    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var isCollectionUnlocked by remember {
        mutableStateOf(app.container.appLockManager.isCollectionUnlocked(collectionId))
    }
    var showCreateSubCollectionDialog by remember { mutableStateOf(false) }
    var showEditCollectionDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var bookmarkToMove by remember { mutableStateOf<Bookmark?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    val cardConfig = rememberBookmarkCardConfig(app.container.appPreferencesRepository)
    val layoutMode by app.container.appPreferencesRepository.observeLayoutDisplayMode().collectAsState(initial = "List")
    var isGridView by remember(layoutMode) { mutableStateOf(layoutMode.equals("Grid", ignoreCase = true)) }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            onNavigateBack()
        }
    }

    // Authenticate locked collection
    fun promptUnlock() {
        if (activity != null) {
            app.container.biometricAuthManager.authenticate(
                activity = activity,
                title = "Unlock Collection",
                subtitle = "Authenticate to access \"${uiState.collection?.name}\"",
                onSuccess = {
                    app.container.appLockManager.unlockCollection(collectionId)
                    isCollectionUnlocked = true
                },
                onError = { _: Int, err: CharSequence ->
                    coroutineScope.launch { snackbarHostState.showSnackbar(err.toString()) }
                }
            )
        }
    }

    LaunchedEffect(uiState.collection) {
        val col = uiState.collection
        if (col != null && col.isLocked && !isCollectionUnlocked) {
            promptUnlock()
        }
    }

    // Filter bookmarks with in-collection search & filters
    val displayedBookmarks = remember(uiState.bookmarks, searchQuery, selectedFilter, uiState.childCollections) {
        uiState.bookmarks.filter { b ->
            val matchesQuery = if (searchQuery.isBlank()) true else {
                val q = searchQuery.trim().lowercase()
                b.title.lowercase().contains(q) ||
                b.url.lowercase().contains(q) ||
                b.domain.lowercase().contains(q) ||
                (b.notes?.lowercase()?.contains(q) == true)
            }
            val matchesFilter = when (selectedFilter) {
                "All" -> true
                "Favorites" -> b.isFavorite
                "Pinned" -> b.isPinned
                else -> {
                    val matchedChild = uiState.childCollections.firstOrNull { it.name == selectedFilter }
                    if (matchedChild != null) {
                        b.collectionId == matchedChild.id
                    } else {
                        true
                    }
                }
            }
            matchesQuery && matchesFilter
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

    if (showCreateSubCollectionDialog) {
        CreateEditCollectionDialog(
            defaultParentId = collectionId,
            availableParents = uiState.allCollections,
            onDismiss = { showCreateSubCollectionDialog = false },
            onSave = { name, _, icon, color, isLocked, isHidden, description, keywords ->
                coroutineScope.launch {
                    app.container.createCollectionUseCase(
                        name = name,
                        parentId = collectionId,
                        icon = icon,
                        color = color,
                        isLocked = isLocked,
                        isHidden = isHidden,
                        description = description,
                        keywords = keywords
                    )
                    showCreateSubCollectionDialog = false
                }
            }
        )
    }

    if (showEditCollectionDialog && uiState.collection != null) {
        CreateEditCollectionDialog(
            initialCollection = uiState.collection,
            availableParents = uiState.allCollections,
            onDismiss = { showEditCollectionDialog = false },
            onSave = { name, parentId, icon, color, isLocked, isHidden, description, keywords ->
                coroutineScope.launch {
                    app.container.updateCollectionUseCase(
                        collectionId = collectionId,
                        newName = name,
                        newParentId = parentId,
                        icon = icon,
                        color = color,
                        isLocked = isLocked,
                        isHidden = isHidden,
                        description = description,
                        keywords = keywords
                    )
                    showEditCollectionDialog = false
                }
            }
        )
    }

    if (showDeleteConfirmDialog && uiState.collection != null) {
        DeleteCollectionConfirmDialog(
            collectionName = uiState.collection?.name.orEmpty(),
            onDismiss = { showDeleteConfirmDialog = false },
            onConfirm = {
                viewModel.deleteCollection(onSuccess = {
                    showDeleteConfirmDialog = false
                })
            }
        )
    }

    if (bookmarkToMove != null) {
        MoveBookmarkDialog(
            currentCollectionId = bookmarkToMove!!.collectionId,
            availableCollections = uiState.allCollections,
            onDismiss = { bookmarkToMove = null },
            onSelectCollection = { targetId ->
                viewModel.moveBookmark(bookmarkToMove!!.id, targetId)
                bookmarkToMove = null
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = ArchivalBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(
                color = ArchivalBackground.copy(alpha = 0.95f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.medium, vertical = Spacing.small),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = ArchivalTextPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Text(
                            text = uiState.collection?.name ?: "Collection",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = FontFamily.Serif,
                            color = ArchivalTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(onClick = { showDeleteConfirmDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Collection",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        },
        bottomBar = {
            // Sticky Bottom "+ ADD LINK" action button
            if (uiState.collection != null && (!uiState.collection!!.isLocked || isCollectionUnlocked)) {
                Surface(
                    color = ArchivalBackground.copy(alpha = 0.95f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ArchivalSurfaceContainerHighest),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(Spacing.medium)) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ArchivalTextPrimary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .clickable { onNavigateToAddBookmark(collectionId) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = ArchivalSurfaceContainerLowest,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ADD LINK TO COLLECTION",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = ArchivalSurfaceContainerLowest
                                )
                            }
                        }
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
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BrassGold)
                }
            } else if (uiState.collection == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Collection not found",
                        style = MaterialTheme.typography.titleMedium,
                        color = ArchivalTextSecondary
                    )
                }
            } else if (uiState.collection!!.isLocked && !isCollectionUnlocked) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Spacing.large),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = BrassGold,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(Spacing.medium))
                    Text(
                        text = "Locked Collection",
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = FontFamily.Serif,
                        color = ArchivalTextPrimary
                    )
                    Text(
                        text = "This collection is confidential and requires authentication.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ArchivalTextSecondary
                    )
                    Spacer(modifier = Modifier.height(Spacing.medium))
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = ArchivalPrimary,
                        modifier = Modifier.clickable { promptUnlock() }
                    ) {
                        Text(
                            text = "Unlock",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = ArchivalSurfaceContainerLowest,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
                        )
                    }
                }
            } else {
                val currentCollection = uiState.collection!!
                val collectionColor = parseHexColor(currentCollection.color)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = Spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(Spacing.medium)
                ) {
                    // Curatorial Folio Dossier Header Card
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = ArchivalSurfaceContainerLow,
                            border = BorderStroke(1.dp, ArchivalSurfaceContainerHighest),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Top row: Edit Collection button on the right (like Tag Detail screen)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.Transparent,
                                        modifier = Modifier.clickable { showEditCollectionDialog = true }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit Collection",
                                                tint = ArchivalTextSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "Edit Collection",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ArchivalTextSecondary,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }

                                // Collection Title Row
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(ArchivalSurfaceContainerHigh),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = getIconVector(currentCollection.icon),
                                            contentDescription = null,
                                            tint = collectionColor,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Text(
                                        text = currentCollection.name,
                                        style = MaterialTheme.typography.displaySmall,
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ArchivalTextPrimary,
                                        letterSpacing = (-0.5).sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (!currentCollection.description.isNullOrBlank()) {
                                    Text(
                                        text = currentCollection.description!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ArchivalTextSecondary,
                                        lineHeight = 18.sp
                                    )
                                }

                                // Curatorial Taxonomy Keywords (Chips without #)
                                if (!currentCollection.keywords.isNullOrBlank()) {
                                    val kws = currentCollection.keywords!!.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                    if (kws.isNotEmpty()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            kws.forEach { kw ->
                                                Surface(
                                                    shape = RoundedCornerShape(9999.dp),
                                                    color = ArchivalSurfaceContainerHighest
                                                ) {
                                                    Text(
                                                        text = kw,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = ArchivalTextSecondary,
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Bottom row: Favicon cluster on left, Blue bordered count pill on right
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val favicons = remember(uiState.bookmarks) {
                                        uiState.bookmarks.mapNotNull { it.faviconUrl.takeIf { f -> !f.isNullOrBlank() } }
                                            .distinct()
                                            .take(4)
                                    }

                                    if (favicons.isNotEmpty()) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy((-6).dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            favicons.forEach { favUrl ->
                                                Surface(
                                                    shape = CircleShape,
                                                    color = ArchivalSurfaceContainerLowest,
                                                    border = BorderStroke(1.5.dp, ArchivalSurfaceContainerLow),
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        BookmarkFavicon(faviconUrl = favUrl, size = 14.dp)
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(ArchivalSurfaceContainerHigh),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Folder,
                                                contentDescription = null,
                                                tint = ArchivalTextSecondary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }

                                    val count = uiState.bookmarks.size
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.Transparent,
                                        border = BorderStroke(1.dp, Color(0xFF4C669F))
                                    ) {
                                        Text(
                                            text = when {
                                                count == 0 -> "0 sources registered"
                                                count == 1 -> "1 unique source registered"
                                                else -> "+$count unique sources registered"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF90A4DF),
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Nested Sub-Collections Section (Stitch UI)
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
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
                                    Text(
                                        text = "NESTED COLLECTIONS",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ArchivalTextPrimary,
                                        letterSpacing = 1.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (uiState.childCollections.isNotEmpty()) {
                                        Surface(
                                            shape = RoundedCornerShape(9999.dp),
                                            color = ArchivalSurfaceContainerHighest
                                        ) {
                                            Text(
                                                text = "${uiState.childCollections.size} sub-folders",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ArchivalTextSecondary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = ArchivalSurfaceContainerLow,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, ArchivalSurfaceContainerHighest),
                                    modifier = Modifier.clickable { showCreateSubCollectionDialog = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            tint = BrassGold,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "New Sub-folder",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = BrassGold,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            if (uiState.childCollections.isEmpty()) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showCreateSubCollectionDialog = true },
                                    shape = RoundedCornerShape(12.dp),
                                    color = ArchivalSurfaceContainerLow,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, ArchivalSurfaceContainerHighest)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(ArchivalSurfaceContainerHigh),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = null,
                                                tint = BrassGold,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Add Sub-Collection",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontFamily = FontFamily.Serif,
                                                color = ArchivalTextPrimary
                                            )
                                            Text(
                                                text = "Organize bookmarks into a nested topic ledger within this collection.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = ArchivalTextSecondary
                                            )
                                        }
                                    }
                                }
                            } else {
                                // Horizontal Carousel of Sub-collections (Stitch Design)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    uiState.childCollections.forEach { child ->
                                        SubCollectionCard(
                                            collection = child,
                                            onClick = { onNavigateToCollectionDetail(child.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Scoped in-collection search input
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    "Search within ${currentCollection.name}...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ArchivalTextSecondary
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = ArchivalTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear",
                                            tint = ArchivalTextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = ArchivalSurfaceContainerLowest,
                                unfocusedContainerColor = ArchivalSurfaceContainerLowest,
                                focusedBorderColor = ArchivalPrimary,
                                unfocusedBorderColor = ArchivalSurfaceContainerHighest,
                                focusedTextColor = ArchivalTextPrimary,
                                unfocusedTextColor = ArchivalTextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Filter Pills & View Switcher (Like Tag Detail Screen)
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Filter Pills: All, Favorites, Pinned + Sub-collection names
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val baseFilters = listOf("All", "Favorites", "Pinned")
                                val allFilters = baseFilters + uiState.childCollections.map { it.name }

                                allFilters.forEach { filterName ->
                                    val isSelected = selectedFilter == filterName
                                    val isChildFilter = filterName !in baseFilters
                                    val childCol = if (isChildFilter) uiState.childCollections.firstOrNull { it.name == filterName } else null

                                    val bgColor by animateColorAsState(
                                        targetValue = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                                        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                                        label = "colFilterBg"
                                    )
                                    val textColor by animateColorAsState(
                                        targetValue = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                                        label = "colFilterText"
                                    )

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(bgColor)
                                            .clickable { selectedFilter = filterName }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            if (childCol != null) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(parseHexColor(childCol.color))
                                                )
                                            }
                                            Text(
                                                text = when (filterName) {
                                                    "All" -> "All (${uiState.bookmarks.size})"
                                                    else -> filterName
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = textColor,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Grid or List toggle
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(2.dp)
                            ) {
                                IconButton(
                                    onClick = { isGridView = false },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.List,
                                        contentDescription = "List format",
                                        tint = if (!isGridView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { isGridView = true },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.GridView,
                                        contentDescription = "Grid format",
                                        tint = if (isGridView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
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
                                text = "BOOKMARKS (${displayedBookmarks.size})",
                                style = MaterialTheme.typography.labelSmall,
                                color = ArchivalTextPrimary,
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (displayedBookmarks.isEmpty()) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = ArchivalSurfaceContainerLow,
                                border = BorderStroke(1.dp, ArchivalSurfaceContainerHighest)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(Spacing.large),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(Spacing.small)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bookmark,
                                        contentDescription = null,
                                        tint = BrassGold,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        text = if (searchQuery.isEmpty() && selectedFilter == "All") "No Bookmarks in this Collection" else "No matching bookmarks",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontFamily = FontFamily.Serif,
                                        color = ArchivalTextPrimary
                                    )
                                    Text(
                                        text = if (searchQuery.isEmpty() && selectedFilter == "All") "Save links directly into this collection." else "Try adjusting your filter or search terms.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ArchivalTextSecondary
                                    )
                                }
                            }
                        }
                    } else if (isGridView) {
                        val chunked = displayedBookmarks.chunked(2)
                        items(chunked) { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowItems.forEach { bookmark ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        BookmarkGridPlate(
                                            bookmark = bookmark,
                                            onClick = { onNavigateToBookmarkDetail(bookmark.id) },
                                            onShare = { shareBookmark(bookmark) },
                                            onMove = { bookmarkToMove = bookmark },
                                            onDelete = { viewModel.deleteBookmark(bookmark.id) },
                                            config = cardConfig
                                        )
                                    }
                                }
                                if (rowItems.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    } else {
                        items(displayedBookmarks, key = { it.id }) { bookmark ->
                            ListBookmarkCard(
                                bookmark = bookmark,
                                onClick = { onNavigateToBookmarkDetail(bookmark.id) },
                                onShare = { shareBookmark(bookmark) },
                                onMove = { bookmarkToMove = bookmark },
                                onDelete = { viewModel.deleteBookmark(bookmark.id) },
                                config = cardConfig
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SubCollectionCard(
    collection: Collection,
    onClick: () -> Unit
) {
    val color = parseHexColor(collection.color)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1.0f,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "subColCardScale"
    )

    Surface(
        modifier = Modifier
            .width(220.dp)
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                onClick = onClick
            ),
        shape = RoundedCornerShape(12.dp),
        color = ArchivalSurfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(1.dp, ArchivalSurfaceContainerHighest)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ArchivalSurfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getIconVector(collection.icon),
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = ArchivalTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = collection.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    color = ArchivalTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!collection.description.isNullOrBlank()) {
                    Text(
                        text = collection.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = ArchivalTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 11.sp
                    )
                } else if (!collection.keywords.isNullOrBlank()) {
                    Text(
                        text = collection.keywords,
                        style = MaterialTheme.typography.bodySmall,
                        color = ArchivalTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 11.sp
                    )
                } else {
                    Text(
                        text = "Curated sub-ledger",
                        style = MaterialTheme.typography.labelSmall,
                        color = ArchivalTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            HorizontalDivider(
                color = ArchivalSurfaceContainerHighest,
                thickness = 0.5.dp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Nested Collection",
                    style = MaterialTheme.typography.labelSmall,
                    color = BrassGold,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
                if (collection.isLocked) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = BrassGold,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}


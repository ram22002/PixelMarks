package com.pixelmarks.feature.collection

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.launch
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pixelmarks.app.PixelMarksApplication
import com.pixelmarks.core.model.Bookmark
import com.pixelmarks.core.model.Collection
import com.pixelmarks.feature.home.BookmarkListItem
import com.pixelmarks.ui.components.PixelMarksScreenScaffold
import com.pixelmarks.ui.theme.Spacing

@Composable
fun CollectionDetailScreen(
    collectionId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToCollectionDetail: (Long) -> Unit = {},
    onNavigateToBookmarkDetail: (Long) -> Unit = {},
    onNavigateToAddBookmark: (Long) -> Unit = {},
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
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showCreateSubCollectionDialog by remember { mutableStateOf(false) }
    var showEditCollectionDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var bookmarkToMove by remember { mutableStateOf<Bookmark?>(null) }

    // If collection was deleted, automatically navigate back
    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            onNavigateBack()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    val context = LocalContext.current
    val app = context.applicationContext as PixelMarksApplication
    val activity = context as? androidx.fragment.app.FragmentActivity
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    val unlockedCollectionIds by app.container.appLockManager.unlockedCollectionIds.collectAsState()
    val isPrivateSessionUnlocked by app.container.appLockManager.isPrivateSessionUnlocked.collectAsState()
    val isCollectionUnlocked = isPrivateSessionUnlocked || unlockedCollectionIds.contains(collectionId)

    fun triggerUnlock() {
        if (activity != null) {
            val name = uiState.collection?.name ?: "Collection"
            app.container.biometricAuthManager.authenticate(
                activity = activity,
                title = "Unlock Collection",
                subtitle = "Authenticate to view \"$name\"",
                onSuccess = {
                    app.container.appLockManager.unlockCollection(collectionId)
                },
                onError = { _: Int, errString: CharSequence ->
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(errString.toString())
                    }
                }
            )
        }
    }

    LaunchedEffect(uiState.collection, isCollectionUnlocked) {
        if (uiState.collection?.isLocked == true && !isCollectionUnlocked) {
            triggerUnlock()
        }
    }

    // Dialogs
    if (showCreateSubCollectionDialog) {
        CreateEditCollectionDialog(
            defaultParentId = collectionId,
            availableParents = uiState.allCollections,
            onDismiss = { showCreateSubCollectionDialog = false },
            onSave = { name, _, icon, color, isLocked, isHidden ->
                coroutineScope.launch {
                    app.container.createCollectionUseCase(
                        name = name,
                        parentId = collectionId,
                        icon = icon,
                        color = color,
                        isLocked = isLocked,
                        isHidden = isHidden
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
            onSave = { name, parentId, icon, color, isLocked, isHidden ->
                coroutineScope.launch {
                    app.container.updateCollectionUseCase(
                        collectionId = collectionId,
                        newName = name,
                        newParentId = parentId,
                        icon = icon,
                        color = color,
                        isLocked = isLocked,
                        isHidden = isHidden
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

    bookmarkToMove?.let { bookmark ->
        MoveBookmarkDialog(
            currentCollectionId = bookmark.collectionId,
            availableCollections = uiState.allCollections,
            onDismiss = { bookmarkToMove = null },
            onSelectCollection = { targetId ->
                viewModel.moveBookmark(bookmark.id, targetId)
                bookmarkToMove = null
            }
        )
    }

    PixelMarksScreenScaffold(
        title = uiState.collection?.name ?: "Collection",
        onNavigateBack = onNavigateBack,
        actions = {
            IconButton(onClick = { showCreateSubCollectionDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New sub-collection"
                )
            }
            IconButton(onClick = { showEditCollectionDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit collection"
                )
            }
            IconButton(onClick = { showDeleteConfirmDialog = true }) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete collection"
                )
            }
        },
        floatingActionButton = {
            if (uiState.collection != null && (!uiState.collection!!.isLocked || isCollectionUnlocked)) {
                FloatingActionButton(
                    onClick = { onNavigateToAddBookmark(collectionId) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add bookmark"
                    )
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
                    CircularProgressIndicator()
                }
            } else if (uiState.collection == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Collection not found",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.medium))

                    Text(
                        text = "Collection is Locked",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(Spacing.small))

                    Text(
                        text = "Authenticate to view bookmarks inside \"${uiState.collection!!.name}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(Spacing.large))

                    Button(onClick = { triggerUnlock() }) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Unlock Collection")
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
                    item {
                        Spacer(modifier = Modifier.height(Spacing.extraSmall))
                        // Collection Summary Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.medium),
                                verticalArrangement = Arrangement.spacedBy(Spacing.small)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(collectionColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = getIconVector(currentCollection.icon),
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = currentCollection.name,
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Text(
                                            text = "${uiState.bookmarks.size} bookmarks • ${uiState.childCollections.size} sub-collections",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }

                                // Parent Navigation Chip if nested
                                uiState.parentCollection?.let { parent ->
                                    SuggestionChip(
                                        onClick = { onNavigateToCollectionDetail(parent.id) },
                                        label = {
                                            Text("Inside: ${parent.name}")
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = Icons.Default.Folder,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = MaterialTheme.colorScheme.surface
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Sub-collections section
                    if (uiState.childCollections.isNotEmpty()) {
                        item {
                            Text(
                                text = "Sub-collections (${uiState.childCollections.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        items(uiState.childCollections, key = { "sub_${it.id}" }) { child ->
                            SubCollectionCard(
                                collection = child,
                                onClick = { onNavigateToCollectionDetail(child.id) }
                            )
                        }
                    }

                    // Bookmarks section
                    item {
                        Text(
                            text = "Bookmarks (${uiState.bookmarks.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    if (uiState.bookmarks.isEmpty()) {
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
                                        imageVector = Icons.Default.Bookmark,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        text = "No Bookmarks in this Collection",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Save links to this collection or move existing bookmarks here.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(Spacing.small))
                                    Button(onClick = { onNavigateToAddBookmark(collectionId) }) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(Spacing.extraSmall))
                                        Text("Add Bookmark")
                                    }
                                }
                            }
                        }
                    } else {
                        items(
                            items = uiState.bookmarks,
                            key = { it.id }
                        ) { bookmark ->
                            BookmarkListItem(
                                bookmark = bookmark,
                                onClick = { onNavigateToBookmarkDetail(bookmark.id) },
                                onDelete = { viewModel.deleteBookmark(bookmark.id) },
                                onMove = { bookmarkToMove = bookmark },
                                onRemoveFromCollection = { viewModel.moveBookmark(bookmark.id, null) }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(Spacing.large))
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

@Composable
private fun SubCollectionCard(
    collection: Collection,
    onClick: () -> Unit
) {
    val color = parseHexColor(collection.color)

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.medium, vertical = Spacing.small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getIconVector(collection.icon),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = collection.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

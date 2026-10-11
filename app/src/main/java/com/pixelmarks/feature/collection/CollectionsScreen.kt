package com.pixelmarks.feature.collection

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.pixelmarks.core.image.BookmarkFavicon
import com.pixelmarks.core.model.Collection
import com.pixelmarks.ui.components.ArchivalSubHeader
import com.pixelmarks.ui.components.PixelMarksTopAppBar
import com.pixelmarks.ui.components.getEditorialSalutation
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class CollectionHierarchyNode(
    val collection: Collection,
    val depth: Int
)

fun flattenCollectionHierarchy(collections: List<Collection>): List<CollectionHierarchyNode> {
    val result = mutableListOf<CollectionHierarchyNode>()
    val byParent = collections.groupBy { it.parentId }
    val visited = mutableSetOf<Long>()

    fun traverse(parentId: Long?, depth: Int) {
        val children = byParent[parentId]?.sortedWith(
            compareBy({ it.position }, { it.name.lowercase() })
        ) ?: emptyList()

        for (child in children) {
            if (visited.add(child.id)) {
                result.add(CollectionHierarchyNode(child, depth))
                traverse(child.id, depth + 1)
            }
        }
    }

    traverse(null, 0)

    val orphans = collections.filter { !visited.contains(it.id) }
    for (orphan in orphans) {
        result.add(CollectionHierarchyNode(orphan, 0))
    }

    return result
}

private enum class CollectionSortOption {
    RECENT, ALPHABETICAL, COUNT
}

@Composable
fun CollectionsScreen(
    onNavigateToCollectionDetail: (Long) -> Unit,
    onNavigateToSearch: () -> Unit = {},
    onNavigateToConnectPc: () -> Unit = {},
    onNavigateToReminders: () -> Unit = {},
    viewModel: CollectionsViewModel = viewModel(
        factory = run {
            val app = LocalContext.current.applicationContext as PixelMarksApplication
            CollectionsViewModel.provideFactory(
                observeCollectionsUseCase = app.container.observeCollectionsUseCase,
                observeBookmarksUseCase = app.container.observeBookmarksUseCase,
                createCollectionUseCase = app.container.createCollectionUseCase,
                updateCollectionUseCase = app.container.updateCollectionUseCase,
                deleteCollectionUseCase = app.container.deleteCollectionUseCase,
                privateModeManager = app.container.privateModeManager
            )
        }
    )
) {
    val context = LocalContext.current
    val app = context.applicationContext as PixelMarksApplication
    val activity = context as? androidx.fragment.app.FragmentActivity
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showCreateDialog by remember { mutableStateOf(false) }
    var createParentId by remember { mutableStateOf<Long?>(null) }
    var editingCollection by remember { mutableStateOf<Collection?>(null) }
    var deletingCollection by remember { mutableStateOf<Collection?>(null) }
    var isGridView by remember { mutableStateOf(false) }
    var sortOption by remember { mutableStateOf(CollectionSortOption.RECENT) }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    if (showCreateDialog) {
        CreateEditCollectionDialog(
            defaultParentId = createParentId,
            availableParents = uiState.collections,
            onDismiss = {
                showCreateDialog = false
                createParentId = null
            },
            onSave = { name, parentId, icon, color, isLocked, isHidden, description, keywords ->
                viewModel.createCollection(
                    name = name,
                    parentId = parentId,
                    icon = icon,
                    color = color,
                    isLocked = isLocked,
                    isHidden = isHidden,
                    description = description,
                    keywords = keywords,
                    onSuccess = {
                        showCreateDialog = false
                        createParentId = null
                    }
                )
            }
        )
    }

    editingCollection?.let { collection ->
        CreateEditCollectionDialog(
            initialCollection = collection,
            availableParents = uiState.collections,
            onDismiss = { editingCollection = null },
            onSave = { name, parentId, icon, color, isLocked, isHidden, description, keywords ->
                viewModel.updateCollection(
                    collectionId = collection.id,
                    newName = name,
                    newParentId = parentId,
                    icon = icon,
                    color = color,
                    isLocked = isLocked,
                    isHidden = isHidden,
                    description = description,
                    keywords = keywords,
                    onSuccess = { editingCollection = null }
                )
            }
        )
    }

    deletingCollection?.let { collection ->
        DeleteCollectionConfirmDialog(
            collectionName = collection.name,
            onDismiss = { deletingCollection = null },
            onConfirm = {
                viewModel.deleteCollection(collection.id)
                deletingCollection = null
            }
        )
    }

    fun handleCollectionClick(col: Collection) {
        if (col.isLocked && !app.container.appLockManager.isCollectionUnlocked(col.id)) {
            if (activity != null) {
                app.container.biometricAuthManager.authenticate(
                    activity = activity,
                    title = "Unlock Collection",
                    subtitle = "Authenticate to open \"${col.name}\"",
                    onSuccess = {
                        app.container.appLockManager.unlockCollection(col.id)
                        onNavigateToCollectionDetail(col.id)
                    },
                    onError = { _: Int, err: CharSequence ->
                        coroutineScope.launch { snackbarHostState.showSnackbar(err.toString()) }
                    }
                )
            }
        } else {
            onNavigateToCollectionDetail(col.id)
        }
    }

    val sortedCollections = remember(uiState.collections, sortOption, uiState.bookmarkCounts) {
        when (sortOption) {
            CollectionSortOption.RECENT -> uiState.collections.sortedByDescending { it.updatedAt }
            CollectionSortOption.ALPHABETICAL -> uiState.collections.sortedBy { it.name.lowercase() }
            CollectionSortOption.COUNT -> uiState.collections.sortedByDescending { uiState.bookmarkCounts[it.id] ?: 0 }
        }
    }

    val flatNodes = remember(sortedCollections) {
        flattenCollectionHierarchy(sortedCollections)
    }

    val rootCollections = remember(sortedCollections) {
        sortedCollections.filter { it.parentId == null }
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
                onClick = {
                    createParentId = null
                    showCreateDialog = true
                },
                containerColor = TerracottaAccent,
                contentColor = ArchivalSurfaceContainerLowest,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Collection",
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
            // Editorial Date & Title Header
            item {
                Spacer(modifier = Modifier.height(4.dp))
                ArchivalSubHeader(
                    title = getEditorialSalutation(),
                    isGridView = isGridView,
                    onToggleGridView = { isGridView = it }
                )
            }

            // Curatorial Sort Pills
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SortPill(
                        label = "Recently Updated",
                        isSelected = sortOption == CollectionSortOption.RECENT,
                        onClick = { sortOption = CollectionSortOption.RECENT }
                    )
                    SortPill(
                        label = "Alphabetical (A–Z)",
                        isSelected = sortOption == CollectionSortOption.ALPHABETICAL,
                        onClick = { sortOption = CollectionSortOption.ALPHABETICAL }
                    )
                    SortPill(
                        label = "Item Count",
                        isSelected = sortOption == CollectionSortOption.COUNT,
                        onClick = { sortOption = CollectionSortOption.COUNT }
                    )
                }
            }

            // Loading / Empty
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
            } else if (uiState.collections.isEmpty()) {
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
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = BrassGold,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "No Collections in Folio",
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = FontFamily.Serif,
                                color = ArchivalTextPrimary
                            )
                            Text(
                                text = "Organize bookmarks into structured curatorial folders with custom metadata.",
                                style = MaterialTheme.typography.bodySmall,
                                color = ArchivalTextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = ArchivalPrimary,
                                modifier = Modifier.clickable {
                                    createParentId = null
                                    showCreateDialog = true
                                }
                            ) {
                                Text(
                                    text = "Create Collection",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ArchivalSurfaceContainerLowest,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                // Section Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${rootCollections.size} ledgers",
                            style = MaterialTheme.typography.labelSmall,
                            color = ArchivalTextSecondary
                        )
                    }
                }

                // Grid or Folio List
                if (isGridView) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            rootCollections.chunked(2).forEach { rowPair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowPair.forEach { col ->
                                        val count = uiState.bookmarkCounts[col.id] ?: 0
                                        val favicons = uiState.collectionFavicons[col.id].orEmpty()
                                        Box(modifier = Modifier.weight(1f)) {
                                            CollectionGridCard(
                                                collection = col,
                                                bookmarkCount = count,
                                                favicons = favicons,
                                                onClick = { handleCollectionClick(col) },
                                                onEdit = { editingCollection = col },
                                                onDelete = { deletingCollection = col }
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
                    items(rootCollections, key = { it.id }) { col ->
                        val bookmarkCount = uiState.bookmarkCounts[col.id] ?: 0
                        val childCount = uiState.childCounts[col.id] ?: 0
                        val favicons = uiState.collectionFavicons[col.id].orEmpty()

                        CollectionFolioRowCard(
                            collection = col,
                            depth = 0,
                            bookmarkCount = bookmarkCount,
                            childCount = childCount,
                            favicons = favicons,
                            onClick = { handleCollectionClick(col) },
                            onAddSubCollection = {
                                createParentId = col.id
                                showCreateDialog = true
                            },
                            onEdit = { editingCollection = col },
                            onDelete = { deletingCollection = col }
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
private fun SortPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) TerracottaAccent else ArchivalSurfaceContainer,
        contentColor = if (isSelected) ArchivalSurfaceContainerLowest else ArchivalTextSecondary,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun CollectionFolioRowCard(
    collection: Collection,
    depth: Int,
    bookmarkCount: Int,
    childCount: Int,
    favicons: List<String> = emptyList(),
    onClick: () -> Unit,
    onAddSubCollection: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val color = parseHexColor(collection.color)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = ArchivalSurfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(1.dp, ArchivalSurfaceContainerHighest),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 16).dp)
            .clickable(onClick = onClick)
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
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (depth > 0) {
                        Icon(
                            imageVector = Icons.Default.SubdirectoryArrowRight,
                            contentDescription = null,
                            tint = ArchivalTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
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

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = collection.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = FontFamily.Serif,
                                color = ArchivalTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (collection.isLocked) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Locked",
                                    tint = BrassGold,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        if (!collection.description.isNullOrBlank()) {
                            Text(
                                text = collection.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = ArchivalTextSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "$bookmarkCount links",
                                style = MaterialTheme.typography.labelSmall,
                                color = TerracottaAccent,
                                fontWeight = FontWeight.Medium
                            )
                            if (childCount > 0) {
                                Text(
                                    text = "• $childCount sub-folders",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ArchivalTextSecondary
                                )
                            }
                        }
                    }
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = ArchivalTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(ArchivalSurfaceContainerHigh)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Add Sub-Collection", color = ArchivalTextPrimary) },
                            onClick = {
                                menuExpanded = false
                                onAddSubCollection()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Edit", color = ArchivalTextPrimary) },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            // Curatorial Keywords Chips without #
            if (!collection.keywords.isNullOrBlank()) {
                val keywordsList = collection.keywords.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                if (keywordsList.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        keywordsList.take(5).forEach { kw ->
                            Surface(
                                shape = RoundedCornerShape(9999.dp),
                                color = ArchivalSurfaceContainerHighest
                            ) {
                                Text(
                                    text = kw,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ArchivalTextSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    fontSize = 10.sp
                                )
                            }
                        }
                        if (keywordsList.size > 5) {
                            Text(
                                text = "+${keywordsList.size - 5} more",
                                style = MaterialTheme.typography.labelSmall,
                                color = BrassGoldLight,
                                modifier = Modifier.align(Alignment.CenterVertically),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            // Overlapping Saved Links Favicons
            if (favicons.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy((-6).dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    favicons.forEach { favUrl ->
                        Surface(
                            shape = CircleShape,
                            color = ArchivalSurfaceContainerLowest,
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, ArchivalSurfaceContainerLow),
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
            }
        }
    }
}

@Composable
private fun CollectionGridCard(
    collection: Collection,
    bookmarkCount: Int,
    favicons: List<String> = emptyList(),
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val color = parseHexColor(collection.color)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = ArchivalSurfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(1.dp, ArchivalSurfaceContainerHighest),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = ArchivalSurfaceContainerHighest
                ) {
                    Text(
                        text = "$bookmarkCount",
                        style = MaterialTheme.typography.labelSmall,
                        color = TerracottaAccent,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = collection.name,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Serif,
                color = ArchivalTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (!collection.description.isNullOrBlank()) {
                Text(
                    text = collection.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = ArchivalTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 11.sp
                )
            }

            if (!collection.keywords.isNullOrBlank()) {
                val firstKw = collection.keywords.split(",").firstOrNull()?.trim().orEmpty()
                if (firstKw.isNotEmpty()) {
                    Text(
                        text = firstKw,
                        style = MaterialTheme.typography.labelSmall,
                        color = ArchivalTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 10.sp
                    )
                }
            }

            if (favicons.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy((-5).dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    favicons.forEach { favUrl ->
                        Surface(
                            shape = CircleShape,
                            color = ArchivalSurfaceContainerLowest,
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, ArchivalSurfaceContainerLow),
                            modifier = Modifier.size(20.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                BookmarkFavicon(faviconUrl = favUrl, size = 11.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatCuratorialDate(): String {
    val cal = Calendar.getInstance()
    val sdf = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
    return sdf.format(cal.time).uppercase()
}

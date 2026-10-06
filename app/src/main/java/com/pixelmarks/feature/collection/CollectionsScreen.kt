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
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pixelmarks.app.PixelMarksApplication
import com.pixelmarks.core.model.Collection
import com.pixelmarks.ui.components.PixelMarksScreenScaffold
import com.pixelmarks.ui.theme.PixelMarksTheme
import com.pixelmarks.ui.theme.Spacing

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

    // Append any disconnected orphan items safely (if parentId points to non-existent ID)
    val orphans = collections.filter { !visited.contains(it.id) }
    for (orphan in orphans) {
        result.add(CollectionHierarchyNode(orphan, 0))
    }

    return result
}

@Composable
fun CollectionsScreen(
    onNavigateToCollectionDetail: (Long) -> Unit,
    viewModel: CollectionsViewModel = viewModel(
        factory = run {
            val app = LocalContext.current.applicationContext as PixelMarksApplication
            CollectionsViewModel.provideFactory(
                observeCollectionsUseCase = app.container.observeCollectionsUseCase,
                observeBookmarksUseCase = app.container.observeBookmarksUseCase,
                createCollectionUseCase = app.container.createCollectionUseCase,
                updateCollectionUseCase = app.container.updateCollectionUseCase,
                deleteCollectionUseCase = app.container.deleteCollectionUseCase
            )
        }
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showCreateDialog by remember { mutableStateOf(false) }
    var createParentId by remember { mutableStateOf<Long?>(null) }
    var editingCollection by remember { mutableStateOf<Collection?>(null) }
    var deletingCollection by remember { mutableStateOf<Collection?>(null) }

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
            onSave = { name, parentId, icon, color ->
                viewModel.createCollection(
                    name = name,
                    parentId = parentId,
                    icon = icon,
                    color = color,
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
            onSave = { name, parentId, icon, color ->
                viewModel.updateCollection(
                    collectionId = collection.id,
                    newName = name,
                    newParentId = parentId,
                    icon = icon,
                    color = color,
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

    PixelMarksScreenScaffold(
        title = "Collections",
        actions = {
            IconButton(
                onClick = {
                    createParentId = null
                    showCreateDialog = true
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New collection"
                )
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
            } else if (uiState.collections.isEmpty()) {
                // Empty State
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.medium),
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
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No Collections Yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Organize your bookmarks into hierarchical folders with custom colors and icons.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(Spacing.small))
                        Button(
                            onClick = {
                                createParentId = null
                                showCreateDialog = true
                            }
                        ) {
                            Text("Create First Collection")
                        }
                    }
                }
            } else {
                val flatNodes = remember(uiState.collections) {
                    flattenCollectionHierarchy(uiState.collections)
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = Spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(Spacing.small)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(Spacing.small))
                        Text(
                            text = "All Collections (${uiState.collections.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    items(flatNodes, key = { it.collection.id }) { node ->
                        val col = node.collection
                        val bookmarkCount = uiState.bookmarkCounts[col.id] ?: 0
                        val childCount = uiState.childCounts[col.id] ?: 0

                        CollectionListItemCard(
                            collection = col,
                            depth = node.depth,
                            bookmarkCount = bookmarkCount,
                            childCount = childCount,
                            onClick = { onNavigateToCollectionDetail(col.id) },
                            onAddSubCollection = {
                                createParentId = col.id
                                showCreateDialog = true
                            },
                            onEdit = { editingCollection = col },
                            onDelete = { deletingCollection = col }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }

            FloatingActionButton(
                onClick = {
                    createParentId = null
                    showCreateDialog = true
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(Spacing.medium)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create collection"
                )
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 72.dp)
            )
        }
    }
}

@Composable
fun CollectionListItemCard(
    collection: Collection,
    depth: Int,
    bookmarkCount: Int,
    childCount: Int,
    onClick: () -> Unit,
    onAddSubCollection: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val color = parseHexColor(collection.color)
    val indentPadding = (depth * 20).dp
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = indentPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (depth > 0) {
            Icon(
                imageVector = Icons.Default.SubdirectoryArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(18.dp)
            )
        }

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
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(color),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getIconVector(collection.icon),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
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

                    val subtitle = buildString {
                        append("$bookmarkCount bookmarks")
                        if (childCount > 0) {
                            append(" • $childCount sub-folders")
                        }
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Collection options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Add Sub-folder") },
                            onClick = {
                                menuExpanded = false
                                onAddSubCollection()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Edit") },
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
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CollectionsScreenPreview() {
    PixelMarksTheme {
        CollectionsScreen(onNavigateToCollectionDetail = {})
    }
}

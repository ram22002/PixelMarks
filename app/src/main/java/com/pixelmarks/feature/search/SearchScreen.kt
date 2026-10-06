package com.pixelmarks.feature.search

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pixelmarks.app.PixelMarksApplication
import com.pixelmarks.core.model.BookmarkSortOption
import com.pixelmarks.feature.home.BookmarkListItem
import com.pixelmarks.ui.components.PixelMarksScreenScaffold
import com.pixelmarks.ui.theme.Spacing

@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit,
    onNavigateToBookmarkDetail: (Long) -> Unit,
    viewModel: SearchViewModel = viewModel(
        factory = run {
            val app = LocalContext.current.applicationContext as PixelMarksApplication
            SearchViewModel.provideFactory(
                searchBookmarksUseCase = app.container.searchBookmarksUseCase,
                observeCollectionsUseCase = app.container.observeCollectionsUseCase,
                observeTagsUseCase = app.container.observeTagsUseCase,
                deleteBookmarkUseCase = app.container.deleteBookmarkUseCase,
                toggleBookmarkFavoriteUseCase = app.container.toggleBookmarkFavoriteUseCase,
                toggleBookmarkPinUseCase = app.container.toggleBookmarkPinUseCase,
                archiveBookmarkUseCase = app.container.archiveBookmarkUseCase,
                unarchiveBookmarkUseCase = app.container.unarchiveBookmarkUseCase,
                trashBookmarkUseCase = app.container.trashBookmarkUseCase
            )
        }
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    var collectionDropdownExpanded by remember { mutableStateOf(false) }
    var tagDropdownExpanded by remember { mutableStateOf(false) }
    var sortDropdownExpanded by remember { mutableStateOf(false) }

    PixelMarksScreenScaffold(
        title = "Search & Filter",
        onNavigateBack = onNavigateBack
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.small)
        ) {
            Spacer(modifier = Modifier.height(Spacing.extraSmall))

            // Search Bar
            OutlinedTextField(
                value = uiState.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "Search title, URL, domain, notes, tags...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search"
                    )
                },
                trailingIcon = {
                    if (uiState.query.isNotEmpty()) {
                        IconButton(onClick = viewModel::onClearQuery) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )

            // Horizontal Filter Chips Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Favorites
                FilterChip(
                    selected = uiState.filters.favoriteOnly,
                    onClick = viewModel::onToggleFavoriteFilter,
                    label = { Text("Favorites") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                )

                // Pinned
                FilterChip(
                    selected = uiState.filters.pinnedOnly,
                    onClick = viewModel::onTogglePinnedFilter,
                    label = { Text("Pinned") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                )

                // Archived
                FilterChip(
                    selected = uiState.filters.archivedOnly,
                    onClick = viewModel::onToggleArchivedFilter,
                    label = { Text("Archived") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Archive,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                )

                // Collection Filter Dropdown
                Box {
                    val selectedCollection = uiState.availableCollections.find { it.id == uiState.filters.collectionId }
                    val collectionLabel = selectedCollection?.name?.let { "Collection: $it" } ?: "Collection"
                    FilterChip(
                        selected = uiState.filters.collectionId != null,
                        onClick = { collectionDropdownExpanded = true },
                        label = { Text(collectionLabel) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select collection",
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        }
                    )
                    DropdownMenu(
                        expanded = collectionDropdownExpanded,
                        onDismissRequest = { collectionDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Collections") },
                            onClick = {
                                viewModel.onSelectCollection(null)
                                collectionDropdownExpanded = false
                            }
                        )
                        uiState.availableCollections.forEach { col ->
                            DropdownMenuItem(
                                text = { Text(col.name) },
                                onClick = {
                                    viewModel.onSelectCollection(col.id)
                                    collectionDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Tag Filter Dropdown
                Box {
                    val selectedTag = uiState.availableTags.find { it.id == uiState.filters.tagId }
                    val tagLabel = selectedTag?.name?.let { "Tag: $it" } ?: "Tag"
                    FilterChip(
                        selected = uiState.filters.tagId != null,
                        onClick = { tagDropdownExpanded = true },
                        label = { Text(tagLabel) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select tag",
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        }
                    )
                    DropdownMenu(
                        expanded = tagDropdownExpanded,
                        onDismissRequest = { tagDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Tags") },
                            onClick = {
                                viewModel.onSelectTag(null)
                                tagDropdownExpanded = false
                            }
                        )
                        uiState.availableTags.forEach { tag ->
                            DropdownMenuItem(
                                text = { Text(tag.name) },
                                onClick = {
                                    viewModel.onSelectTag(tag.id)
                                    tagDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Sort Dropdown
                Box {
                    FilterChip(
                        selected = uiState.sortOption != BookmarkSortOption.NEWEST,
                        onClick = { sortDropdownExpanded = true },
                        label = { Text("Sort: ${uiState.sortOption.displayName}") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select sort option",
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        }
                    )
                    DropdownMenu(
                        expanded = sortDropdownExpanded,
                        onDismissRequest = { sortDropdownExpanded = false }
                    ) {
                        BookmarkSortOption.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.displayName) },
                                onClick = {
                                    viewModel.onSelectSortOption(option)
                                    sortDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Reset Filters Button
                if (uiState.filters.isActive) {
                    OutlinedButton(
                        onClick = viewModel::onResetFilters
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("Reset")
                    }
                }
            }

            // Results Count Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.extraSmall),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        uiState.isLoading -> "Searching..."
                        uiState.bookmarks.size == 1 -> "1 bookmark"
                        else -> "${uiState.bookmarks.size} bookmarks"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Bookmarks Content or Empty States
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Spacing.large),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.totalBookmarksCount == 0) {
                // Empty State: Database has no bookmarks yet
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
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No bookmarks yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Add your first link from the Home screen.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (uiState.bookmarks.isEmpty()) {
                // Empty State: Query or Filter produces no results
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
                        if (uiState.query.isNotBlank()) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "No matching bookmarks",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Try a different search or remove a filter.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.FilterAltOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "No bookmarks match these filters.",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(Spacing.extraSmall))
                            Button(onClick = viewModel::onResetFilters) {
                                Text("Reset Filters")
                            }
                        }
                    }
                }
            } else {
                // Bookmarks List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.medium)
                ) {
                    items(
                        items = uiState.bookmarks,
                        key = { it.id }
                    ) { bookmark ->
                        BookmarkListItem(
                            bookmark = bookmark,
                            onClick = { onNavigateToBookmarkDetail(bookmark.id) },
                            onEdit = { onNavigateToBookmarkDetail(bookmark.id) },
                            onDelete = { viewModel.moveToTrash(bookmark) },
                            onToggleFavorite = { viewModel.toggleFavorite(bookmark) },
                            onTogglePin = { viewModel.togglePin(bookmark) },
                            onArchive = { viewModel.archiveBookmark(bookmark) },
                            onUnarchive = { viewModel.unarchiveBookmark(bookmark) }
                        )
                    }
                }
            }
        }
    }
}

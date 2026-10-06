package com.pixelmarks.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pixelmarks.app.PixelMarksApplication
import com.pixelmarks.core.model.Bookmark
import com.pixelmarks.ui.components.PixelMarksScreenScaffold
import com.pixelmarks.ui.theme.PixelMarksTheme
import com.pixelmarks.ui.theme.Spacing

@Composable
fun HomeScreen(
    onNavigateToSearch: () -> Unit,
    onNavigateToAddBookmark: () -> Unit,
    onNavigateToBookmarkDetail: (Long) -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToBrowser: () -> Unit,
    onNavigateToConnectPc: () -> Unit,
    viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.provideFactory(
            (LocalContext.current.applicationContext as PixelMarksApplication).container.observeBookmarksUseCase,
            (LocalContext.current.applicationContext as PixelMarksApplication).container.deleteBookmarkUseCase
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    PixelMarksScreenScaffold(
        title = "PixelMarks",
        actions = {
            IconButton(onClick = onNavigateToSearch) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search bookmarks"
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
                        onDelete = { viewModel.deleteBookmark(bookmark.id) }
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
                        onClick = onNavigateToBrowser,
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
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall)
            ) {
                Text(
                    text = bookmark.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = bookmark.domain,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = bookmark.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete bookmark",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
            onNavigateToConnectPc = {}
        )
    }
}

package com.pixelmarks.feature.tags

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Tag
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pixelmarks.app.PixelMarksApplication
import com.pixelmarks.core.model.Tag
import com.pixelmarks.ui.components.ArchivalSubHeader
import com.pixelmarks.ui.components.PixelMarksTopAppBar
import com.pixelmarks.ui.components.getEditorialSalutation
import com.pixelmarks.ui.theme.ArchivalBackground
import com.pixelmarks.ui.theme.ArchivalError
import com.pixelmarks.ui.theme.ArchivalSurfaceContainerHigh
import com.pixelmarks.ui.theme.ArchivalSurfaceContainerHighest
import com.pixelmarks.ui.theme.ArchivalSurfaceContainerLow
import com.pixelmarks.ui.theme.ArchivalSurfaceContainerLowest
import com.pixelmarks.ui.theme.ArchivalTextPrimary
import com.pixelmarks.ui.theme.ArchivalTextSecondary
import com.pixelmarks.ui.theme.PixelMarksTheme
import com.pixelmarks.ui.theme.Spacing
import com.pixelmarks.ui.theme.TerracottaAccent

@Composable
fun TagsScreen(
    onNavigateToTagDetail: (Long) -> Unit,
    onNavigateToSearch: () -> Unit = {},
    onNavigateToConnectPc: () -> Unit = {},
    onNavigateToReminders: () -> Unit = {},
    viewModel: TagsViewModel = viewModel(
        factory = run {
            val app = LocalContext.current.applicationContext as PixelMarksApplication
            TagsViewModel.provideFactory(
                observeTagsUseCase = app.container.observeTagsUseCase,
                createTagUseCase = app.container.createTagUseCase,
                updateTagUseCase = app.container.updateTagUseCase,
                deleteTagUseCase = app.container.deleteTagUseCase,
                privateModeManager = app.container.privateModeManager
            )
        }
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showCreateDialog by remember { mutableStateOf(false) }
    var editingTag by remember { mutableStateOf<Tag?>(null) }
    var deletingTag by remember { mutableStateOf<Tag?>(null) }
    var selectedSortFilter by remember { mutableStateOf("Most Used") }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    if (showCreateDialog) {
        CreateEditTagDialog(
            onDismiss = { showCreateDialog = false },
            onSave = { name, color ->
                viewModel.createTag(
                    name = name,
                    color = color,
                    onSuccess = { showCreateDialog = false }
                )
            }
        )
    }

    editingTag?.let { tag ->
        CreateEditTagDialog(
            initialTag = tag,
            onDismiss = { editingTag = null },
            onSave = { name, color ->
                viewModel.updateTag(
                    tagId = tag.id,
                    newName = name,
                    color = color,
                    onSuccess = { editingTag = null }
                )
            }
        )
    }

    deletingTag?.let { tag ->
        DeleteTagConfirmDialog(
            tagName = tag.name,
            onDismiss = { deletingTag = null },
            onConfirm = {
                viewModel.deleteTag(tag.id)
                deletingTag = null
            }
        )
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
                onClick = { showCreateDialog = true },
                containerColor = TerracottaAccent,
                contentColor = ArchivalSurfaceContainerLowest,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New tag",
                    modifier = Modifier.size(26.dp)
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
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (uiState.tags.isEmpty()) {
                // Empty state with archival folio styling
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = Spacing.medium),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Label,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "No Tags Registered",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Assemble topic tags to categorize, cross-reference, and curate your archival records.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { showCreateDialog = true },
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text("Register First Tag")
                            }
                        }
                    }
                }
            } else {
                // Filtered & sorted tags
                val sortedTags = remember(uiState.tags, uiState.bookmarkCounts, selectedSortFilter) {
                    when (selectedSortFilter) {
                        "Most Used", "By Volume" -> uiState.tags.sortedWith(
                            compareByDescending<Tag> { uiState.bookmarkCounts[it.id] ?: 0 }
                                .thenBy { it.name.lowercase() }
                        )
                        "Recently Added" -> uiState.tags.sortedByDescending { it.id }
                        "Alphabetical (A–Z)" -> uiState.tags.sortedBy { it.name.lowercase() }
                        else -> uiState.tags.sortedWith(
                            compareByDescending<Tag> { uiState.bookmarkCounts[it.id] ?: 0 }
                                .thenBy { it.name.lowercase() }
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Editorial Header
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        ArchivalSubHeader()
                    }

                    // Filter & Sorting Pills
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Most Used", "Recently Added", "Alphabetical (A–Z)").forEach { filter ->
                                val isSelected = selectedSortFilter == filter
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.secondary
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable { selectedSortFilter = filter }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = filter.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSecondary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    // Total tags count
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${uiState.tags.size} tags",
                                style = MaterialTheme.typography.labelSmall,
                                color = ArchivalTextSecondary
                            )
                        }
                    }

                    if (selectedSortFilter == "Alphabetical (A–Z)") {
                        val alphabeticalGroups = sortedTags.groupBy { it.name.firstOrNull()?.uppercaseChar() ?: '#' }
                            .toSortedMap()

                        alphabeticalGroups.forEach { (char, tagsInGroup) ->
                            item(key = "header_$char") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp, bottom = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = char.toString(),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(1.dp)
                                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    )
                                }
                            }

                            items(tagsInGroup, key = { it.id }) { tag ->
                                val count = uiState.bookmarkCounts[tag.id] ?: 0
                                ArchivalTagListItem(
                                    tag = tag,
                                    bookmarkCount = count,
                                    onClick = { onNavigateToTagDetail(tag.id) },
                                    onEdit = { editingTag = tag },
                                    onDelete = { deletingTag = tag }
                                )
                            }
                        }
                    } else {
                        items(sortedTags, key = { it.id }) { tag ->
                            val count = uiState.bookmarkCounts[tag.id] ?: 0
                            ArchivalTagListItem(
                                tag = tag,
                                bookmarkCount = count,
                                onClick = { onNavigateToTagDetail(tag.id) },
                                onEdit = { editingTag = tag },
                                onDelete = { deletingTag = tag }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(84.dp))
                    }
                }
            }


        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArchivalTagListItem(
    tag: Tag,
    bookmarkCount: Int,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = ArchivalSurfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(1.dp, ArchivalSurfaceContainerHighest),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { menuExpanded = true }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Prominent '#' icon on the left matching the image
            Icon(
                imageVector = Icons.Default.Tag,
                contentDescription = null,
                tint = ArchivalTextPrimary,
                modifier = Modifier.size(24.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = tag.name.removePrefix("#"),
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = FontFamily.Monospace,
                    color = ArchivalTextPrimary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$bookmarkCount bookmarks",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = ArchivalTextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box {
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(ArchivalSurfaceContainerHigh)
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit Tag", color = ArchivalTextPrimary) },
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete Tag", color = ArchivalError) },
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

@Preview(showBackground = true)
@Composable
private fun TagsScreenPreview() {
    PixelMarksTheme {
        TagsScreen(onNavigateToTagDetail = {})
    }
}

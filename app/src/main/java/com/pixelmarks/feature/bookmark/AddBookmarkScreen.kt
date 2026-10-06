package com.pixelmarks.feature.bookmark

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedCard
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.pixelmarks.feature.collection.SelectCollectionDialog
import com.pixelmarks.feature.collection.getIconVector
import com.pixelmarks.feature.collection.parseHexColor
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pixelmarks.app.PixelMarksApplication
import com.pixelmarks.ui.components.PixelMarksScreenScaffold
import com.pixelmarks.ui.theme.Spacing

@Composable
fun AddBookmarkScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddBookmarkViewModel = viewModel(
        factory = run {
            val app = LocalContext.current.applicationContext as PixelMarksApplication
            AddBookmarkViewModel.provideFactory(
                addBookmarkUseCase = app.container.addBookmarkUseCase,
                observeCollectionsUseCase = app.container.observeCollectionsUseCase
            )
        }
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCollectionPicker by remember { mutableStateOf(false) }

    // When save succeeds, return to previous screen
    LaunchedEffect(uiState.isSavedSuccessfully) {
        if (uiState.isSavedSuccessfully) {
            onNavigateBack()
        }
    }

    if (showCollectionPicker) {
        SelectCollectionDialog(
            title = "Choose Collection",
            confirmButtonText = "Select",
            currentCollectionId = uiState.selectedCollectionId,
            availableCollections = uiState.availableCollections,
            onDismiss = { showCollectionPicker = false },
            onSelectCollection = { colId ->
                viewModel.onCollectionSelected(colId)
                showCollectionPicker = false
            }
        )
    }

    // Duplicate alert dialog
    uiState.duplicateBookmark?.let { existing ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissDuplicateDialog() },
            title = {
                Text(
                    text = "Bookmark Already Exists",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
                    Text(
                        text = "A bookmark with this URL is already saved:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = existing.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = existing.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.saveAnyway() }
                ) {
                    Text("Save Anyway")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissDuplicateDialog() }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    PixelMarksScreenScaffold(
        title = "Add Bookmark",
        onNavigateBack = onNavigateBack
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(Spacing.medium)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            Text(
                text = "Save New Link",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            // URL Input
            OutlinedTextField(
                value = uiState.url,
                onValueChange = viewModel::onUrlChanged,
                label = { Text("Web URL") },
                placeholder = { Text("https://example.com or example.com") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null
                    )
                },
                trailingIcon = {
                    if (uiState.url.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onUrlChanged("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear URL"
                            )
                        }
                    }
                },
                isError = uiState.errorMessage != null,
                supportingText = {
                    if (uiState.errorMessage != null) {
                        Text(
                            text = uiState.errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text("Protocol defaults to https:// if omitted")
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Title Input (Optional override)
            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::onTitleChanged,
                label = { Text("Title (Optional)") },
                placeholder = { Text("Auto-derived from domain if left blank") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Title,
                        contentDescription = null
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { viewModel.saveBookmark() }
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Collection Selector (Optional)
            val selectedCol = uiState.availableCollections.firstOrNull { it.id == uiState.selectedCollectionId }
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCollectionPicker = true },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.medium, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
                ) {
                    if (selectedCol != null) {
                        val colColor = parseHexColor(selectedCol.color)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(colColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getIconVector(selectedCol.icon),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Collection",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = selectedCol.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = { viewModel.onCollectionSelected(null) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear collection selection",
                                tint = MaterialTheme.colorScheme.outline
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Collection (Optional)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = "None (Unsorted)",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isLoading
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = viewModel::saveBookmark,
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isLoading && uiState.url.isNotBlank()
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Save Bookmark")
                    }
                }
            }
        }
    }
}

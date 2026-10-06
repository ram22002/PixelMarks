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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pixelmarks.app.PixelMarksApplication
import com.pixelmarks.ui.components.PixelMarksScreenScaffold
import com.pixelmarks.ui.theme.Spacing

@Composable
fun AddBookmarkScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddBookmarkViewModel = viewModel(
        factory = AddBookmarkViewModel.provideFactory(
            (LocalContext.current.applicationContext as PixelMarksApplication).container.addBookmarkUseCase
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    // When save succeeds, return to previous screen
    LaunchedEffect(uiState.isSavedSuccessfully) {
        if (uiState.isSavedSuccessfully) {
            onNavigateBack()
        }
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

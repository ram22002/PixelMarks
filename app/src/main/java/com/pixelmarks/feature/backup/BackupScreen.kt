package com.pixelmarks.feature.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pixelmarks.app.PixelMarksApplication
import com.pixelmarks.core.backup.model.RestoreMode
import com.pixelmarks.ui.components.PixelMarksScreenScaffold
import com.pixelmarks.ui.theme.Spacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupScreen(
    onNavigateBack: () -> Unit,
    viewModel: BackupViewModel = viewModel(
        factory = run {
            val app = LocalContext.current.applicationContext as PixelMarksApplication
            BackupViewModel.provideFactory(
                createBackupUseCase = app.container.createBackupUseCase,
                inspectBackupUseCase = app.container.inspectBackupUseCase,
                restoreBackupUseCase = app.container.restoreBackupUseCase,
                observeLatestBackupUseCase = app.container.observeLatestBackupUseCase
            )
        }
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val latestBackup by viewModel.latestBackup.collectAsState()
    val context = LocalContext.current
    val contentResolver = context.contentResolver
    val snackbarHostState = remember { SnackbarHostState() }

    var showCreatePasswordDialog by remember { mutableStateOf(false) }
    var passwordForExport by remember { mutableStateOf("") }
    var confirmPasswordForExport by remember { mutableStateOf("") }
    var exportPasswordVisible by remember { mutableStateOf(false) }
    var exportPasswordError by remember { mutableStateOf<String?>(null) }

    var passwordForImport by remember { mutableStateOf("") }
    var importPasswordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    // SAF Document Creators and Pickers
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        if (uri != null && passwordForExport.isNotBlank()) {
            viewModel.createBackup(passwordForExport.toCharArray(), uri, contentResolver)
            passwordForExport = ""
            confirmPasswordForExport = ""
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onFileSelectedForRestore(uri, contentResolver)
        }
    }

    PixelMarksScreenScaffold(
        title = "Backup & Restore",
        onNavigateBack = onNavigateBack,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Spacing.medium)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.medium)
            ) {
                // Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = MaterialTheme.shapes.large
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.medium),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Encrypted Local Backup",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Local-first, AES-256-GCM authenticated backup. Bookmarks, collections, tags, reminders, and preferences.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                // Status Card
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
                            .padding(Spacing.medium),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Backup Status",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        val lastBackupText = if (latestBackup != null) {
                            val sdf = SimpleDateFormat("MMM d, yyyy, h:mm a", Locale.getDefault())
                            val dateStr = sdf.format(Date(latestBackup!!.createdAt))
                            val kb = (latestBackup!!.sizeBytes / 1024.0).let { "%.1f KB".format(it) }
                            "$dateStr ($kb)"
                        } else {
                            "Never"
                        }
                        Text(
                            text = "Last backup: $lastBackupText",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Actions Section
                Text(
                    text = "Actions",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )

                // Create Backup Button
                Button(
                    onClick = {
                        passwordForExport = ""
                        confirmPasswordForExport = ""
                        exportPasswordError = null
                        showCreatePasswordDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading
                ) {
                    Icon(imageVector = Icons.Default.FileUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create Encrypted Backup")
                }

                // Restore Backup Button
                OutlinedButton(
                    onClick = {
                        openDocumentLauncher.launch(arrayOf("*/*"))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading
                ) {
                    Icon(imageVector = Icons.Default.FileDownload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Restore Backup")
                }

                Spacer(modifier = Modifier.height(Spacing.medium))

                // Security Note
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = MaterialTheme.shapes.small
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.medium),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Security & Privacy",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Backups are protected with a unique 16-byte random salt, 120,000 PBKDF2 iterations, and AES-256-GCM. Your password is never stored or transmitted.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Loading overlay
            if (uiState.isLoading) {
                AlertDialog(
                    onDismissRequest = {},
                    confirmButton = {},
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp))
                            Text(
                                text = uiState.loadingMessage ?: "Processing...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                )
            }
        }
    }

    // 1. Create Backup Dialog
    if (showCreatePasswordDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePasswordDialog = false },
            title = { Text("Create Encrypted Backup") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
                    Text(
                        text = "Set an encryption password for your .pmb backup file. You will need this password to restore your data.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = passwordForExport,
                        onValueChange = {
                            passwordForExport = it
                            exportPasswordError = null
                        },
                        label = { Text("Backup Password") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = if (exportPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { exportPasswordVisible = !exportPasswordVisible }) {
                                Icon(
                                    imageVector = if (exportPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        isError = exportPasswordError != null,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = confirmPasswordForExport,
                        onValueChange = {
                            confirmPasswordForExport = it
                            exportPasswordError = null
                        },
                        label = { Text("Confirm Password") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = if (exportPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        isError = exportPasswordError != null,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (exportPasswordError != null) {
                        Text(
                            text = exportPasswordError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Text(
                        text = "Warning: PixelMarks cannot recover your data if you forget this password.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (passwordForExport.length < 8) {
                            exportPasswordError = "Password must be at least 8 characters"
                        } else if (passwordForExport != confirmPasswordForExport) {
                            exportPasswordError = "Passwords do not match"
                        } else {
                            showCreatePasswordDialog = false
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            val defaultName = "PixelMarks_Backup_${sdf.format(Date())}.pmb"
                            createDocumentLauncher.launch(defaultName)
                        }
                    }
                ) {
                    Text("Export File")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePasswordDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 2. Unlock & Decrypt Password Dialog
    if (uiState.showPasswordPrompt) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissPasswordPrompt() },
            title = { Text("Enter Backup Password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
                    val dateStr = uiState.pendingHeaderInfo?.let {
                        val sdf = SimpleDateFormat("MMM d, yyyy, h:mm a", Locale.getDefault())
                        sdf.format(Date(it.createdAt))
                    } ?: "Unknown"
                    Text(
                        text = "Backup created on: $dateStr\nEnter the password used to encrypt this file:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = passwordForImport,
                        onValueChange = { passwordForImport = it },
                        label = { Text("Password") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = if (importPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { importPasswordVisible = !importPasswordVisible }) {
                                Icon(
                                    imageVector = if (importPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (passwordForImport.isNotBlank()) {
                            val pass = passwordForImport.toCharArray()
                            passwordForImport = ""
                            viewModel.unlockAndPrepareRestore(pass, contentResolver)
                        }
                    }
                ) {
                    Text("Unlock")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissPasswordPrompt() }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 3. Restore Mode Selection Dialog
    if (uiState.showModeSelectionPrompt) {
        val payload = uiState.pendingRestorePayload
        AlertDialog(
            onDismissRequest = { viewModel.dismissModeSelectionPrompt() },
            title = { Text("Restore Data") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
                    if (payload != null) {
                        Text(
                            text = "Found ${payload.bookmarks.size} bookmarks, ${payload.collections.size} collections, ${payload.tags.size} tags, and ${payload.reminders.size} reminders.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(
                        text = "Choose how to apply this backup:",
                        style = MaterialTheme.typography.bodySmall
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // Option A: Replace Everything
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectRestoreMode(RestoreMode.REPLACE) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = uiState.selectedRestoreMode == RestoreMode.REPLACE,
                            onClick = { viewModel.selectRestoreMode(RestoreMode.REPLACE) }
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text("Replace Everything", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text("Wipe current local data and replace completely with backup.", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    // Option B: Merge
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectRestoreMode(RestoreMode.MERGE) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = uiState.selectedRestoreMode == RestoreMode.MERGE,
                            onClick = { viewModel.selectRestoreMode(RestoreMode.MERGE) }
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text("Merge", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text("Keep current items and add non-duplicate bookmarks.", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    // Option C: Import as New
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectRestoreMode(RestoreMode.IMPORT_AS_NEW) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = uiState.selectedRestoreMode == RestoreMode.IMPORT_AS_NEW,
                            onClick = { viewModel.selectRestoreMode(RestoreMode.IMPORT_AS_NEW) }
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text("Import as New", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text("Import all items as new entries with fresh IDs.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.confirmAndExecuteRestore() }) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissModeSelectionPrompt() }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 4. Success Dialog (Export or Restore)
    if (uiState.exportSuccess != null) {
        val result = uiState.exportSuccess!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissSuccessDialog() },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Backup Created") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Encrypted file exported successfully.")
                    Text("Size: ${(result.fileSizeBytes / 1024.0).let { "%.1f KB".format(it) }}")
                    Text("Bookmarks: ${result.bookmarkCount}")
                    Text("Collections: ${result.collectionCount}")
                    Text("Tags: ${result.tagCount}")
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.dismissSuccessDialog() }) {
                    Text("Done")
                }
            }
        )
    }

    if (uiState.restoreSuccess != null) {
        val result = uiState.restoreSuccess!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissSuccessDialog() },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Restore Complete") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Mode: ${result.mode.name}")
                    Text("Bookmarks restored: ${result.bookmarksRestored}")
                    Text("Collections restored: ${result.collectionsRestored}")
                    Text("Tags restored: ${result.tagsRestored}")
                    Text("Reminders restored: ${result.remindersRestored}")
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.dismissSuccessDialog() }) {
                    Text("Done")
                }
            }
        )
    }
}

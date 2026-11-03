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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.Switch
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
import com.pixelmarks.core.backup.model.AutoBackupFrequency
import com.pixelmarks.core.backup.model.DriveBackupFile
import com.pixelmarks.core.backup.model.DriveConnectionState
import com.pixelmarks.core.backup.model.RestoreMode
import com.pixelmarks.ui.components.PixelMarksScreenScaffold
import com.pixelmarks.ui.theme.Spacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val GOOGLE_DRIVE_BACKUP_ENABLED = false

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
                observeLatestBackupUseCase = app.container.observeLatestBackupUseCase,
                observeDriveConnectionUseCase = app.container.observeDriveConnectionUseCase,
                connectDriveUseCase = app.container.connectDriveUseCase,
                disconnectDriveUseCase = app.container.disconnectDriveUseCase,
                listDriveBackupsUseCase = app.container.listDriveBackupsUseCase,
                uploadDriveBackupUseCase = app.container.uploadDriveBackupUseCase,
                downloadAndInspectDriveBackupUseCase = app.container.downloadAndInspectDriveBackupUseCase,
                restoreDriveBackupUseCase = app.container.restoreDriveBackupUseCase,
                deleteDriveBackupUseCase = app.container.deleteDriveBackupUseCase,
                observeAutoBackupPreferencesUseCase = app.container.observeAutoBackupPreferencesUseCase,
                updateAutoBackupPreferencesUseCase = app.container.updateAutoBackupPreferencesUseCase
            )
        }
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val latestBackup by viewModel.latestBackup.collectAsState()
    val context = LocalContext.current
    val contentResolver = context.contentResolver
    val snackbarHostState = remember { SnackbarHostState() }

    // Local backup password states
    var showCreatePasswordDialog by remember { mutableStateOf(false) }
    var passwordForExport by remember { mutableStateOf("") }
    var confirmPasswordForExport by remember { mutableStateOf("") }
    var exportPasswordVisible by remember { mutableStateOf(false) }
    var exportPasswordError by remember { mutableStateOf<String?>(null) }

    var passwordForImport by remember { mutableStateOf("") }
    var importPasswordVisible by remember { mutableStateOf(false) }

    // Google Drive backup password states
    var showDriveUploadPasswordDialog by remember { mutableStateOf(false) }
    var passwordForDriveUpload by remember { mutableStateOf("") }
    var confirmPasswordForDriveUpload by remember { mutableStateOf("") }
    var driveUploadPasswordVisible by remember { mutableStateOf(false) }
    var driveUploadPasswordError by remember { mutableStateOf<String?>(null) }

    var passwordForDriveRestore by remember { mutableStateOf("") }
    var driveRestorePasswordVisible by remember { mutableStateOf(false) }

    // Auto-backup password setup states
    var autoBackupPassword by remember { mutableStateOf("") }
    var confirmAutoBackupPassword by remember { mutableStateOf("") }
    var autoBackupPasswordVisible by remember { mutableStateOf(false) }
    var autoBackupPasswordError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    // Google Sign-In Activity Launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.handleGoogleSignInResult(result.data)
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
                                text = "Encrypted Backup & Restore",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Local-first, AES-256-GCM authenticated backups. Bookmarks, collections, tags, reminders, and preferences.",
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
                            "$dateStr ($kb) [${latestBackup!!.type}]"
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

                // -------------------------------------------------------------
                // SECTION 1: Local Backup & Restore
                // -------------------------------------------------------------
                Text(
                    text = "Local Storage",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )

                // Create Local Backup Button
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
                    Text("Create Encrypted Local Backup")
                }

                // Restore Local Backup Button
                OutlinedButton(
                    onClick = {
                        openDocumentLauncher.launch(arrayOf("*/*"))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading
                ) {
                    Icon(imageVector = Icons.Default.FileDownload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Restore from Local Storage")
                }

                Spacer(modifier = Modifier.height(Spacing.small))

                if (GOOGLE_DRIVE_BACKUP_ENABLED) {
                    // -------------------------------------------------------------
                    // SECTION 2: Google Drive Cloud Backup
                    // -------------------------------------------------------------
                    Text(
                        text = "Google Drive (Cloud)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )

                    // Google Drive Connection Card
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
                            verticalArrangement = Arrangement.spacedBy(Spacing.small)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (uiState.driveConnectionState == DriveConnectionState.CONNECTED) {
                                            Icons.Default.CloudDone
                                        } else {
                                            Icons.Default.CloudOff
                                        },
                                        contentDescription = null,
                                        tint = if (uiState.driveConnectionState == DriveConnectionState.CONNECTED) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                    Column {
                                        Text(
                                            text = "Google Drive",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        val statusText = when (uiState.driveConnectionState) {
                                            DriveConnectionState.CONNECTED -> uiState.driveConnectedAccount?.email ?: "Connected"
                                            DriveConnectionState.CONNECTING -> "Connecting..."
                                            DriveConnectionState.AUTH_EXPIRED -> "Authorization Expired"
                                            DriveConnectionState.DISCONNECTED -> "Not Connected"
                                        }
                                        Text(
                                            text = statusText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (uiState.driveConnectionState == DriveConnectionState.CONNECTED) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                    }
                                }

                                if (uiState.driveConnectionState == DriveConnectionState.CONNECTED) {
                                    OutlinedButton(
                                        onClick = { viewModel.disconnectGoogleDrive() },
                                        enabled = !uiState.isLoading,
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                        )
                                    ) {
                                        Text("Disconnect")
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            val intent = viewModel.getGoogleSignInIntent()
                                            if (intent != null) {
                                                googleSignInLauncher.launch(intent)
                                            }
                                        },
                                        enabled = !uiState.isLoading && uiState.driveConnectionState != DriveConnectionState.CONNECTING
                                    ) {
                                        Icon(imageVector = Icons.Default.AccountCircle, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Connect")
                                    }
                                }
                            }

                            Text(
                                text = "Backups are stored privately in your Google Drive App Data folder. Other apps cannot access them, and files are always AES-256-GCM encrypted before upload.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }

                    // If Google Drive is Connected: Show Backup Actions & Cloud Backups List
                    if (uiState.driveConnectionState == DriveConnectionState.CONNECTED) {

                        // Back up to Drive Button
                        Button(
                            onClick = {
                                passwordForDriveUpload = ""
                                confirmPasswordForDriveUpload = ""
                                driveUploadPasswordError = null
                                showDriveUploadPasswordDialog = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isLoading
                        ) {
                            Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Back Up to Google Drive")
                        }

                        // Cloud Backups List Card
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
                                verticalArrangement = Arrangement.spacedBy(Spacing.small)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Cloud Backups",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    IconButton(
                                        onClick = { viewModel.refreshDriveBackups() },
                                        enabled = !uiState.isDriveBackupsLoading
                                    ) {
                                        if (uiState.isDriveBackupsLoading) {
                                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                        } else {
                                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh")
                                        }
                                    }
                                }

                                if (uiState.driveBackups.isEmpty() && !uiState.isDriveBackupsLoading) {
                                    Text(
                                        text = "No backups found in your Drive App Data folder.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    uiState.driveBackups.forEach { backupFile ->
                                        DriveBackupItem(
                                            backupFile = backupFile,
                                            onRestore = {
                                                viewModel.onDriveBackupSelectedForRestore(backupFile, context.cacheDir)
                                            },
                                            onDelete = {
                                                viewModel.requestDeleteDriveBackup(backupFile)
                                            }
                                        )
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                    }
                                }
                            }
                        }

                        // Automatic Cloud Backup Settings Card
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
                                verticalArrangement = Arrangement.spacedBy(Spacing.small)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Automatic Cloud Backup",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Periodically back up your encrypted data in the background.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = uiState.autoBackupPreferences.enabled,
                                        onCheckedChange = { isChecked ->
                                            if (isChecked) {
                                                autoBackupPassword = ""
                                                confirmAutoBackupPassword = ""
                                                autoBackupPasswordError = null
                                                viewModel.promptAutoBackupPassword()
                                            } else {
                                                viewModel.setAutoBackupEnabled(false)
                                            }
                                        }
                                    )
                                }

                                if (uiState.autoBackupPreferences.enabled) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                    // Frequency Selection
                                    Text(
                                        text = "Backup Frequency",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
                                    ) {
                                        AutoBackupFrequency.entries.forEach { freq ->
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.clickable {
                                                    viewModel.setAutoBackupFrequency(freq)
                                                }
                                            ) {
                                                RadioButton(
                                                    selected = uiState.autoBackupPreferences.frequency == freq,
                                                    onClick = { viewModel.setAutoBackupFrequency(freq) }
                                                )
                                                Text(
                                                    text = freq.displayName,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                        }
                                    }

                                    // Wi-Fi Only Preference
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Default.Wifi, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            Text(
                                                text = "Wi-Fi Only",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                        Switch(
                                            checked = uiState.autoBackupPreferences.wifiOnly,
                                            onCheckedChange = { viewModel.setWifiOnly(it) }
                                        )
                                    }
                                }
                            }
                        }
                    }
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
                            text = "Backups are protected with a unique 16-byte random salt, 120,000 PBKDF2 iterations, and AES-256-GCM. Passwords and recovery keys are never transmitted to the cloud or saved unencrypted.",
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

    // -------------------------------------------------------------
    // DIALOGS
    // -------------------------------------------------------------

    // 1. Create Local Backup Dialog
    if (showCreatePasswordDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePasswordDialog = false },
            title = { Text("Create Encrypted Local Backup") },
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

    // 2. Drive Upload Password Dialog
    if (showDriveUploadPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showDriveUploadPasswordDialog = false },
            title = { Text("Encrypt & Upload to Drive") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
                    Text(
                        text = "Enter an encryption password. The backup will be encrypted locally before uploading to your Google Drive App Data folder.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = passwordForDriveUpload,
                        onValueChange = {
                            passwordForDriveUpload = it
                            driveUploadPasswordError = null
                        },
                        label = { Text("Backup Password") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = if (driveUploadPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { driveUploadPasswordVisible = !driveUploadPasswordVisible }) {
                                Icon(
                                    imageVector = if (driveUploadPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        isError = driveUploadPasswordError != null,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = confirmPasswordForDriveUpload,
                        onValueChange = {
                            confirmPasswordForDriveUpload = it
                            driveUploadPasswordError = null
                        },
                        label = { Text("Confirm Password") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = if (driveUploadPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        isError = driveUploadPasswordError != null,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (driveUploadPasswordError != null) {
                        Text(
                            text = driveUploadPasswordError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (passwordForDriveUpload.length < 8) {
                            driveUploadPasswordError = "Password must be at least 8 characters"
                        } else if (passwordForDriveUpload != confirmPasswordForDriveUpload) {
                            driveUploadPasswordError = "Passwords do not match"
                        } else {
                            showDriveUploadPasswordDialog = false
                            val pass = passwordForDriveUpload.toCharArray()
                            passwordForDriveUpload = ""
                            confirmPasswordForDriveUpload = ""
                            viewModel.uploadBackupToDrive(pass)
                        }
                    }
                ) {
                    Text("Upload")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDriveUploadPasswordDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 3. Drive Restore Password Dialog
    if (uiState.showDrivePasswordPrompt) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDriveRestore() },
            title = { Text("Enter Cloud Backup Password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
                    val dateStr = uiState.pendingHeaderInfo?.let {
                        val sdf = SimpleDateFormat("MMM d, yyyy, h:mm a", Locale.getDefault())
                        sdf.format(Date(it.createdAt))
                    } ?: "Unknown"
                    Text(
                        text = "Cloud backup: ${uiState.pendingDriveRestoreFile?.name ?: ""}\nCreated on: $dateStr\nEnter the encryption password:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = passwordForDriveRestore,
                        onValueChange = { passwordForDriveRestore = it },
                        label = { Text("Password") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = if (driveRestorePasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { driveRestorePasswordVisible = !driveRestorePasswordVisible }) {
                                Icon(
                                    imageVector = if (driveRestorePasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
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
                        if (passwordForDriveRestore.isNotBlank()) {
                            val pass = passwordForDriveRestore.toCharArray()
                            passwordForDriveRestore = ""
                            viewModel.unlockAndPrepareDriveRestore(pass)
                        }
                    }
                ) {
                    Text("Unlock")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDriveRestore() }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 4. Drive Mode Selection Dialog
    if (uiState.showDriveModeSelectionPrompt) {
        val payload = uiState.pendingRestorePayload
        AlertDialog(
            onDismissRequest = { viewModel.dismissDriveRestore() },
            title = { Text("Restore Cloud Data") },
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
                        text = "Choose how to apply this cloud backup:",
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
                Button(onClick = { viewModel.confirmAndExecuteDriveRestore() }) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDriveRestore() }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 5. Delete Cloud Backup Confirmation Dialog
    uiState.showDeleteDriveBackupConfirm?.let { fileToDelete ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteDriveBackup() },
            title = { Text("Delete Cloud Backup?") },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete \"${fileToDelete.name}\" from your Google Drive? This cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmDeleteDriveBackup() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteDriveBackup() }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 6. Setup Auto-Backup Password Dialog
    if (uiState.showAutoBackupPasswordDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissAutoBackupPassword() },
            title = { Text("Set Auto-Backup Password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
                    Text(
                        text = "To enable automatic backups in the background, set an encryption password. This password will be securely protected in Android KeyStore hardware security.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = autoBackupPassword,
                        onValueChange = {
                            autoBackupPassword = it
                            autoBackupPasswordError = null
                        },
                        label = { Text("Password") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = if (autoBackupPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { autoBackupPasswordVisible = !autoBackupPasswordVisible }) {
                                Icon(
                                    imageVector = if (autoBackupPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        isError = autoBackupPasswordError != null,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = confirmAutoBackupPassword,
                        onValueChange = {
                            confirmAutoBackupPassword = it
                            autoBackupPasswordError = null
                        },
                        label = { Text("Confirm Password") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = if (autoBackupPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        isError = autoBackupPasswordError != null,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (autoBackupPasswordError != null) {
                        Text(
                            text = autoBackupPasswordError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (autoBackupPassword.length < 8) {
                            autoBackupPasswordError = "Password must be at least 8 characters"
                        } else if (autoBackupPassword != confirmAutoBackupPassword) {
                            autoBackupPasswordError = "Passwords do not match"
                        } else {
                            val pass = autoBackupPassword.toCharArray()
                            autoBackupPassword = ""
                            confirmAutoBackupPassword = ""
                            viewModel.setAutoBackupEnabled(true, pass)
                        }
                    }
                ) {
                    Text("Save & Enable")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissAutoBackupPassword() }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 7. Local Unlock & Decrypt Dialog
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

    // 8. Local Restore Mode Selection Dialog
    if (uiState.showModeSelectionPrompt) {
        val payload = uiState.pendingRestorePayload
        AlertDialog(
            onDismissRequest = { viewModel.dismissModeSelectionPrompt() },
            title = { Text("Restore Local Data") },
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

    // 9. Success Dialogs
    if (uiState.exportSuccess != null) {
        val result = uiState.exportSuccess!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissSuccessDialog() },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Local Backup Created") },
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

    if (uiState.cloudUploadSuccess != null) {
        val result = uiState.cloudUploadSuccess!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissSuccessDialog() },
            icon = { Icon(Icons.Default.CloudDone, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Cloud Backup Uploaded") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("File: ${result.name}")
                    Text("Size: ${(result.sizeBytes / 1024.0).let { "%.1f KB".format(it) }}")
                    Text("Stored in private Google Drive App Data folder.")
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

@Composable
private fun DriveBackupItem(
    backupFile: DriveBackupFile,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = backupFile.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            val sdf = SimpleDateFormat("MMM d, yyyy, h:mm a", Locale.getDefault())
            val dateStr = sdf.format(Date(backupFile.createdAt))
            val sizeStr = (backupFile.sizeBytes / 1024.0).let { "%.1f KB".format(it) }
            Text(
                text = "$dateStr • $sizeStr",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(onClick = onRestore) {
                Icon(
                    imageVector = Icons.Default.Restore,
                    contentDescription = "Restore",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

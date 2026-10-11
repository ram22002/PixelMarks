package com.pixelmarks.feature.importexport

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pixelmarks.app.PixelMarksApplication
import com.pixelmarks.core.model.html.HtmlExportScope
import com.pixelmarks.core.model.html.HtmlImportCollectionTarget
import com.pixelmarks.core.model.html.HtmlImportDuplicatePolicy
import com.pixelmarks.ui.theme.Spacing
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HtmlImportExportScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as PixelMarksApplication
    val container = app.container

    val viewModel: HtmlImportExportViewModel = viewModel(
        factory = HtmlImportExportViewModel.Factory(
            parseBookmarksHtmlUseCase = container.parseBookmarksHtmlUseCase,
            getHtmlImportPreviewUseCase = container.getHtmlImportPreviewUseCase,
            importBookmarksHtmlUseCase = container.importBookmarksHtmlUseCase,
            exportBookmarksHtmlUseCase = container.exportBookmarksHtmlUseCase,
            collectionRepository = container.collectionRepository,
            privateModeManager = container.privateModeManager
        )
    )

    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showPrivateExportWarningDialog by remember { mutableStateOf(false) }
    var pendingExportFileName by remember { mutableStateOf("") }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onFileSelected(uri, context.contentResolver)
        }
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/html")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.exportToUri(uri, context.contentResolver)
        }
    }

    fun initiateExport() {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US)
        val timestamp = dateFormat.format(Date())
        val prefix = if (uiState.isPrivate) "pixelmarks_private_bookmarks" else "pixelmarks_bookmarks"
        val fileName = "${prefix}_$timestamp.html"

        if (uiState.isPrivate) {
            pendingExportFileName = fileName
            showPrivateExportWarningDialog = true
        } else {
            createDocumentLauncher.launch(fileName)
        }
    }

    LaunchedEffect(uiState.exportSuccessMessage) {
        uiState.exportSuccessMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.exportErrorMessage) {
        uiState.exportErrorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.importErrorMessage) {
        uiState.importErrorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text("Import & Export (HTML)") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            // Privacy Boundary Indicator Card
            PrivacyBoundaryCard(isPrivate = uiState.isPrivate)

            // Export Bookmarks Card
            ExportSectionCard(
                uiState = uiState,
                onExportScopeChange = { viewModel.setExportScope(it) },
                onExportClick = { initiateExport() }
            )

            // Import Bookmarks Card
            ImportSectionCard(
                uiState = uiState,
                onSelectFileClick = {
                    openDocumentLauncher.launch(
                        arrayOf("text/html", "application/xhtml+xml", "*/*")
                    )
                },
                onDuplicatePolicyChange = { viewModel.updateDuplicatePolicy(it) },
                onCollectionTargetChange = { viewModel.updateCollectionTarget(it) },
                onConfirmImport = { viewModel.confirmImport() },
                onCancelImport = { viewModel.cancelImport() }
            )

            Spacer(modifier = Modifier.height(Spacing.large))
        }

        // Private Export Confirmation Dialog
        if (showPrivateExportWarningDialog) {
            AlertDialog(
                onDismissRequest = { showPrivateExportWarningDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = { Text("Export Private Bookmarks?") },
                text = {
                    Text(
                        "Netscape HTML bookmark files are unencrypted and can be opened in any web browser. " +
                            "Private bookmarks exported to this file will not be password-protected."
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showPrivateExportWarningDialog = false
                            createDocumentLauncher.launch(pendingExportFileName)
                        }
                    ) {
                        Text("Export Anyway")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPrivateExportWarningDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Import Summary Dialog
        uiState.importSummary?.let { summary ->
            AlertDialog(
                onDismissRequest = { viewModel.dismissSummary() },
                icon = {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = { Text("Import Complete") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
                        Text("• Imported: ${summary.importedCount} bookmarks")
                        if (summary.createdCollectionsCount > 0) {
                            Text("• Created: ${summary.createdCollectionsCount} collections")
                        }
                        if (summary.updatedCount > 0) {
                            Text("• Updated: ${summary.updatedCount} bookmarks")
                        }
                        if (summary.skippedDuplicatesCount > 0) {
                            Text("• Skipped duplicates: ${summary.skippedDuplicatesCount}")
                        }
                        if (summary.invalidCount > 0) {
                            Text("• Skipped invalid: ${summary.invalidCount}")
                        }
                        if (summary.failedCount > 0) {
                            Text("• Failed: ${summary.failedCount}")
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { viewModel.dismissSummary() }) {
                        Text("Done")
                    }
                }
            )
        }
    }
}

@Composable
private fun PrivacyBoundaryCard(isPrivate: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isPrivate) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(Spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isPrivate) Icons.Default.Lock else Icons.Default.Public,
                contentDescription = null,
                tint = if (isPrivate) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(Spacing.medium))
            Column {
                Text(
                    text = if (isPrivate) "Secure Folder Active (Private Mode)" else "Public Mode",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isPrivate) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isPrivate) {
                        "All imports and exports will operate exclusively on your private bookmarks."
                    } else {
                        "All imports and exports will operate exclusively on your public bookmarks."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportSectionCard(
    uiState: HtmlImportExportUiState,
    onExportScopeChange: (HtmlExportScope) -> Unit,
    onExportClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.small)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(Spacing.small))
                Text(
                    text = "Export Bookmarks (HTML)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "Export your bookmarks to standard Netscape HTML for import into Chrome, Firefox, Safari, or Edge.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.extraSmall))

            // Export Scope Radio Options
            Text(
                text = "Export Scope",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExportScopeChange(HtmlExportScope.All) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = uiState.exportScope is HtmlExportScope.All,
                    onClick = { onExportScopeChange(HtmlExportScope.All) }
                )
                Text("All Bookmarks & Collections", style = MaterialTheme.typography.bodyMedium)
            }

            if (uiState.availableCollections.isNotEmpty()) {
                val isSubtreeSelected = uiState.exportScope is HtmlExportScope.CollectionSubtree

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val first = uiState.availableCollections.first()
                            onExportScopeChange(HtmlExportScope.CollectionSubtree(first.id, first.name))
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSubtreeSelected,
                        onClick = {
                            val first = uiState.availableCollections.first()
                            onExportScopeChange(HtmlExportScope.CollectionSubtree(first.id, first.name))
                        }
                    )
                    Text("Specific Collection Subtree", style = MaterialTheme.typography.bodyMedium)
                }

                if (isSubtreeSelected) {
                    val currentSelectedId = (uiState.exportScope as? HtmlExportScope.CollectionSubtree)?.collectionId
                    val currentSelectedName = uiState.availableCollections.find { it.id == currentSelectedId }?.name ?: "Select Collection"

                    var dropdownExpanded by remember { mutableStateOf(false) }

                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded,
                        onExpandedChange = { dropdownExpanded = it },
                        modifier = Modifier.padding(start = Spacing.large)
                    ) {
                        OutlinedTextField(
                            value = currentSelectedName,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyMedium
                        )

                        ExposedDropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            uiState.availableCollections.forEach { col ->
                                DropdownMenuItem(
                                    text = { Text(col.name) },
                                    onClick = {
                                        onExportScopeChange(HtmlExportScope.CollectionSubtree(col.id, col.name))
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.extraSmall))

            Button(
                onClick = onExportClick,
                enabled = !uiState.isExporting,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(Spacing.small))
                    Text("Exporting...")
                } else {
                    Icon(imageVector = Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(Spacing.small))
                    Text("Export as HTML")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportSectionCard(
    uiState: HtmlImportExportUiState,
    onSelectFileClick: () -> Unit,
    onDuplicatePolicyChange: (HtmlImportDuplicatePolicy) -> Unit,
    onCollectionTargetChange: (HtmlImportCollectionTarget) -> Unit,
    onConfirmImport: () -> Unit,
    onCancelImport: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.small)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Upload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(Spacing.small))
                Text(
                    text = "Import Bookmarks (HTML)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "Import bookmarks exported from Chrome, Firefox, Edge, or Safari. Choose duplicate handling and folder preservation before importing.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.extraSmall))

            if (uiState.importPreview == null && !uiState.isParsing && !uiState.isImporting) {
                Button(
                    onClick = onSelectFileClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Upload, contentDescription = null)
                    Spacer(modifier = Modifier.width(Spacing.small))
                    Text("Select HTML Bookmark File")
                }
            }

            if (uiState.isParsing) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(Spacing.medium))
                    Text("Reading and parsing bookmarks...", style = MaterialTheme.typography.bodyMedium)
                }
            }

            if (uiState.isImporting) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.small)
                ) {
                    val progress = uiState.importProgress
                    if (progress != null && progress.second > 0) {
                        Text(
                            text = "Importing ${progress.first} of ${progress.second} bookmarks...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        LinearProgressIndicator(
                            progress = { progress.first.toFloat() / progress.second.toFloat() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text("Committing transaction...", style = MaterialTheme.typography.bodyMedium)
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            // Preview Section
            uiState.importPreview?.let { preview ->
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
                    Text(
                        text = "File Preview",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(Spacing.medium), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("• Total entries found: ${preview.totalFound}", style = MaterialTheme.typography.bodyMedium)
                            Text("• Valid bookmarks: ${preview.validCount}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                            Text("• Existing in library: ${preview.existingDbDuplicateCount}", style = MaterialTheme.typography.bodyMedium)
                            if (preview.fileDuplicateCount > 0) {
                                Text("• Duplicates within file: ${preview.fileDuplicateCount}", style = MaterialTheme.typography.bodySmall)
                            }
                            if (preview.invalidCount > 0) {
                                Text(
                                    "• Invalid/Skipped entries: ${preview.invalidCount}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            if (preview.detectedFolders.isNotEmpty()) {
                                Text("• Folder hierarchy detected: ${preview.detectedFolders.size} folders", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    // Duplicate Policy Selection
                    Text(
                        text = "Duplicate Policy",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDuplicatePolicyChange(HtmlImportDuplicatePolicy.SKIP) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = uiState.importOptions.duplicatePolicy == HtmlImportDuplicatePolicy.SKIP,
                            onClick = { onDuplicatePolicyChange(HtmlImportDuplicatePolicy.SKIP) }
                        )
                        Text("Skip duplicates (Recommended)", style = MaterialTheme.typography.bodyMedium)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDuplicatePolicyChange(HtmlImportDuplicatePolicy.IMPORT_NEW) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = uiState.importOptions.duplicatePolicy == HtmlImportDuplicatePolicy.IMPORT_NEW,
                            onClick = { onDuplicatePolicyChange(HtmlImportDuplicatePolicy.IMPORT_NEW) }
                        )
                        Text("Import as new (Allow duplicates)", style = MaterialTheme.typography.bodyMedium)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDuplicatePolicyChange(HtmlImportDuplicatePolicy.UPDATE) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = uiState.importOptions.duplicatePolicy == HtmlImportDuplicatePolicy.UPDATE,
                            onClick = { onDuplicatePolicyChange(HtmlImportDuplicatePolicy.UPDATE) }
                        )
                        Text("Update existing metadata", style = MaterialTheme.typography.bodyMedium)
                    }

                    // Folder Mapping
                    Text(
                        text = "Folder Mapping",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCollectionTargetChange(HtmlImportCollectionTarget.PreserveFolders) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = uiState.importOptions.collectionTarget is HtmlImportCollectionTarget.PreserveFolders,
                            onClick = { onCollectionTargetChange(HtmlImportCollectionTarget.PreserveFolders) }
                        )
                        Text("Preserve folders as nested collections", style = MaterialTheme.typography.bodyMedium)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCollectionTargetChange(HtmlImportCollectionTarget.RootOnly) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = uiState.importOptions.collectionTarget is HtmlImportCollectionTarget.RootOnly,
                            onClick = { onCollectionTargetChange(HtmlImportCollectionTarget.RootOnly) }
                        )
                        Text("Import all into root (no collection)", style = MaterialTheme.typography.bodyMedium)
                    }

                    if (uiState.availableCollections.isNotEmpty()) {
                        val isExistingTarget = uiState.importOptions.collectionTarget is HtmlImportCollectionTarget.ExistingCollection

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val first = uiState.availableCollections.first()
                                    onCollectionTargetChange(HtmlImportCollectionTarget.ExistingCollection(first.id))
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isExistingTarget,
                                onClick = {
                                    val first = uiState.availableCollections.first()
                                    onCollectionTargetChange(HtmlImportCollectionTarget.ExistingCollection(first.id))
                                }
                            )
                            Text("Import all into specific collection", style = MaterialTheme.typography.bodyMedium)
                        }

                        if (isExistingTarget) {
                            val targetId = (uiState.importOptions.collectionTarget as? HtmlImportCollectionTarget.ExistingCollection)?.collectionId
                            val targetName = uiState.availableCollections.find { it.id == targetId }?.name ?: "Select"

                            var targetDropdownExpanded by remember { mutableStateOf(false) }

                            ExposedDropdownMenuBox(
                                expanded = targetDropdownExpanded,
                                onExpandedChange = { targetDropdownExpanded = it },
                                modifier = Modifier.padding(start = Spacing.large)
                            ) {
                                OutlinedTextField(
                                    value = targetName,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = targetDropdownExpanded) },
                                    modifier = Modifier
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                        .fillMaxWidth(),
                                    textStyle = MaterialTheme.typography.bodyMedium
                                )

                                ExposedDropdownMenu(
                                    expanded = targetDropdownExpanded,
                                    onDismissRequest = { targetDropdownExpanded = false }
                                ) {
                                    uiState.availableCollections.forEach { col ->
                                        DropdownMenuItem(
                                            text = { Text(col.name) },
                                            onClick = {
                                                onCollectionTargetChange(HtmlImportCollectionTarget.ExistingCollection(col.id))
                                                targetDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.small))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
                    ) {
                        OutlinedButton(
                            onClick = onCancelImport,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = onConfirmImport,
                            enabled = preview.validCount > 0,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Confirm Import")
                        }
                    }
                }
            }
        }
    }
}

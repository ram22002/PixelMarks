package com.pixelmarks.feature.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.pixelmarks.core.model.Collection
import com.pixelmarks.ui.theme.Spacing

val COLLECTION_PALETTE = listOf(
    "#3B82F6", // Blue
    "#10B981", // Emerald
    "#8B5CF6", // Purple
    "#F59E0B", // Amber
    "#EC4899", // Pink
    "#06B6D4", // Cyan
    "#EF4444"  // Red
)

val COLLECTION_ICONS = listOf(
    "folder" to Icons.Default.Folder,
    "work" to Icons.Default.Work,
    "home" to Icons.Default.Home,
    "star" to Icons.Default.Star,
    "code" to Icons.Default.Code,
    "school" to Icons.Default.School,
    "bookmark" to Icons.Default.Bookmark
)

fun parseHexColor(hex: String?, fallback: Color = Color(0xFF3B82F6)): Color {
    if (hex.isNullOrBlank()) return fallback
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        fallback
    }
}

fun getIconVector(iconKey: String?): ImageVector {
    return COLLECTION_ICONS.firstOrNull { it.first == iconKey }?.second ?: Icons.Default.Folder
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateEditCollectionDialog(
    initialCollection: Collection? = null,
    defaultParentId: Long? = null,
    availableParents: List<Collection>,
    onDismiss: () -> Unit,
    onSave: (name: String, parentId: Long?, icon: String?, color: String?, isLocked: Boolean, isHidden: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(initialCollection?.name ?: "") }
    var selectedParentId by remember {
        mutableStateOf(initialCollection?.parentId ?: defaultParentId)
    }
    var selectedColor by remember {
        mutableStateOf(initialCollection?.color ?: COLLECTION_PALETTE.first())
    }
    var selectedIcon by remember {
        mutableStateOf(initialCollection?.icon ?: "folder")
    }
    var isLocked by remember {
        mutableStateOf(initialCollection?.isLocked ?: false)
    }
    var isHidden by remember {
        mutableStateOf(initialCollection?.isHidden ?: false)
    }
    var isParentDropdownExpanded by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    // Filter out the current collection itself from available parents
    val validParents = availableParents.filter { initialCollection == null || it.id != initialCollection.id }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialCollection == null) "Create Collection" else "Edit Collection",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.medium)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        validationError = null
                    },
                    label = { Text("Collection Name") },
                    isError = validationError != null,
                    supportingText = validationError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Parent Collection Selector
                ExposedDropdownMenuBox(
                    expanded = isParentDropdownExpanded,
                    onExpandedChange = { isParentDropdownExpanded = it }
                ) {
                    val currentParentName = validParents.firstOrNull { it.id == selectedParentId }?.name
                        ?: "None (Root Collection)"
                    OutlinedTextField(
                        value = currentParentName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Parent Folder") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isParentDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = isParentDropdownExpanded,
                        onDismissRequest = { isParentDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("None (Root Collection)") },
                            onClick = {
                                selectedParentId = null
                                isParentDropdownExpanded = false
                            }
                        )
                        validParents.forEach { parent ->
                            DropdownMenuItem(
                                text = { Text(parent.name) },
                                onClick = {
                                    selectedParentId = parent.id
                                    isParentDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Color Picker Row
                Text("Color", style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    COLLECTION_PALETTE.forEach { hexColor ->
                        val color = parseHexColor(hexColor)
                        val isSelected = selectedColor.equals(hexColor, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 2.5.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = hexColor },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Icon Picker Row
                Text("Icon", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    COLLECTION_ICONS.forEach { (key, vector) ->
                        val isSelected = selectedIcon == key
                        IconButton(
                            onClick = { selectedIcon = key },
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                        ) {
                            Icon(
                                imageVector = vector,
                                contentDescription = key,
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Privacy Section
                androidx.compose.material3.HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                Text(
                    text = "Privacy",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Lock collection",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Require authentication to open",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    androidx.compose.material3.Switch(
                        checked = isLocked,
                        onCheckedChange = { checked ->
                            isLocked = checked
                            if (!checked) isHidden = false
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hide collection",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Hide from normal collection lists and search",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    androidx.compose.material3.Switch(
                        checked = isHidden,
                        onCheckedChange = { checked ->
                            isHidden = checked
                            if (checked) isLocked = true // Hidden collection automatically requires protection
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isEmpty()) {
                        validationError = "Name cannot be empty"
                    } else {
                        onSave(trimmed, selectedParentId, selectedIcon, selectedColor, isLocked, isHidden)
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DeleteCollectionConfirmDialog(
    collectionName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Collection?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
                Text("Are you sure you want to delete \"$collectionName\"?")
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "• Bookmarks inside this collection will NOT be deleted. They will remain safely saved in Unsorted.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "• Any sub-collections inside will also be removed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SelectCollectionDialog(
    title: String = "Select Collection",
    confirmButtonText: String = "Select",
    currentCollectionId: Long?,
    availableCollections: List<Collection>,
    onDismiss: () -> Unit,
    onSelectCollection: (Long?) -> Unit
) {
    var selectedId by remember { mutableStateOf(currentCollectionId) }
    val flatNodes = remember(availableCollections) {
        flattenCollectionHierarchy(availableCollections)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedId = null }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedId == null,
                            onClick = { selectedId = null }
                        )
                        Text(
                            text = "No Collection (Unsorted)",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                items(flatNodes, key = { it.collection.id }) { node ->
                    val col = node.collection
                    val indent = (node.depth * 20).dp
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedId = col.id }
                            .padding(start = indent, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedId == col.id,
                            onClick = { selectedId = col.id }
                        )
                        Icon(
                            imageVector = getIconVector(col.icon),
                            contentDescription = null,
                            tint = parseHexColor(col.color),
                            modifier = Modifier
                                .size(20.dp)
                                .padding(start = 4.dp)
                        )
                        Text(
                            text = col.name,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .padding(start = 8.dp)
                        )
                        if (col.isLocked || col.isHidden) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Private collection",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSelectCollection(selectedId)
                }
            ) {
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun MoveBookmarkDialog(
    currentCollectionId: Long?,
    availableCollections: List<Collection>,
    onDismiss: () -> Unit,
    onSelectCollection: (Long?) -> Unit
) {
    SelectCollectionDialog(
        title = "Move Bookmark",
        confirmButtonText = "Move",
        currentCollectionId = currentCollectionId,
        availableCollections = availableCollections,
        onDismiss = onDismiss,
        onSelectCollection = onSelectCollection
    )
}

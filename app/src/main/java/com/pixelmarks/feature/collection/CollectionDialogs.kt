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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pixelmarks.core.model.Collection
import com.pixelmarks.ui.theme.ArchivalError
import com.pixelmarks.ui.theme.ArchivalSurfaceContainerHigh
import com.pixelmarks.ui.theme.ArchivalSurfaceContainerHighest
import com.pixelmarks.ui.theme.ArchivalSurfaceContainerLowest
import com.pixelmarks.ui.theme.ArchivalTextPrimary
import com.pixelmarks.ui.theme.ArchivalTextSecondary
import com.pixelmarks.ui.theme.BrassGold
import com.pixelmarks.ui.theme.Spacing
import com.pixelmarks.ui.theme.TerracottaAccent

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
    onSave: (name: String, parentId: Long?, icon: String?, color: String?, isLocked: Boolean, isHidden: Boolean, description: String?, keywords: String?) -> Unit
) {
    var name by remember { mutableStateOf(initialCollection?.name ?: "") }
    var description by remember { mutableStateOf(initialCollection?.description ?: "") }
    var keywords by remember { mutableStateOf(initialCollection?.keywords ?: "") }
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
    var isParentDropdownExpanded by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    // Filter out the current collection itself from available parents
    val validParents = availableParents.filter { initialCollection == null || it.id != initialCollection.id }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ArchivalSurfaceContainerHigh,
        titleContentColor = ArchivalTextPrimary,
        textContentColor = ArchivalTextSecondary,
        shape = RoundedCornerShape(18.dp),
        title = {
            Text(
                text = if (initialCollection == null) "Create Collection" else "Edit Collection",
                style = MaterialTheme.typography.titleLarge,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                color = ArchivalTextPrimary
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
                    label = { Text("Collection Name", color = ArchivalTextSecondary) },
                    isError = validationError != null,
                    supportingText = validationError?.let { { Text(it, color = ArchivalError) } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ArchivalTextPrimary,
                        unfocusedTextColor = ArchivalTextPrimary,
                        focusedBorderColor = BrassGold,
                        unfocusedBorderColor = ArchivalSurfaceContainerHighest,
                        focusedLabelColor = BrassGold,
                        unfocusedLabelColor = ArchivalTextSecondary,
                        cursorColor = BrassGold
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)", color = ArchivalTextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ArchivalTextPrimary,
                        unfocusedTextColor = ArchivalTextPrimary,
                        focusedBorderColor = BrassGold,
                        unfocusedBorderColor = ArchivalSurfaceContainerHighest,
                        focusedLabelColor = BrassGold,
                        unfocusedLabelColor = ArchivalTextSecondary,
                        cursorColor = BrassGold
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = keywords,
                    onValueChange = { keywords = it },
                    label = { Text("Keywords (Optional)", color = ArchivalTextSecondary) },
                    placeholder = { Text("e.g. Kotlin, Compose, Architecture", color = ArchivalTextSecondary.copy(alpha = 0.5f)) },
                    supportingText = { Text("Taxonomy keywords, comma-separated", color = ArchivalTextSecondary.copy(alpha = 0.7f)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ArchivalTextPrimary,
                        unfocusedTextColor = ArchivalTextPrimary,
                        focusedBorderColor = BrassGold,
                        unfocusedBorderColor = ArchivalSurfaceContainerHighest,
                        focusedLabelColor = BrassGold,
                        unfocusedLabelColor = ArchivalTextSecondary,
                        cursorColor = BrassGold
                    ),
                    shape = RoundedCornerShape(10.dp)
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
                        label = { Text("Parent Folder", color = ArchivalTextSecondary) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isParentDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ArchivalTextPrimary,
                            unfocusedTextColor = ArchivalTextPrimary,
                            focusedBorderColor = BrassGold,
                            unfocusedBorderColor = ArchivalSurfaceContainerHighest,
                            focusedLabelColor = BrassGold,
                            unfocusedLabelColor = ArchivalTextSecondary,
                            cursorColor = BrassGold
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = isParentDropdownExpanded,
                        onDismissRequest = { isParentDropdownExpanded = false },
                        modifier = Modifier.background(ArchivalSurfaceContainerHigh)
                    ) {
                        DropdownMenuItem(
                            text = { Text("None (Root Collection)", color = ArchivalTextPrimary) },
                            onClick = {
                                selectedParentId = null
                                isParentDropdownExpanded = false
                            }
                        )
                        validParents.forEach { parent ->
                            DropdownMenuItem(
                                text = { Text(parent.name, color = ArchivalTextPrimary) },
                                onClick = {
                                    selectedParentId = parent.id
                                    isParentDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Color Picker Row
                Text("Color", style = MaterialTheme.typography.labelMedium, color = ArchivalTextSecondary)
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
                                    color = if (isSelected) BrassGold else Color.Transparent,
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
                Text("Icon", style = MaterialTheme.typography.labelMedium, color = ArchivalTextSecondary)
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
                                    color = if (isSelected) BrassGold.copy(alpha = 0.25f) else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                        ) {
                            Icon(
                                imageVector = vector,
                                contentDescription = key,
                                tint = if (isSelected) BrassGold else ArchivalTextSecondary
                            )
                        }
                    }
                }

                // Privacy Section
                HorizontalDivider(
                    color = ArchivalSurfaceContainerHighest
                )
                Text(
                    text = "Privacy",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = BrassGold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Lock collection",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = ArchivalTextPrimary
                        )
                        Text(
                            text = "Require authentication to open",
                            style = MaterialTheme.typography.bodySmall,
                            color = ArchivalTextSecondary
                        )
                    }
                    Switch(
                        checked = isLocked,
                        onCheckedChange = { checked ->
                            isLocked = checked
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ArchivalSurfaceContainerLowest,
                            checkedTrackColor = BrassGold,
                            uncheckedThumbColor = ArchivalTextSecondary,
                            uncheckedTrackColor = ArchivalSurfaceContainerHighest
                        )
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
                        onSave(trimmed, selectedParentId, selectedIcon, selectedColor, isLocked, false, description.trim().ifEmpty { null }, keywords.trim().ifEmpty { null })
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = TerracottaAccent,
                    contentColor = ArchivalSurfaceContainerLowest
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = ArchivalTextSecondary
                )
            ) {
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
        containerColor = ArchivalSurfaceContainerHigh,
        titleContentColor = ArchivalTextPrimary,
        textContentColor = ArchivalTextSecondary,
        shape = RoundedCornerShape(18.dp),
        title = {
            Text(
                text = "Delete Collection?",
                style = MaterialTheme.typography.titleLarge,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                color = ArchivalTextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
                Text("Are you sure you want to delete \"$collectionName\"?", color = ArchivalTextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "• Bookmarks inside this collection will NOT be deleted. They will remain safely saved in Unsorted.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ArchivalTextSecondary
                )
                Text(
                    text = "• Any sub-collections inside will also be removed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ArchivalTextSecondary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ArchivalError,
                    contentColor = ArchivalSurfaceContainerLowest
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Delete", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = ArchivalTextSecondary
                )
            ) {
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
    var searchQuery by remember { mutableStateOf("") }

    val flatNodes = remember(availableCollections) {
        flattenCollectionHierarchy(availableCollections)
    }

    val filteredNodes = remember(flatNodes, searchQuery) {
        if (searchQuery.isBlank()) flatNodes
        else flatNodes.filter { it.collection.name.contains(searchQuery.trim(), ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ArchivalSurfaceContainerHigh,
        titleContentColor = ArchivalTextPrimary,
        textContentColor = ArchivalTextSecondary,
        shape = RoundedCornerShape(18.dp),
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                color = ArchivalTextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                if (availableCollections.isNotEmpty()) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search collections...", color = ArchivalTextSecondary.copy(alpha = 0.5f)) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = BrassGold, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = ArchivalTextSecondary, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ArchivalTextPrimary,
                            unfocusedTextColor = ArchivalTextPrimary,
                            focusedBorderColor = BrassGold,
                            unfocusedBorderColor = ArchivalSurfaceContainerHighest,
                            focusedLabelColor = BrassGold,
                            unfocusedLabelColor = ArchivalTextSecondary,
                            cursorColor = BrassGold
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (availableCollections.isEmpty()) {
                    Text("No collections available.", color = ArchivalTextSecondary)
                } else if (filteredNodes.isEmpty()) {
                    Text("No collections matching \"$searchQuery\"", color = ArchivalTextSecondary, modifier = Modifier.padding(vertical = 16.dp))
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (searchQuery.isBlank()) {
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
                                        onClick = { selectedId = null },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = BrassGold,
                                            unselectedColor = ArchivalTextSecondary
                                        )
                                    )
                                    Text(
                                        text = "No Collection (Unsorted)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = ArchivalTextPrimary,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                        }

                        items(filteredNodes, key = { it.collection.id }) { node ->
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
                            onClick = { selectedId = col.id },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = BrassGold,
                                unselectedColor = ArchivalTextSecondary
                            )
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
                            color = ArchivalTextPrimary,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .padding(start = 8.dp)
                        )
                        if (col.isLocked || col.isHidden) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Private collection",
                                tint = BrassGold,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    }
                }
            }
        }
    },
        confirmButton = {
            Button(
                onClick = {
                    onSelectCollection(selectedId)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = TerracottaAccent,
                    contentColor = ArchivalSurfaceContainerLowest
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(confirmButtonText, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = ArchivalTextSecondary
                )
            ) {
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

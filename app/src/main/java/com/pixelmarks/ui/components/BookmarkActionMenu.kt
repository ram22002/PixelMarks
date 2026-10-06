package com.pixelmarks.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import com.pixelmarks.core.model.Bookmark

@Composable
fun BookmarkActionMenu(
    bookmark: Bookmark,
    onOpen: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onToggleFavorite: (() -> Unit)? = null,
    onTogglePin: (() -> Unit)? = null,
    onArchive: (() -> Unit)? = null,
    onUnarchive: (() -> Unit)? = null,
    onMoveToTrash: (() -> Unit)? = null,
    onRestore: (() -> Unit)? = null,
    onDeletePermanently: (() -> Unit)? = null,
    onMoveToCollection: (() -> Unit)? = null,
    onRemoveFromCollection: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Bookmark actions",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            if (bookmark.isTrashed) {
                // Trashed state actions
                if (onRestore != null) {
                    DropdownMenuItem(
                        text = { Text("Restore") },
                        leadingIcon = {
                            Icon(Icons.Default.RestoreFromTrash, contentDescription = null)
                        },
                        onClick = {
                            expanded = false
                            onRestore()
                        }
                    )
                }
                if (onDeletePermanently != null) {
                    DropdownMenuItem(
                        text = { Text("Delete Permanently", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        },
                        onClick = {
                            expanded = false
                            onDeletePermanently()
                        }
                    )
                }
            } else {
                // Active or Archived state actions
                DropdownMenuItem(
                    text = { Text("Open in Browser") },
                    leadingIcon = {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = null)
                    },
                    onClick = {
                        expanded = false
                        if (onOpen != null) {
                            onOpen()
                        } else {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(bookmark.url))
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        }
                    }
                )

                if (onEdit != null) {
                    DropdownMenuItem(
                        text = { Text("Edit / Details") },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null)
                        },
                        onClick = {
                            expanded = false
                            onEdit()
                        }
                    )
                }

                if (onToggleFavorite != null) {
                    DropdownMenuItem(
                        text = { Text(if (bookmark.isFavorite) "Unfavorite" else "Favorite") },
                        leadingIcon = {
                            Icon(
                                if (bookmark.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                tint = if (bookmark.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            expanded = false
                            onToggleFavorite()
                        }
                    )
                }

                if (onTogglePin != null) {
                    DropdownMenuItem(
                        text = { Text(if (bookmark.isPinned) "Unpin" else "Pin") },
                        leadingIcon = {
                            Icon(
                                if (bookmark.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            expanded = false
                            onTogglePin()
                        }
                    )
                }

                if (bookmark.isArchived) {
                    if (onUnarchive != null) {
                        DropdownMenuItem(
                            text = { Text("Unarchive") },
                            leadingIcon = {
                                Icon(Icons.Default.Unarchive, contentDescription = null)
                            },
                            onClick = {
                                expanded = false
                                onUnarchive()
                            }
                        )
                    }
                } else {
                    if (onArchive != null) {
                        DropdownMenuItem(
                            text = { Text("Archive") },
                            leadingIcon = {
                                Icon(Icons.Default.Archive, contentDescription = null)
                            },
                            onClick = {
                                expanded = false
                                onArchive()
                            }
                        )
                    }
                }

                if (onMoveToCollection != null) {
                    DropdownMenuItem(
                        text = { Text("Move to Collection") },
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = null)
                        },
                        onClick = {
                            expanded = false
                            onMoveToCollection()
                        }
                    )
                }

                if (onRemoveFromCollection != null) {
                    DropdownMenuItem(
                        text = { Text("Remove from Collection") },
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = null)
                        },
                        onClick = {
                            expanded = false
                            onRemoveFromCollection()
                        }
                    )
                }

                DropdownMenuItem(
                    text = { Text("Copy Link") },
                    leadingIcon = {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                    },
                    onClick = {
                        expanded = false
                        clipboardManager.setText(AnnotatedString(bookmark.url))
                    }
                )

                DropdownMenuItem(
                    text = { Text("Share") },
                    leadingIcon = {
                        Icon(Icons.Default.Share, contentDescription = null)
                    },
                    onClick = {
                        expanded = false
                        try {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, bookmark.url)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share bookmark"))
                        } catch (_: Exception) {}
                    }
                )

                if (onMoveToTrash != null) {
                    DropdownMenuItem(
                        text = { Text("Move to Trash", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        },
                        onClick = {
                            expanded = false
                            onMoveToTrash()
                        }
                    )
                }
            }
        }
    }
}

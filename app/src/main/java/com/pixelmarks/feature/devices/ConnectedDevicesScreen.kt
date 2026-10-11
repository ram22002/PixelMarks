package com.pixelmarks.feature.devices

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.NoAccounts
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pixelmarks.app.PixelMarksApplication
import com.pixelmarks.core.database.entity.ConnectedDeviceEntity
import com.pixelmarks.ui.components.PixelMarksScreenScaffold
import com.pixelmarks.ui.theme.Spacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val TerracottaColor = Color(0xFFF9B89A)
private val ParchmentBg = Color(0xFF181306)
private val CharcoalColor = Color(0xFFEDE1CB)
private val MutedCharcoal = Color(0xFFD2C5B5)
private val CardBg = Color(0xFF251F11)
private val ThinBorderColor = Color(0x404E4539)
private val RevokedGray = Color(0xFF88827D)
private val ActiveGreen = Color(0xFF81C784)

@Composable
fun ConnectedDevicesScreen(
    onNavigateBack: () -> Unit,
    viewModel: ConnectedDevicesViewModel = viewModel(
        factory = run {
            val app = LocalContext.current.applicationContext as PixelMarksApplication
            ConnectedDevicesViewModel.provideFactory(app.container.connectedDeviceRepository)
        }
    )
) {
    val devices by viewModel.devices.collectAsState()
    var deviceToRevoke by remember { mutableStateOf<ConnectedDeviceEntity?>(null) }
    var showRevokeAllDialog by remember { mutableStateOf(false) }

    val activeCount = devices.count { it.revokedAt == null && !it.isBlocked }

    PixelMarksScreenScaffold(
        title = "Connected Devices",
        onNavigateBack = onNavigateBack,
        actions = {
            if (activeCount > 0) {
                TextButton(onClick = { showRevokeAllDialog = true }) {
                    Text("Revoke All", color = TerracottaColor, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(ParchmentBg)
        ) {
            if (devices.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Spacing.large),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(ThinBorderColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Laptop,
                            contentDescription = null,
                            tint = MutedCharcoal,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(Spacing.medium))
                    Text(
                        text = "No Paired Devices",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CharcoalColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "When you pair a PC browser using Connect to PC, authorized devices will appear here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedCharcoal,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(Spacing.small)
                ) {
                    item {
                        Text(
                            text = "Active & Past Browser Sessions",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = CharcoalColor,
                            modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                        )
                    }

                    items(devices, key = { it.id }) { device ->
                        DeviceItemCard(
                            device = device,
                            onRevokeClick = { deviceToRevoke = device },
                            onDeleteClick = { viewModel.deleteDevice(device.id) }
                        )
                    }
                }
            }
        }

        // Single device revoke confirmation dialog
        deviceToRevoke?.let { device ->
            AlertDialog(
                onDismissRequest = { deviceToRevoke = null },
                title = { Text("Revoke Access?") },
                text = {
                    Text("This will immediately invalidate the session token for '${device.deviceName}'. The PC browser will no longer be able to access your bookmarks without re-pairing.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.revokeDevice(device.id)
                            deviceToRevoke = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TerracottaColor)
                    ) {
                        Text("Revoke Access")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { deviceToRevoke = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Revoke all devices confirmation dialog
        if (showRevokeAllDialog) {
            AlertDialog(
                onDismissRequest = { showRevokeAllDialog = false },
                title = { Text("Revoke All Devices?") },
                text = {
                    Text("This will revoke access for all currently paired PC devices. All active browser sessions will be immediately terminated.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.revokeAllDevices()
                            showRevokeAllDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TerracottaColor)
                    ) {
                        Text("Revoke All")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showRevokeAllDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun DeviceItemCard(
    device: ConnectedDeviceEntity,
    onRevokeClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isRevoked = device.revokedAt != null || device.isBlocked
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ThinBorderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isRevoked) ThinBorderColor else TerracottaColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Laptop,
                        contentDescription = null,
                        tint = if (isRevoked) RevokedGray else TerracottaColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(Spacing.medium))

                Column {
                    Text(
                        text = device.deviceName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isRevoked) RevokedGray else CharcoalColor
                    )

                    Text(
                        text = if (isRevoked) {
                            "Revoked ${dateFormat.format(Date(device.revokedAt ?: device.lastSeenAt))}"
                        } else {
                            "Last seen: ${dateFormat.format(Date(device.lastSeenAt))}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedCharcoal
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isRevoked) RevokedGray else ActiveGreen)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isRevoked) "Revoked" else "Authorized",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isRevoked) RevokedGray else ActiveGreen
                        )
                    }
                }
            }

            if (!isRevoked) {
                OutlinedButton(
                    onClick = onRevokeClick,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TerracottaColor)
                ) {
                    Text("Revoke", style = MaterialTheme.typography.labelMedium)
                }
            } else {
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Remove Record",
                        tint = MutedCharcoal
                    )
                }
            }
        }
    }
}

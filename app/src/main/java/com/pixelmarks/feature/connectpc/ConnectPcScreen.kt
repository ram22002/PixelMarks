package com.pixelmarks.feature.connectpc

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pixelmarks.app.PixelMarksApplication
import com.pixelmarks.core.pcserver.model.PairingState
import com.pixelmarks.core.pcserver.model.PcServerState
import com.pixelmarks.ui.components.PixelMarksScreenScaffold
import com.pixelmarks.ui.theme.Spacing

private val TerracottaColor = Color(0xFFC85A32)
private val ParchmentBg = Color(0xFFFAF7F2)
private val CharcoalColor = Color(0xFF2A2624)
private val MutedCharcoal = Color(0xFF5C5551)
private val ThinBorderColor = Color(0xFFE5DFD7)
private val SuccessGreen = Color(0xFF2E7D32)
private val AmberWarning = Color(0xFFE65100)

@Composable
fun ConnectPcScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDevices: () -> Unit = {},
    viewModel: ConnectPcViewModel = viewModel(
        factory = run {
            val app = LocalContext.current.applicationContext as PixelMarksApplication
            ConnectPcViewModel.provideFactory(
                context = app.applicationContext,
                serverManager = app.container.localServerManager,
                pairingManager = app.container.pairingManager,
                deviceRepository = app.container.connectedDeviceRepository
            )
        }
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    PixelMarksScreenScaffold(
        title = "Connect to PC",
        onNavigateBack = onNavigateBack,
        actions = {
            IconButton(onClick = onNavigateToDevices) {
                Icon(
                    imageVector = Icons.Default.Devices,
                    contentDescription = "Connected Devices",
                    tint = TerracottaColor
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(ParchmentBg)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {

            // Wi-Fi Status Banner
            if (!uiState.isWifiConnected) {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.outlinedCardColors(containerColor = Color(0xFFFFF3E0)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AmberWarning)
                ) {
                    Row(
                        modifier = Modifier.padding(Spacing.medium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.WifiOff,
                            contentDescription = null,
                            tint = AmberWarning,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(Spacing.small))
                        Column {
                            Text(
                                text = "Wi-Fi Disconnected",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = AmberWarning
                            )
                            Text(
                                text = "Connect your phone and PC to the same Wi-Fi network to use PC Connect.",
                                style = MaterialTheme.typography.bodySmall,
                                color = CharcoalColor
                            )
                        }
                    }
                }
            }

            // Server Status & Power Control Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ThinBorderColor)
            ) {
                Column(
                    modifier = Modifier.padding(Spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(Spacing.small)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val dotColor = when (uiState.serverState) {
                                is PcServerState.Running -> SuccessGreen
                                is PcServerState.Starting -> AmberWarning
                                else -> Color.Gray
                            }
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (uiState.serverState) {
                                    is PcServerState.Running -> "Server Active & Listening"
                                    is PcServerState.Starting -> "Starting Server..."
                                    is PcServerState.Error -> "Server Error"
                                    else -> "Server Disconnected"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = CharcoalColor
                            )
                        }

                        Switch(
                            checked = uiState.serverState is PcServerState.Running || uiState.serverState is PcServerState.Starting,
                            onCheckedChange = { viewModel.toggleServer() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = TerracottaColor
                            )
                        )
                    }

                    Text(
                        text = "PixelMarks runs a private, local-only HTTP server over your local network. Server must be started explicitly and never runs automatically on boot.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedCharcoal
                    )

                    // Error note if any
                    uiState.errorMessage?.let { error ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFC5221F)
                        )
                    }
                }
            }

            // Running Server Details & Pairing
            if (uiState.serverState is PcServerState.Running) {
                val running = uiState.serverState as PcServerState.Running

                // Server Address Card
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.outlinedCardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ThinBorderColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.medium),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "PC Browser Address",
                                style = MaterialTheme.typography.labelMedium,
                                color = MutedCharcoal
                            )
                            Text(
                                text = running.serverUrl,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TerracottaColor
                            )
                        }

                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("PixelMarks Server URL", running.serverUrl)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Address copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Address",
                                tint = MutedCharcoal
                            )
                        }
                    }
                }

                // Pairing Credentials Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ThinBorderColor)
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.medium),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.medium)
                    ) {
                        Text(
                            text = "Single-Use Pairing Credential",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CharcoalColor
                        )

                        Text(
                            text = "Scan the QR code in your PC browser or enter the 6-digit PIN on the pairing web page.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedCharcoal,
                            textAlign = TextAlign.Center
                        )

                        // 6-digit Code display
                        when (val pairing = uiState.pairingState) {
                            is PairingState.Active -> {
                                Surface(
                                    color = ParchmentBg,
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, ThinBorderColor),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        text = pairing.formattedCode,
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            letterSpacing = 6.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        color = TerracottaColor,
                                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                                    )
                                }

                                Text(
                                    text = "Valid for 5 minutes • Replay protected",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MutedCharcoal
                                )

                                // Genuine QR Code display
                                uiState.qrBitmap?.let { bitmap ->
                                    Box(
                                        modifier = Modifier
                                            .size(200.dp)
                                            .border(1.dp, ThinBorderColor, RoundedCornerShape(12.dp))
                                            .padding(12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = "Pairing QR Code",
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                            is PairingState.Locked -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = Color(0xFFC5221F),
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = pairing.message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFFC5221F),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            else -> {
                                Text(
                                    text = "No active pairing code.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MutedCharcoal
                                )
                            }
                        }

                        // Regenerate code button
                        OutlinedButton(
                            onClick = { viewModel.generatePairingCode() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TerracottaColor)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Generate New Code")
                        }
                    }
                }

                // Paired Devices Summary Card
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.outlinedCardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ThinBorderColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.medium),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Paired PC Devices",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = CharcoalColor
                            )
                            Text(
                                text = "${uiState.activeDevicesCount} authorized browser session(s)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedCharcoal
                            )
                        }

                        TextButton(onClick = onNavigateToDevices) {
                            Text("Manage", color = TerracottaColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }

            } else {
                // Stopped / How to use guide
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ThinBorderColor)
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.medium),
                        verticalArrangement = Arrangement.spacedBy(Spacing.medium)
                    ) {
                        Text(
                            text = "How to Connect to PC",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CharcoalColor
                        )

                        StepItem(
                            stepNumber = "1",
                            title = "Connect to Same Wi-Fi",
                            description = "Ensure your phone and computer are on the same local network."
                        )

                        StepItem(
                            stepNumber = "2",
                            title = "Start the Server",
                            description = "Toggle the switch above to start the listening endpoint on your local IP."
                        )

                        StepItem(
                            stepNumber = "3",
                            title = "Pair & Authorize",
                            description = "Open the browser address on your PC and enter the single-use pairing code. Confirm the request on your phone."
                        )

                        StepItem(
                            stepNumber = "4",
                            title = "Secure Isolation",
                            description = "Only public bookmarks are shared. Private mode & Secure Folder items remain completely isolated."
                        )
                    }
                }
            }
        }

        // On-Device Approval Dialog for Incoming Pairing Requests
        uiState.pendingConfirmation?.let { pending ->
            AlertDialog(
                onDismissRequest = { viewModel.denyPairing(pending.requestId) },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Devices,
                            contentDescription = null,
                            tint = TerracottaColor
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Authorize PC Browser?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CharcoalColor
                        )
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "A PC browser is requesting permission to view your public bookmarks:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CharcoalColor
                        )
                        Surface(
                            color = ParchmentBg,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(Spacing.small)) {
                                Text(
                                    text = "Device: ${pending.deviceName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = CharcoalColor
                                )
                                Text(
                                    text = "IP Address: ${pending.clientIp}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedCharcoal
                                )
                            }
                        }
                        Text(
                            text = "This will generate an authenticated session token for this browser. You can revoke it anytime in Connected Devices.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedCharcoal
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.approvePairing(pending.requestId) },
                        colors = ButtonDefaults.buttonColors(containerColor = TerracottaColor)
                    ) {
                        Text("Allow Connection")
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { viewModel.denyPairing(pending.requestId) }
                    ) {
                        Text("Deny")
                    }
                }
            )
        }
    }
}

@Composable
private fun StepItem(
    stepNumber: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(TerracottaColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = TerracottaColor
            )
        }
        Spacer(modifier = Modifier.width(Spacing.medium))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = CharcoalColor
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MutedCharcoal
            )
        }
    }
}

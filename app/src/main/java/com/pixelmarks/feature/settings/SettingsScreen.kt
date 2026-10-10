package com.pixelmarks.feature.settings

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pixelmarks.app.PixelMarksApplication
import com.pixelmarks.core.browser.BrowserPreference
import com.pixelmarks.core.security.model.AppLockTimeout
import com.pixelmarks.core.security.model.BiometricAuthCapability
import com.pixelmarks.ui.components.PixelMarksScreenScaffold
import com.pixelmarks.ui.theme.PixelMarksTheme
import com.pixelmarks.ui.theme.Spacing
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onNavigateToConnectPc: () -> Unit = {},
    onNavigateToConnectedDevices: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToHtmlImportExport: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel(
        factory = run {
            val app = LocalContext.current.applicationContext as PixelMarksApplication
            SettingsViewModel.provideFactory(
                observeBrowserPreferenceUseCase = app.container.observeBrowserPreferenceUseCase,
                setBrowserPreferenceUseCase = app.container.setBrowserPreferenceUseCase,
                observeAppLockSettingsUseCase = app.container.observeAppLockSettingsUseCase,
                setAppLockEnabledUseCase = app.container.setAppLockEnabledUseCase,
                setAppLockTimeoutUseCase = app.container.setAppLockTimeoutUseCase,
                checkBiometricCapabilityUseCase = app.container.checkBiometricCapabilityUseCase,
                biometricAuthManager = app.container.biometricAuthManager,
                privatePreferencesRepository = app.container.privatePreferencesRepository
            )
        }
    )
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentBrowserPref by viewModel.browserPreference.collectAsState()
    val isAppLockEnabled by viewModel.isAppLockEnabled.collectAsState()
    val appLockTimeout by viewModel.appLockTimeout.collectAsState()
    val biometricCapability by viewModel.biometricCapability.collectAsState()

    val privateAuthMethod by viewModel.privateAuthMethod.collectAsState()
    val hasCustomPassword by viewModel.hasCustomPassword.collectAsState()
    val passwordHint by viewModel.passwordHint.collectAsState()
    val isRecoveryEnabled by viewModel.isRecoveryEnabled.collectAsState()
    var showSetupPasswordDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    PixelMarksScreenScaffold(
        title = "Settings"
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.medium),
                        verticalArrangement = Arrangement.spacedBy(Spacing.small)
                    ) {
                        Text(
                            text = "PixelMarks Settings",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "App preferences, biometric security, browser link opening, and backup settings.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Security Section
                Text(
                    text = "Security",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = Spacing.small)
                    ) {
                        // App Lock Switch
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.medium, vertical = Spacing.small),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = Spacing.medium)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "App Lock",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Require biometric or device credentials when opening PixelMarks.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                            Switch(
                                checked = isAppLockEnabled,
                                onCheckedChange = { targetState ->
                                    if (activity != null) {
                                        viewModel.requestToggleAppLock(targetState, activity) { success, errorMsg ->
                                            if (!success && errorMsg != null) {
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar(errorMsg)
                                                }
                                            }
                                        }
                                    }
                                }
                            )
                        }

                        if (!biometricCapability.canLock) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = Spacing.medium, vertical = Spacing.extraSmall)
                            ) {
                                Text(
                                    text = biometricCapability.userMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                                if (biometricCapability == BiometricAuthCapability.NONE_ENROLLED) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedButton(
                                        onClick = {
                                            try {
                                                context.startActivity(viewModel.createEnrollIntent())
                                            } catch (_: Exception) {
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar("Unable to open device security settings")
                                                }
                                            }
                                        }
                                    ) {
                                        Text("Set up biometric authentication")
                                    }
                                }
                            }
                        }

                        if (isAppLockEnabled) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = Spacing.medium, vertical = Spacing.small),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )

                            Text(
                                text = "Lock automatically",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = Spacing.medium, vertical = 4.dp)
                            )

                            AppLockTimeout.entries.forEach { timeoutOption ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.setLockTimeout(timeoutOption) }
                                        .padding(horizontal = Spacing.medium, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = appLockTimeout == timeoutOption,
                                        onClick = { viewModel.setLockTimeout(timeoutOption) }
                                    )
                                    Text(
                                        text = timeoutOption.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(start = Spacing.small)
                                    )
                                }
                            }
                        }
                    }
                }

                // Secure Folder Section
                Text(
                    text = "Secure Folder (Private Mode)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.medium),
                        verticalArrangement = Arrangement.spacedBy(Spacing.small)
                    ) {
                        Text(
                            text = "Access your protected bookmarks and folders by long-pressing the Settings tab icon in the bottom bar.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Authentication Method",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setPrivateAuthMethod(com.pixelmarks.core.security.model.PrivateAuthMethod.BIOMETRIC) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = privateAuthMethod == com.pixelmarks.core.security.model.PrivateAuthMethod.BIOMETRIC,
                                onClick = { viewModel.setPrivateAuthMethod(com.pixelmarks.core.security.model.PrivateAuthMethod.BIOMETRIC) }
                            )
                            Text(
                                text = "Biometric (Fingerprint)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = Spacing.small)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setPrivateAuthMethod(com.pixelmarks.core.security.model.PrivateAuthMethod.DEVICE_CREDENTIAL) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = privateAuthMethod == com.pixelmarks.core.security.model.PrivateAuthMethod.DEVICE_CREDENTIAL,
                                onClick = { viewModel.setPrivateAuthMethod(com.pixelmarks.core.security.model.PrivateAuthMethod.DEVICE_CREDENTIAL) }
                            )
                            Text(
                                text = "Device Screen Lock (PIN / Pattern / Password)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = Spacing.small)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setPrivateAuthMethod(com.pixelmarks.core.security.model.PrivateAuthMethod.CUSTOM_PASSWORD)
                                    if (!hasCustomPassword) {
                                        showSetupPasswordDialog = true
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = privateAuthMethod == com.pixelmarks.core.security.model.PrivateAuthMethod.CUSTOM_PASSWORD,
                                onClick = {
                                    viewModel.setPrivateAuthMethod(com.pixelmarks.core.security.model.PrivateAuthMethod.CUSTOM_PASSWORD)
                                    if (!hasCustomPassword) {
                                        showSetupPasswordDialog = true
                                    }
                                }
                            )
                            Text(
                                text = "Custom Secure Folder Password",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = Spacing.small)
                            )
                        }

                        if (privateAuthMethod == com.pixelmarks.core.security.model.PrivateAuthMethod.CUSTOM_PASSWORD) {
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedButton(
                                onClick = { showSetupPasswordDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (hasCustomPassword) "Change Password" else "Set Custom Password")
                            }

                            if (!passwordHint.isNullOrBlank()) {
                                Text(
                                    text = "Hint: $passwordHint",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = Spacing.extraSmall),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Device Recovery",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Allow biometric / device unlock if password forgotten",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                }
                                Switch(
                                    checked = isRecoveryEnabled,
                                    onCheckedChange = { viewModel.setRecoveryEnabled(it) }
                                )
                            }
                        }
                    }
                }

                if (showSetupPasswordDialog) {
                    com.pixelmarks.feature.security.SetupSecureFolderPasswordDialog(
                        onDismiss = { showSetupPasswordDialog = false },
                        onSuccess = {
                            showSetupPasswordDialog = false
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Secure Folder password saved")
                            }
                        }
                    )
                }

                // Browser Section
                Text(
                    text = "Browser",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = Spacing.small)
                    ) {
                        BrowserPreferenceRadioItem(
                            title = "In-App Browser",
                            description = "Most private — uses a browser inside the app,\nwithout signed-in accounts.",
                            selected = currentBrowserPref == BrowserPreference.IN_APP,
                            onSelect = { viewModel.onBrowserPreferenceSelected(BrowserPreference.IN_APP) }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = Spacing.medium),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        BrowserPreferenceRadioItem(
                            title = "System Preferred",
                            description = "Let the system decide the best way to view links,\nusually without leaving the app.",
                            selected = currentBrowserPref == BrowserPreference.SYSTEM_PREFERRED,
                            onSelect = { viewModel.onBrowserPreferenceSelected(BrowserPreference.SYSTEM_PREFERRED) }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = Spacing.medium),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        BrowserPreferenceRadioItem(
                            title = "Default Browser",
                            description = "Use your device's default browser for full features\nand saved accounts.",
                            selected = currentBrowserPref == BrowserPreference.DEFAULT_BROWSER,
                            onSelect = { viewModel.onBrowserPreferenceSelected(BrowserPreference.DEFAULT_BROWSER) }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = Spacing.medium),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        BrowserPreferenceRadioItem(
                            title = "Ask Every Time",
                            description = "Ask you each time how you'd like to open the link.",
                            selected = currentBrowserPref == BrowserPreference.ASK_EVERY_TIME,
                            onSelect = { viewModel.onBrowserPreferenceSelected(BrowserPreference.ASK_EVERY_TIME) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.small))

                // Other Settings Sections
                Text(
                    text = "Data & Connectivity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp)
                )

                OutlinedButton(
                    onClick = onNavigateToConnectPc,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Connect to PC")
                }

                OutlinedButton(
                    onClick = onNavigateToConnectedDevices,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Connected PC Devices")
                }


                OutlinedButton(
                    onClick = onNavigateToHtmlImportExport,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Import & Export Bookmarks (HTML)")
                }

                Button(
                    onClick = onNavigateToBackup,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Backup & Restore")
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = Spacing.medium)
            )
        }
    }
}

@Composable
private fun BrowserPreferenceRadioItem(
    title: String,
    description: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(horizontal = Spacing.medium, vertical = Spacing.small),
        verticalAlignment = Alignment.Top
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
            modifier = Modifier.padding(top = 2.dp)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = Spacing.small),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    PixelMarksTheme {
        SettingsScreen(
            onNavigateToConnectedDevices = {},
            onNavigateToBackup = {}
        )
    }
}

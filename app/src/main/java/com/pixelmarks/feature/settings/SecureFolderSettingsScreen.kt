package com.pixelmarks.feature.settings

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pixelmarks.app.PixelMarksApplication
import com.pixelmarks.core.security.model.PrivateAuthMethod
import com.pixelmarks.ui.theme.ArchivalBackground
import com.pixelmarks.ui.theme.ArchivalPrimary
import com.pixelmarks.ui.theme.ArchivalSurfaceContainer
import com.pixelmarks.ui.theme.ArchivalSurfaceContainerHigh
import com.pixelmarks.ui.theme.ArchivalSurfaceContainerLow
import com.pixelmarks.ui.theme.ArchivalTextPrimary
import com.pixelmarks.ui.theme.ArchivalTextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecureFolderSettingsScreen(
    onNavigateBack: () -> Unit,
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
                privatePreferencesRepository = app.container.privatePreferencesRepository,
                appPreferencesRepository = app.container.appPreferencesRepository
            )
        }
    )
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val privateAuthMethod by viewModel.privateAuthMethod.collectAsState()
    val hasCustomPassword by viewModel.hasCustomPassword.collectAsState()
    val passwordHint by viewModel.passwordHint.collectAsState()
    val isRecoveryEnabled by viewModel.isRecoveryEnabled.collectAsState()
    var showSetupPasswordDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Secure Folder",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = ArchivalTextPrimary,
                        fontFamily = FontFamily.Serif
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ArchivalTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ArchivalBackground
                )
            )
        },
        containerColor = ArchivalBackground,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Overview & How to Access
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ArchivalSurfaceContainerLow),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ArchivalSurfaceContainerHigh),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderSpecial,
                                contentDescription = null,
                                tint = ArchivalPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ARCHIVAL VAULT (PRIVATE BOOKMARKS)",
                                style = MaterialTheme.typography.labelSmall,
                                color = ArchivalPrimary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Bookmarks moved to your Secure Folder are completely hidden from regular tabs and search.",
                                style = MaterialTheme.typography.bodySmall,
                                color = ArchivalTextSecondary,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = ArchivalSurfaceContainerHigh)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = ArchivalPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "How to open: Long-press the Settings icon in the bottom navigation bar to authenticate and access your Secure Folder.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ArchivalTextPrimary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Authentication Protocol Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ArchivalSurfaceContainer),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Vault Authentication Protocol",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = ArchivalTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Select how you would like to authenticate when accessing your hidden vault.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ArchivalTextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Biometric
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { viewModel.setPrivateAuthMethod(PrivateAuthMethod.BIOMETRIC) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = privateAuthMethod == PrivateAuthMethod.BIOMETRIC,
                            onClick = { viewModel.setPrivateAuthMethod(PrivateAuthMethod.BIOMETRIC) },
                            colors = RadioButtonDefaults.colors(selectedColor = ArchivalPrimary)
                        )
                        Column(modifier = Modifier.padding(start = 10.dp)) {
                            Text(
                                text = "Biometric (Fingerprint / Face ID)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = ArchivalTextPrimary
                            )
                            Text(
                                text = "Quick hardware key verification",
                                style = MaterialTheme.typography.bodySmall,
                                color = ArchivalTextSecondary
                            )
                        }
                    }

                    // Device Credential
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { viewModel.setPrivateAuthMethod(PrivateAuthMethod.DEVICE_CREDENTIAL) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = privateAuthMethod == PrivateAuthMethod.DEVICE_CREDENTIAL,
                            onClick = { viewModel.setPrivateAuthMethod(PrivateAuthMethod.DEVICE_CREDENTIAL) },
                            colors = RadioButtonDefaults.colors(selectedColor = ArchivalPrimary)
                        )
                        Column(modifier = Modifier.padding(start = 10.dp)) {
                            Text(
                                text = "Device Screen Lock",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = ArchivalTextPrimary
                            )
                            Text(
                                text = "Inherit system PIN, pattern, or password",
                                style = MaterialTheme.typography.bodySmall,
                                color = ArchivalTextSecondary
                            )
                        }
                    }

                    // Custom Password
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                viewModel.setPrivateAuthMethod(PrivateAuthMethod.CUSTOM_PASSWORD)
                                if (!hasCustomPassword) showSetupPasswordDialog = true
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = privateAuthMethod == PrivateAuthMethod.CUSTOM_PASSWORD,
                            onClick = {
                                viewModel.setPrivateAuthMethod(PrivateAuthMethod.CUSTOM_PASSWORD)
                                if (!hasCustomPassword) showSetupPasswordDialog = true
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = ArchivalPrimary)
                        )
                        Column(modifier = Modifier.padding(start = 10.dp)) {
                            Text(
                                text = "Custom Vault Passphrase",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = ArchivalTextPrimary
                            )
                            Text(
                                text = "Independent local encryption password",
                                style = MaterialTheme.typography.bodySmall,
                                color = ArchivalTextSecondary
                            )
                        }
                    }

                    if (privateAuthMethod == PrivateAuthMethod.CUSTOM_PASSWORD) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { showSetupPasswordDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (hasCustomPassword) "Change Custom Password" else "Set Custom Password")
                        }

                        if (!passwordHint.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Hint: $passwordHint",
                                style = MaterialTheme.typography.bodySmall,
                                color = ArchivalPrimary
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = ArchivalSurfaceContainerHigh
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
                                    color = ArchivalTextPrimary
                                )
                                Text(
                                    text = "Allow biometric / system PIN if vault password is forgotten",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ArchivalTextSecondary
                                )
                            }
                            Switch(
                                checked = isRecoveryEnabled,
                                onCheckedChange = { viewModel.setRecoveryEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = ArchivalBackground,
                                    checkedTrackColor = ArchivalPrimary,
                                    uncheckedTrackColor = ArchivalSurfaceContainerHigh
                                )
                            )
                        }
                    }
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
}

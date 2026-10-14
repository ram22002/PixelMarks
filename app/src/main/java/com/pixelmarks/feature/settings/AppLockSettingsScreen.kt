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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pixelmarks.app.PixelMarksApplication
import com.pixelmarks.core.security.model.AppLockTimeout
import com.pixelmarks.core.security.model.BiometricAuthCapability
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
fun AppLockSettingsScreen(
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
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val isAppLockEnabled by viewModel.isAppLockEnabled.collectAsState()
    val appLockTimeout by viewModel.appLockTimeout.collectAsState()
    val biometricCapability by viewModel.biometricCapability.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "App Lock",
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
            // Overview Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ArchivalSurfaceContainerLow),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
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
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = ArchivalPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "APP ACCESS PROTECTION",
                            style = MaterialTheme.typography.labelSmall,
                            color = ArchivalPrimary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Protect your bookmarks and reading sessions by requiring biometric verification or your device PIN when PixelMarks opens.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ArchivalTextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Main Lock Toggle Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ArchivalSurfaceContainer),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ArchivalSurfaceContainerLow),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = ArchivalPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp)
                        ) {
                            Text(
                                text = "Require Unlock on Open",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = ArchivalTextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Lock application when backgrounded or closed",
                                style = MaterialTheme.typography.bodySmall,
                                color = ArchivalTextSecondary
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
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ArchivalBackground,
                                checkedTrackColor = ArchivalPrimary,
                                uncheckedTrackColor = ArchivalSurfaceContainerHigh
                            )
                        )
                    }

                    if (!biometricCapability.canLock) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = biometricCapability.userMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        if (biometricCapability == BiometricAuthCapability.NONE_ENROLLED) {
                            Spacer(modifier = Modifier.height(6.dp))
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

                    if (isAppLockEnabled) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = ArchivalSurfaceContainerHigh
                        )

                        Text(
                            text = "Lock Automatically",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = ArchivalTextPrimary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        AppLockTimeout.entries.forEach { timeoutOption ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setLockTimeout(timeoutOption) }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = appLockTimeout == timeoutOption,
                                    onClick = { viewModel.setLockTimeout(timeoutOption) },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = ArchivalPrimary,
                                        unselectedColor = ArchivalTextSecondary.copy(alpha = 0.6f)
                                    )
                                )
                                Text(
                                    text = timeoutOption.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (appLockTimeout == timeoutOption) ArchivalTextPrimary else ArchivalTextSecondary,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

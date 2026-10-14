package com.pixelmarks.feature.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pixelmarks.app.PixelMarksApplication
import com.pixelmarks.core.browser.BrowserPreference
import com.pixelmarks.ui.theme.ArchivalBackground
import com.pixelmarks.ui.theme.ArchivalPrimary
import com.pixelmarks.ui.theme.ArchivalSurfaceContainer
import com.pixelmarks.ui.theme.ArchivalSurfaceContainerHigh
import com.pixelmarks.ui.theme.ArchivalSurfaceContainerLow
import com.pixelmarks.ui.theme.ArchivalTextPrimary
import com.pixelmarks.ui.theme.ArchivalTextSecondary
import com.pixelmarks.ui.theme.TerracottaAccent

import com.pixelmarks.ui.components.PixelMarksScreenScaffold

@Composable
fun BrowserPreferenceScreen(
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
    val currentPref by viewModel.browserPreference.collectAsState()

    PixelMarksScreenScaffold(
        title = "Browser Preferences",
        onNavigateBack = onNavigateBack
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Explanatory Hub Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = ArchivalSurfaceContainerLow
                ),
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
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = ArchivalPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "LINK DISPATCH ARCHITECTURE",
                            style = MaterialTheme.typography.labelSmall,
                            color = ArchivalPrimary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Choose how PixelMarks dispatches bookmark URLs. Your selection determines isolation, cookie tracking, and system overlay behavior.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ArchivalTextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Options Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = ArchivalSurfaceContainer
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    BrowserOptionRow(
                        title = "In-App Browser",
                        description = "Most private — sandboxed viewer with zero shared session cookies, trackers, or signed-in account telemetry.",
                        badgeText = "RECOMMENDED • PRIVATE",
                        icon = Icons.Default.Shield,
                        selected = currentPref == BrowserPreference.IN_APP,
                        onSelect = { viewModel.onBrowserPreferenceSelected(BrowserPreference.IN_APP) }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = ArchivalSurfaceContainerHigh
                    )

                    BrowserOptionRow(
                        title = "System Preferred (Custom Tabs)",
                        description = "Opens lightweight system overlays without fully departing PixelMarks. Retains fast back navigation.",
                        icon = Icons.Default.Web,
                        selected = currentPref == BrowserPreference.SYSTEM_PREFERRED,
                        onSelect = { viewModel.onBrowserPreferenceSelected(BrowserPreference.SYSTEM_PREFERRED) }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = ArchivalSurfaceContainerHigh
                    )

                    BrowserOptionRow(
                        title = "Default Browser (External)",
                        description = "Dispatches URL to your default system browser (Chrome, Firefox, Brave) with full desktop extensions and saved passwords.",
                        icon = Icons.Default.OpenInBrowser,
                        selected = currentPref == BrowserPreference.DEFAULT_BROWSER,
                        onSelect = { viewModel.onBrowserPreferenceSelected(BrowserPreference.DEFAULT_BROWSER) }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = ArchivalSurfaceContainerHigh
                    )

                    BrowserOptionRow(
                        title = "Ask Every Time",
                        description = "Presents an interactive prompt on each link tap allowing contextual browser selection.",
                        icon = Icons.Default.QuestionMark,
                        selected = currentPref == BrowserPreference.ASK_EVERY_TIME,
                        onSelect = { viewModel.onBrowserPreferenceSelected(BrowserPreference.ASK_EVERY_TIME) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BrowserOptionRow(
    title: String,
    description: String,
    icon: ImageVector,
    selected: Boolean,
    badgeText: String? = null,
    onSelect: () -> Unit
) {
    val iconBgColor by animateColorAsState(
        targetValue = if (selected) ArchivalPrimary.copy(alpha = 0.15f) else ArchivalSurfaceContainerLow,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "browserOptionIconBg"
    )
    val iconTint by animateColorAsState(
        targetValue = if (selected) ArchivalPrimary else ArchivalTextSecondary,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "browserOptionIconTint"
    )
    val titleColor by animateColorAsState(
        targetValue = if (selected) ArchivalPrimary else ArchivalTextPrimary,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "browserOptionTitleColor"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor
                )
                if (badgeText != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(ArchivalPrimary.copy(alpha = 0.18f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = ArchivalPrimary,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = ArchivalTextSecondary,
                lineHeight = 17.sp
            )
        }

        RadioButton(
            selected = selected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(
                selectedColor = ArchivalPrimary,
                unselectedColor = ArchivalTextSecondary.copy(alpha = 0.6f)
            ),
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

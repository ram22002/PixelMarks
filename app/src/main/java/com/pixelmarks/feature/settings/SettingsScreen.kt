package com.pixelmarks.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pixelmarks.app.PixelMarksApplication
import com.pixelmarks.core.browser.BrowserPreference
import com.pixelmarks.ui.components.PixelMarksScreenScaffold
import com.pixelmarks.ui.theme.PixelMarksTheme
import com.pixelmarks.ui.theme.Spacing

@Composable
fun SettingsScreen(
    onNavigateToConnectedDevices: () -> Unit,
    onNavigateToBackup: () -> Unit,
    viewModel: SettingsViewModel = viewModel(
        factory = run {
            val app = LocalContext.current.applicationContext as PixelMarksApplication
            SettingsViewModel.provideFactory(
                observeBrowserPreferenceUseCase = app.container.observeBrowserPreferenceUseCase,
                setBrowserPreferenceUseCase = app.container.setBrowserPreferenceUseCase
            )
        }
    )
) {
    val currentBrowserPref by viewModel.browserPreference.collectAsState()

    PixelMarksScreenScaffold(
        title = "Settings"
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                        text = "App preferences, appearance, browser link opening, backup, and PC sync settings.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
                onClick = onNavigateToConnectedDevices,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Connected PC Devices")
            }

            Button(
                onClick = onNavigateToBackup,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Backup & Restore")
            }
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

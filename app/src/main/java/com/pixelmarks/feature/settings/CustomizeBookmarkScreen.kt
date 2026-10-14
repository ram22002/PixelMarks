package com.pixelmarks.feature.settings

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.ViewHeadline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.pixelmarks.ui.theme.ArchivalBackground
import com.pixelmarks.ui.theme.ArchivalPrimary
import com.pixelmarks.ui.theme.ArchivalSurfaceContainer
import com.pixelmarks.ui.theme.ArchivalSurfaceContainerHigh
import com.pixelmarks.ui.theme.ArchivalSurfaceContainerLow
import com.pixelmarks.ui.theme.ArchivalTextPrimary
import com.pixelmarks.ui.theme.ArchivalTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizeBookmarkScreen(
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
    val showFavicon by viewModel.bookmarkShowFavicon.collectAsState()
    val showDomain by viewModel.bookmarkShowDomain.collectAsState()
    val showNotes by viewModel.bookmarkShowNotes.collectAsState()
    val compactMode by viewModel.bookmarkCompactMode.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Customize Bookmark",
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ArchivalBackground)
            )
        },
        containerColor = ArchivalBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Preview Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ArchivalSurfaceContainerLow),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "PREVIEW",
                        style = MaterialTheme.typography.labelSmall,
                        color = ArchivalPrimary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = ArchivalSurfaceContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(if (compactMode) 10.dp else 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (showFavicon) {
                                Box(
                                    modifier = Modifier
                                        .size(if (compactMode) 28.dp else 36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ArchivalSurfaceContainerHigh),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Link,
                                        contentDescription = null,
                                        tint = ArchivalPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.padding(horizontal = 6.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Jetpack Compose Modern UI Architecture",
                                    style = if (compactMode) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ArchivalTextPrimary
                                )
                                if (showDomain) {
                                    Text(
                                        text = "developer.android.com",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ArchivalPrimary
                                    )
                                }
                                if (showNotes && !compactMode) {
                                    Text(
                                        text = "\"Review state hoisting pattern before sprint refactor...\"",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ArchivalTextSecondary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Toggles
            Text(
                text = "CARD ELEMENTS & DENSITY",
                style = MaterialTheme.typography.labelSmall,
                color = ArchivalPrimary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ArchivalSurfaceContainer),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    CustomizeToggleRow(
                        title = "Website Favicons",
                        subtitle = "Display origin website icons on cards",
                        icon = Icons.Default.Image,
                        checked = showFavicon,
                        onCheckedChange = { viewModel.setBookmarkShowFavicon(it) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = ArchivalSurfaceContainerHigh)

                    CustomizeToggleRow(
                        title = "Domain Host",
                        subtitle = "Show cleaned domain name below title",
                        icon = Icons.Default.Link,
                        checked = showDomain,
                        onCheckedChange = { viewModel.setBookmarkShowDomain(it) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = ArchivalSurfaceContainerHigh)

                    CustomizeToggleRow(
                        title = "Personal Notes",
                        subtitle = "Show margin note quotes on the card",
                        icon = Icons.Default.Description,
                        checked = showNotes,
                        onCheckedChange = { viewModel.setBookmarkShowNotes(it) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = ArchivalSurfaceContainerHigh)

                    CustomizeToggleRow(
                        title = "Compact Card Density",
                        subtitle = "Condense cards to fit more items on screen",
                        icon = Icons.Default.ViewHeadline,
                        checked = compactMode,
                        onCheckedChange = { viewModel.setBookmarkCompactMode(it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomizeToggleRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
                imageVector = icon,
                contentDescription = null,
                tint = if (checked) ArchivalPrimary else ArchivalTextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = ArchivalTextPrimary)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = ArchivalTextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ArchivalBackground,
                checkedTrackColor = ArchivalPrimary,
                uncheckedTrackColor = ArchivalSurfaceContainerHigh
            )
        )
    }
}

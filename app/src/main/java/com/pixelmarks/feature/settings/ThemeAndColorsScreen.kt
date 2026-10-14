package com.pixelmarks.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pixelmarks.app.PixelMarksApplication
import com.pixelmarks.data.repository.AppColorScheme
import com.pixelmarks.data.repository.ThemeMode
import com.pixelmarks.ui.theme.ArchivalBackground
import com.pixelmarks.ui.theme.ArchivalPrimary
import com.pixelmarks.ui.theme.ArchivalSurfaceContainer
import com.pixelmarks.ui.theme.ArchivalSurfaceContainerHigh
import com.pixelmarks.ui.theme.ArchivalTextPrimary
import com.pixelmarks.ui.theme.ArchivalTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeAndColorsScreen(
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
    val themeMode by viewModel.themeMode.collectAsState()
    val dynamicTheme by viewModel.dynamicThemeEnabled.collectAsState()
    val colorScheme by viewModel.colorScheme.collectAsState()

    val accentColor = Color(android.graphics.Color.parseColor(colorScheme.accentHex))
    val primaryColor = Color(android.graphics.Color.parseColor(colorScheme.primaryHex))
    val secondaryColor = Color(android.graphics.Color.parseColor(colorScheme.secondaryHex))
    val tertiaryColor = Color(android.graphics.Color.parseColor(colorScheme.tertiaryHex))

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Theme & Colors",
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
                .padding(paddingValues),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Live Interactive Phone Mock Preview (Screenshot 2)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                val mockBgColor = when (themeMode) {
                    ThemeMode.LIGHT -> Color(0xFFF7F5F0)
                    ThemeMode.AMOLED -> Color(0xFF000000)
                    else -> Color(0xFF191617)
                }

                Box(
                    modifier = Modifier
                        .width(200.dp)
                        .height(310.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(mockBgColor)
                        .border(3.dp, accentColor.copy(alpha = 0.55f), RoundedCornerShape(32.dp))
                        .padding(14.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Top Header Dots
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(primaryColor)
                            )
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(secondaryColor)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Large Hero Card (Soft Pastel Primary)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(95.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(primaryColor)
                        )

                        // Split Column Cards (Secondary + Tertiary Pastels)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(85.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(secondaryColor)
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(tertiaryColor)
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Bottom Floating Button (Deep Accent Tone)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(accentColor)
                            )
                        }
                    }
                }
            }

            // Bottom Theme & Color Palette Card (Screenshot 2)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = ArchivalSurfaceContainer)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Section 1: Theme Mode
                    Text(
                        text = "Theme Mode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = ArchivalTextPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1: System Default (Cycle / Sync icon)
                        val isSystemSelected = themeMode == ThemeMode.SYSTEM
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (isSystemSelected) primaryColor.copy(alpha = 0.25f) else Color(0xFF1E1E1E))
                                .border(
                                    width = if (isSystemSelected) 2.dp else 1.dp,
                                    color = if (isSystemSelected) primaryColor else Color(0xFF333333),
                                    shape = CircleShape
                                )
                                .clickable { viewModel.setThemeMode(ThemeMode.SYSTEM) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "System Default",
                                tint = if (isSystemSelected) primaryColor else Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Divider line
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(26.dp)
                                .background(ArchivalSurfaceContainerHigh)
                        )

                        // 2: Light Mode (Pure White circle)
                        val isLightSelected = themeMode == ThemeMode.LIGHT
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFFFFF))
                                .border(
                                    width = if (isLightSelected) 3.dp else 1.dp,
                                    color = if (isLightSelected) primaryColor else Color(0xFFDDDDDD),
                                    shape = CircleShape
                                )
                                .clickable { viewModel.setThemeMode(ThemeMode.LIGHT) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLightSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Light Selected",
                                    tint = Color.Black,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        // 3: Dark Mode (Dark Charcoal circle)
                        val isDarkSelected = themeMode == ThemeMode.DARK
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF262626))
                                .border(
                                    width = if (isDarkSelected) 3.dp else 1.dp,
                                    color = if (isDarkSelected) primaryColor else Color(0xFF444444),
                                    shape = CircleShape
                                )
                                .clickable { viewModel.setThemeMode(ThemeMode.DARK) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isDarkSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Dark Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        // 4: AMOLED Pure Black Mode
                        val isAmoledSelected = themeMode == ThemeMode.AMOLED
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF000000))
                                .border(
                                    width = if (isAmoledSelected) 3.dp else 1.dp,
                                    color = if (isAmoledSelected) primaryColor else Color(0xFF555555),
                                    shape = CircleShape
                                )
                                .clickable { viewModel.setThemeMode(ThemeMode.AMOLED) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isAmoledSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "AMOLED Selected",
                                    tint = primaryColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    // Section 2: Color Palette
                    Text(
                        text = "Color Palette",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = ArchivalTextPrimary
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Palette Icon squircle box
                        Box(
                            modifier = Modifier
                                .size(width = 48.dp, height = 48.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF1E1E1E))
                                .border(1.dp, Color(0xFF333333), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "Color Palette",
                                tint = primaryColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // All Palette Swatches matching Screenshot 2 (Rounded Rectangles)
                        val allPalettes = listOf(
                            AppColorScheme.ROSE_GOLD,
                            AppColorScheme.CRIMSON_GOLD,
                            AppColorScheme.PURPLE_PEACH,
                            AppColorScheme.INDIGO_PINK,
                            AppColorScheme.NAVY_LAVENDER,
                            AppColorScheme.ARCHIVAL_GOLD,
                            AppColorScheme.EMERALD_SAGE
                        )

                        allPalettes.forEach { palette ->
                            val isSelected = colorScheme == palette
                            val aColor = Color(android.graphics.Color.parseColor(palette.accentHex))
                            val pColor = Color(android.graphics.Color.parseColor(palette.primaryHex))
                            val sColor = Color(android.graphics.Color.parseColor(palette.secondaryHex))

                            Box(
                                modifier = Modifier
                                    .size(width = 56.dp, height = 48.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) Color.White else Color(0xFF383838),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable { viewModel.setColorScheme(palette) }
                                    .padding(if (isSelected) 3.5.dp else 1.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                // Split Two-Tone Swatch (Top: Dark Accent, Bottom: Two Pastels)
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                            .background(aColor)
                                    )
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxSize()
                                                .background(pColor)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxSize()
                                                .background(sColor)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Section 3: Dynamic Theme Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Dynamic Colors",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = ArchivalTextPrimary
                            )
                            Text(
                                text = "Adapt theme colors according to bookmark images",
                                style = MaterialTheme.typography.bodySmall,
                                color = ArchivalTextSecondary
                            )
                        }
                        Switch(
                            checked = dynamicTheme,
                            onCheckedChange = { viewModel.setDynamicTheme(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ArchivalBackground,
                                checkedTrackColor = primaryColor,
                                uncheckedTrackColor = ArchivalSurfaceContainerHigh
                            )
                        )
                    }
                }
            }
        }
    }
}

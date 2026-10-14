package com.pixelmarks.feature.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.pixelmarks.ui.theme.ArchivalOutlineVariant
import com.pixelmarks.ui.theme.ArchivalSurfaceContainer
import com.pixelmarks.ui.theme.ArchivalSurfaceContainerHigh
import com.pixelmarks.ui.theme.ArchivalSurfaceContainerHighest
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

    val animatedAccentColor by animateColorAsState(
        targetValue = accentColor,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "accentAnim"
    )
    val animatedPrimaryColor by animateColorAsState(
        targetValue = primaryColor,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "primaryAnim"
    )
    val animatedSecondaryColor by animateColorAsState(
        targetValue = secondaryColor,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "secAnim"
    )
    val animatedTertiaryColor by animateColorAsState(
        targetValue = tertiaryColor,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "tertAnim"
    )

    val isSystemDark = isSystemInDarkTheme()
    val isEffectiveDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemDark
        ThemeMode.DARK, ThemeMode.AMOLED -> true
        ThemeMode.LIGHT -> false
    }

    val mockBgColor = when {
        !isEffectiveDark -> Color(0xFFF7F5F0)
        themeMode == ThemeMode.AMOLED -> Color(0xFF000000)
        else -> Color(0xFF191617)
    }
    val animatedMockBgColor by animateColorAsState(
        targetValue = mockBgColor,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "mockBgAnim"
    )

    val animatedScreenBg by animateColorAsState(
        targetValue = ArchivalBackground,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "screenBgAnim"
    )
    val animatedCardBg by animateColorAsState(
        targetValue = ArchivalSurfaceContainer,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "cardBgAnim"
    )

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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = animatedScreenBg)
            )
        },
        containerColor = animatedScreenBg
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
                Box(
                    modifier = Modifier
                        .width(208.dp)
                        .height(318.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(animatedMockBgColor)
                        .border(3.dp, animatedAccentColor.copy(alpha = 0.55f), RoundedCornerShape(32.dp))
                        .padding(14.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Phone Speaker Pill / Notch
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(animatedAccentColor.copy(alpha = 0.35f))
                                .align(Alignment.CenterHorizontally)
                        )

                        // Top Header Dots
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(animatedPrimaryColor)
                            )
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(animatedSecondaryColor)
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // Large Hero Card (Soft Pastel Primary)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(95.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(animatedPrimaryColor)
                                .padding(10.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(60.dp)
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(animatedAccentColor.copy(alpha = 0.4f))
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.75f)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(animatedAccentColor.copy(alpha = 0.25f))
                                )
                            }
                        }

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
                                    .background(animatedSecondaryColor)
                                    .padding(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(animatedAccentColor.copy(alpha = 0.35f))
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(animatedTertiaryColor)
                                    .padding(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(32.dp)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(animatedAccentColor.copy(alpha = 0.35f))
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Bottom Floating Button (Deep Accent Tone - smoothly animated!)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(animatedAccentColor)
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
                colors = CardDefaults.cardColors(containerColor = animatedCardBg)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Section 1: Theme Mode Header + Active Mode Label
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Theme Mode",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = ArchivalTextPrimary
                        )
                        Text(
                            text = themeMode.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = animatedPrimaryColor,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1: System Default (Cycle / Sync icon)
                        val isSystemSelected = themeMode == ThemeMode.SYSTEM
                        val sysBorderWidth by animateDpAsState(
                            targetValue = if (isSystemSelected) 3.dp else 1.dp,
                            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                            label = "sysBorderWidth"
                        )
                        val sysBorderColor by animateColorAsState(
                            targetValue = if (isSystemSelected) animatedPrimaryColor else ArchivalOutlineVariant.copy(alpha = 0.5f),
                            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                            label = "sysBorderColor"
                        )
                        val sysBgColor by animateColorAsState(
                            targetValue = if (isSystemSelected) animatedPrimaryColor.copy(alpha = 0.22f) else ArchivalSurfaceContainerHigh,
                            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                            label = "sysBgColor"
                        )
                        val sysScale by animateFloatAsState(
                            targetValue = if (isSystemSelected) 1.06f else 1.0f,
                            animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow),
                            label = "sysScale"
                        )

                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .graphicsLayer {
                                    scaleX = sysScale
                                    scaleY = sysScale
                                }
                                .clip(CircleShape)
                                .background(sysBgColor)
                                .border(
                                    width = sysBorderWidth,
                                    color = sysBorderColor,
                                    shape = CircleShape
                                )
                                .clickable { viewModel.setThemeMode(ThemeMode.SYSTEM) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "System Default",
                                tint = if (isSystemSelected) animatedPrimaryColor else ArchivalTextPrimary,
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
                        ThemeModeCircle(
                            isSelected = isLightSelected,
                            fillColor = Color(0xFFFFFFFF),
                            unselectedBorder = Color(0xFFDDDDDD),
                            selectedBorder = animatedPrimaryColor,
                            checkTint = Color.Black,
                            onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                            contentDescription = "Light Selected"
                        )

                        // 3: Dark Mode (Dark Charcoal circle)
                        val isDarkSelected = themeMode == ThemeMode.DARK
                        ThemeModeCircle(
                            isSelected = isDarkSelected,
                            fillColor = Color(0xFF262626),
                            unselectedBorder = Color(0xFF444444),
                            selectedBorder = animatedPrimaryColor,
                            checkTint = Color.White,
                            onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                            contentDescription = "Dark Selected"
                        )

                        // 4: AMOLED Pure Black Mode
                        val isAmoledSelected = themeMode == ThemeMode.AMOLED
                        ThemeModeCircle(
                            isSelected = isAmoledSelected,
                            fillColor = Color(0xFF000000),
                            unselectedBorder = Color(0xFF555555),
                            selectedBorder = animatedPrimaryColor,
                            checkTint = animatedPrimaryColor,
                            onClick = { viewModel.setThemeMode(ThemeMode.AMOLED) },
                            contentDescription = "AMOLED Selected"
                        )
                    }

                    // Section 2: Color Palette Header + Active Palette Label
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Color Palette",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = ArchivalTextPrimary
                        )
                        Text(
                            text = colorScheme.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = animatedPrimaryColor,
                            fontWeight = FontWeight.Medium
                        )
                    }

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
                                .background(ArchivalSurfaceContainerHigh)
                                .border(1.dp, ArchivalOutlineVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "Color Palette",
                                tint = animatedPrimaryColor,
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

                            val animatedBorderWidth by animateDpAsState(
                                targetValue = if (isSelected) 2.5.dp else 1.dp,
                                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                                label = "paletteBorderWidth_${palette.name}"
                            )
                            val animatedBorderColor by animateColorAsState(
                                targetValue = if (isSelected) animatedPrimaryColor else ArchivalOutlineVariant.copy(alpha = 0.45f),
                                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                                label = "paletteBorderColor_${palette.name}"
                            )
                            val animatedPadding by animateDpAsState(
                                targetValue = if (isSelected) 3.5.dp else 1.dp,
                                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                                label = "palettePadding_${palette.name}"
                            )
                            val animatedScale by animateFloatAsState(
                                targetValue = if (isSelected) 1.06f else 1.0f,
                                animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow),
                                label = "paletteScale_${palette.name}"
                            )

                            Box(
                                modifier = Modifier
                                    .size(width = 56.dp, height = 48.dp)
                                    .graphicsLayer {
                                        scaleX = animatedScale
                                        scaleY = animatedScale
                                    }
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(
                                        width = animatedBorderWidth,
                                        color = animatedBorderColor,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable { viewModel.setColorScheme(palette) }
                                    .padding(animatedPadding)
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
                                checkedThumbColor = animatedScreenBg,
                                checkedTrackColor = animatedPrimaryColor,
                                uncheckedTrackColor = ArchivalSurfaceContainerHigh
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeModeCircle(
    isSelected: Boolean,
    fillColor: Color,
    unselectedBorder: Color,
    selectedBorder: Color,
    checkTint: Color,
    onClick: () -> Unit,
    contentDescription: String
) {
    val animatedBorderWidth by animateDpAsState(
        targetValue = if (isSelected) 3.dp else 1.dp,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "modeBorderWidth"
    )
    val animatedBorderColor by animateColorAsState(
        targetValue = if (isSelected) selectedBorder else unselectedBorder,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "modeBorderColor"
    )
    val animatedScale by animateFloatAsState(
        targetValue = if (isSelected) 1.06f else 1.0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow),
        label = "modeScale"
    )

    Box(
        modifier = Modifier
            .size(48.dp)
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .clip(CircleShape)
            .background(fillColor)
            .border(
                width = animatedBorderWidth,
                color = animatedBorderColor,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = isSelected,
            enter = scaleIn(
                animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium)
            ) + fadeIn(animationSpec = tween(150)),
            exit = scaleOut(
                animationSpec = tween(120)
            ) + fadeOut(animationSpec = tween(120))
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = contentDescription,
                tint = checkTint,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

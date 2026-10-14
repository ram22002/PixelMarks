package com.pixelmarks.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import com.pixelmarks.data.repository.AppColorScheme
import com.pixelmarks.data.repository.DisplayFont
import com.pixelmarks.data.repository.ThemeMode

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark
)

@Composable
fun PixelMarksTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    appColorScheme: AppColorScheme = AppColorScheme.ARCHIVAL_GOLD,
    displayFont: DisplayFont = DisplayFont.EDITORIAL_SERIF,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val baseScheme = if (isDark) DarkColorScheme else LightColorScheme
    val primaryColor = when (appColorScheme) {
        AppColorScheme.ARCHIVAL_GOLD -> if (isDark) ArchivalPrimary else PrimaryLight
        AppColorScheme.TERRACOTTA -> if (isDark) TerracottaAccent else Color(0xFF8B4B32)
        AppColorScheme.EMERALD -> if (isDark) Color(0xFF7EBA8C) else Color(0xFF2E6B3E)
        AppColorScheme.OBSIDIAN -> if (isDark) Color(0xFFA3B8CC) else Color(0xFF365A7C)
    }
    val styledScheme = baseScheme.copy(
        primary = primaryColor
    )

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> styledScheme
    }

    val font = when (displayFont) {
        DisplayFont.EDITORIAL_SERIF -> FontFamily.Serif
        DisplayFont.TECHNICAL_MONO -> FontFamily.Monospace
        DisplayFont.CLEAN_SANS -> FontFamily.SansSerif
    }
    val typography = getPixelMarksTypography(font)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        shapes = PixelMarksShapes,
        content = content
    )
}

@Composable
fun PixelMarksTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    PixelMarksTheme(
        themeMode = if (darkTheme) ThemeMode.DARK else ThemeMode.LIGHT,
        dynamicColor = dynamicColor,
        content = content
    )
}


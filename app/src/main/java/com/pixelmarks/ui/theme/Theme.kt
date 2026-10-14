package com.pixelmarks.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import com.pixelmarks.data.repository.AppColorScheme
import com.pixelmarks.data.repository.DisplayFont
import com.pixelmarks.data.repository.ThemeMode

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
        ThemeMode.DARK, ThemeMode.AMOLED -> true
        ThemeMode.LIGHT -> false
    }

    val isAmoled = themeMode == ThemeMode.AMOLED

    // Select base palette based on mode
    val basePalette = when {
        !isDark -> DefaultLightArchivalPalette
        isAmoled -> DefaultAmoledArchivalPalette
        else -> DefaultDarkArchivalPalette
    }

    // Apply chosen color palette
    val primaryColor = Color(android.graphics.Color.parseColor(appColorScheme.primaryHex))
    val secondaryColor = Color(android.graphics.Color.parseColor(appColorScheme.secondaryHex))
    val tertiaryColor = Color(android.graphics.Color.parseColor(appColorScheme.tertiaryHex))

    val activePalette = basePalette.copy(
        primary = primaryColor,
        secondary = secondaryColor,
        tertiary = tertiaryColor
    )

    // Build Material 3 ColorScheme matching activePalette
    val baseMaterialScheme = if (isDark) {
        darkColorScheme(
            primary = activePalette.primary,
            onPrimary = activePalette.onPrimary,
            secondary = activePalette.secondary,
            onSecondary = activePalette.onSecondary,
            tertiary = activePalette.tertiary,
            onTertiary = activePalette.onTertiary,
            background = activePalette.background,
            onBackground = activePalette.textPrimary,
            surface = activePalette.surface,
            onSurface = activePalette.textPrimary,
            surfaceVariant = activePalette.surfaceVariant,
            onSurfaceVariant = activePalette.textSecondary,
            surfaceContainerLowest = activePalette.surfaceContainerLowest,
            surfaceContainerLow = activePalette.surfaceContainerLow,
            surfaceContainer = activePalette.surfaceContainer,
            surfaceContainerHigh = activePalette.surfaceContainerHigh,
            surfaceContainerHighest = activePalette.surfaceContainerHighest,
            outline = activePalette.outline,
            outlineVariant = activePalette.outlineVariant,
            error = activePalette.error,
            onError = activePalette.onError
        )
    } else {
        lightColorScheme(
            primary = activePalette.primary,
            onPrimary = activePalette.onPrimary,
            secondary = activePalette.secondary,
            onSecondary = activePalette.onSecondary,
            tertiary = activePalette.tertiary,
            onTertiary = activePalette.onTertiary,
            background = activePalette.background,
            onBackground = activePalette.textPrimary,
            surface = activePalette.surface,
            onSurface = activePalette.textPrimary,
            surfaceVariant = activePalette.surfaceVariant,
            onSurfaceVariant = activePalette.textSecondary,
            surfaceContainerLowest = activePalette.surfaceContainerLowest,
            surfaceContainerLow = activePalette.surfaceContainerLow,
            surfaceContainer = activePalette.surfaceContainer,
            surfaceContainerHigh = activePalette.surfaceContainerHigh,
            surfaceContainerHighest = activePalette.surfaceContainerHighest,
            outline = activePalette.outline,
            outlineVariant = activePalette.outlineVariant,
            error = activePalette.error,
            onError = activePalette.onError
        )
    }

    val finalMaterialScheme = baseMaterialScheme

    val font = when (displayFont) {
        DisplayFont.EDITORIAL_SERIF -> FontFamily.Serif
        DisplayFont.TECHNICAL_MONO -> FontFamily.Monospace
        DisplayFont.CLEAN_SANS -> FontFamily.SansSerif
    }
    val typography = getPixelMarksTypography(font)

    CompositionLocalProvider(
        LocalArchivalColors provides activePalette
    ) {
        MaterialTheme(
            colorScheme = finalMaterialScheme,
            typography = typography,
            shapes = PixelMarksShapes,
            content = content
        )
    }
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

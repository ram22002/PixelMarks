package com.pixelmarks.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Cohesive palette model allowing the whole app to adapt between Light, Dark,
 * AMOLED pure black, custom color palettes, and bookmark dynamic colors.
 */
data class ArchivalPalette(
    val background: Color,
    val surface: Color,
    val surfaceContainerLowest: Color,
    val surfaceContainerLow: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val surfaceVariant: Color,
    val surfaceBright: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val outline: Color,
    val outlineVariant: Color,
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val onSecondary: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val error: Color,
    val onError: Color
)

val DefaultDarkArchivalPalette = ArchivalPalette(
    background = Color(0xFF141714),
    surface = Color(0xFF141714),
    surfaceContainerLowest = Color(0xFF0F120F),
    surfaceContainerLow = Color(0xFF1B1F1B),
    surfaceContainer = Color(0xFF222722),
    surfaceContainerHigh = Color(0xFF2B312B),
    surfaceContainerHighest = Color(0xFF353C35),
    surfaceVariant = Color(0xFF353C35),
    surfaceBright = Color(0xFF3F473F),
    textPrimary = Color(0xFFEDE1CB),
    textSecondary = Color(0xFFD2C5B5),
    outline = Color(0xFF9A8F81),
    outlineVariant = Color(0xFF4E4539),
    primary = Color(0xFFEAC07F), // Warm Brass / Gold
    onPrimary = Color(0xFF432C00),
    secondary = Color(0xFFF9B89A), // Terracotta
    onSecondary = Color(0xFF4D2611),
    tertiary = Color(0xFF7EBA8C), // Sage Emerald
    onTertiary = Color(0xFF11381B),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

val DefaultAmoledArchivalPalette = DefaultDarkArchivalPalette.copy(
    background = Color(0xFF000000),
    surface = Color(0xFF000000),
    surfaceContainerLowest = Color(0xFF0A0A0A),
    surfaceContainerLow = Color(0xFF121212),
    surfaceContainer = Color(0xFF1A1A1A),
    surfaceContainerHigh = Color(0xFF242424),
    surfaceContainerHighest = Color(0xFF2E2E2E),
    surfaceVariant = Color(0xFF242424)
)

val DefaultLightArchivalPalette = ArchivalPalette(
    background = Color(0xFFF7F5F0),
    surface = Color(0xFFFDFCFA),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF0ECE3),
    surfaceContainer = Color(0xFFE8E3D7),
    surfaceContainerHigh = Color(0xFFDFD9CB),
    surfaceContainerHighest = Color(0xFFD5CEBE),
    surfaceVariant = Color(0xFFE2DCCF),
    surfaceBright = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF221E18),
    textSecondary = Color(0xFF5A5348),
    outline = Color(0xFF8A8275),
    outlineVariant = Color(0xFFCCC5B8),
    primary = Color(0xFF8B6424),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFFA05537),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFF387246),
    onTertiary = Color(0xFFFFFFFF),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF)
)

val LocalArchivalColors = staticCompositionLocalOf {
    DefaultDarkArchivalPalette
}

// Dynamic theme-aware properties for seamless backward compatibility
val ArchivalBackground: Color
    @Composable @ReadOnlyComposable get() = LocalArchivalColors.current.background

val ArchivalSurface: Color
    @Composable @ReadOnlyComposable get() = LocalArchivalColors.current.surface

val ArchivalSurfaceContainerLowest: Color
    @Composable @ReadOnlyComposable get() = LocalArchivalColors.current.surfaceContainerLowest

val ArchivalSurfaceContainerLow: Color
    @Composable @ReadOnlyComposable get() = LocalArchivalColors.current.surfaceContainerLow

val ArchivalSurfaceContainer: Color
    @Composable @ReadOnlyComposable get() = LocalArchivalColors.current.surfaceContainer

val ArchivalSurfaceContainerHigh: Color
    @Composable @ReadOnlyComposable get() = LocalArchivalColors.current.surfaceContainerHigh

val ArchivalSurfaceContainerHighest: Color
    @Composable @ReadOnlyComposable get() = LocalArchivalColors.current.surfaceContainerHighest

val ArchivalSurfaceVariant: Color
    @Composable @ReadOnlyComposable get() = LocalArchivalColors.current.surfaceVariant

val ArchivalSurfaceBright: Color
    @Composable @ReadOnlyComposable get() = LocalArchivalColors.current.surfaceBright

val ArchivalTextPrimary: Color
    @Composable @ReadOnlyComposable get() = LocalArchivalColors.current.textPrimary

val ArchivalTextSecondary: Color
    @Composable @ReadOnlyComposable get() = LocalArchivalColors.current.textSecondary

val ArchivalOutline: Color
    @Composable @ReadOnlyComposable get() = LocalArchivalColors.current.outline

val ArchivalOutlineVariant: Color
    @Composable @ReadOnlyComposable get() = LocalArchivalColors.current.outlineVariant

val ArchivalPrimary: Color
    @Composable @ReadOnlyComposable get() = LocalArchivalColors.current.primary

val ArchivalOnPrimary: Color
    @Composable @ReadOnlyComposable get() = LocalArchivalColors.current.onPrimary

val ArchivalSecondary: Color
    @Composable @ReadOnlyComposable get() = LocalArchivalColors.current.secondary

val ArchivalOnSecondary: Color
    @Composable @ReadOnlyComposable get() = LocalArchivalColors.current.onSecondary

val ArchivalTertiary: Color
    @Composable @ReadOnlyComposable get() = LocalArchivalColors.current.tertiary

val ArchivalOnTertiary: Color
    @Composable @ReadOnlyComposable get() = LocalArchivalColors.current.onTertiary

val ArchivalError: Color
    @Composable @ReadOnlyComposable get() = LocalArchivalColors.current.error

val ArchivalOnError: Color
    @Composable @ReadOnlyComposable get() = LocalArchivalColors.current.onError

val BrassGold: Color
    @Composable @ReadOnlyComposable get() = LocalArchivalColors.current.primary

val BrassGoldDark: Color
    @Composable @ReadOnlyComposable get() = LocalArchivalColors.current.surfaceContainerHighest

val BrassGoldLight: Color
    @Composable @ReadOnlyComposable get() = LocalArchivalColors.current.secondary

val TerracottaAccent: Color
    @Composable @ReadOnlyComposable get() = LocalArchivalColors.current.secondary

// Static color constants for fallback and previews
val StaticArchivalBackground = Color(0xFF141714)
val StaticArchivalSurface = Color(0xFF141714)
val StaticArchivalSurfaceContainer = Color(0xFF222722)
val StaticArchivalPrimary = Color(0xFFEAC07F)
val StaticArchivalTextPrimary = Color(0xFFEDE1CB)
val StaticArchivalTextSecondary = Color(0xFFD2C5B5)
val StaticTerracottaAccent = Color(0xFFF9B89A)

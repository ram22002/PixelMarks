package com.pixelmarks.ui.theme

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import coil3.toBitmap
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import com.pixelmarks.data.repository.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * Extracts a dominant, vibrant brand color from an image URL (or falls back to a deterministic domain hash),
 * and creates a harmonized dynamic [ArchivalPalette] when dynamic themes are enabled.
 */
@Composable
fun rememberBookmarkDynamicPalette(
    imageUrl: String?,
    domain: String,
    themeMode: ThemeMode,
    enabled: Boolean
): ArchivalPalette {
    val context = LocalContext.current
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemDark
        ThemeMode.DARK, ThemeMode.AMOLED -> true
        ThemeMode.LIGHT -> false
    }
    val isAmoled = themeMode == ThemeMode.AMOLED
    val currentPalette = LocalArchivalColors.current

    if (!enabled) {
        return currentPalette
    }

    var dominantColor by remember(imageUrl, domain) {
        mutableStateOf(generateFallbackColor(domain))
    }

    LaunchedEffect(imageUrl, domain) {
        if (!imageUrl.isNullOrBlank()) {
            withContext(Dispatchers.IO) {
                try {
                    val loader = ImageLoader(context)
                    val request = ImageRequest.Builder(context)
                        .data(imageUrl)
                        .allowHardware(false)
                        .build()
                    val result = loader.execute(request)
                    if (result is SuccessResult) {
                        val bitmap = result.image.toBitmap()
                        val extracted = extractDominantColor(bitmap)
                        if (extracted != null) {
                            withContext(Dispatchers.Main) {
                                dominantColor = extracted
                            }
                        }
                    }
                } catch (_: Exception) {
                    // Fall back gracefully to domain-based color
                }
            }
        }
    }

    return remember(dominantColor, isDark, isAmoled) {
        createDynamicBookmarkPalette(dominantColor, isDark, isAmoled)
    }
}

/**
 * Extracts the most prominent, saturated color from a scaled bitmap.
 */
private fun extractDominantColor(bitmap: Bitmap): Color? {
    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

    var bestColor: Color? = null
    var highestScore = -1f

    for (pixel in pixels) {
        val alpha = (pixel shr 24) and 0xff
        if (alpha < 128) continue

        val r = ((pixel shr 16) and 0xff) / 255f
        val g = ((pixel shr 8) and 0xff) / 255f
        val b = (pixel and 0xff) / 255f

        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min

        // Skip near greys, whites, and deep blacks
        if (delta < 0.15f || max < 0.2f || min > 0.85f) continue

        // Saturation * brightness score
        val saturation = if (max == 0f) 0f else delta / max
        val score = saturation * 0.7f + max * 0.3f

        if (score > highestScore) {
            highestScore = score
            bestColor = Color(r, g, b)
        }
    }

    return bestColor
}

/**
 * Deterministically generates a vibrant accent color based on domain name.
 */
private fun generateFallbackColor(domain: String): Color {
    val hash = abs(domain.hashCode())
    val hues = floatArrayOf(24f, 42f, 150f, 195f, 260f, 330f, 175f, 210f)
    val hue = hues[hash % hues.size]
    val hsv = floatArrayOf(hue, 0.65f, 0.85f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

/**
 * Harmonizes the screen's accent colors with the bookmark's dominant color
 * while keeping backgrounds and card surfaces clean, natural, and comfortable for the eyes.
 */
fun createDynamicBookmarkPalette(
    dominantColor: Color,
    isDark: Boolean,
    isAmoled: Boolean = false
): ArchivalPalette {
    val base = if (!isDark) DefaultLightArchivalPalette else if (isAmoled) DefaultAmoledArchivalPalette else DefaultDarkArchivalPalette

    // Normalize dominant color into a gentle, non-fatiguing pastel / comfortable accent
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(dominantColor.toArgb(), hsv)

    val normalizedAccent = if (isDark) {
        // Dark & AMOLED modes: gentle pastel accent, soft on dark background without harsh contrast
        val normS = hsv[1].coerceIn(0.28f, 0.44f)
        val normV = hsv[2].coerceIn(0.78f, 0.88f)
        Color(android.graphics.Color.HSVToColor(floatArrayOf(hsv[0], normS, normV)))
    } else {
        // Light mode: comfortable, legible contrast without harsh dark neon
        val normS = hsv[1].coerceIn(0.35f, 0.52f)
        val normV = hsv[2].coerceIn(0.42f, 0.56f)
        Color(android.graphics.Color.HSVToColor(floatArrayOf(hsv[0], normS, normV)))
    }

    return base.copy(
        primary = normalizedAccent,
        secondary = normalizedAccent.copy(alpha = 0.85f),
        tertiary = normalizedAccent,
        onPrimary = if (isDark) Color(0xFF141714) else Color.White,
        // Surfaces & backgrounds remain 100% normal, clean, and restful for the eyes
        background = base.background,
        surface = base.surface,
        surfaceContainer = base.surfaceContainer,
        surfaceContainerLow = base.surfaceContainerLow,
        surfaceContainerLowest = base.surfaceContainerLowest,
        surfaceContainerHigh = base.surfaceContainerHigh,
        surfaceContainerHighest = base.surfaceContainerHighest,
        textPrimary = base.textPrimary,
        textSecondary = base.textSecondary,
        outline = base.outline,
        outlineVariant = base.outlineVariant
    )
}

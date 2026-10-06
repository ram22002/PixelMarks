package com.pixelmarks.core.image

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade

/**
 * Reusable Compose component for rendering bookmark preview thumbnails with Coil 3.
 *
 * Implements:
 * - Scheme validation (only http/https permitted).
 * - Graceful loading placeholder and failure fallback.
 * - Dimension clipping and aspect scaling.
 */
@Composable
fun BookmarkThumbnail(
    thumbnailUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp
) {
    val validUrl = if (ImageUrlValidator.isValidImageUrl(thumbnailUrl)) thumbnailUrl else null

    if (validUrl != null) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(validUrl)
                .crossfade(true)
                .build(),
            contentDescription = "Bookmark thumbnail",
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop,
            loading = {
                ThumbnailFallbackBox(icon = Icons.Default.Image)
            },
            error = {
                ThumbnailFallbackBox(icon = Icons.Default.Bookmark)
            }
        )
    }
}

/**
 * Reusable Compose component for rendering website favicons with Coil 3.
 *
 * Implements:
 * - Scheme validation (only http/https permitted).
 * - Subtle local language/globe icon fallback during loading or failure.
 */
@Composable
fun BookmarkFavicon(
    faviconUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 16.dp
) {
    val validUrl = if (ImageUrlValidator.isValidImageUrl(faviconUrl)) faviconUrl else null

    if (validUrl != null) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(validUrl)
                .crossfade(true)
                .build(),
            contentDescription = "Website favicon",
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(2.dp)),
            contentScale = ContentScale.Fit,
            loading = {
                FaviconFallbackIcon(size = size)
            },
            error = {
                FaviconFallbackIcon(size = size)
            }
        )
    } else {
        FaviconFallbackIcon(size = size, modifier = modifier)
    }
}

@Composable
private fun ThumbnailFallbackBox(
    icon: ImageVector
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun FaviconFallbackIcon(
    size: Dp,
    modifier: Modifier = Modifier
) {
    Icon(
        imageVector = Icons.Default.Language,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = modifier.size(size)
    )
}

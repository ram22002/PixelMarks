package com.pixelmarks.feature.bookmark

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pixelmarks.ui.components.PixelMarksScreenScaffold
import com.pixelmarks.ui.theme.Spacing

@Composable
fun BookmarkDetailScreen(
    bookmarkId: Long,
    onNavigateBack: () -> Unit
) {
    PixelMarksScreenScaffold(
        title = "Bookmark Detail",
        onNavigateBack = onNavigateBack
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            Text(
                text = "Bookmark #$bookmarkId",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Detailed metadata, preview image, tags, collection selector, and actions will be shown here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

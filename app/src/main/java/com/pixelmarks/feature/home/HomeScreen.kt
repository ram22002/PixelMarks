package com.pixelmarks.feature.home

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.pixelmarks.ui.components.PixelMarksScreenScaffold
import com.pixelmarks.ui.theme.PixelMarksTheme
import com.pixelmarks.ui.theme.Spacing

@Composable
fun HomeScreen(
    onNavigateToSearch: () -> Unit,
    onNavigateToAddBookmark: () -> Unit,
    onNavigateToBookmarkDetail: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToBrowser: () -> Unit,
    onNavigateToConnectPc: () -> Unit
) {
    PixelMarksScreenScaffold(
        title = "PixelMarks",
        actions = {
            IconButton(onClick = onNavigateToSearch) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search bookmarks"
                )
            }
            IconButton(onClick = onNavigateToAddBookmark) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add new bookmark"
                )
            }
        }
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
                        text = "Home Foundation",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Bookmark dashboard will be implemented here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "Feature Placeholders",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                Button(
                    onClick = onNavigateToAddBookmark,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Add Bookmark")
                }
                OutlinedButton(
                    onClick = onNavigateToBookmarkDetail,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Detail Sample")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                OutlinedButton(
                    onClick = onNavigateToReminders,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reminders")
                }
                OutlinedButton(
                    onClick = onNavigateToBrowser,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Browser")
                }
            }

            OutlinedButton(
                onClick = onNavigateToConnectPc,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Connect to PC")
            }

            Spacer(modifier = Modifier.height(Spacing.large))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    PixelMarksTheme {
        HomeScreen(
            onNavigateToSearch = {},
            onNavigateToAddBookmark = {},
            onNavigateToBookmarkDetail = {},
            onNavigateToReminders = {},
            onNavigateToBrowser = {},
            onNavigateToConnectPc = {}
        )
    }
}

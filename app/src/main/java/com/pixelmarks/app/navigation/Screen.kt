package com.pixelmarks.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Route declarations for all PixelMarks destinations.
 */
sealed class Screen(val route: String) {
    // Bottom Nav Primary Destinations
    data object Home : Screen("home")
    data object Collections : Screen("collections")
    data object Tags : Screen("tags")
    data object Settings : Screen("settings")

    // Secondary / Modal Destinations
    data object Search : Screen("search")
    data object AddBookmark : Screen("add_bookmark")
    data object EditBookmark : Screen("edit_bookmark/{bookmarkId}") {
        fun createRoute(bookmarkId: Long) = "edit_bookmark/$bookmarkId"
    }
    data object BookmarkDetail : Screen("bookmark_detail/{bookmarkId}") {
        fun createRoute(bookmarkId: Long = 1L) = "bookmark_detail/$bookmarkId"
    }
    data object CollectionDetail : Screen("collection_detail/{collectionId}") {
        fun createRoute(collectionId: Long = 1L) = "collection_detail/$collectionId"
    }
    data object TagDetail : Screen("tag_detail/{tagId}") {
        fun createRoute(tagId: Long = 1L) = "tag_detail/$tagId"
    }
    data object Reminders : Screen("reminders")
    data object Browser : Screen("browser")
    data object ConnectPc : Screen("connect_pc")
    data object ConnectedDevices : Screen("connected_devices")
    data object Backup : Screen("backup")
    data object Trash : Screen("trash")
}

/**
 * Definition for bottom navigation bar items.
 */
data class BottomNavItem(
    val screen: Screen,
    val title: String,
    val icon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home, "Home", Icons.Default.Home),
    BottomNavItem(Screen.Collections, "Collections", Icons.Default.Folder),
    BottomNavItem(Screen.Tags, "Tags", Icons.AutoMirrored.Filled.Label),
    BottomNavItem(Screen.Settings, "Settings", Icons.Default.Settings)
)

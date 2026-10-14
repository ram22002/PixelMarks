package com.pixelmarks.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.pixelmarks.feature.backup.BackupScreen
import com.pixelmarks.feature.bookmark.AddBookmarkScreen
import com.pixelmarks.feature.bookmark.BookmarkDetailScreen
import com.pixelmarks.feature.bookmark.EditBookmarkScreen
import com.pixelmarks.feature.browser.BrowserScreen
import com.pixelmarks.feature.collection.CollectionDetailScreen
import com.pixelmarks.feature.collection.CollectionsScreen
import com.pixelmarks.feature.connectpc.ConnectPcScreen
import com.pixelmarks.feature.devices.ConnectedDevicesScreen
import com.pixelmarks.feature.home.HomeScreen
import com.pixelmarks.feature.importexport.HtmlImportExportScreen
import com.pixelmarks.feature.reminders.RemindersScreen
import com.pixelmarks.feature.search.SearchScreen
import com.pixelmarks.feature.settings.AppLockSettingsScreen
import com.pixelmarks.feature.settings.BrowserPreferenceScreen
import com.pixelmarks.feature.settings.CustomizeBookmarkScreen
import com.pixelmarks.feature.settings.SecureFolderSettingsScreen
import com.pixelmarks.feature.settings.SecuritySettingsScreen
import com.pixelmarks.feature.settings.SettingsScreen
import com.pixelmarks.feature.tags.TagDetailScreen
import com.pixelmarks.feature.settings.ThemeAndColorsScreen
import com.pixelmarks.feature.tags.TagsScreen
import com.pixelmarks.feature.trash.TrashScreen

@Composable
fun PixelMarksNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Home.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Bottom Navigation Roots
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                onNavigateToAddBookmark = { navController.navigate(Screen.AddBookmark.route) },
                onNavigateToBookmarkDetail = { bookmarkId ->
                    navController.navigate(Screen.BookmarkDetail.createRoute(bookmarkId))
                },
                onNavigateToReminders = { navController.navigate(Screen.Reminders.route) },
                onNavigateToBrowser = { url -> navController.navigate(Screen.Browser.createRoute(url)) },
                onNavigateToConnectPc = { navController.navigate(Screen.ConnectPc.route) },
                onNavigateToTrash = { navController.navigate(Screen.Trash.route) }
            )
        }

        composable(Screen.Collections.route) {
            CollectionsScreen(
                onNavigateToCollectionDetail = { collectionId ->
                    navController.navigate(Screen.CollectionDetail.createRoute(collectionId))
                },
                onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                onNavigateToConnectPc = { navController.navigate(Screen.ConnectPc.route) },
                onNavigateToReminders = { navController.navigate(Screen.Reminders.route) }
            )
        }

        composable(Screen.Tags.route) {
            TagsScreen(
                onNavigateToTagDetail = { tagId ->
                    navController.navigate(Screen.TagDetail.createRoute(tagId))
                },
                onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                onNavigateToConnectPc = { navController.navigate(Screen.ConnectPc.route) },
                onNavigateToReminders = { navController.navigate(Screen.Reminders.route) }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateToConnectPc = { navController.navigate(Screen.ConnectPc.route) },
                onNavigateToConnectedDevices = { navController.navigate(Screen.ConnectedDevices.route) },
                onNavigateToBackup = { navController.navigate(Screen.Backup.route) },
                onNavigateToHtmlImportExport = { navController.navigate(Screen.HtmlImportExport.route) },
                onNavigateToManageTags = { navController.navigate(Screen.Tags.route) },
                onNavigateToBrowserPreference = { navController.navigate(Screen.BrowserPreferenceSetting.route) },
                onNavigateToAppLock = { navController.navigate(Screen.AppLockSetting.route) },
                onNavigateToSecureFolder = { navController.navigate(Screen.SecureFolderSetting.route) },
                onNavigateToSecurity = { navController.navigate(Screen.AppLockSetting.route) },
                onNavigateToCustomizeBookmark = { navController.navigate(Screen.CustomizeBookmark.route) },
                onNavigateToThemeAndColors = { navController.navigate(Screen.ThemeAndColorsSetting.route) }
            )
        }

        // Secondary / Modal Routes
        composable(
            route = Screen.Search.route,
            arguments = listOf(
                navArgument("collectionId") { type = NavType.LongType; defaultValue = -1L },
                navArgument("tagId") { type = NavType.LongType; defaultValue = -1L }
            )
        ) { backStackEntry ->
            val initialColId = backStackEntry.arguments?.getLong("collectionId")?.takeIf { it > 0 }
            val initialTId = backStackEntry.arguments?.getLong("tagId")?.takeIf { it > 0 }
            SearchScreen(
                initialCollectionId = initialColId,
                initialTagId = initialTId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToBookmarkDetail = { bookmarkId ->
                    navController.navigate(Screen.BookmarkDetail.createRoute(bookmarkId))
                }
            )
        }

        composable(
            route = Screen.AddBookmark.route,
            arguments = listOf(
                navArgument("collectionId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val initialColId = backStackEntry.arguments?.getLong("collectionId")?.takeIf { it > 0 }
            AddBookmarkScreen(
                initialCollectionId = initialColId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToBookmarkDetail = { id ->
                    navController.navigate(Screen.BookmarkDetail.createRoute(id))
                },
                onNavigateToEditBookmark = { id ->
                    navController.navigate(Screen.EditBookmark.createRoute(id))
                }
            )
        }

        composable(
            route = Screen.EditBookmark.route,
            arguments = listOf(
                navArgument("bookmarkId") { type = NavType.LongType; defaultValue = 1L }
            )
        ) { backStackEntry ->
            val bookmarkId = backStackEntry.arguments?.getLong("bookmarkId") ?: 1L
            EditBookmarkScreen(
                bookmarkId = bookmarkId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.BookmarkDetail.route,
            arguments = listOf(
                navArgument("bookmarkId") { type = NavType.LongType; defaultValue = 1L }
            )
        ) { backStackEntry ->
            val bookmarkId = backStackEntry.arguments?.getLong("bookmarkId") ?: 1L
            BookmarkDetailScreen(
                bookmarkId = bookmarkId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEditBookmark = { id ->
                    navController.navigate(Screen.EditBookmark.createRoute(id))
                },
                onNavigateToTagDetail = { tagId ->
                    navController.navigate(Screen.TagDetail.createRoute(tagId))
                },
                onNavigateToBrowser = { url ->
                    navController.navigate(Screen.Browser.createRoute(url))
                }
            )
        }

        composable(
            route = Screen.CollectionDetail.route,
            arguments = listOf(
                navArgument("collectionId") { type = NavType.LongType; defaultValue = 1L }
            )
        ) { backStackEntry ->
            val collectionId = backStackEntry.arguments?.getLong("collectionId") ?: 1L
            CollectionDetailScreen(
                collectionId = collectionId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCollectionDetail = { childId ->
                    navController.navigate(Screen.CollectionDetail.createRoute(childId))
                },
                onNavigateToBookmarkDetail = { bookmarkId ->
                    navController.navigate(Screen.BookmarkDetail.createRoute(bookmarkId))
                },
                onNavigateToAddBookmark = { colId ->
                    navController.navigate(Screen.AddBookmark.createRoute(colId))
                }
            )
        }

        composable(
            route = Screen.TagDetail.route,
            arguments = listOf(
                navArgument("tagId") { type = NavType.LongType; defaultValue = 1L }
            )
        ) { backStackEntry ->
            val tagId = backStackEntry.arguments?.getLong("tagId") ?: 1L
            TagDetailScreen(
                tagId = tagId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToBookmarkDetail = { bookmarkId ->
                    navController.navigate(Screen.BookmarkDetail.createRoute(bookmarkId))
                }
            )
        }

        composable(Screen.Reminders.route) {
            RemindersScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToBookmarkDetail = { bookmarkId ->
                    navController.navigate(Screen.BookmarkDetail.createRoute(bookmarkId))
                }
            )
        }

        composable(
            route = Screen.Browser.route,
            arguments = listOf(
                navArgument("url") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val rawUrl = backStackEntry.arguments?.getString("url") ?: ""
            val decodedUrl = android.net.Uri.decode(rawUrl)
            BrowserScreen(
                initialUrl = decodedUrl,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ConnectPc.route) {
            ConnectPcScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDevices = { navController.navigate(Screen.ConnectedDevices.route) }
            )
        }

        composable(Screen.ConnectedDevices.route) {
            ConnectedDevicesScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Backup.route) {
            BackupScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Trash.route) {
            TrashScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToBookmarkDetail = { bookmarkId ->
                    navController.navigate(Screen.BookmarkDetail.createRoute(bookmarkId))
                }
            )
        }

        composable(Screen.HtmlImportExport.route) {
            HtmlImportExportScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.BrowserPreferenceSetting.route) {
            BrowserPreferenceScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AppLockSetting.route) {
            AppLockSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.SecureFolderSetting.route) {
            SecureFolderSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.SecuritySettings.route) {
            SecuritySettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.CustomizeBookmark.route) {
            CustomizeBookmarkScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ThemeAndColorsSetting.route) {
            ThemeAndColorsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

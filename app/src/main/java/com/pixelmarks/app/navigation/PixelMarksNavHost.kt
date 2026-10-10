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
import com.pixelmarks.feature.settings.SettingsScreen
import com.pixelmarks.feature.tags.TagDetailScreen
import com.pixelmarks.feature.tags.TagsScreen
import com.pixelmarks.feature.trash.TrashScreen

@Composable
fun PixelMarksNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
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
                }
            )
        }

        composable(Screen.Tags.route) {
            TagsScreen(
                onNavigateToTagDetail = { tagId ->
                    navController.navigate(Screen.TagDetail.createRoute(tagId))
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateToConnectedDevices = { navController.navigate(Screen.ConnectedDevices.route) },
                onNavigateToBackup = { navController.navigate(Screen.Backup.route) },
                onNavigateToHtmlImportExport = { navController.navigate(Screen.HtmlImportExport.route) }
            )
        }

        // Secondary / Modal Routes
        composable(Screen.Search.route) {
            SearchScreen(
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
                onNavigateBack = { navController.popBackStack() }
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
    }
}

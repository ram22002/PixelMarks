package com.pixelmarks.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pixelmarks.app.navigation.PixelMarksNavHost
import com.pixelmarks.app.navigation.Screen
import com.pixelmarks.ui.components.PixelMarksBottomBar
import com.pixelmarks.ui.theme.PixelMarksTheme

/**
 * Root composable establishing theme and top-level navigation shell.
 */
@Composable
fun PixelMarksApp() {
    PixelMarksTheme {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        val isBottomBarVisible = currentRoute in listOf(
            Screen.Home.route,
            Screen.Collections.route,
            Screen.Tags.route,
            Screen.Settings.route
        )

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                if (isBottomBarVisible) {
                    PixelMarksBottomBar(
                        currentRoute = currentRoute,
                        onNavigateToRoute = { route ->
                            if (currentRoute != route) {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            PixelMarksNavHost(
                navController = navController,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

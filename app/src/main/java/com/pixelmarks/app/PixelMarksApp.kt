package com.pixelmarks.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pixelmarks.app.navigation.PixelMarksNavHost
import com.pixelmarks.app.navigation.Screen
import com.pixelmarks.ui.components.PixelMarksBottomBar
import com.pixelmarks.ui.theme.PixelMarksTheme

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import com.pixelmarks.core.security.model.PrivacyMode
import com.pixelmarks.feature.security.AppLockScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Root composable establishing theme, security lock shield, and top-level navigation shell.
 */
@Composable
fun PixelMarksApp(
    initialBookmarkId: Long? = null
) {
    val context = LocalContext.current
    val app = context.applicationContext as PixelMarksApplication
    val isAppLocked by app.container.appLockManager.isAppLocked.collectAsState()
    val activity = context as? FragmentActivity
    var lockErrorMessage by remember { mutableStateOf<String?>(null) }

    fun launchUnlock() {
        if (activity != null) {
            lockErrorMessage = null
            app.container.biometricAuthManager.authenticate(
                activity = activity,
                title = "Unlock PixelMarks",
                subtitle = "Confirm biometric or device credentials",
                onSuccess = {
                    lockErrorMessage = null
                    app.container.appLockManager.unlockApp()
                },
                onError = { _: Int, errString: CharSequence ->
                    lockErrorMessage = errString.toString()
                }
            )
        }
    }

    LaunchedEffect(isAppLocked) {
        if (isAppLocked) {
            launchUnlock()
        }
    }

    val privacyMode by app.container.privateModeManager.mode.collectAsState()
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    var showPasswordPrompt by remember { mutableStateOf(false) }
    var pendingPrivateReminderBookmarkId by remember { mutableStateOf<Long?>(null) }

    fun triggerSecureFolderAuth(onSuccessAction: (() -> Unit)? = null) {
        coroutineScope.launch {
            val method = app.container.privatePreferencesRepository.authMethod.first()
            if (method == com.pixelmarks.core.security.model.PrivateAuthMethod.CUSTOM_PASSWORD) {
                showPasswordPrompt = true
            } else if (activity != null) {
                app.container.biometricAuthManager.authenticate(
                    activity = activity,
                    title = "Secure Folder",
                    subtitle = "Authenticate to enter Private Mode",
                    onSuccess = {
                        app.container.privateModeManager.enterPrivateMode()
                        onSuccessAction?.invoke()
                    },
                    onError = { _, _ ->
                        pendingPrivateReminderBookmarkId = null
                    }
                )
            }
        }
    }

    val themeMode by app.container.appPreferencesRepository.observeThemeMode().collectAsState(initial = com.pixelmarks.data.repository.ThemeMode.DARK)
    val dynamicTheme by app.container.appPreferencesRepository.observeDynamicTheme().collectAsState(initial = false)
    val colorScheme by app.container.appPreferencesRepository.observeColorScheme().collectAsState(initial = com.pixelmarks.data.repository.AppColorScheme.ARCHIVAL_GOLD)
    val displayFont by app.container.appPreferencesRepository.observeDisplayFont().collectAsState(initial = com.pixelmarks.data.repository.DisplayFont.EDITORIAL_SERIF)
    val startupPage by app.container.appPreferencesRepository.observeStartupPage().collectAsState(initial = com.pixelmarks.data.repository.StartupPage.HOME)

    val startDestinationRoute = remember(startupPage) {
        when (startupPage) {
            com.pixelmarks.data.repository.StartupPage.HOME -> Screen.Home.route
            com.pixelmarks.data.repository.StartupPage.COLLECTIONS -> Screen.Collections.route
            com.pixelmarks.data.repository.StartupPage.TAGS -> Screen.Tags.route
            com.pixelmarks.data.repository.StartupPage.SEARCH -> Screen.Search.route
        }
    }

    PixelMarksTheme(
        themeMode = themeMode,
        appColorScheme = colorScheme,
        displayFont = displayFont,
        dynamicColor = dynamicTheme
    ) {
        if (isAppLocked) {
            AppLockScreen(
                onUnlockClicked = { launchUnlock() },
                errorMessage = lockErrorMessage
            )
        } else {
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            var previousPrivacyMode by remember { mutableStateOf(privacyMode) }
            LaunchedEffect(privacyMode) {
                if (previousPrivacyMode == PrivacyMode.PRIVATE && privacyMode == PrivacyMode.PUBLIC) {
                    // App left private mode: evict back stack and safely return to Home
                    navController.navigate(Screen.Home.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
                previousPrivacyMode = privacyMode
            }

            if (showPasswordPrompt) {
                com.pixelmarks.feature.security.SecureFolderPasswordDialog(
                    onDismiss = {
                        showPasswordPrompt = false
                        pendingPrivateReminderBookmarkId = null
                    },
                    onSuccess = {
                        showPasswordPrompt = false
                        app.container.privateModeManager.enterPrivateMode()
                        pendingPrivateReminderBookmarkId?.let { bId ->
                            navController.navigate(Screen.BookmarkDetail.createRoute(bId))
                            pendingPrivateReminderBookmarkId = null
                        }
                    },
                    onFallbackAuth = if (activity != null) {
                        {
                            showPasswordPrompt = false
                            app.container.biometricAuthManager.authenticate(
                                activity = activity,
                                title = "Secure Folder Recovery",
                                subtitle = "Authenticate to enter Private Mode",
                                onSuccess = {
                                    app.container.privateModeManager.enterPrivateMode()
                                    pendingPrivateReminderBookmarkId?.let { bId ->
                                        navController.navigate(Screen.BookmarkDetail.createRoute(bId))
                                        pendingPrivateReminderBookmarkId = null
                                    }
                                },
                                onError = { _, _ ->
                                    pendingPrivateReminderBookmarkId = null
                                }
                            )
                        }
                    } else null
                )
            }

            LaunchedEffect(initialBookmarkId) {
                val bId = initialBookmarkId
                if (bId != null && bId > 0) {
                    coroutineScope.launch {
                        val bookmark = app.container.bookmarkRepository.getById(bId)
                        if (bookmark != null) {
                            if (bookmark.isPrivate && privacyMode != PrivacyMode.PRIVATE) {
                                pendingPrivateReminderBookmarkId = bId
                                triggerSecureFolderAuth {
                                    navController.navigate(Screen.BookmarkDetail.createRoute(bId))
                                    pendingPrivateReminderBookmarkId = null
                                }
                            } else {
                                navController.navigate(Screen.BookmarkDetail.createRoute(bId))
                            }
                        }
                    }
                }
            }

            val isBottomBarVisible = currentRoute in listOf(
                Screen.Home.route,
                Screen.Collections.route,
                Screen.Tags.route,
                Screen.Settings.route
            )

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isBottomBarVisible,
                        enter = androidx.compose.animation.slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = androidx.compose.animation.core.tween(durationMillis = 220, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                        ) + androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(160)),
                        exit = androidx.compose.animation.slideOutVertically(
                            targetOffsetY = { it },
                            animationSpec = androidx.compose.animation.core.tween(durationMillis = 180, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                        ) + androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(140))
                    ) {
                        PixelMarksBottomBar(
                            currentRoute = currentRoute,
                            privacyMode = privacyMode,
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
                            },
                            onEnterPrivateRequested = {
                                triggerSecureFolderAuth()
                            },
                            onExitPrivateRequested = {
                                app.container.privateModeManager.exitPrivateMode()
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }
                }
            ) { innerPadding ->
                PixelMarksNavHost(
                    navController = navController,
                    modifier = Modifier.padding(innerPadding),
                    startDestination = startDestinationRoute
                )
            }
        }
    }
}

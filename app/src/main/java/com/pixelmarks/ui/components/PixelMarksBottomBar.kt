package com.pixelmarks.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import com.pixelmarks.app.navigation.Screen
import com.pixelmarks.app.navigation.bottomNavItems
import com.pixelmarks.core.security.model.PrivacyMode
import com.pixelmarks.ui.theme.PixelMarksTheme
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun PixelMarksBottomBar(
    currentRoute: String?,
    privacyMode: PrivacyMode = PrivacyMode.PUBLIC,
    onNavigateToRoute: (String) -> Unit,
    onEnterPrivateRequested: () -> Unit = {},
    onExitPrivateRequested: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val isPrivate = privacyMode == PrivacyMode.PRIVATE

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        bottomNavItems.forEach { item ->
            val isSelected = currentRoute == item.screen.route

            val displayTitle = if (item.screen == Screen.Settings && isPrivate) {
                "Secure Folder"
            } else {
                item.title
            }

            val displayIcon = if (item.screen == Screen.Settings && isPrivate) {
                Icons.Default.Lock
            } else {
                item.icon
            }

            val itemModifier = if (item.screen == Screen.Settings && !isPrivate) {
                Modifier.pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(pass = PointerEventPass.Initial)
                        val longPressTimeout = viewConfiguration.longPressTimeoutMillis
                        val upOrCancel = withTimeoutOrNull(longPressTimeout) {
                            waitForUpOrCancellation(pass = PointerEventPass.Initial)
                        }
                        if (upOrCancel == null) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onEnterPrivateRequested()
                        }
                    }
                }
            } else {
                Modifier
            }

            NavigationBarItem(
                modifier = itemModifier,
                selected = isSelected,
                onClick = {
                    if (item.screen == Screen.Settings && isPrivate) {
                        onExitPrivateRequested()
                    } else if (item.screen != Screen.Settings) {
                        onNavigateToRoute(item.screen.route)
                    } else {
                        onNavigateToRoute(item.screen.route)
                    }
                },
                icon = {
                    Icon(
                        imageVector = displayIcon,
                        contentDescription = displayTitle,
                        tint = if (item.screen == Screen.Settings && isPrivate) {
                            MaterialTheme.colorScheme.error
                        } else {
                            androidx.compose.ui.graphics.Color.Unspecified
                        }
                    )
                },
                label = {
                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (item.screen == Screen.Settings && isPrivate) {
                            MaterialTheme.colorScheme.error
                        } else {
                            androidx.compose.ui.graphics.Color.Unspecified
                        }
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PixelMarksBottomBarPreview() {
    PixelMarksTheme {
        PixelMarksBottomBar(
            currentRoute = Screen.Home.route,
            onNavigateToRoute = {}
        )
    }
}

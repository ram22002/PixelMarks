package com.pixelmarks.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pixelmarks.app.navigation.Screen
import com.pixelmarks.app.navigation.bottomNavItems
import com.pixelmarks.core.security.model.PrivacyMode
import com.pixelmarks.ui.theme.ArchivalError
import com.pixelmarks.ui.theme.ArchivalSurfaceContainerHigh
import com.pixelmarks.ui.theme.ArchivalSurfaceContainerHighest
import com.pixelmarks.ui.theme.ArchivalSurfaceContainerLowest
import com.pixelmarks.ui.theme.ArchivalTextPrimary
import com.pixelmarks.ui.theme.ArchivalTextSecondary
import com.pixelmarks.ui.theme.PixelMarksTheme
import com.pixelmarks.ui.theme.TerracottaAccent
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

    Column {
        HorizontalDivider(color = ArchivalSurfaceContainerHighest, thickness = 0.5.dp)
        NavigationBar(
            containerColor = ArchivalSurfaceContainerLowest,
            contentColor = ArchivalTextPrimary,
            tonalElevation = 0.dp
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

                val targetColor = when {
                    item.screen == Screen.Settings && isPrivate -> ArchivalError
                    isSelected -> TerracottaAccent
                    else -> ArchivalTextSecondary
                }

                val animatedColor by animateColorAsState(
                    targetValue = targetColor,
                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                    label = "tabColor"
                )

                val iconScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.10f else 1.0f,
                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                    label = "tabIconScale"
                )

                NavigationBarItem(
                    modifier = itemModifier,
                    selected = isSelected,
                    onClick = {
                        if (item.screen == Screen.Settings && isPrivate) {
                            onExitPrivateRequested()
                        } else {
                            onNavigateToRoute(item.screen.route)
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = displayIcon,
                            contentDescription = displayTitle,
                            tint = animatedColor,
                            modifier = Modifier.graphicsLayer {
                                scaleX = iconScale
                                scaleY = iconScale
                            }
                        )
                    },
                    label = {
                        Text(
                            text = displayTitle,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = animatedColor
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TerracottaAccent,
                        selectedTextColor = TerracottaAccent,
                        indicatorColor = ArchivalSurfaceContainerHigh,
                        unselectedIconColor = ArchivalTextSecondary,
                        unselectedTextColor = ArchivalTextSecondary
                    )
                )
            }
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

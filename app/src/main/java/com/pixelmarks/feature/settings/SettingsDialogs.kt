package com.pixelmarks.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelmarks.data.repository.AppColorScheme
import com.pixelmarks.data.repository.CollectionStyle
import com.pixelmarks.data.repository.DisplayFont
import com.pixelmarks.data.repository.StartupPage
import com.pixelmarks.data.repository.ThemeMode
import com.pixelmarks.ui.theme.ArchivalPrimary
import com.pixelmarks.ui.theme.ArchivalSurfaceContainer
import com.pixelmarks.ui.theme.ArchivalSurfaceContainerHigh
import com.pixelmarks.ui.theme.ArchivalTextPrimary
import com.pixelmarks.ui.theme.ArchivalTextSecondary

@Composable
fun ThemeModeDialog(
    currentMode: ThemeMode,
    onDismiss: () -> Unit,
    onSelectMode: (ThemeMode) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ArchivalSurfaceContainer,
        title = {
            Text(
                text = "Theme Mode",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                color = ArchivalTextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ThemeMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onSelectMode(mode)
                                onDismiss()
                            }
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentMode == mode,
                            onClick = {
                                onSelectMode(mode)
                                onDismiss()
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = ArchivalPrimary)
                        )
                        Text(
                            text = mode.label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (currentMode == mode) ArchivalPrimary else ArchivalTextPrimary,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = ArchivalPrimary)
            }
        }
    )
}

@Composable
fun ColorSchemeDialog(
    currentScheme: AppColorScheme,
    onDismiss: () -> Unit,
    onSelectScheme: (AppColorScheme) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ArchivalSurfaceContainer,
        title = {
            Text(
                text = "Pick Color Scheme",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                color = ArchivalTextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppColorScheme.entries.forEach { scheme ->
                    val color = Color(android.graphics.Color.parseColor(scheme.hexColor))
                    val selected = currentScheme == scheme
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                onSelectScheme(scheme)
                                onDismiss()
                            }
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(color)
                                .then(
                                    if (selected) Modifier.border(2.dp, ArchivalTextPrimary, CircleShape) else Modifier
                                )
                        )
                        Text(
                            text = scheme.label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) ArchivalPrimary else ArchivalTextPrimary,
                            modifier = Modifier.padding(start = 14.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = ArchivalPrimary)
            }
        }
    )
}

@Composable
fun DisplayFontDialog(
    currentFont: DisplayFont,
    onDismiss: () -> Unit,
    onSelectFont: (DisplayFont) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ArchivalSurfaceContainer,
        title = {
            Text(
                text = "Display Font",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                color = ArchivalTextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                DisplayFont.entries.forEach { fontOption ->
                    val selected = currentFont == fontOption
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onSelectFont(fontOption)
                                onDismiss()
                            }
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = {
                                onSelectFont(fontOption)
                                onDismiss()
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = ArchivalPrimary)
                        )
                        Text(
                            text = fontOption.label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) ArchivalPrimary else ArchivalTextPrimary,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = ArchivalPrimary)
            }
        }
    )
}

@Composable
fun LanguageDialog(
    currentLanguage: String,
    onDismiss: () -> Unit,
    onSelectLanguage: (String) -> Unit
) {
    val languages = listOf("English", "System Default")
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ArchivalSurfaceContainer,
        title = {
            Text(
                text = "Language",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                color = ArchivalTextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                languages.forEach { lang ->
                    val selected = currentLanguage == lang
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onSelectLanguage(lang)
                                onDismiss()
                            }
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = {
                                onSelectLanguage(lang)
                                onDismiss()
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = ArchivalPrimary)
                        )
                        Text(
                            text = lang,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) ArchivalPrimary else ArchivalTextPrimary,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = ArchivalPrimary)
            }
        }
    )
}

@Composable
fun StartupPageDialog(
    currentPage: StartupPage,
    onDismiss: () -> Unit,
    onSelectPage: (StartupPage) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ArchivalSurfaceContainer,
        title = {
            Text(
                text = "Startup Page",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                color = ArchivalTextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                StartupPage.entries.forEach { page ->
                    val selected = currentPage == page
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onSelectPage(page)
                                onDismiss()
                            }
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = {
                                onSelectPage(page)
                                onDismiss()
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = ArchivalPrimary)
                        )
                        Text(
                            text = page.label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) ArchivalPrimary else ArchivalTextPrimary,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = ArchivalPrimary)
            }
        }
    )
}

@Composable
fun CollectionStyleDialog(
    currentStyle: CollectionStyle,
    onDismiss: () -> Unit,
    onSelectStyle: (CollectionStyle) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ArchivalSurfaceContainer,
        title = {
            Text(
                text = "Collection Style",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                color = ArchivalTextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                CollectionStyle.entries.forEach { style ->
                    val selected = currentStyle == style
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onSelectStyle(style)
                                onDismiss()
                            }
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = {
                                onSelectStyle(style)
                                onDismiss()
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = ArchivalPrimary)
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(
                                text = style.label,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) ArchivalPrimary else ArchivalTextPrimary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = ArchivalPrimary)
            }
        }
    )
}

@Composable
fun DeveloperMessageDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ArchivalSurfaceContainer,
        title = {
            Text(
                text = "Developer Message",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                color = ArchivalTextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Welcome to PixelMarks Archival Index.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = ArchivalPrimary
                )
                Text(
                    text = "PixelMarks was crafted with care as an uncompromising, air-gapped bookmark vault for researchers, engineers, and digital curators.\n\nAll your bookmarks, tags, and encrypted credentials exist solely in your device storage with zero telemetry or hidden sync.\n\nThank you for curating your personal web with PixelMarks.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ArchivalTextSecondary,
                    lineHeight = 19.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = ArchivalPrimary)
            }
        }
    )
}

@Composable
fun LicensesDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ArchivalSurfaceContainer,
        title = {
            Text(
                text = "Open Source Licenses",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                color = ArchivalTextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 350.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LicenseItem("Android Jetpack & Compose", "Apache License 2.0\nGoogle LLC")
                LicenseItem("Kotlin & Coroutines", "Apache License 2.0\nJetBrains s.r.o.")
                LicenseItem("Coil Image Loading", "Apache License 2.0\nCoil Contributors")
                LicenseItem("OkHttp & JSoup", "Apache License 2.0 & MIT License\nSquare, Inc. & Jonathan Hedley")
                LicenseItem("ZXing QR Engine", "Apache License 2.0\nZXing authors")
                LicenseItem("NanoHTTPD PC Bridge", "Modified BSD License\nNanoHTTPD community")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = ArchivalPrimary)
            }
        }
    )
}

@Composable
private fun LicenseItem(title: String, details: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(ArchivalSurfaceContainerHigh)
            .padding(10.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = ArchivalTextPrimary)
        Text(text = details, style = MaterialTheme.typography.bodySmall, color = ArchivalTextSecondary, fontSize = 11.sp)
    }
}

package com.pixelmarks.feature.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewHeadline
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pixelmarks.app.PixelMarksApplication
import com.pixelmarks.ui.theme.ArchivalBackground
import com.pixelmarks.ui.theme.ArchivalPrimary
import com.pixelmarks.ui.theme.ArchivalSurfaceContainer
import com.pixelmarks.ui.theme.ArchivalSurfaceContainerHigh
import com.pixelmarks.ui.theme.ArchivalSurfaceContainerHighest
import com.pixelmarks.ui.theme.ArchivalSurfaceContainerLow
import com.pixelmarks.ui.theme.ArchivalTextPrimary
import com.pixelmarks.ui.theme.ArchivalTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizeBookmarkScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(
        factory = run {
            val app = LocalContext.current.applicationContext as PixelMarksApplication
            SettingsViewModel.provideFactory(
                observeBrowserPreferenceUseCase = app.container.observeBrowserPreferenceUseCase,
                setBrowserPreferenceUseCase = app.container.setBrowserPreferenceUseCase,
                observeAppLockSettingsUseCase = app.container.observeAppLockSettingsUseCase,
                setAppLockEnabledUseCase = app.container.setAppLockEnabledUseCase,
                setAppLockTimeoutUseCase = app.container.setAppLockTimeoutUseCase,
                checkBiometricCapabilityUseCase = app.container.checkBiometricCapabilityUseCase,
                biometricAuthManager = app.container.biometricAuthManager,
                privatePreferencesRepository = app.container.privatePreferencesRepository,
                appPreferencesRepository = app.container.appPreferencesRepository
            )
        }
    )
) {
    val layoutDisplayMode by viewModel.layoutDisplayMode.collectAsState()
    val showDomainThumbnail by viewModel.showDomainThumbnail.collectAsState()
    val showTags by viewModel.showTags.collectAsState()
    val maxTagsToShow by viewModel.maxTagsToShow.collectAsState()
    val showCreationTime by viewModel.showCreationTime.collectAsState()
    val creationTimeFormat by viewModel.creationTimeFormat.collectAsState()
    val showTitle by viewModel.showTitle.collectAsState()
    val titleMaxLines by viewModel.titleMaxLines.collectAsState()
    val showSubtitle by viewModel.showSubtitle.collectAsState()
    val subtitleMaxLines by viewModel.subtitleMaxLines.collectAsState()
    val showSiteName by viewModel.showSiteName.collectAsState()
    val showTimeIcons by viewModel.showTimeIcons.collectAsState()
    val showPinIcon by viewModel.showPinIcon.collectAsState()
    val showNote by viewModel.showNote.collectAsState()
    val noteMaxLines by viewModel.noteMaxLines.collectAsState()

    var showDisplayModeDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Customize Bookmark",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = ArchivalTextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ArchivalTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ArchivalBackground)
            )
        },
        containerColor = ArchivalBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Live Preview Card matching Screenshot 1 exactly
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = 0.8f,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ),
                colors = CardDefaults.cardColors(containerColor = ArchivalSurfaceContainerLow),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // Left Image / Domain Thumbnail
                    AnimatedVisibility(
                        visible = showDomainThumbnail,
                        enter = fadeIn(animationSpec = tween(150)) + expandHorizontally(),
                        exit = fadeOut(animationSpec = tween(150)) + shrinkHorizontally()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ArchivalSurfaceContainerHigh)
                                .border(1.dp, ArchivalSurfaceContainerHighest, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            // Representing the smartphone mockup thumbnail
                            Box(
                                modifier = Modifier
                                    .width(42.dp)
                                    .height(72.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF38B2AC))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .align(Alignment.Center)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.8f))
                                )
                            }
                        }
                    }

                    // Right Content details
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Site Name + Time / Pin Icons
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AnimatedVisibility(visible = showPinIcon) {
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = null,
                                    tint = ArchivalPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            AnimatedVisibility(visible = showTimeIcons) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = ArchivalPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            AnimatedVisibility(visible = showSiteName) {
                                Text(
                                    text = "News",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ArchivalPrimary,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Title
                        AnimatedVisibility(
                            visible = showTitle,
                            enter = fadeIn(animationSpec = tween(150)) + expandVertically(),
                            exit = fadeOut(animationSpec = tween(150)) + shrinkVertically()
                        ) {
                            Text(
                                text = "One UI 7 rollout resumes globally for Galaxy devices",
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = ArchivalTextPrimary,
                                maxLines = titleMaxLines,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 13.sp,
                                lineHeight = 17.sp
                            )
                        }

                        // Subtitle / Description
                        AnimatedVisibility(
                            visible = showSubtitle,
                            enter = fadeIn(animationSpec = tween(150)) + expandVertically(),
                            exit = fadeOut(animationSpec = tween(150)) + shrinkVertically()
                        ) {
                            Text(
                                text = "After resuming in Korea last week, Samsung is expanding updates across more regions.",
                                style = MaterialTheme.typography.bodySmall,
                                color = ArchivalTextSecondary,
                                maxLines = subtitleMaxLines,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }

                        // Creation Time
                        AnimatedVisibility(
                            visible = showCreationTime,
                            enter = fadeIn(animationSpec = tween(150)) + expandVertically(),
                            exit = fadeOut(animationSpec = tween(150)) + shrinkVertically()
                        ) {
                            Text(
                                text = when (creationTimeFormat) {
                                    1 -> "4h ago"
                                    2 -> "Created 4 hours ago"
                                    else -> "Oct 14, 2026 • 4 hours ago"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = ArchivalTextSecondary.copy(alpha = 0.8f),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Tags Pills
                        AnimatedVisibility(
                            visible = showTags,
                            enter = fadeIn(animationSpec = tween(150)) + expandVertically(),
                            exit = fadeOut(animationSpec = tween(150)) + shrinkVertically()
                        ) {
                            val sampleTags = listOf("News", "Samsung", "Galaxy", "One UI", "Android", "Update")
                            val displayedTags = sampleTags.take(maxTagsToShow)
                            val remaining = sampleTags.size - displayedTags.size

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                displayedTags.forEach { tag ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(ArchivalSurfaceContainerHighest)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = tag,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ArchivalTextSecondary,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                                if (remaining > 0) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(ArchivalSurfaceContainerHighest)
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "+$remaining",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ArchivalPrimary,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }

                        // Note Pill
                        AnimatedVisibility(
                            visible = showNote,
                            enter = fadeIn(animationSpec = tween(150)) + expandVertically(),
                            exit = fadeOut(animationSpec = tween(150)) + shrinkVertically()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ArchivalPrimary.copy(alpha = 0.12f))
                                    .border(1.dp, ArchivalPrimary.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Includes key details about the new One UI 7 update...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ArchivalPrimary,
                                    fontSize = 10.sp,
                                    maxLines = noteMaxLines,
                                    overflow = TextOverflow.Ellipsis,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            // Section 1: Layout
            CustomizeCategorySection(title = "Layout") {
                CustomizeRow(
                    title = "Display Mode",
                    subtitle = layoutDisplayMode,
                    icon = Icons.AutoMirrored.Filled.ViewList,
                    onClick = { showDisplayModeDialog = true }
                )
            }

            // Section 2: Thumbnail
            CustomizeCategorySection(title = "Thumbnail") {
                CustomizeSwitchRow(
                    title = "Show Thumbnail",
                    subtitle = "Show preview image or domain thumbnail on bookmarks",
                    icon = Icons.Default.Image,
                    checked = showDomainThumbnail,
                    onCheckedChange = { viewModel.setShowDomainThumbnail(it) }
                )
            }

            // Section 3: Tags
            CustomizeCategorySection(title = "Tags") {
                CustomizeSwitchRow(
                    title = "Show Tags",
                    subtitle = "Show tags on bookmarks so you can quickly access them by tapping.",
                    icon = Icons.Default.Tag,
                    checked = showTags,
                    onCheckedChange = { viewModel.setShowTags(it) }
                )

                HorizontalDivider(color = ArchivalSurfaceContainerHigh)

                CustomizeSliderRow(
                    title = "Maximum Tags to Show",
                    subtitle = "Choose how many tags to display per bookmark.",
                    icon = Icons.Default.ViewAgenda,
                    value = maxTagsToShow,
                    min = 1,
                    max = 6,
                    onValueChange = { viewModel.setMaxTagsToShow(it) }
                )
            }

            // Section 4: Creation time
            CustomizeCategorySection(title = "Creation time") {
                CustomizeSwitchRow(
                    title = "Show creation time",
                    subtitle = "Display when the bookmark was created",
                    icon = Icons.Default.Schedule,
                    checked = showCreationTime,
                    onCheckedChange = { viewModel.setShowCreationTime(it) }
                )

                HorizontalDivider(color = ArchivalSurfaceContainerHigh)

                CustomizeSliderRow(
                    title = "Creation time format",
                    subtitle = "Slide to choose how detailed the creation time looks",
                    icon = Icons.Default.AccessTime,
                    value = creationTimeFormat,
                    min = 1,
                    max = 3,
                    onValueChange = { viewModel.setCreationTimeFormat(it) }
                )
            }

            // Section 5: Title
            CustomizeCategorySection(title = "Title") {
                CustomizeSwitchRow(
                    title = "Show Title",
                    subtitle = "Display the bookmark title on each item",
                    icon = Icons.Default.Title,
                    checked = showTitle,
                    onCheckedChange = { viewModel.setShowTitle(it) }
                )

                HorizontalDivider(color = ArchivalSurfaceContainerHigh)

                CustomizeSliderRow(
                    title = "Maximum Lines",
                    subtitle = "Set how many lines the title can expand to",
                    icon = Icons.Default.ViewHeadline,
                    value = titleMaxLines,
                    min = 1,
                    max = 4,
                    onValueChange = { viewModel.setTitleMaxLines(it) }
                )
            }

            // Section 6: Subtitle
            CustomizeCategorySection(title = "Subtitle") {
                CustomizeSwitchRow(
                    title = "Show Subtitle",
                    subtitle = "Display the bookmark's subtitle below the title",
                    icon = Icons.Default.TextFields,
                    checked = showSubtitle,
                    onCheckedChange = { viewModel.setShowSubtitle(it) }
                )

                HorizontalDivider(color = ArchivalSurfaceContainerHigh)

                CustomizeSliderRow(
                    title = "Maximum Lines",
                    subtitle = "Set how many lines the subtitle can expand to",
                    icon = Icons.Default.ViewHeadline,
                    value = subtitleMaxLines,
                    min = 1,
                    max = 4,
                    onValueChange = { viewModel.setSubtitleMaxLines(it) }
                )
            }

            // Section 7: Site Name
            CustomizeCategorySection(title = "Site Name") {
                CustomizeSwitchRow(
                    title = "Show Site Name",
                    subtitle = "Display the website name on each bookmark",
                    icon = Icons.Default.Link,
                    checked = showSiteName,
                    onCheckedChange = { viewModel.setShowSiteName(it) }
                )

                HorizontalDivider(color = ArchivalSurfaceContainerHigh)

                CustomizeSwitchRow(
                    title = "Show Time Icons",
                    subtitle = "Show reminder or expiration icons next to the name",
                    icon = Icons.Default.Alarm,
                    checked = showTimeIcons,
                    onCheckedChange = { viewModel.setShowTimeIcons(it) }
                )

                HorizontalDivider(color = ArchivalSurfaceContainerHigh)

                CustomizeSwitchRow(
                    title = "Show Pin Icon",
                    subtitle = "Show a pin icon when the bookmark is pinned",
                    icon = Icons.Default.PushPin,
                    checked = showPinIcon,
                    onCheckedChange = { viewModel.setShowPinIcon(it) }
                )
            }

            // Section 8: Note
            CustomizeCategorySection(title = "Note") {
                CustomizeSwitchRow(
                    title = "Show Note",
                    subtitle = "Display the bookmark's note below its details",
                    icon = Icons.Default.EditNote,
                    checked = showNote,
                    onCheckedChange = { viewModel.setShowNote(it) }
                )

                HorizontalDivider(color = ArchivalSurfaceContainerHigh)

                CustomizeSliderRow(
                    title = "Maximum Lines",
                    subtitle = "Set how many lines of the note are visible",
                    icon = Icons.Default.ViewHeadline,
                    value = noteMaxLines,
                    min = 1,
                    max = 4,
                    onValueChange = { viewModel.setNoteMaxLines(it) }
                )
            }

            // Wavy Decorative Line
            WavyLineDivider(color = ArchivalPrimary.copy(alpha = 0.7f))

            // Reset All Settings Button
            OutlinedButton(
                onClick = { viewModel.resetAllBookmarkSettings() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Reset All Settings",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = ArchivalPrimary,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
        }
    }

    if (showDisplayModeDialog) {
        val modes = listOf("List", "Grid", "Folio Cards", "Compact")
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDisplayModeDialog = false },
            title = {
                Text(
                    text = "Display Mode",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = ArchivalTextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    modes.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setLayoutDisplayMode(mode)
                                    showDisplayModeDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.RadioButton(
                                selected = layoutDisplayMode == mode,
                                onClick = {
                                    viewModel.setLayoutDisplayMode(mode)
                                    showDisplayModeDialog = false
                                },
                                colors = androidx.compose.material3.RadioButtonDefaults.colors(selectedColor = ArchivalPrimary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = mode, color = ArchivalTextPrimary, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            },
            confirmButton = {},
            containerColor = ArchivalSurfaceContainer
        )
    }
}

@Composable
private fun CustomizeCategorySection(
    title: String,
    badge: String? = null,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = ArchivalTextSecondary,
                fontFamily = FontFamily.Monospace
            )
            if (badge != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(ArchivalPrimary.copy(alpha = 0.18f))
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = ArchivalPrimary,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ArchivalSurfaceContainer),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
private fun CustomizeRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = ArchivalTextSecondary, modifier = Modifier.size(20.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = ArchivalTextPrimary, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = ArchivalTextSecondary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
    }
}

@Composable
private fun CustomizeSwitchRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = ArchivalTextSecondary, modifier = Modifier.size(20.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = ArchivalTextPrimary, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = ArchivalTextSecondary, fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 14.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ArchivalBackground,
                checkedTrackColor = ArchivalPrimary,
                uncheckedTrackColor = ArchivalSurfaceContainerHigh
            )
        )
    }
}

@Composable
private fun CustomizeSliderRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    value: Int,
    min: Int,
    max: Int,
    onValueChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = ArchivalTextSecondary, modifier = Modifier.size(20.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = ArchivalTextPrimary, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = ArchivalTextSecondary, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Slider(
                value = value.toFloat(),
                onValueChange = { onValueChange(it.toInt()) },
                valueRange = min.toFloat()..max.toFloat(),
                steps = max - min - 1,
                colors = SliderDefaults.colors(
                    thumbColor = ArchivalPrimary,
                    activeTrackColor = ArchivalPrimary,
                    inactiveTrackColor = ArchivalSurfaceContainerHigh
                ),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = ArchivalTextPrimary
            )
        }
    }
}

@Composable
private fun WavyLineDivider(color: Color) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(18.dp)
            .padding(horizontal = 4.dp)
    ) {
        val width = size.width
        val height = size.height
        val waveLength = 24f
        val amplitude = 5f
        val path = Path()

        var x = 0f
        path.moveTo(0f, height / 2f)

        while (x < width) {
            path.relativeQuadraticTo(waveLength / 4f, -amplitude, waveLength / 2f, 0f)
            path.relativeQuadraticTo(waveLength / 4f, amplitude, waveLength / 2f, 0f)
            x += waveLength
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

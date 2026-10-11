package com.pixelmarks.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelmarks.ui.theme.ArchivalBackground
import com.pixelmarks.ui.theme.ArchivalSurfaceContainer
import com.pixelmarks.ui.theme.ArchivalSurfaceContainerLowest
import com.pixelmarks.ui.theme.ArchivalTextPrimary
import com.pixelmarks.ui.theme.ArchivalTextSecondary
import com.pixelmarks.ui.theme.BrassGold
import com.pixelmarks.ui.theme.BrassGoldLight
import com.pixelmarks.ui.theme.Spacing
import com.pixelmarks.ui.theme.TerracottaAccent
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Editorial Archival Top Bar matching Stitch UI specification:
 * - Peach terracotta bookmark tile + Newsreader Serif "PixelMarks"
 * - Quick actions: Laptop (PC Bridge), Search, and Notifications Bell with unread dot
 */
@Composable
fun PixelMarksTopAppBar(
    isPrivateMode: Boolean = false,
    onNavigateToConnectPc: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToReminders: () -> Unit = {},
    hasUnreadReminders: Boolean = true
) {
    Surface(
        color = ArchivalBackground.copy(alpha = 0.95f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.medium, vertical = Spacing.small),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(TerracottaAccent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = null,
                        tint = Color(0xFF281900),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = if (isPrivateMode) "🔒 Secure Vault" else "PixelMarks",
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    color = ArchivalTextPrimary,
                    letterSpacing = (-0.5).sp
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateToConnectPc) {
                    Icon(
                        imageVector = Icons.Default.Laptop,
                        contentDescription = "Connect PC",
                        tint = ArchivalTextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                IconButton(onClick = onNavigateToSearch) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = ArchivalTextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Box {
                    IconButton(onClick = onNavigateToReminders) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Reminders",
                            tint = ArchivalTextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    if (hasUnreadReminders) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 8.dp, end = 8.dp)
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(TerracottaAccent)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Editorial Sub-Header matching Stitch UI:
 * - Calendar icon with curatorial date
 * - Newsreader serif salutation/title
 * - Grid/List view switcher toggle
 */
@Composable
fun ArchivalSubHeader(
    title: String = getEditorialSalutation(),
    dateString: String = formatCuratorialDate(),
    isGridView: Boolean? = null,
    onToggleGridView: ((Boolean) -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = TerracottaAccent,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = dateString,
                    style = MaterialTheme.typography.labelSmall,
                    color = BrassGoldLight,
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Normal,
                color = ArchivalTextPrimary,
                letterSpacing = (-0.5).sp
            )
        }

        if (isGridView != null && onToggleGridView != null) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = ArchivalSurfaceContainer,
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Row(modifier = Modifier.padding(3.dp)) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(if (isGridView) ArchivalSurfaceContainerLowest else Color.Transparent)
                            .clickable { onToggleGridView(true) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = "Grid View",
                            tint = if (isGridView) BrassGold else ArchivalTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(if (!isGridView) ArchivalSurfaceContainerLowest else Color.Transparent)
                            .clickable { onToggleGridView(false) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
                            contentDescription = "List View",
                            tint = if (!isGridView) BrassGold else ArchivalTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

fun getEditorialSalutation(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
}

fun formatCuratorialDate(): String {
    val cal = Calendar.getInstance()
    val sdf = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
    return sdf.format(cal.time).uppercase()
}

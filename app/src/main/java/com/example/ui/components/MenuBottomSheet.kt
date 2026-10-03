package com.example.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HideImage
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BrowserSettings
import com.example.data.model.BrowserTab

private data class MenuItem(
    val title: String,
    val icon: ImageVector,
    val isActive: Boolean = false,
    val testTag: String,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuBottomSheet(
    activeTab: BrowserTab,
    settings: BrowserSettings,
    isBookmarked: Boolean,
    onDismiss: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenDownloads: () -> Unit,
    onToggleBookmark: () -> Unit,
    onToggleNightMode: () -> Unit,
    onToggleNoImageMode: () -> Unit,
    onToggleDesktopMode: () -> Unit,
    onToggleAdBlock: () -> Unit,
    onFindInPage: () -> Unit,
    onViewSource: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()

    val menuItems = listOf(
        MenuItem(
            title = "Bookmarks",
            icon = Icons.Default.Bookmark,
            testTag = "menu_bookmarks",
            onClick = {
                onDismiss()
                onOpenBookmarks()
            }
        ),
        MenuItem(
            title = "History",
            icon = Icons.Default.History,
            testTag = "menu_history",
            onClick = {
                onDismiss()
                onOpenHistory()
            }
        ),
        MenuItem(
            title = "Downloads",
            icon = Icons.Default.Download,
            testTag = "menu_downloads",
            onClick = {
                onDismiss()
                onOpenDownloads()
            }
        ),
        MenuItem(
            title = if (isBookmarked) "Bookmarked" else "Add Bookmark",
            icon = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
            isActive = isBookmarked,
            testTag = "menu_toggle_bookmark",
            onClick = {
                onToggleBookmark()
                onDismiss()
            }
        ),
        MenuItem(
            title = "Night Mode",
            icon = Icons.Default.DarkMode,
            isActive = settings.nightMode,
            testTag = "menu_night_mode",
            onClick = onToggleNightMode
        ),
        MenuItem(
            title = "No-Image Mode",
            icon = Icons.Default.HideImage,
            isActive = settings.noImageMode,
            testTag = "menu_no_image_mode",
            onClick = onToggleNoImageMode
        ),
        MenuItem(
            title = "Desktop Site",
            icon = Icons.Default.Laptop,
            isActive = settings.desktopMode,
            testTag = "menu_desktop_mode",
            onClick = onToggleDesktopMode
        ),
        MenuItem(
            title = "Ad Blocker",
            icon = Icons.Default.Shield,
            isActive = settings.adBlockEnabled,
            testTag = "menu_ad_block",
            onClick = onToggleAdBlock
        ),
        MenuItem(
            title = "Find in Page",
            icon = Icons.Default.FindInPage,
            testTag = "menu_find_in_page",
            onClick = {
                onDismiss()
                onFindInPage()
            }
        ),
        MenuItem(
            title = "View Source",
            icon = Icons.Default.Code,
            testTag = "menu_view_source",
            onClick = {
                onDismiss()
                onViewSource()
            }
        ),
        MenuItem(
            title = "Share Link",
            icon = Icons.Default.Share,
            testTag = "menu_share",
            onClick = {
                onDismiss()
                if (activeTab.url.isNotBlank() && !activeTab.isHome) {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, activeTab.title)
                        putExtra(Intent.EXTRA_TEXT, activeTab.url)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Link"))
                }
            }
        ),
        MenuItem(
            title = "Settings",
            icon = Icons.Default.Settings,
            testTag = "menu_settings",
            onClick = {
                onDismiss()
                onOpenSettings()
            }
        )
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = "Tools & Features",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(menuItems, key = { it.title }) { item ->
                    MenuCell(item = item)
                }
            }
        }
    }
}

@Composable
private fun MenuCell(item: MenuItem) {
    val activeColor = MaterialTheme.colorScheme.primary
    val defaultColor = MaterialTheme.colorScheme.surfaceVariant
    val iconTint = if (item.isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = item.onClick)
            .padding(vertical = 6.dp)
            .testTag(item.testTag)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(if (item.isActive) activeColor else defaultColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = if (item.isActive) activeColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
            fontWeight = if (item.isActive) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

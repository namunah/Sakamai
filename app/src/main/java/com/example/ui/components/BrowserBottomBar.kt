package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BrowserTab

@Composable
fun BrowserBottomBar(
    activeTab: BrowserTab,
    tabCount: Int,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onHome: () -> Unit,
    onTabsClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(52.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back Button
            IconButton(
                onClick = onBack,
                enabled = activeTab.canGoBack,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("bottom_bar_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = if (activeTab.canGoBack) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                    },
                    modifier = Modifier.size(22.dp)
                )
            }

            // Forward Button
            IconButton(
                onClick = onForward,
                enabled = activeTab.canGoForward,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("bottom_bar_forward_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Forward",
                    tint = if (activeTab.canGoForward) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                    },
                    modifier = Modifier.size(22.dp)
                )
            }

            // Home Button
            IconButton(
                onClick = onHome,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("bottom_bar_home_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home",
                    tint = if (activeTab.isHome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Tabs Switcher Button (Box with counter)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(onClick = onTabsClick)
                    .testTag("bottom_bar_tabs_button"),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .border(
                            width = 1.8.dp,
                            color = MaterialTheme.colorScheme.onSurface,
                            shape = RoundedCornerShape(6.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tabCount.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Menu Button
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("bottom_bar_menu_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

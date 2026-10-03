package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BrowserTab
import com.example.util.UrlUtils

@Composable
fun BrowserAddressBar(
    tab: BrowserTab,
    isBookmarked: Boolean,
    onLoadUrl: (String) -> Unit,
    onReload: () -> Unit,
    onStop: () -> Unit,
    onToggleBookmark: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditing by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    val isHttps = tab.url.startsWith("https://", ignoreCase = true)
    val displayDomain = remember(tab.url, tab.title) {
        if (tab.isHome) "" else UrlUtils.extractDomain(tab.url)
    }

    LaunchedEffect(tab.url) {
        if (!isEditing) {
            inputText = tab.url
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Security / Search Icon
                if (isHttps) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Secure connection",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(start = 2.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Address / Search Input or Domain Display
                if (isEditing) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
                            .onFocusChanged {
                                if (!it.isFocused) {
                                    isEditing = false
                                }
                            }
                            .testTag("address_bar_input"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                isEditing = false
                                if (inputText.isNotBlank()) {
                                    onLoadUrl(inputText)
                                }
                            }
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )

                    LaunchedEffect(Unit) {
                        focusRequester.requestFocus()
                    }

                    // Clear button
                    IconButton(
                        onClick = { inputText = "" },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                inputText = if (tab.isHome) "" else tab.url
                                isEditing = true
                            }
                            .padding(vertical = 8.dp)
                            .testTag("address_bar_display"),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = if (tab.isHome) "Search or enter address" else (displayDomain.ifBlank { tab.title }),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (tab.isHome) FontWeight.Normal else FontWeight.Medium,
                            color = if (tab.isHome) {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Bookmark toggle
                if (!tab.isHome && !isEditing) {
                    IconButton(
                        onClick = onToggleBookmark,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("address_bar_bookmark_button")
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = if (isBookmarked) "Bookmarked" else "Bookmark page",
                            tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Reload or Stop loading
                if (!tab.isHome && !isEditing) {
                    IconButton(
                        onClick = {
                            if (tab.isLoading) onStop() else onReload()
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("address_bar_refresh_button")
                    ) {
                        Icon(
                            imageVector = if (tab.isLoading) Icons.Default.Close else Icons.Default.Refresh,
                            contentDescription = if (tab.isLoading) "Stop" else "Reload",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Animated Web Loading Progress Bar
        val animatedProgress by animateFloatAsState(
            targetValue = if (tab.isLoading) (tab.progress / 100f).coerceIn(0.05f, 1f) else 0f,
            label = "web_progress"
        )

        if (tab.isLoading && animatedProgress > 0f) {
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Transparent
            )
        } else {
            Spacer(modifier = Modifier.height(2.5.dp))
        }
    }
}

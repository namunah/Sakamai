package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Bookmark
import com.example.data.model.HistoryItem
import com.example.util.UrlUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BookmarksHistoryDialog(
    initialTab: Int = 0,
    bookmarks: List<Bookmark>,
    history: List<HistoryItem>,
    onOpenUrl: (String) -> Unit,
    onDeleteBookmark: (Long) -> Unit,
    onDeleteHistoryItem: (Long) -> Unit,
    onClearAllHistory: () -> Unit,
    onDismiss: () -> Unit,
    onAddBookmark: ((title: String, url: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(initialTab) }
    var bookmarkSearchQuery by remember { mutableStateOf("") }
    var historySearchQuery by remember { mutableStateOf("") }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showAddBookmarkDialog by remember { mutableStateOf(false) }

    val filteredBookmarks = remember(bookmarks, bookmarkSearchQuery) {
        if (bookmarkSearchQuery.isBlank()) bookmarks
        else bookmarks.filter {
            it.title.contains(bookmarkSearchQuery, ignoreCase = true) ||
            it.url.contains(bookmarkSearchQuery, ignoreCase = true)
        }
    }

    val filteredHistory = remember(history, historySearchQuery) {
        if (historySearchQuery.isBlank()) history
        else history.filter {
            it.title.contains(historySearchQuery, ignoreCase = true) ||
            it.url.contains(historySearchQuery, ignoreCase = true)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("bookmarks_history_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                Text(
                    text = if (selectedTab == 0) "Bookmarks" else "Browsing History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selectedTab == 0 && onAddBookmark != null) {
                        IconButton(
                            onClick = { showAddBookmarkDialog = true },
                            modifier = Modifier.testTag("add_bookmark_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add bookmark",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else if (selectedTab == 1 && history.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearHistoryDialog = true },
                            modifier = Modifier.testTag("clear_all_history_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear all history",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(48.dp))
                    }
                }
            }

            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Bookmarks (${bookmarks.size})")
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("History (${history.size})")
                        }
                    }
                )
            }

            // Body
            if (selectedTab == 0) {
                // Bookmarks List with Search
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (bookmarks.isNotEmpty()) {
                        OutlinedTextField(
                            value = bookmarkSearchQuery,
                            onValueChange = { bookmarkSearchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .testTag("bookmarks_search_input"),
                            placeholder = { Text("Search bookmarks...") },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = {
                                if (bookmarkSearchQuery.isNotBlank()) {
                                    IconButton(onClick = { bookmarkSearchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(20.dp)
                        )
                    }

                    if (bookmarks.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No bookmarks yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = "Tap the bookmark icon while browsing or save one directly",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                                )
                                if (onAddBookmark != null) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = { showAddBookmarkDialog = true },
                                        modifier = Modifier.testTag("empty_add_bookmark_button")
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Add Bookmark")
                                    }
                                }
                            }
                        }
                    } else if (filteredBookmarks.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No matching bookmarks found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            items(filteredBookmarks, key = { it.id }) { bookmark ->
                                BookmarkRow(
                                    bookmark = bookmark,
                                    onClick = {
                                        onOpenUrl(bookmark.url)
                                        onDismiss()
                                    },
                                    onDelete = { onDeleteBookmark(bookmark.id) }
                                )
                            }
                        }
                    }
                }
            } else {
                // History List with Search
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (history.isNotEmpty()) {
                        OutlinedTextField(
                            value = historySearchQuery,
                            onValueChange = { historySearchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .testTag("history_search_input"),
                            placeholder = { Text("Search history...") },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = {
                                if (historySearchQuery.isNotBlank()) {
                                    IconButton(onClick = { historySearchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(20.dp)
                        )
                    }

                    if (filteredHistory.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (historySearchQuery.isBlank()) "No history records" else "No matching records found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            items(filteredHistory, key = { it.id }) { item ->
                                HistoryRow(
                                    item = item,
                                    onClick = {
                                        onOpenUrl(item.url)
                                        onDismiss()
                                    },
                                    onDelete = { onDeleteHistoryItem(item.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Clear History Confirmation
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear browsing history?") },
            text = { Text("This will delete all visited website history records.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearAllHistory()
                        showClearHistoryDialog = false
                    }
                ) {
                    Text("Clear All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Bookmark Dialog
    if (showAddBookmarkDialog && onAddBookmark != null) {
        AddBookmarkDialog(
            onDismiss = { showAddBookmarkDialog = false },
            onAddBookmark = { title, url ->
                onAddBookmark(title, url)
                showAddBookmarkDialog = false
            }
        )
    }
}

@Composable
fun AddBookmarkDialog(
    onDismiss: () -> Unit,
    onAddBookmark: (title: String, url: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Bookmark") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    placeholder = { Text("e.g. My Favorite Site") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_bookmark_title_input")
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        if (it.isNotBlank()) isError = false
                    },
                    label = { Text("Website URL") },
                    placeholder = { Text("e.g. https://example.com") },
                    isError = isError,
                    supportingText = if (isError) {
                        { Text("URL cannot be empty") }
                    } else null,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_bookmark_url_input")
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (url.isBlank()) {
                        isError = true
                    } else {
                        onAddBookmark(title.trim(), url.trim())
                    }
                },
                modifier = Modifier.testTag("save_bookmark_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun BookmarkRow(
    bookmark: Bookmark,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val initial = UrlUtils.extractFaviconLetter(bookmark.title, bookmark.url)
    val domain = UrlUtils.extractDomain(bookmark.url)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("bookmark_row_${bookmark.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = bookmark.title.ifBlank { domain },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = bookmark.url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete bookmark",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun HistoryRow(
    item: HistoryItem,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val initial = UrlUtils.extractFaviconLetter(item.title, item.url)
    val domain = UrlUtils.extractDomain(item.url)
    val dateText = remember(item.visitedAt) {
        SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(item.visitedAt))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("history_row_${item.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title.ifBlank { domain },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = domain,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    text = " • $dateText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    fontSize = 11.sp
                )
            }
        }

        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Delete record",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

package com.example.data.repository

import com.example.data.local.BrowserDao
import com.example.data.model.Bookmark
import com.example.data.model.HistoryItem
import com.example.data.model.SpeedDialItem
import kotlinx.coroutines.flow.Flow

class BrowserRepository(private val dao: BrowserDao) {

    val bookmarks: Flow<List<Bookmark>> = dao.getAllBookmarks()
    val history: Flow<List<HistoryItem>> = dao.getAllHistory()
    val speedDialItems: Flow<List<SpeedDialItem>> = dao.getAllSpeedDial()

    suspend fun initDefaultSpeedDialsIfEmpty() {
        if (dao.countSpeedDial() == 0) {
            val defaults = listOf(
                SpeedDialItem(title = "Google", url = "https://www.google.com", iconColor = 0xFF4285F4, displayOrder = 0),
                SpeedDialItem(title = "Wikipedia", url = "https://www.wikipedia.org", iconColor = 0xFF6B7280, displayOrder = 1),
                SpeedDialItem(title = "GitHub", url = "https://github.com", iconColor = 0xFF24292F, displayOrder = 2),
                SpeedDialItem(title = "Reddit", url = "https://www.reddit.com", iconColor = 0xFFFF4500, displayOrder = 3),
                SpeedDialItem(title = "DuckDuckGo", url = "https://duckduckgo.com", iconColor = 0xFFDE5833, displayOrder = 4),
                SpeedDialItem(title = "Weather", url = "https://weather.com", iconColor = 0xFF0284C7, displayOrder = 5)
            )
            dao.insertAllSpeedDial(defaults)
        }
    }

    suspend fun addBookmark(title: String, url: String) {
        dao.insertBookmark(Bookmark(title = title, url = url))
    }

    suspend fun getBookmarkByUrl(url: String): Bookmark? = dao.getBookmarkByUrl(url)

    fun searchBookmarks(query: String): Flow<List<Bookmark>> = dao.searchBookmarks(query)

    suspend fun removeBookmarkById(id: Long) {
        dao.deleteBookmarkById(id)
    }

    suspend fun removeBookmarkByUrl(url: String) {
        dao.deleteBookmarkByUrl(url)
    }

    fun isBookmarked(url: String): Flow<Boolean> = dao.isBookmarked(url)

    suspend fun addHistory(title: String, url: String) {
        if (url.isNotBlank() && url != "about:blank" && !url.startsWith("data:")) {
            dao.insertHistory(HistoryItem(title = title.ifBlank { url }, url = url))
        }
    }

    suspend fun deleteHistoryItem(id: Long) {
        dao.deleteHistoryById(id)
    }

    suspend fun clearHistory() {
        dao.clearAllHistory()
    }

    fun searchHistory(query: String): Flow<List<HistoryItem>> = dao.searchHistory(query)

    suspend fun addSpeedDial(title: String, url: String) {
        val colors = listOf(0xFF0284C7, 0xFF0D9488, 0xFF8B5CF6, 0xFFEC4899, 0xFFF59E0B, 0xFF10B981)
        val color = colors.random()
        dao.insertSpeedDial(SpeedDialItem(title = title, url = url, iconColor = color))
    }

    suspend fun removeSpeedDial(id: Long) {
        dao.deleteSpeedDialById(id)
    }
}

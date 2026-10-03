package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.Bookmark
import com.example.data.model.HistoryItem
import com.example.data.model.SpeedDialItem
import kotlinx.coroutines.flow.Flow

@Dao
interface BrowserDao {
    // Bookmarks
    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun getAllBookmarks(): Flow<List<Bookmark>>

    @Query("SELECT * FROM bookmarks WHERE url = :url LIMIT 1")
    suspend fun getBookmarkByUrl(url: String): Bookmark?

    @Query("SELECT * FROM bookmarks WHERE title LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchBookmarks(query: String): Flow<List<Bookmark>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: Bookmark): Long

    @Delete
    suspend fun deleteBookmark(bookmark: Bookmark)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmarkById(id: Long)

    @Query("DELETE FROM bookmarks WHERE url = :url")
    suspend fun deleteBookmarkByUrl(url: String)

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE url = :url LIMIT 1)")
    fun isBookmarked(url: String): Flow<Boolean>

    // History
    @Query("SELECT * FROM history ORDER BY visitedAt DESC LIMIT 200")
    fun getAllHistory(): Flow<List<HistoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: HistoryItem): Long

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteHistoryById(id: Long)

    @Query("DELETE FROM history")
    suspend fun clearAllHistory()

    @Query("SELECT * FROM history WHERE title LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%' ORDER BY visitedAt DESC")
    fun searchHistory(query: String): Flow<List<HistoryItem>>

    // Speed Dial
    @Query("SELECT * FROM speed_dial ORDER BY displayOrder ASC, id ASC")
    fun getAllSpeedDial(): Flow<List<SpeedDialItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpeedDial(item: SpeedDialItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSpeedDial(items: List<SpeedDialItem>)

    @Query("DELETE FROM speed_dial WHERE id = :id")
    suspend fun deleteSpeedDialById(id: Long)

    @Query("SELECT COUNT(*) FROM speed_dial")
    suspend fun countSpeedDial(): Int
}

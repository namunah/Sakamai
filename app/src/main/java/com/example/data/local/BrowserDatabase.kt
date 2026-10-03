package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.Bookmark
import com.example.data.model.HistoryItem
import com.example.data.model.SpeedDialItem

@Database(
    entities = [Bookmark::class, HistoryItem::class, SpeedDialItem::class],
    version = 2,
    exportSchema = false
)
abstract class BrowserDatabase : RoomDatabase() {
    abstract fun browserDao(): BrowserDao

    companion object {
        @Volatile
        private var INSTANCE: BrowserDatabase? = null

        fun getInstance(context: Context): BrowserDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BrowserDatabase::class.java,
                    "via_browser_lite.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

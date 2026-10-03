package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "speed_dial")
data class SpeedDialItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val url: String,
    val iconColor: Long = 0xFF0284C7,
    val displayOrder: Int = 0
)

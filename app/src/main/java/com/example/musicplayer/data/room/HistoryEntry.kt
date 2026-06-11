package com.example.musicplayer.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class HistoryEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val songId: Int,
    val title: String,
    val artist: String,
    val album: String?,
    val path: String = "-",
    val timestamp: Long = System.currentTimeMillis()
)

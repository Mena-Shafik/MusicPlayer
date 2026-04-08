package com.example.musicplayer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true)
    val playlistId: Long = 0,
    val name: String,
    /** mark if this playlist was created as a temporary/ephemeral playlist */
    val isTemporary: Boolean = false
)


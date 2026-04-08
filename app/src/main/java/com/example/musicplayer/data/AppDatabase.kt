package com.example.musicplayer.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Song::class, Playlist::class, PlaylistEntry::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
}


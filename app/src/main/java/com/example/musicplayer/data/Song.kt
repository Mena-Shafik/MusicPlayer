package com.example.musicplayer.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Basic Song entity mirroring key MediaStore fields we care about.
 */
@Entity(tableName = "songs")
data class Song(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Long,

    val title: String?,
    val artist: String?,
    val album: String?,
    @ColumnInfo(name = "album_id")
    val albumId: Long?,
    @ColumnInfo(name = "track_number")
    val trackNumber: Int?,
    /** Raw track string from MediaStore, preserved for sorting when trackNumber is unreliable */
    @ColumnInfo(name = "raw_track")
    val rawTrack: String?,
    val duration: Long?,
    /** file path or content Uri string */
    val data: String?,
    val genre: String?,
    val favorite: Boolean = false,
)


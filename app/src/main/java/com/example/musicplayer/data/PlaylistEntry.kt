package com.example.musicplayer.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "playlist_entries",
    foreignKeys = [
        ForeignKey(entity = Playlist::class, parentColumns = ["playlistId"], childColumns = ["playlist_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Song::class, parentColumns = ["id"], childColumns = ["song_id"], onDelete = ForeignKey.CASCADE)
    ]
)
data class PlaylistEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "playlist_id")
    val playlistId: Long,
    @ColumnInfo(name = "song_id")
    val songId: Long,
    /** Position within playlist for ordering */
    val position: Int
)


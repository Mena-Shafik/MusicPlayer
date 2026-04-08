package com.example.musicplayer.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

data class SongWithPosition(
    val songId: Long,
    val playlistId: Long,
    val position: Int,
    val id: Long,
    val title: String?,
    val artist: String?,
    val album: String?,
    val albumId: Long?,
    val trackNumber: Int?,
    val rawTrack: String?,
    val duration: Long?,
    val data: String?,
    val genre: String?
)

@Dao
interface PlaylistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: PlaylistEntry): Long

    @Query("SELECT * FROM playlists WHERE playlistId = :playlistId LIMIT 1")
    suspend fun getPlaylistById(playlistId: Long): Playlist?

    @Transaction
    @Query(
        "SELECT pe.playlist_id as playlistId, pe.song_id as songId, pe.position as position, s.* FROM playlist_entries pe INNER JOIN songs s ON s.id = pe.song_id WHERE pe.playlist_id = :playlistId ORDER BY pe.position ASC"
    )
    fun getSongsForPlaylistOrdered(playlistId: Long): Flow<List<SongWithPosition>>

    @Query("DELETE FROM playlist_entries WHERE playlist_id = :playlistId")
    suspend fun clearPlaylist(playlistId: Long)
}


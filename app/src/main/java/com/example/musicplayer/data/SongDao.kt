package com.example.musicplayer.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(vararg songs: Song)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: Song)

    @Query("SELECT * FROM songs WHERE id = :id LIMIT 1")
    suspend fun getSongById(id: Long): Song?

    /** Return songs for an album ordered by trackNumber (ascending), fallback to rawTrack then id */
    @Query("SELECT * FROM songs WHERE album_id = :albumId ORDER BY track_number ASC, raw_track ASC, id ASC")
    fun getSongsByAlbumOrdered(albumId: Long): Flow<List<Song>>

    @Query("SELECT * FROM songs ORDER BY title ASC")
    fun getAllSongs(): Flow<List<Song>>
}


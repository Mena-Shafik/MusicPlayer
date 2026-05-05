package com.example.musicplayer.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete
import androidx.room.OnConflictStrategy
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    // Flow-backed query so consumers can observe changes automatically
    @Query("SELECT * FROM playlists")
    fun getAllFlow(): Flow<List<PlaylistEntity>>

    // keep a suspend helper for tests or one-off reads
    @Query("SELECT * FROM playlists")
    suspend fun getAll(): List<PlaylistEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(playlist: PlaylistEntity): Long

    @Delete
    suspend fun delete(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM playlists WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): PlaylistEntity?
}


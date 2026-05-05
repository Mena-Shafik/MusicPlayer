package com.example.musicplayer.playlist

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.musicplayer.model.Playlist
import com.example.musicplayer.data.room.AppDatabase
import com.example.musicplayer.data.room.PlaylistEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore(name = "playlists_db")
private val PLAYLISTS_KEY = stringPreferencesKey("all_playlists")

/**
 * PlaylistRepository backed by Room (AppDatabase / PlaylistDao).
 * Keeps the same public API as the previous DataStore-based repo so callers don't need changes.
 */
class PlaylistRepository(private val context: Context) {
    private val dao = AppDatabase.getInstance(context).playlistDao()

    private val gson = Gson()

    init {
        // One-time migration: if DB is empty, try importing playlists from the old DataStore JSON.
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val existing = dao.getAll()
                if (existing.isEmpty()) {
                    val prefs = context.dataStore.data.first()
                    val jsonString = prefs[PLAYLISTS_KEY] ?: "[]"
                    val type = object : TypeToken<List<Playlist>>() {}.type
                    val lists: List<Playlist> = try {
                        gson.fromJson(jsonString, type) ?: emptyList()
                    } catch (_: Exception) {
                        emptyList()
                    }
                    // insert into Room
                    lists.forEach { p ->
                        dao.insert(p.toEntity())
                    }
                    // If we successfully migrated (there were playlists), clear the old DataStore key
                    if (lists.isNotEmpty()) {
                        try {
                            context.dataStore.edit { prefs ->
                                prefs.remove(PLAYLISTS_KEY)
                            }
                        } catch (_: Exception) {
                            // best-effort cleanup failed; leave original data intact
                        }
                    }
                }
            } catch (_: Exception) {
                // migration is best-effort; ignore on failure
            }
        }
    }

    val allPlaylists: Flow<List<Playlist>> = dao.getAllFlow().map { list ->
        list.map { it.toModel() }
    }

    suspend fun createPlaylist(name: String, description: String = ""): Playlist {
        val entity = PlaylistEntity(name = name, songIds = "")
        val id = dao.insert(entity)
        return Playlist(id = id, name = name, songIds = emptyList(), description = description)
    }

    suspend fun addPlaylist(playlist: Playlist) {
        val entity = playlist.toEntity()
        dao.insert(entity)
    }

    suspend fun updatePlaylist(playlist: Playlist) {
        addPlaylist(playlist) // insert(onConflict = REPLACE) will update
    }

    suspend fun deletePlaylist(id: Long) {
        dao.deleteById(id)
    }

    suspend fun addSongToPlaylist(playlistId: Long, songId: Int) {
        val p = dao.findById(playlistId) ?: return
        val list = p.songIds.split(',').mapNotNull { it.trim().takeIf { s -> s.isNotEmpty() }?.toIntOrNull() }.toMutableList()
        if (!list.contains(songId)) list.add(songId)
        val updated = p.copy(songIds = list.joinToString(","))
        dao.insert(updated)
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Int) {
        val p = dao.findById(playlistId) ?: return
        val list = p.songIds.split(',').mapNotNull { it.trim().takeIf { s -> s.isNotEmpty() }?.toIntOrNull() }
        val updated = p.copy(songIds = list.filter { it != songId }.joinToString(","))
        dao.insert(updated)
    }

    // mapping helpers
    private fun PlaylistEntity.toModel(): Playlist {
        val ids = if (songIds.isBlank()) emptyList() else songIds.split(',').mapNotNull { it.trim().toIntOrNull() }
        return Playlist(id = id, name = name, songIds = ids)
    }

    private fun Playlist.toEntity(): PlaylistEntity {
        val ids = if (songIds.isEmpty()) "" else songIds.joinToString(",")
        // if id is 0 we let Room generate it; otherwise keep existing id so insert(REPLACE) updates
        val eid = if (id == 0L) 0L else id
        return PlaylistEntity(id = eid, name = name, songIds = ids)
    }
}

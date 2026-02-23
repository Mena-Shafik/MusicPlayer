package com.example.musicplayer.playlist

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.musicplayer.model.Playlist
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "playlists_db")

class PlaylistRepository(private val context: Context) {
    private val PLAYLISTS_KEY = stringPreferencesKey("all_playlists")
    private val gson = Gson()

    val allPlaylists: Flow<List<Playlist>> = context.dataStore.data.map { preferences ->
        val jsonString = preferences[PLAYLISTS_KEY] ?: "[]"
        try {
            val lists = parsePlaylistsJson(jsonString)
            lists
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun createPlaylist(name: String, description: String = ""): Playlist {
        val playlist = Playlist(
            name = name,
            description = description
        )
        addPlaylist(playlist)
        return playlist
    }

    suspend fun addPlaylist(playlist: Playlist) {
        context.dataStore.edit { preferences ->
            val current = getPlaylistsSync(preferences[PLAYLISTS_KEY] ?: "[]")
            // sanitize songIds to remove duplicates before saving
            val sanitized = playlist.copy(songIds = playlist.songIds.distinct())
            val updated = current.filter { it.id != sanitized.id } + sanitized
            preferences[PLAYLISTS_KEY] = gson.toJson(updated)
        }
    }

    suspend fun updatePlaylist(playlist: Playlist) {
        addPlaylist(playlist)
    }

    suspend fun deletePlaylist(id: Long) {
        context.dataStore.edit { preferences ->
            val current = getPlaylistsSync(preferences[PLAYLISTS_KEY] ?: "[]")
            val updated = current.filter { it.id != id }
            preferences[PLAYLISTS_KEY] = gson.toJson(updated)
        }
    }

    suspend fun addSongToPlaylist(playlistId: Long, songId: Int) {
        context.dataStore.edit { preferences ->
            val current = getPlaylistsSync(preferences[PLAYLISTS_KEY] ?: "[]")
            val updated = current.map { playlist ->
                if (playlist.id == playlistId && !playlist.songIds.contains(songId)) {
                    val newSongIds = (playlist.songIds + songId).distinct()
                    playlist.copy(songIds = newSongIds)
                } else {
                    playlist
                }
            }
            preferences[PLAYLISTS_KEY] = gson.toJson(updated)
        }
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Int) {
        context.dataStore.edit { preferences ->
            val current = getPlaylistsSync(preferences[PLAYLISTS_KEY] ?: "[]")
            val updated = current.map { playlist ->
                if (playlist.id == playlistId) {
                    val newSongIds = playlist.songIds.filter { it != songId }
                    playlist.copy(songIds = newSongIds)
                } else {
                    playlist
                }
            }
            preferences[PLAYLISTS_KEY] = gson.toJson(updated)
        }
    }

    private fun getPlaylistsSync(jsonString: String): List<Playlist> {
        return try {
            val type = object : TypeToken<List<Playlist>>() {}.type
            gson.fromJson(jsonString, type)
        } catch (_: Exception) {
            emptyList()
        }
    }

    // Minimal parse implementation used previously — keeps backward compatibility by delegating to Gson.
    private fun parsePlaylistsJson(jsonString: String): List<Playlist> {
        return try {
            val type = object : TypeToken<List<Playlist>>() {}.type
            gson.fromJson<List<Playlist>>(jsonString, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }
}

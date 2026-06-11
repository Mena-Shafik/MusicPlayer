package com.example.musicplayer.history

import android.content.Context
import android.util.Log
import com.example.musicplayer.data.room.AppDatabase
import com.example.musicplayer.data.room.HistoryEntry
import com.example.musicplayer.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HistoryRepository(private val context: Context) {
    private val TAG = "HistoryRepository"
    private val database = AppDatabase.getInstance(context)
    private val historyDao = database.historyDao()

    private val _history = MutableStateFlow<List<Song>>(emptyList())
    val history: StateFlow<List<Song>> = _history

    private val MAX_HISTORY_SIZE = 20

    init {
        // Load history from database on initialization
        loadHistory()
    }

    private fun loadHistory() {
        try {
            kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                val entries = historyDao.getLastNSongs(MAX_HISTORY_SIZE)
                val songs = entries.map { entry ->
                    Song(
                        id = entry.songId,
                        title = entry.title,
                        artist = entry.artist,
                        duration = 0.0,
                        path = entry.path,
                        album = entry.album
                    )
                }
                _history.value = songs
                Log.d(TAG, "Loaded ${songs.size} history entries from database")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading history: ${e.message}")
        }
    }

    suspend fun addToHistory(song: Song) = withContext(Dispatchers.IO) {
        try {
            // Get the last entry to check for duplicates
            val lastEntry = historyDao.getLastEntry()
            val lastSongId = lastEntry?.songId

            // Don't add if it's the same as the last entry (no consecutive duplicates)
            if (lastSongId == song.id) {
                Log.d(TAG, "Skipping duplicate song: ${song.title}")
                return@withContext
            }

            // Create a new history entry
            val entry = HistoryEntry(
                songId = song.id,
                title = song.title ?: "-",
                artist = song.artist ?: "-",
                album = song.album,
                path = song.path,
                timestamp = System.currentTimeMillis()
            )

            // Add to database
            historyDao.addEntry(entry)

            // Reload history to ensure we stay within the limit
            val entries = historyDao.getLastNSongs(MAX_HISTORY_SIZE)
            
            // If we have more than MAX_HISTORY_SIZE entries, delete the oldest ones
            if (entries.size > MAX_HISTORY_SIZE) {
                val toDelete = entries.drop(MAX_HISTORY_SIZE)
                toDelete.forEach { historyDao.deleteEntry(it) }
            }

            // Update the in-memory state
            val recentEntries = historyDao.getLastNSongs(MAX_HISTORY_SIZE)
            val songs = recentEntries.map { entry ->
                Song(
                    id = entry.songId,
                    title = entry.title,
                    artist = entry.artist,
                    duration = 0.0,
                    path = entry.path,
                    album = entry.album
                )
            }
            _history.value = songs
            Log.d(TAG, "Added song to history: ${song.title}, total history size: ${songs.size}")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding to history: ${e.message}")
        }
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        try {
            historyDao.clearAll()
            _history.value = emptyList()
            Log.d(TAG, "History cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing history: ${e.message}")
        }
    }
}

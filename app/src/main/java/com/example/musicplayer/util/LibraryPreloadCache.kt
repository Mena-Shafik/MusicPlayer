package com.example.musicplayer.util

import com.example.musicplayer.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Holds the song list MainActivity already fetches on startup to gate the splash screen,
 * so ListSongsScreen can reuse that result instead of independently re-querying MediaStore
 * the moment the splash dismisses — that duplicate scan was what made the song list render
 * empty and then pop in a beat after the splash disappeared.
 */
object LibraryPreloadCache {
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs

    fun set(list: List<Song>) {
        _songs.value = list
    }
}

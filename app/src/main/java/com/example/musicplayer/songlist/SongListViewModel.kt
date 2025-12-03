package com.example.musicplayer.songlist

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.collections.filter
import kotlin.collections.filterNot
import kotlin.collections.indices
import kotlin.collections.sortedBy
import kotlin.collections.sortedByDescending
import kotlin.collections.toList
import kotlin.text.contains
import kotlin.text.isBlank
import kotlin.text.lowercase
import kotlin.text.trim

class SongListViewModel(initialSongs: List<Song> = emptyList()) : ViewModel() {

    // raw playlist
    private val _songs = MutableStateFlow<List<Song>>(initialSongs)
    val songs: StateFlow<List<Song>> = _songs

    // UI controls
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    enum class SortOrder { TITLE_ASC, TITLE_DESC, ADDED_DESC }

    private val _sortOrder = MutableStateFlow(SortOrder.TITLE_ASC)
    val sortOrder: StateFlow<SortOrder> = _sortOrder

    // selected song id (null = none)
    private val _selectedId = MutableStateFlow<Int?>(null)
    val selectedId: StateFlow<Int?> = _selectedId

    // filtered + sorted list derived from songs, query and sort order
    val filteredSongs: StateFlow<List<Song>> = combine(
        _songs,
        _query,
        _sortOrder
    ) { list, q, sort ->
        var res = if (q.isBlank()) list else list.filter { song ->
            val title = try {
                song.title ?: ""
            } catch (_: Throwable) {
                ""
            }
            val artist = try {
                song.artist ?: ""
            } catch (_: Throwable) {
                ""
            }
            val lower = q.trim().lowercase()
            title.lowercase().contains(lower) || artist.lowercase().contains(lower)
        }
        res = when (sort) {
            SortOrder.TITLE_ASC -> res.sortedBy { (it.title ?: "").lowercase() }
            SortOrder.TITLE_DESC -> res.sortedByDescending { (it.title ?: "").lowercase() }
            SortOrder.ADDED_DESC -> res // assume original order is newest-first if provided
        }
        res
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // public actions

    fun load(songs: List<Song>, selectId: Int? = null) {
        viewModelScope.launch {
            _songs.value = songs
            _selectedId.value = selectId
        }
    }

    fun refresh() {
        // placeholder for a repository refresh; re-emit current list
        viewModelScope.launch {
            _songs.value = _songs.value.toList()
        }
    }

    fun setQuery(q: String) {
        // log when query is set to help debugging search
        Log.d("SongListViewModel", "setQuery: '$q'")
        _query.value = q
    }

    fun clearQuery() {
        _query.value = ""
    }

    fun setSort(order: SortOrder) {
        _sortOrder.value = order
    }

    fun toggleSortTitle() {
        _sortOrder.value = when (_sortOrder.value) {
            SortOrder.TITLE_ASC -> SortOrder.TITLE_DESC
            else -> SortOrder.TITLE_ASC
        }
    }

    fun selectSongById(id: Int?) {
        _selectedId.value = id
    }

    fun selectSongAt(index: Int) {
        val list = _songs.value
        if (index in list.indices) _selectedId.value = list[index].id
    }

    fun removeSongById(id: Int) {
        viewModelScope.launch {
            _songs.value = _songs.value.filterNot { it.id == id }
            if (_selectedId.value == id) _selectedId.value = null
        }
    }

    fun clearSelection() {
        _selectedId.value = null
    }
}
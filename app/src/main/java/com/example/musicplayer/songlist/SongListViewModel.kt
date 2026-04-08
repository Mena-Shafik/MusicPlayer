package com.example.musicplayer.songlist

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.model.RadioStation
import com.example.musicplayer.model.Song
import com.example.musicplayer.preferences.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.musicplayer.util.Util

class SongListViewModel(
    initialSongs: List<Song> = emptyList(),
    userStationsInitial: List<RadioStation> = emptyList(),
    private val context: Context? = null
) : ViewModel() {

    // raw playlist
    private val _songs = MutableStateFlow<List<Song>>(initialSongs)
    val songs: StateFlow<List<Song>> = _songs

    // UI controls
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    enum class SortOrder { TITLE_ASC, TITLE_DESC, GENRE_ASC, ADDED_DESC, ALBUM_TRACK, ARTIST_ASC }

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
            SortOrder.GENRE_ASC -> res.sortedWith(compareBy({ (it.genre ?: "").lowercase() }, { (it.title ?: "").lowercase() }))
            SortOrder.ALBUM_TRACK -> res.sortedWith(
                compareBy(
                    { (it.album ?: "").lowercase() },
                    { it.track ?: Int.MAX_VALUE },
                    { (it.title ?: "").lowercase() }
                )
            )
            SortOrder.ARTIST_ASC -> res.sortedWith(compareBy({ (it.artist ?: "").lowercase() }, { (it.album ?: "").lowercase() }, { it.track ?: Int.MAX_VALUE }))
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

    // --- Radio stations caching/fetching ---
    private val _radioStations = MutableStateFlow<List<RadioStation>>(emptyList())
    val radioStations: StateFlow<List<RadioStation>> = _radioStations

    private val _radioLoading = MutableStateFlow(false)
    val radioLoading: StateFlow<Boolean> = _radioLoading

    private val _radioError = MutableStateFlow<String?>(null)
    val radioError: StateFlow<String?> = _radioError

    /**
     * Fetch radio stations for GTA only when we don't already have cached stations.
     * Use this to avoid re-fetching when navigating back from the player screen.
     */
    fun fetchRadioStationsIfNeeded(limit: Int = 100) {
        if (_radioStations.value.isNotEmpty() || _radioLoading.value) return
        viewModelScope.launch {
            _radioLoading.value = true
            _radioError.value = null
            try {
                val list = withContext(Dispatchers.IO) { Util.fetchStationsNearGTA(limit = limit) }
                _radioStations.value = list
                if (list.isEmpty()) _radioError.value = "No stations found in the GTA"
            } catch (e: Exception) {
                _radioError.value = e.message ?: "Failed to load GTA stations"
            } finally {
                _radioLoading.value = false
            }
        }
    }

    /** Force refresh the radio station list. */
    fun refreshRadioStations(limit: Int = 100) {
        viewModelScope.launch {
            _radioLoading.value = true
            _radioError.value = null
            try {
                val list = withContext(Dispatchers.IO) { Util.fetchStationsNearGTA(limit = limit) }
                _radioStations.value = list
                if (list.isEmpty()) _radioError.value = "No stations found in the GTA"
            } catch (e: Exception) {
                _radioError.value = e.message ?: "Failed to load GTA stations"
            } finally {
                _radioLoading.value = false
            }
        }
    }

    // --- Provide the default (built-in) user stations for the UI ---
    // Initialize from constructor param so previews can be synchronous and not recreate UI
    private val _userStations = MutableStateFlow<List<RadioStation>>(userStationsInitial)
    val userStations: StateFlow<List<RadioStation>> = _userStations

    /** Load the built-in default user stations from Util (synchronous, cheap). */
    fun loadDefaultUserStations() {
        viewModelScope.launch {
            try {
                context?.let { ctx ->
                    _userStations.value = Util.getDefaultUserStations(ctx)
                } ?: run {
                    _userStations.value = emptyList()
                }
            } catch (e: Exception) {
                _userStations.value = emptyList()
            }
        }
    }

    // --- Album view toggle (persist UI choice) ---
    private val _isAlbumView = MutableStateFlow(false)
    val isAlbumView: StateFlow<Boolean> = _isAlbumView

    fun toggleAlbumView(enabled: Boolean) {
        if (enabled) {
            // ensure only one grouped view is active
            _isArtistView.value = false
            _isEraView.value = false
            _isGenreView.value = false
        }
        _isAlbumView.value = enabled
        // When enabling album view, prefer album+track sorting for correct track order within albums
        if (enabled) _sortOrder.value = SortOrder.ALBUM_TRACK
        context?.let {
            viewModelScope.launch {
                PreferencesManager.setAlbumView(it, enabled)
            }
        }
    }

    fun toggleAlbumView() {
        val newValue = !_isAlbumView.value
        if (newValue) {
            // turning album view on -> turn others off
            _isArtistView.value = false
            _isEraView.value = false
            _isGenreView.value = false
        }
        _isAlbumView.value = newValue
        // Ensure album view uses album+track sorting
        if (newValue) _sortOrder.value = SortOrder.ALBUM_TRACK
        context?.let {
            viewModelScope.launch {
                PreferencesManager.setAlbumView(it, newValue)
            }
        }
    }

    // NEW: artist view state
    private val _isArtistView = MutableStateFlow(false)
    val isArtistView: StateFlow<Boolean> = _isArtistView

    fun toggleArtistView(enabled: Boolean) {
        if (enabled) {
            _isAlbumView.value = false
            _isEraView.value = false
            _isGenreView.value = false
        }
        _isArtistView.value = enabled
        if (enabled) _sortOrder.value = SortOrder.ARTIST_ASC
        context?.let {
            viewModelScope.launch {
                PreferencesManager.setArtistView(it, enabled)
            }
        }
    }

    fun toggleArtistView() {
        val newValue = !_isArtistView.value
        if (newValue) {
            _isAlbumView.value = false
            _isEraView.value = false
            _isGenreView.value = false
        }
        _isArtistView.value = newValue
        if (newValue) _sortOrder.value = SortOrder.ARTIST_ASC
        context?.let {
            viewModelScope.launch {
                PreferencesManager.setArtistView(it, newValue)
            }
        }
    }

    // --- Era view state ---
    private val _isEraView = MutableStateFlow(false)
    val isEraView: StateFlow<Boolean> = _isEraView

    fun toggleEraView(enabled: Boolean) {
        // When enabling era view, turn off album/artist/genre mutually exclusive states
        if (enabled) {
            _isAlbumView.value = false
            _isArtistView.value = false
            _isGenreView.value = false
        }
        _isEraView.value = enabled
        context?.let {
            viewModelScope.launch {
                PreferencesManager.setEraView(it, enabled)
            }
        }
    }

    fun toggleEraView() {
        val newValue = !_isEraView.value
        if (newValue) {
            _isAlbumView.value = false
            _isArtistView.value = false
            _isGenreView.value = false
        }
        _isEraView.value = newValue
        context?.let {
            viewModelScope.launch {
                PreferencesManager.setEraView(it, newValue)
            }
        }
    }

    // --- Genre view state ---
    private val _isGenreView = MutableStateFlow(false)
    val isGenreView: StateFlow<Boolean> = _isGenreView
    // Small helper to enable exactly one grouped view and disable the others.
    private fun setExclusiveViews(album: Boolean = false, artist: Boolean = false, era: Boolean = false, genre: Boolean = false) {
        _isAlbumView.value = album
        _isArtistView.value = artist
        _isEraView.value = era
        _isGenreView.value = genre
    }

    /**
     * Enable/disable genre grouped view. When enabling, turn off other mutually-exclusive views.
     * This version accepts an explicit boolean.
     */
    fun toggleGenreView(enabled: Boolean) {
        if (enabled) {
            setExclusiveViews(genre = true)
        } else {
            _isGenreView.value = false
        }
        // Persist genre view preference
        context?.let {
            viewModelScope.launch {
                PreferencesManager.setGenreView(it, enabled)
            }
        }
    }

    /** Parameterless toggle that flips the current genre view state. */
    fun toggleGenreView() {
        val newValue = !_isGenreView.value
        if (newValue) setExclusiveViews(genre = true) else _isGenreView.value = false
        // Persist the toggled value
        context?.let {
            viewModelScope.launch {
                PreferencesManager.setGenreView(it, newValue)
            }
        }
    }

    // --- Persistent UI state for radio selection ---
    private val _isRadioSelected = MutableStateFlow(false)
    val isRadioSelected: StateFlow<Boolean> = _isRadioSelected

    fun setRadioSelected(selected: Boolean) {
        _isRadioSelected.value = selected
        context?.let {
            viewModelScope.launch {
                PreferencesManager.setRadioSelected(it, selected)
            }
        }
    }

    fun toggleRadioSelected() {
        val newValue = !_isRadioSelected.value
        _isRadioSelected.value = newValue
        context?.let {
            viewModelScope.launch {
                PreferencesManager.setRadioSelected(it, newValue)
            }
        }
    }

    // --- Use default radio list preference ---
    private val _useDefaultRadioList = MutableStateFlow(true)
    val useDefaultRadioList: StateFlow<Boolean> = _useDefaultRadioList

    fun setUseDefaultRadioList(useDefault: Boolean) {
        _useDefaultRadioList.value = useDefault
        context?.let {
            viewModelScope.launch {
                PreferencesManager.setUseDefaultRadioList(it, useDefault)
            }
        }
    }

    /**
     * Get radio stations based on the useDefaultRadioList preference.
     * If true, returns JSON default stations.
     * If false, fetches from Radio Browser API.
     */
    fun loadRadioStations() {
        val useDefault = _useDefaultRadioList.value
        if (useDefault) {
            loadDefaultUserStations()
        } else {
            fetchRadioStationsIfNeeded()
        }
    }

    // Load persisted preferences when ViewModel is initialized
    init {
        context?.let {
            viewModelScope.launch {
                PreferencesManager.getAlbumViewFlow(it).collect { savedAlbumView ->
                    _isAlbumView.value = savedAlbumView
                    if (savedAlbumView) _isArtistView.value = false
                }
            }
            viewModelScope.launch {
                PreferencesManager.getArtistViewFlow(it).collect { savedArtistView ->
                    _isArtistView.value = savedArtistView
                    if (savedArtistView) _isAlbumView.value = false
                }
            }
            viewModelScope.launch {
                PreferencesManager.getEraViewFlow(it).collect { savedEraView ->
                    _isEraView.value = savedEraView
                    if (savedEraView) {
                        _isAlbumView.value = false
                        _isArtistView.value = false
                    }
                }
            }
            viewModelScope.launch {
                PreferencesManager.getGenreViewFlow(it).collect { savedGenreView ->
                    _isGenreView.value = savedGenreView
                    if (savedGenreView) {
                        _isAlbumView.value = false
                        _isArtistView.value = false
                        _isEraView.value = false
                    }
                }
            }
            viewModelScope.launch {
                PreferencesManager.getRadioSelectedFlow(it).collect { savedRadioSelected ->
                    _isRadioSelected.value = savedRadioSelected
                }
            }
            viewModelScope.launch {
                PreferencesManager.getUseDefaultRadioListFlow(it).collect { savedUseDefault ->
                    _useDefaultRadioList.value = savedUseDefault
                }
            }
        }
    }
}
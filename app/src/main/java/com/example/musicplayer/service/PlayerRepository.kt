package com.example.musicplayer.service

import com.example.musicplayer.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.random.Random

object PlayerRepository {
    private val _playlist = MutableStateFlow<List<Song>>(emptyList())
    val playlist: StateFlow<List<Song>> = _playlist

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs

    // shuffle / replay
    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled

    private val _replayEnabled = MutableStateFlow(false)
    val replayEnabled: StateFlow<Boolean> = _replayEnabled

    // shuffle helpers
    private var shuffleQueue: MutableList<Int> = mutableListOf()
    private val playedHistory: MutableList<Int> = mutableListOf()

    fun setPlaylist(songs: List<Song>, startIndex: Int) {
        _playlist.value = songs
        _currentIndex.value = startIndex
        // reset shuffle/replay state
        shuffleQueue.clear()
        playedHistory.clear()
        _shuffleEnabled.value = false
        _replayEnabled.value = false
        // reset playback state so service will prepare the new item when play is requested
        _positionMs.value = 0L
        _durationMs.value = 0L
        _isPlaying.value = false
    }

    fun setCurrentIndex(idx: Int) { _currentIndex.value = idx }
    fun setIsPlaying(v: Boolean) { _isPlaying.value = v }
    fun setPositionMs(p: Long) { _positionMs.value = p }
    fun setDurationMs(d: Long) { _durationMs.value = d }

    fun toggleReplay() {
        val newVal = !_replayEnabled.value
        _replayEnabled.value = newVal
        if (newVal) {
            // when enabling replay, disable shuffle and clear shuffle state
            if (_shuffleEnabled.value) {
                _shuffleEnabled.value = false
                shuffleQueue.clear()
                playedHistory.clear()
            }
        }
    }

    fun toggleShuffle(enabled: Boolean) {
        if (_shuffleEnabled.value == enabled) return
        _shuffleEnabled.value = enabled
        if (enabled) {
            // enabling shuffle disables replay
            if (_replayEnabled.value) _replayEnabled.value = false
            prepareShufflePool(excludeIndex = _currentIndex.value)
        } else {
            // disable shuffle, clear state
            shuffleQueue.clear()
            playedHistory.clear()
        }
    }

    private fun prepareShufflePool(excludeIndex: Int) {
        val size = _playlist.value.size
        if (size <= 1) {
            shuffleQueue.clear()
            return
        }
        shuffleQueue = (0 until size).filter { it != excludeIndex }.shuffled(Random(System.currentTimeMillis())).toMutableList()
    }

    /**
     * Compute the next index according to current shuffle/replay state without mutating _currentIndex.
     * This method also consumes shuffleQueue and records history when appropriate.
     */
    fun nextIndex(): Int {
        val songs = _playlist.value
        if (songs.isEmpty()) return _currentIndex.value

        return if (_shuffleEnabled.value) {
            if (shuffleQueue.isEmpty()) prepareShufflePool(excludeIndex = _currentIndex.value)
            val next = shuffleQueue.firstOrNull() ?: _currentIndex.value
            if (shuffleQueue.isNotEmpty()) shuffleQueue.removeAt(0)
            playedHistory.add(_currentIndex.value)
            next
        } else if (_replayEnabled.value) {
            // replay current (don't advance)
            _currentIndex.value
        } else {
            (_currentIndex.value + 1) % songs.size
        }
    }

    fun prevIndex(): Int {
        val songs = _playlist.value
        if (songs.isEmpty()) return _currentIndex.value

        return if (_shuffleEnabled.value) {
            if (playedHistory.isNotEmpty()) {
                val last = playedHistory.removeAt(playedHistory.size - 1)
                last
            } else {
                // fallback
                (_currentIndex.value - 1 + songs.size) % songs.size
            }
        } else if (_replayEnabled.value) {
            // replay current
            _currentIndex.value
        } else {
            (_currentIndex.value - 1 + songs.size) % songs.size
        }
    }
}

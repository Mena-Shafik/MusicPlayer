package com.example.musicplayer.service

import android.util.Log
import com.example.musicplayer.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.random.Random

object PlayerRepository {
    private const val TAG = "PlayerRepository"
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

    // --- prepared state helpers ---
    // Tracks whether the underlying MediaPlayer is prepared. Callers (the Service) should
    // mark the player prepared from onPrepared and clear when releasing/resetting the player.
    private val _isPrepared = MutableStateFlow(false)
    val isPrepared: StateFlow<Boolean> = _isPrepared

    /**
     * Mark player as prepared and set a safe duration value.
     * Call this from MediaPlayer.OnPreparedListener with the known duration (mp.duration).
     */
    fun markPrepared(durationMillis: Long) {
        _isPrepared.value = true
        _durationMs.value = durationMillis
        safeLog("markPrepared duration=$durationMillis")
    }

    /**
     * Clear prepared state (call when player is released/reset).
     */
    fun clearPrepared() {
        _isPrepared.value = false
        _durationMs.value = 0L
        safeLog("clearPrepared")
    }

    /**
     * A safe getter for duration. Returns last-known duration (0 if unknown).
     * Avoid calling MediaPlayer.getDuration() directly; instead rely on this value or
     * call markPrepared(...) from your Service's OnPreparedListener.
     */
    fun getSafeDuration(): Long = _durationMs.value

    /**
     * Safely update position using a MediaPlayer instance. This wraps currentPosition
     * in a try/catch to avoid IllegalStateException when the player isn't in a proper
     * state (this is the error you saw: "Attempt to call getDuration in wrong state").
     *
     * Usage: call PlayerRepository.updatePositionFromPlayerSafe(mediaPlayer)
     * on your poll/update loop instead of calling mediaPlayer.currentPosition directly.
     */
    fun updatePositionFromPlayerSafe(mediaPlayer: android.media.MediaPlayer?) {
        if (mediaPlayer == null) {
            _positionMs.value = 0L
            return
        }
        try {
            // currentPosition can throw IllegalStateException if the player isn't prepared/started
            val pos = mediaPlayer.currentPosition.toLong()
            _positionMs.value = pos
        } catch (_: IllegalStateException) {
            safeLog("updatePositionFromPlayerSafe: IllegalStateException - player not prepared")
            // keep previous position value rather than resetting
        } catch (t: Throwable) {
            safeLog("updatePositionFromPlayerSafe: unexpected throwable=${t.message}")
        }
    }

    // shuffle / replay
    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled

    private val _replayEnabled = MutableStateFlow(false)
    val replayEnabled: StateFlow<Boolean> = _replayEnabled

    // shuffle helpers
    private var shuffleQueue: MutableList<Int> = mutableListOf()
    private val playedHistory: MutableList<Int> = mutableListOf()

    // Use a local safe logging helper so JVM unit tests won't fail when android.util.Log is not available.
    private fun safeLog(message: String) {
        try {
            Log.d(TAG, message)
        } catch (_: Throwable) {
            // ignore logging errors during unit tests on the JVM
        }
    }

    fun setPlaylist(songs: List<Song>, startIndex: Int): Boolean {
        // Avoid redundant resets which can restart/prepare the service and cause unexpected switches.
        val current = _playlist.value
        // Consider playlists identical if they have same length and matching song IDs in order
        val same = if (current.size == songs.size) {
            current.indices.all { i -> current.getOrNull(i)?.id == songs.getOrNull(i)?.id }
        } else false

        if (same && _currentIndex.value == startIndex) {
            safeLog("setPlaylist called with identical playlist and index=$startIndex -> ignoring")
            return false
        }

        safeLog("setPlaylist startIndex=$startIndex size=${songs.size}")
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
        // also clear prepared flag because playlist changed
        _isPrepared.value = false
        return true
    }

    fun setCurrentIndex(idx: Int) {
        safeLog("setCurrentIndex -> $idx")
        _currentIndex.value = idx
    }
    fun setIsPlaying(v: Boolean) {
        safeLog("setIsPlaying -> $v")
        _isPlaying.value = v
    }
    fun setPositionMs(p: Long) {
        _positionMs.value = p
    }
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

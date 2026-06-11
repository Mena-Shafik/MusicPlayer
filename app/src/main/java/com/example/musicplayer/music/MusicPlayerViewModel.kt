package com.example.musicplayer.music

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.history.HistoryRepository
import com.example.musicplayer.model.Song
import com.example.musicplayer.service.PlayerIntentBuilder
import com.example.musicplayer.service.PlayerStateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MusicPlayerViewModel : ViewModel() {
    private val TAG = "MusicPlayerVM"
    // Expose flows from repository
    val playlist: StateFlow<List<Song>> = PlayerStateManager.playlist
    val currentIndex: StateFlow<Int> = PlayerStateManager.currentIndex
    val isPlaying: StateFlow<Boolean> = PlayerStateManager.isPlaying
    val positionMs: StateFlow<Long> = PlayerStateManager.positionMs
    val durationMs: StateFlow<Long> = PlayerStateManager.durationMs
    val replayEnabled: StateFlow<Boolean> = PlayerStateManager.replayEnabled
    val shuffleEnabled: StateFlow<Boolean> = PlayerStateManager.shuffleEnabled

    private var historyRepository: HistoryRepository? = null

    fun setPlaylist(context: Context, songs: List<Song>, startIndex: Int = 0) {
        Log.d(TAG, "setPlaylist startIndex=$startIndex size=${songs.size}")
        // Always update repository's current index immediately so UI reflects selection
        try { PlayerStateManager.setCurrentIndex(startIndex) } catch (_: Throwable) {}
        val changed = PlayerStateManager.setPlaylist(songs, startIndex)
        // ask service to prepare and start — always request play so selection reliably starts playback.
        val appCtx = context.applicationContext
        Log.d(TAG, "setPlaylist: requesting startPlay using appCtx=$appCtx (changed=$changed)")
        // Update the service metadata for the selected index (helps notifications/UI sync)
        val title = songs.getOrNull(startIndex)?.title ?: ""
        val artist = songs.getOrNull(startIndex)?.artist ?: ""
        try { PlayerIntentBuilder.startUpdate(appCtx, false, startIndex, title, artist) } catch (_: Throwable) {}
        // Explicitly ask the service to prepare (and start) the requested index. This is
        // more reliable than relying on startPlay coalescing behavior.
        PlayerIntentBuilder.startPrepare(appCtx, startIndex, true)

        // Track the song in history
        val song = songs.getOrNull(startIndex)
        if (song != null) {
            if (historyRepository == null) {
                historyRepository = HistoryRepository(appCtx)
            }
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    historyRepository?.addToHistory(song)
                } catch (e: Exception) {
                    Log.e(TAG, "Error adding to history: ${e.message}")
                }
            }
        }
    }

    // ...existing code...

    fun play(context: Context) {
        val appCtx = context.applicationContext
        Log.d(TAG, "play requested using appCtx=$appCtx")
        PlayerIntentBuilder.startPlay(appCtx)
    }
    fun pause(context: Context) {
        val appCtx = context.applicationContext
        Log.d(TAG, "pause requested using appCtx=$appCtx")
        PlayerIntentBuilder.startPause(appCtx)
    }
    fun togglePlayPause(context: Context) {
        val appCtx = context.applicationContext
        Log.d(TAG, "togglePlayPause current=${isPlaying.value} using appCtx=$appCtx")
        if (isPlaying.value) pause(appCtx) else play(appCtx)
    }
    fun seekTo(context: Context, ms: Int) {
        val appCtx = context.applicationContext
        Log.d(TAG, "seekTo ms=$ms using appCtx=$appCtx")
        PlayerIntentBuilder.startSeek(appCtx, ms.toLong())
    }

    fun toggleReplay() { PlayerStateManager.toggleReplay() }
    fun toggleShuffle(enabled: Boolean) { PlayerStateManager.toggleShuffle(enabled) }

    fun next(context: Context) {
        val appCtx = context.applicationContext
        Log.d(TAG, "next requested using appCtx=$appCtx")
        PlayerIntentBuilder.startNext(appCtx)
    }
    fun previous(context: Context) {
        val appCtx = context.applicationContext
        Log.d(TAG, "previous requested using appCtx=$appCtx")
        PlayerIntentBuilder.startPrev(appCtx)
    }

    // ViewModel cleanup - nothing to release here, service owns media playback.
    override fun onCleared() { super.onCleared() }
}
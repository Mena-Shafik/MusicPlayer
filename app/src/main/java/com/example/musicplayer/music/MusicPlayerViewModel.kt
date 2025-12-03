package com.example.musicplayer.music

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.musicplayer.model.Song
import com.example.musicplayer.service.PlayerIntentBuilder
import com.example.musicplayer.service.PlayerRepository
import kotlinx.coroutines.flow.StateFlow

class MusicPlayerViewModel : ViewModel() {
    private val TAG = "MusicPlayerVM"
    // Expose flows from repository
    val playlist: StateFlow<List<Song>> = PlayerRepository.playlist
    val currentIndex: StateFlow<Int> = PlayerRepository.currentIndex
    val isPlaying: StateFlow<Boolean> = PlayerRepository.isPlaying
    val positionMs: StateFlow<Long> = PlayerRepository.positionMs
    val durationMs: StateFlow<Long> = PlayerRepository.durationMs
    val replayEnabled: StateFlow<Boolean> = PlayerRepository.replayEnabled
    val shuffleEnabled: StateFlow<Boolean> = PlayerRepository.shuffleEnabled

    fun setPlaylist(context: Context, songs: List<Song>, startIndex: Int = 0) {
        Log.d(TAG, "setPlaylist startIndex=$startIndex size=${songs.size}")
        PlayerRepository.setPlaylist(songs, startIndex)
        // ask service to prepare and start using applicationContext for safety
        val appCtx = context.applicationContext
        Log.d(TAG, "setPlaylist: using appCtx=$appCtx")
        PlayerIntentBuilder.startPlay(appCtx)
    }

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

    fun toggleReplay() { PlayerRepository.toggleReplay() }
    fun toggleShuffle(enabled: Boolean) { PlayerRepository.toggleShuffle(enabled) }

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
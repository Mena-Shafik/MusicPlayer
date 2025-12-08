package com.example.musicplayer.service

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.musicplayer.service.PlayerRepository

object PlayerIntentBuilder {
    private const val TAG = "PlayerIntentBuilder"
    // guard: suppress startPlay calls that happen too frequently to avoid racing the service
    private var lastStartPlayMs: Long = 0L
    // guards for skip actions
    private var lastStartNextMs: Long = 0L
    private var lastStartPrevMs: Long = 0L
    private fun intent(context: Context, action: String) = Intent(context, PlayerForegroundService::class.java).apply { this.action = action }

    /**
     * Start the playback service. If [foreground] is true, use startForegroundService on O+ so
     * the service can promote to a foreground service; otherwise use startService which is
     * appropriate for short control messages (pause/next/prev/seek) that shouldn't trigger
     * the system watchdog requiring an immediate startForeground() call.
     */
    private fun startServiceCompat(context: Context, intent: Intent, foreground: Boolean = false) {
        try {
            val appCtx = context.applicationContext
            Log.d(TAG, "startServiceCompat foreground=$foreground action=${intent.action}")
            if (foreground && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) appCtx.startForegroundService(intent) else appCtx.startService(intent)
        } catch (e: Exception) {
            // fallback to startService for safety
            try { context.applicationContext.startService(intent) } catch (_: Exception) { }
        }
    }

    fun startPlay(context: Context) {
        val now = System.currentTimeMillis()
        // coalesce multiple calls within 80ms (shorter to avoid suppressing quick selection actions)
        if (now - lastStartPlayMs < 80L) {
            Log.d(TAG, "startPlay: suppressed duplicate request (delta=${now - lastStartPlayMs}ms)")
            return
        }
        lastStartPlayMs = now
        // Attach the current index as an extra so the service can reliably prepare the requested item
        val i = intent(context, PlayerActions.ACTION_PLAY)
        try {
            i.putExtra(PlayerActions.EXTRA_CURRENT_INDEX, PlayerRepository.currentIndex.value)
        } catch (_: Throwable) {}
        // Play may need the service to run in foreground, so request foreground start on O+
        startServiceCompat(context, i, foreground = true)
    }

    fun startPause(context: Context) {
        // Pause is a quick control message; don't use startForegroundService for pause to avoid
        // the watchdog if the service doesn't call startForeground immediately.
        startServiceCompat(context, intent(context, PlayerActions.ACTION_PAUSE), foreground = false)
    }

    fun startNext(context: Context) {
        val now = System.currentTimeMillis()
        // coalesce multiple next calls within 500ms
        if (now - lastStartNextMs < 400L) {
            Log.d(TAG, "startNext: suppressed duplicate request (delta=${now - lastStartNextMs}ms)")
            return
        }
        lastStartNextMs = now
        // Control action -> non-foreground
        startServiceCompat(context, intent(context, PlayerActions.ACTION_NEXT), foreground = false)
    }

    fun startPrev(context: Context) {
        val now = System.currentTimeMillis()
        // coalesce multiple prev calls within 500ms
        if (now - lastStartPrevMs < 400L) {
            Log.d(TAG, "startPrev: suppressed duplicate request (delta=${now - lastStartPrevMs}ms)")
            return
        }
        lastStartPrevMs = now
        startServiceCompat(context, intent(context, PlayerActions.ACTION_PREV), foreground = false)
    }

    fun startStop(context: Context) {
        // Stopping may involve stopping foreground - ensure service starts in foreground if needed
        startServiceCompat(context, intent(context, PlayerActions.ACTION_STOP), foreground = true)
    }

    fun startSeek(context: Context, positionMs: Long) {
        val i = intent(context, PlayerActions.ACTION_SEEK)
        i.putExtra(PlayerActions.EXTRA_SEEK_POSITION, positionMs)
        startServiceCompat(context, i, foreground = false)
    }

    /**
     * Request the service to prepare (and optionally start) a specific index from the playlist.
     * This is more explicit than startPlay and ensures the requested index is prepared by the service.
     */
    fun startPrepare(context: Context, index: Int, startPlaying: Boolean = false) {
        val i = intent(context, PlayerActions.ACTION_PREPARE)
        try { i.putExtra(PlayerActions.EXTRA_CURRENT_INDEX, index) } catch (_: Throwable) {}
        try { i.putExtra(PlayerActions.EXTRA_IS_PLAYING, startPlaying) } catch (_: Throwable) {}
        startServiceCompat(context, i, foreground = true)
    }

    fun startUpdate(context: Context, isPlaying: Boolean, currentIndex: Int, title: String, artist: String) {
        val i = intent(context, PlayerActions.ACTION_UPDATE)
        i.putExtra(PlayerActions.EXTRA_IS_PLAYING, isPlaying)
        i.putExtra(PlayerActions.EXTRA_CURRENT_INDEX, currentIndex)
        i.putExtra(PlayerActions.EXTRA_SONG_TITLE, title)
        i.putExtra(PlayerActions.EXTRA_SONG_ARTIST, artist)
        // Update may be used to ensure the service is foregrounded when playback starts; mark as foreground
        startServiceCompat(context, i, foreground = true)
    }
}

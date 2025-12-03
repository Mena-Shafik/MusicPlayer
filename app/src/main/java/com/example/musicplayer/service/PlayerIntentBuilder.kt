package com.example.musicplayer.service

import android.content.Context
import android.content.Intent
import android.os.Build

object PlayerIntentBuilder {
    private fun intent(context: Context, action: String) = Intent(context, PlayerForegroundService::class.java).apply { this.action = action }

    /**
     * Start the playback service. If [foreground] is true, use startForegroundService on O+ so
     * the service can promote to a foreground service; otherwise use startService which is
     * appropriate for short control messages (pause/next/prev/seek) that shouldn't trigger
     * the system watchdog requiring an immediate startForeground() call.
     */
    private fun startServiceCompat(context: Context, intent: Intent, foreground: Boolean = false) {
        try {
            if (foreground && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent) else context.startService(intent)
        } catch (e: Exception) {
            // fallback to startService for safety
            try { context.startService(intent) } catch (_: Exception) { }
        }
    }

    fun startPlay(context: Context) {
        // Play may need the service to run in foreground, so request foreground start on O+
        startServiceCompat(context, intent(context, PlayerActions.ACTION_PLAY), foreground = true)
    }

    fun startPause(context: Context) {
        // Pause is a quick control message; don't use startForegroundService for pause to avoid
        // the watchdog if the service doesn't call startForeground immediately.
        startServiceCompat(context, intent(context, PlayerActions.ACTION_PAUSE), foreground = false)
    }

    fun startNext(context: Context) {
        // Control action -> non-foreground
        startServiceCompat(context, intent(context, PlayerActions.ACTION_NEXT), foreground = false)
    }

    fun startPrev(context: Context) {
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

package com.example.musicplayer.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.media.app.NotificationCompat as MediaAppNotificationCompat
import com.example.musicplayer.R
import com.example.musicplayer.MainActivity

object PlayerNotificationManager {
    private const val CHANNEL_ID = "music_player_channel"
    private const val CHANNEL_NAME = "Music Player"
    const val NOTIFICATION_ID = 1001

    fun ensureChannel(context: Context) {
        // minSdk >= 33 in this project, create channel unconditionally
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Playback controls and current track" }
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    /**
     * Build a media notification. progressMs and durationMs are in milliseconds.
     * This converts to seconds internally to keep Notification progress ints small and avoid overflow.
     */
    fun buildNotification(
        context: Context,
        title: String,
        artist: String,
        isPlaying: Boolean,
        artwork: Bitmap?,
        showPrev: Boolean = true,
        showNext: Boolean = true,
        mediaSessionToken: android.os.Parcelable? = null,
        progressMs: Long = 0L,
        durationMs: Long = 0L
    ): Notification {
        ensureChannel(context)

        // Content intent - opens the app when user taps the notification
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Delete intent: broadcast to NotificationDeleteReceiver so dismissing the notification pauses playback
        val deleteIntent = Intent(context, NotificationDeleteReceiver::class.java)
        val stopPi = PendingIntent.getBroadcast(context, 6, deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Action: Previous
        val prevAction = if (showPrev) {
            val prevIntent = Intent(context, PlayerForegroundService::class.java).apply { action = PlayerActions.ACTION_PREV }
            val pi = PendingIntent.getService(context, 4, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            NotificationCompat.Action(android.R.drawable.ic_media_previous, "Previous", pi)
        } else null

        // Action: Seek Back
        val seekBackAction = run {
            val sbIntent = Intent(context, PlayerForegroundService::class.java).apply { action = PlayerActions.ACTION_SEEK_BACK }
            val sbPi = PendingIntent.getService(context, 7, sbIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            NotificationCompat.Action(android.R.drawable.ic_media_rew, "Rewind", sbPi)
        }

        // Action: Play/Pause
        val playPauseIntent = Intent(context, PlayerForegroundService::class.java).apply { action = if (isPlaying) PlayerActions.ACTION_PAUSE else PlayerActions.ACTION_PLAY }
        val playPausePi = PendingIntent.getService(context, 3, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val playPauseAction = NotificationCompat.Action(
            if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
            if (isPlaying) "Pause" else "Play",
            playPausePi
        )

        // Action: Seek Forward
        val seekForwardAction = run {
            val sfIntent = Intent(context, PlayerForegroundService::class.java).apply { action = PlayerActions.ACTION_SEEK_FORWARD }
            val sfPi = PendingIntent.getService(context, 8, sfIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            NotificationCompat.Action(android.R.drawable.ic_media_ff, "Forward", sfPi)
        }

        // Action: Next
        val nextAction = if (showNext) {
            val nextIntent = Intent(context, PlayerForegroundService::class.java).apply { action = PlayerActions.ACTION_NEXT }
            val pi = PendingIntent.getService(context, 5, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            NotificationCompat.Action(android.R.drawable.ic_media_next, "Next", pi)
        } else null

        // Build the notification. Order actions: prev, seekBack, play/pause, seekForward, next (indices 0..4)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentTitle(title)
            .setContentText(artist)
            .setContentIntent(contentIntent)
            .setDeleteIntent(stopPi)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        // Create MediaStyle and attach support MediaSessionCompat.Token when available
        // Keep compact view showing Previous, Play/Pause, Next (indices 0,2,4)
        val style = MediaAppNotificationCompat.MediaStyle().setShowActionsInCompactView(0, 2, 4)
        if (mediaSessionToken is android.support.v4.media.session.MediaSessionCompat.Token) {
            // attach the compat token directly
            style.setMediaSession(mediaSessionToken)
        }

        builder.setStyle(style)

        if (artwork != null) builder.setLargeIcon(artwork)

        // Add actions in the order described above
        prevAction?.let { builder.addAction(it) }
        builder.addAction(seekBackAction)
        builder.addAction(playPauseAction)
        builder.addAction(seekForwardAction)
        nextAction?.let { builder.addAction(it) }

        // If duration provided (>0), attach a determinate progress bar reflecting playback position
        if (durationMs > 0L) {
            // Convert milliseconds to seconds to keep int range safe and the progress readable
            val maxSec = (durationMs / 1000L).coerceAtLeast(1L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            val progSec = (progressMs / 1000L).coerceIn(0L, maxSec.toLong()).toInt()
            builder.setProgress(maxSec, progSec, false)
        }

        return builder.build()
    }

    @Suppress("MissingPermission")
    fun postNotification(context: Context, notification: Notification) {
        // Runtime permission check for POST_NOTIFICATIONS (Android 13+). Project minSdk >= 33,
        // so perform the check unconditionally to be explicit.
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch ( _: SecurityException) {
            // In case permission check race or other security issue, fail gracefully
        }
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }
}

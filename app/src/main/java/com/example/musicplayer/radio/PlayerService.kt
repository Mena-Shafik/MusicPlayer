package com.example.musicplayer.radio

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat as MediaAppNotificationCompat
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.example.musicplayer.MainActivity
import android.app.Service
import androidx.media3.common.util.UnstableApi

@UnstableApi
class PlayerService : Service() {

    private lateinit var player: ExoPlayer
    private var currentTitle: String? = null

    @SuppressLint("RestrictedApi")
    override fun onCreate() {
        super.onCreate()

        // Initialize Media3 ExoPlayer
        player = ExoPlayer.Builder(this).build()

        // Update notification when playback state changes
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateNotification(isPlaying)
            }
        })

        createNotificationChannel()

        // Start with a foreground notification so the service isn't killed immediately
        val initial = buildNotification(currentTitle ?: getString(com.example.musicplayer.R.string.app_name), false)
        startForeground(NOTIFICATION_ID, initial)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_PLAY -> player.play()
            ACTION_PAUSE -> player.pause()
            ACTION_STOP -> {
                stopForeground(true)
                stopSelf()
            }
            ACTION_PLAY_STATION -> {
                val url = intent.getStringExtra(EXTRA_STATION_URL)
                val title = intent.getStringExtra(EXTRA_STATION_TITLE)
                if (!url.isNullOrBlank()) {
                    currentTitle = title
                    playUrl(url)
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun playUrl(url: String) {
        val mediaItem = MediaItem.fromUri(url)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    private fun updateNotification(isPlaying: Boolean) {
        val title = currentTitle ?: getString(com.example.musicplayer.R.string.app_name)
        val notif = buildNotification(title, isPlaying)
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, notif)
    }

    private fun buildNotification(title: String, isPlaying: Boolean): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val playIntent = Intent(this, PlayerService::class.java).apply { action = ACTION_PLAY }
        val playPi = PendingIntent.getService(this, 1, playIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val pauseIntent = Intent(this, PlayerService::class.java).apply { action = ACTION_PAUSE }
        val pausePi = PendingIntent.getService(this, 2, pauseIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val stopIntent = Intent(this, PlayerService::class.java).apply { action = ACTION_STOP }
        val stopPi = PendingIntent.getService(this, 3, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(com.example.musicplayer.R.drawable.ic_music_note)
            .setContentTitle(title)
            .setContentText(if (isPlaying) "Playing" else "Paused")
            .setContentIntent(contentIntent)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(if (isPlaying) NotificationCompat.Action(0, "Pause", pausePi) else NotificationCompat.Action(0, "Play", playPi))
            .addAction(NotificationCompat.Action(0, "Stop", stopPi))

        val style = MediaAppNotificationCompat.MediaStyle()
            .setShowActionsInCompactView(0)

        builder.setStyle(style)

        return builder.build()
    }

    override fun onDestroy() {
        try { player.release() } catch (_: Throwable) {}
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Radio Playback", NotificationManager.IMPORTANCE_LOW)
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "radio_playback_channel"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY = "com.example.musicplayer.action.PLAY"
        const val ACTION_PAUSE = "com.example.musicplayer.action.PAUSE"
        const val ACTION_STOP = "com.example.musicplayer.action.STOP"
        const val ACTION_PLAY_STATION = "com.example.musicplayer.action.PLAY_STATION"
        const val EXTRA_STATION_URL = "extra_station_url"
        const val EXTRA_STATION_TITLE = "extra_station_title"
    }
}

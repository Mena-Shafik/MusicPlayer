package com.example.musicplayer.radio

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat as MediaAppNotificationCompat
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import com.example.musicplayer.MainActivity
import android.app.Service
import androidx.media3.common.util.UnstableApi
import android.media.MediaPlayer

@UnstableApi
class RadioPlayerService : Service() {

    private lateinit var player: ExoPlayer
    private var currentTitle: String? = null
    private var currentUrl: String? = null
    private var androidPlayer: MediaPlayer? = null

    @SuppressLint("RestrictedApi")
    override fun onCreate() {
        super.onCreate()

        // Initialize Media3 ExoPlayer
        player = ExoPlayer.Builder(this).build()

        // Configure audio attributes so ExoPlayer requests audio focus correctly
        try {
            val attrs = AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build()
            player.setAudioAttributes(attrs, true)
        } catch (t: Throwable) {
            Log.w("RadioPlayerService", "Failed to set audio attributes: ${t.message}")
        }

        // Update notification and track playback state when playback state changes
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateNotification(isPlaying)
                lastStatus = if (isPlaying) "playing" else "paused"
                Log.d("RadioPlayerService", "onIsPlayingChanged: $isPlaying")
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val stateName = when (playbackState) {
                    Player.STATE_IDLE -> "IDLE"
                    Player.STATE_BUFFERING -> "BUFFERING"
                    Player.STATE_READY -> "READY"
                    Player.STATE_ENDED -> "ENDED"
                    else -> "UNKNOWN"
                }
                lastStatus = stateName
                Log.d("RadioPlayerService", "Playback state changed: $stateName ($playbackState)")
            }

            override fun onPlayerError(error: PlaybackException) {
                lastStatus = "error: ${error.message}"
                Log.e("RadioPlayerService", "Player error: ${error.message}", error)
                // Try fallback to Android MediaPlayer for simple HTTP streams
                currentUrl?.let { url ->
                    Log.w("RadioPlayerService", "ExoPlayer failed, attempting Android MediaPlayer fallback for $url")
                    startAndroidMediaPlayer(url)
                }
            }
        })

        createNotificationChannel()

        // Start with a foreground notification so the service isn't killed immediately
        val initial = buildNotification(currentTitle ?: getString(com.example.musicplayer.R.string.app_name), false)
        startForeground(NOTIFICATION_ID, initial)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d("RadioPlayerService", "onStartCommand action=$action extras=${intent?.extras}")
        when (action) {
            ACTION_PLAY -> {
                Log.d("RadioPlayerService", "ACTION_PLAY: play() called")
                lastStatus = "play_request"
                player.play()
            }
            ACTION_PAUSE -> {
                Log.d("RadioPlayerService", "ACTION_PAUSE: pause() called")
                lastStatus = "pause_request"
                player.pause()
            }
            ACTION_STOP -> {
                Log.d("RadioPlayerService", "ACTION_STOP: stopping service")
                lastStatus = "stopped"
                try { stopForeground(true) } catch (_: Throwable) {}
                stopSelf()
            }
            ACTION_PLAY_STATION -> {
                val url = intent.getStringExtra(EXTRA_STATION_URL)
                val title = intent.getStringExtra(EXTRA_STATION_TITLE)
                Log.d("RadioPlayerService", "ACTION_PLAY_STATION url=$url title=$title")
                if (!url.isNullOrBlank()) {
                    currentTitle = title
                    playUrl(url)
                } else {
                    Log.w("RadioPlayerService", "ACTION_PLAY_STATION: url was blank")
                }
            }
            else -> {
                Log.d("RadioPlayerService", "onStartCommand: unknown action")
            }
        }
        return START_NOT_STICKY
    }

    private fun playUrl(url: String) {
        try {
            lastStatus = "preparing"
            Log.d("RadioPlayerService", "playUrl: preparing $url")
            currentUrl = url

            // Reset previous playback state to avoid conflicts
            try { player.stop() } catch (_: Throwable) {}
            try { player.clearMediaItems() } catch (_: Throwable) {}

            // Ensure volume is at a reasonable level
            try { player.volume = 1.0f } catch (_: Throwable) {}

            val mediaItem = MediaItem.fromUri(Uri.parse(url))
            player.setMediaItem(mediaItem)
            player.prepare()
            player.playWhenReady = true
            player.play()
            Log.d("RadioPlayerService", "playUrl: requested play for $url")
        } catch (e: Exception) {
            Log.e("RadioPlayerService", "Failed to play url=$url: ${e.message}", e)
            lastStatus = "error: ${e.message}"
            // On primary failure try Android MediaPlayer fallback
            startAndroidMediaPlayer(url)
        }
    }

    // Start android.media.MediaPlayer as a fallback for streams ExoPlayer can't handle
    private fun startAndroidMediaPlayer(url: String) {
        try {
            stopAndroidMediaPlayer()
            lastStatus = "androidplayer_preparing"
            androidPlayer = MediaPlayer().apply {
                setDataSource(url)
                setOnPreparedListener { mp ->
                    try { mp.start() } catch (_: Throwable) {}
                    lastStatus = "androidplayer_playing"
                    Log.d("RadioPlayerService", "Android MediaPlayer started for $url")
                }
                setOnErrorListener { mp, what, extra ->
                    Log.e("RadioPlayerService", "Android MediaPlayer error what=$what extra=$extra")
                    lastStatus = "androidplayer_error:$what"
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e("RadioPlayerService", "Failed android MediaPlayer fallback: ${e.message}", e)
            lastStatus = "androidplayer_error:${e.message}"
        }
    }

    private fun stopAndroidMediaPlayer() {
        try {
            androidPlayer?.let { mp ->
                try { mp.stop() } catch (_: Throwable) {}
                try { mp.release() } catch (_: Throwable) {}
            }
        } catch (_: Throwable) {}
        androidPlayer = null
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

        val playIntent = Intent(this, RadioPlayerService::class.java).apply { action = ACTION_PLAY }
        val playPi = PendingIntent.getService(this, 1, playIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val pauseIntent = Intent(this, RadioPlayerService::class.java).apply { action = ACTION_PAUSE }
        val pausePi = PendingIntent.getService(this, 2, pauseIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val stopIntent = Intent(this, RadioPlayerService::class.java).apply { action = ACTION_STOP }
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
        try { stopAndroidMediaPlayer() } catch (_: Throwable) {}
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

        // Expose a volatile status field so UI can read quick debug status
        @JvmStatic
        @Volatile
        var lastStatus: String = "idle"
    }
}

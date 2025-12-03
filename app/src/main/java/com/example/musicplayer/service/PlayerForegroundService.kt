package com.example.musicplayer.service

import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.media.session.MediaButtonReceiver
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import android.graphics.Bitmap
import com.example.musicplayer.Util
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri

class PlayerForegroundService : Service() {
    private val TAG = "PlayerFgService"
    private var mediaSession: MediaSessionCompat? = null
    private val binder = Binder()

    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var pollJob: Job? = null
    private var currentArtwork: Bitmap? = null
    // track which playlist index is currently loaded/prepared in the mediaPlayer
    private var currentPreparedIndex: Int = -1
    // track last time we updated the notification (ms) to avoid excessive updates
    private var lastNotificationUpdateTime: Long = 0L
    // track which playlist index and path is currently loaded/prepared in the mediaPlayer
    private var currentPreparedPath: String? = null
    // whether we've called startForeground already (to satisfy startForegroundService requirement)
    private var isForegroundStarted: Boolean = false

    override fun onCreate() {
        super.onCreate()
        // Ensure notification channel exists and promote to foreground immediately with a minimal notification.
        try {
            PlayerNotificationManager.ensureChannel(this)
            if (!isForegroundStarted) {
                val minimal = NotificationCompat.Builder(this, "music_player_channel")
                    .setSmallIcon(com.example.musicplayer.R.drawable.ic_music_note)
                    .setContentTitle("Music Player")
                    .setContentText("")
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setOngoing(true)
                    .build()
                startForeground(PlayerNotificationManager.NOTIFICATION_ID, minimal)
                isForegroundStarted = true
                Log.d(TAG, "onCreate: started immediate minimal foreground notification")
            }
        } catch (e: Exception) {
            Log.w(TAG, "onCreate: failed to start immediate foreground notification: ${e.message}")
        }
        mediaSession = MediaSessionCompat(this, "PlayerService").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    // start playback
                    this@PlayerForegroundService.playInternal()
                }

                override fun onPause() {
                    this@PlayerForegroundService.pauseInternal()
                }

                override fun onSkipToNext() {
                    this@PlayerForegroundService.nextInternal()
                }

                override fun onSkipToPrevious() {
                    this@PlayerForegroundService.prevInternal()
                }

                override fun onSeekTo(pos: Long) {
                    this@PlayerForegroundService.seekInternal(pos)
                }
            })
        }
        mediaSession?.isActive = true

        // initialize player
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA).build()
            )
            setOnCompletionListener {
                // when track completes, move to next
                nextInternal()
            }
            setOnPreparedListener { mp ->
                try {
                    val d = try { mp.duration.toLong() } catch (_: Throwable) { 0L }
                    PlayerRepository.setDurationMs(d)
                    mp.start()
                    PlayerRepository.setIsPlaying(true)
                    updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                    startForegroundNotification()
                } catch (_: Throwable) { }
            }
        }

        // Ensure we enter foreground promptly to satisfy startForegroundService timing.
        // Use a lightweight notification here; it will be updated later when playback starts.
        try {
            if (!isForegroundStarted) {
                Log.d(TAG, "onCreate: starting lightweight foreground notification")
                startForegroundNotification()
                isForegroundStarted = true
            }
        } catch (e: Exception) {
            Log.w(TAG, "onCreate: startForegroundNotification failed: ${e.message}")
        }

        // start position polling
        startPolling()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // As an additional safeguard, if for any reason we didn't call startForeground in onCreate,
        // ensure we promote to foreground immediately here (this must be fast).
        if (!isForegroundStarted) {
            try {
                PlayerNotificationManager.ensureChannel(this)
                val minimal = NotificationCompat.Builder(this, "music_player_channel")
                    .setSmallIcon(com.example.musicplayer.R.drawable.ic_music_note)
                    .setContentTitle("Music Player")
                    .setContentText("")
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setOngoing(true)
                    .build()
                startForeground(PlayerNotificationManager.NOTIFICATION_ID, minimal)
                isForegroundStarted = true
                Log.d(TAG, "onStartCommand: started minimal foreground notification (safeguard)")
            } catch (e: Exception) {
                Log.w(TAG, "onStartCommand: failed to start minimal foreground notification: ${e.message}")
                // Fallback: attempt a minimal notification using the channel-aware constructor (works on all API levels in this project)
                try {
                    val minimal = NotificationCompat.Builder(this, "music_player_channel")
                        .setSmallIcon(com.example.musicplayer.R.drawable.ic_music_note)
                        .setContentTitle("Music Player")
                        .setContentText("")
                        .setPriority(NotificationCompat.PRIORITY_LOW)
                        .setOngoing(true)
                        .build()
                    startForeground(PlayerNotificationManager.NOTIFICATION_ID, minimal)
                    isForegroundStarted = true
                    Log.d(TAG, "onStartCommand: started minimal foreground notification via fallback (channel-aware)")
                } catch (_: Throwable) { }
            }
        }
        val i = intent ?: return START_STICKY
        val action = i.action
        Log.d(TAG, "onStartCommand action=$action | currentIndex=${PlayerRepository.currentIndex.value} preparedIndex=$currentPreparedIndex isPlaying=${PlayerRepository.isPlaying.value}")

        // If this service was started with startForegroundService(), we must call startForeground() quickly.
        // Call a lightweight notification immediately on play/prepare/update so the system won't kill the service.
        try {
            if (!isForegroundStarted && (action == PlayerActions.ACTION_PLAY || action == PlayerActions.ACTION_PREPARE || action == PlayerActions.ACTION_UPDATE)) {
                Log.d(TAG, "Starting foreground with a lightweight notification to satisfy startForegroundService timing")
                try {
                    startForegroundNotification()
                    isForegroundStarted = true
                } catch (e: Exception) {
                    Log.w(TAG, "startForegroundNotification immediate failed: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error while ensuring foreground start: ${e.message}")
        }

        when (action) {
            PlayerActions.ACTION_PLAY -> playInternal()
            PlayerActions.ACTION_PAUSE -> {
                val suppress = i.getBooleanExtra(PlayerActions.EXTRA_SUPPRESS_NOTIFICATION, false)
                pauseInternal(suppress)
            }
            PlayerActions.ACTION_NEXT -> nextInternal()
            PlayerActions.ACTION_PREV -> prevInternal()
            PlayerActions.ACTION_STOP -> stopSelf()
            PlayerActions.ACTION_SEEK -> {
                val pos = i.getLongExtra(PlayerActions.EXTRA_SEEK_POSITION, 0L)
                seekInternal(pos)
            }
            PlayerActions.ACTION_PREPARE -> {
                // prepare current item without auto-starting
                prepareCurrent(startPlaying = false)
            }
            PlayerActions.ACTION_UPDATE -> {
                val title = i.getStringExtra(PlayerActions.EXTRA_SONG_TITLE) ?: ""
                val artist = i.getStringExtra(PlayerActions.EXTRA_SONG_ARTIST) ?: ""
                updateMetadata(title, artist)
                val isPlaying = i.getBooleanExtra(PlayerActions.EXTRA_IS_PLAYING, false)
                updatePlaybackState(if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED)
            }
            PlayerActions.ACTION_SEEK_FORWARD -> {
                // advance by SEEK_STEP_MS
                val cur = PlayerRepository.positionMs.value
                val dur = PlayerRepository.durationMs.value
                val target = (cur + PlayerActions.SEEK_STEP_MS).coerceAtMost(if (dur > 0L) dur else Long.MAX_VALUE)
                seekInternal(target)
            }
            PlayerActions.ACTION_SEEK_BACK -> {
                val cur = PlayerRepository.positionMs.value
                val target = (cur - PlayerActions.SEEK_STEP_MS).coerceAtLeast(0L)
                seekInternal(target)
            }
            Intent.ACTION_MEDIA_BUTTON -> MediaButtonReceiver.handleIntent(mediaSession, i)
            else -> {
                // unhandled action; ignore
            }
        }

        return START_STICKY
    }

    private fun prepareCurrent(startPlaying: Boolean = true) {
        val songs = PlayerRepository.playlist.value
        if (songs.isEmpty()) return
        val idx = PlayerRepository.currentIndex.value.coerceIn(0, songs.size - 1)
        val song = songs.getOrNull(idx) ?: return
        try {
            Log.d(TAG, "prepareCurrent requested idx=$idx path=${song.path}")
            mediaPlayer?.reset()
            // Use the correct setDataSource overload depending on path format.
            try {
                val p = song.path
                if (p.startsWith("content://") || p.startsWith("file://") || p.startsWith("http://") || p.startsWith("https://")) {
                    // content URIs and http(s) - use context/Uri overload
                    mediaPlayer?.setDataSource(this@PlayerForegroundService, p.toUri())
                } else {
                    // plain file path
                    mediaPlayer?.setDataSource(p)
                }
            } catch (e: Exception) {
                Log.w(TAG, "setDataSource failed for path=${song.path}, error=${e.message}")
                // fallback: try setDataSource with raw path
                try {
                    mediaPlayer?.setDataSource(song.path)
                } catch (ex: Exception) {
                    Log.w(TAG, "fallback setDataSource also failed: ${ex.message}")
                    throw e
                }
            }
            Log.d(TAG, "setDataSource succeeded for idx=$idx")
            // update metadata immediately
            updateMetadata(song.title, song.artist)
            // fetch album art asynchronously so we don't block service startup/main thread
            try {
                val artPath = song.path
                // Launch IO work to extract embedded artwork (may use MediaMetadataRetriever)
                scope.launch(Dispatchers.IO) {
                    try {
                        val art = try {
                            Util.getThumbnail(this@PlayerForegroundService, artPath.toUri())
                        } catch (_: Throwable) { null }
                        if (art != null) {
                            currentArtwork = art
                            // update the ongoing notification with artwork on the main thread
                            launch(Dispatchers.Main) { try { updateNotificationFromSession() } catch (_: Throwable) {} }
                        }
                    } catch (_: Throwable) { /* ignore */ }
                }
            } catch (_: Throwable) { currentArtwork = null }
            // mark prepared index/path so playInternal can decide whether reload is needed
            currentPreparedIndex = idx
            currentPreparedPath = song.path
            // prepare async and start when prepared if requested
            mediaPlayer?.prepareAsync()
            if (!startPlaying) {
                // onPrepared will not auto-start in this branch; we can pause after prepared
                // but simpler: we'll let onPrepared start, then pause if !startPlaying
                // So schedule a short job to pause once prepared if necessary
                scope.launch {
                    // wait until prepared (duration > 0) or timeout
                    var waited = 0
                    while (PlayerRepository.durationMs.value == 0L && waited < 5000) {
                        delay(100)
                        waited += 100
                    }
                    if (!startPlaying) {
                        try { mediaPlayer?.pause() } catch (_: Throwable) {}
                    }
                }
            }
            PlayerRepository.setCurrentIndex(idx)
            Log.d(TAG, "prepareCurrent scheduled prepareAsync for idx=$idx (startPlaying=$startPlaying)")
        } catch (_: Throwable) {
            Log.w(TAG, "prepareCurrent failed for idx=$idx")
        }
    }

    private fun playInternal() {
        try {
            val desiredIdx = PlayerRepository.currentIndex.value
            val desiredPath = PlayerRepository.playlist.value.getOrNull(desiredIdx)?.path
            Log.d(TAG, "playInternal called desiredIdx=$desiredIdx desiredPath=$desiredPath currentPreparedIndex=$currentPreparedIndex currentPreparedPath=$currentPreparedPath mediaPlaying=${mediaPlayer?.isPlaying}")
            if (mediaPlayer?.isPlaying == true) {
                // if already playing the requested path, nothing to do
                if (currentPreparedPath != null && currentPreparedPath == desiredPath) {
                    Log.d(TAG, "playInternal early-return: already playing desiredPath=$desiredPath")
                    return
                }
                // otherwise we need to load the new index even while playing
                Log.d(TAG, "playInternal: preparedPath ($currentPreparedPath) != desiredPath ($desiredPath), reloading")
            }
            // if not prepared or different index, prepareCurrent will load the desired track
            if (PlayerRepository.durationMs.value <= 0L || currentPreparedIndex != desiredIdx) {
                prepareCurrent(startPlaying = true)
            } else {
                mediaPlayer?.start()
                PlayerRepository.setIsPlaying(true)
                updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                startForegroundNotification()
                Log.d(TAG, "playInternal: started playback idx=$desiredIdx")
            }
        } catch (_: Throwable) {
            Log.w(TAG, "playInternal error")
        }
    }

    private fun pauseInternal(suppressNotification: Boolean = false) {
        try {
            mediaPlayer?.pause()
            PlayerRepository.setIsPlaying(false)
            updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
            if (suppressNotification) {
                // user dismissed notification; cancel it and do not re-post
                PlayerNotificationManager.cancel(this)
                // stopForeground but keep service running (so playback remains paused and UI can resume)
                @Suppress("DEPRECATION")
                try { stopForeground(STOP_FOREGROUND_DETACH) } catch (_: Throwable) {}
            } else {
                updateNotificationFromSession()
            }
        } catch (_: Throwable) {
            Log.w(TAG, "pauseInternal error")
        }
    }

    private fun nextInternal() {
        val songs = PlayerRepository.playlist.value
        if (songs.isEmpty()) return
        val nextIdx = PlayerRepository.nextIndex()
        PlayerRepository.setCurrentIndex(nextIdx)
        prepareCurrent(startPlaying = true)
    }

    private fun prevInternal() {
        val songs = PlayerRepository.playlist.value
        if (songs.isEmpty()) return
        val prevIdx = PlayerRepository.prevIndex()
        PlayerRepository.setCurrentIndex(prevIdx)
        prepareCurrent(startPlaying = true)
    }

    private fun seekInternal(position: Long) {
        try {
            mediaPlayer?.seekTo(position.toInt())
            PlayerRepository.setPositionMs(position)
        } catch (_: Throwable) { }
    }

    private fun updatePlaybackState(state: Int, position: Long = PlayerRepository.positionMs.value) {
        val playbackState = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_SEEK_TO
            )
            .setState(state, position, if (state == PlaybackStateCompat.STATE_PLAYING) 1f else 0f)
            .build()
        mediaSession?.setPlaybackState(playbackState)
        updateNotificationFromSession()
    }

    private fun updateMetadata(title: String, artist: String) {
        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
            .build()
        mediaSession?.setMetadata(metadata)
        updateNotificationFromSession()
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = scope.launch {
            while (isActive) {
                try {
                    val pos = mediaPlayer?.currentPosition?.toLong() ?: 0L
                    val dur = mediaPlayer?.duration?.toLong() ?: PlayerRepository.durationMs.value
                    PlayerRepository.setPositionMs(pos)
                    if (dur > 0) PlayerRepository.setDurationMs(dur)
                    // Throttle notification updates to ~500ms to keep progress timely without spamming
                    val now = System.currentTimeMillis()
                    if (now - lastNotificationUpdateTime >= 500L) {
                        try { updateNotificationFromSession() } catch (_: Throwable) {}
                        lastNotificationUpdateTime = now
                    }
                } catch (_: Throwable) {}
                delay(200)
            }
        }
    }

    private fun startForegroundNotification() {
        val title = mediaSession?.controller?.metadata?.getString(MediaMetadataCompat.METADATA_KEY_TITLE) ?: "Playing"
        val artist = mediaSession?.controller?.metadata?.getString(MediaMetadataCompat.METADATA_KEY_ARTIST) ?: ""
        val isPlaying = mediaSession?.controller?.playbackState?.state == PlaybackStateCompat.STATE_PLAYING
        val token = mediaSession?.sessionToken
        val prog = PlayerRepository.positionMs.value
        val max = PlayerRepository.durationMs.value
        val notification = PlayerNotificationManager.buildNotification(
            this,
            title,
            artist,
            isPlaying,
            currentArtwork,
            true,
            true,
            token,
            progressMs = prog,
            durationMs = max
        )
        startForeground(PlayerNotificationManager.NOTIFICATION_ID, notification)
    }

    private fun updateNotificationFromSession() {
        val title = mediaSession?.controller?.metadata?.getString(MediaMetadataCompat.METADATA_KEY_TITLE) ?: ""
        val artist = mediaSession?.controller?.metadata?.getString(MediaMetadataCompat.METADATA_KEY_ARTIST) ?: ""
        val isPlaying = mediaSession?.controller?.playbackState?.state == PlaybackStateCompat.STATE_PLAYING
        val token = mediaSession?.sessionToken
        val prog = PlayerRepository.positionMs.value
        val max = PlayerRepository.durationMs.value
        val notification = PlayerNotificationManager.buildNotification(
            this,
            title,
            artist,
            isPlaying,
            currentArtwork,
            true,
            true,
            token,
            progressMs = prog,
            durationMs = max
        )
        PlayerNotificationManager.postNotification(this, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        pollJob?.cancel()
        try { mediaPlayer?.release() } catch (_: Throwable) {}
        try { currentArtwork?.recycle() } catch (_: Throwable) {}
        mediaSession?.release()
        PlayerNotificationManager.cancel(this)
    }

    // When the app task is removed from Recents, stop playback and the service to honor the user's intent
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        try {
            Log.d(TAG, "onTaskRemoved: app task removed, stopping service and playback")
            // stop foreground and remove notification
            try { stopForeground(STOP_FOREGROUND_REMOVE) } catch (_: Throwable) {}
            // ensure media resources are released and service stopped
            try { mediaPlayer?.stop() } catch (_: Throwable) {}
            try { mediaPlayer?.release() } catch (_: Throwable) {}
            PlayerRepository.setIsPlaying(false)
            stopSelf()
        } catch (_: Throwable) {
            // best-effort cleanup
        }
    }
}

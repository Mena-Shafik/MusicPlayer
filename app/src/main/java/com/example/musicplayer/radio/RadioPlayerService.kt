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
import com.example.musicplayer.Util
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import com.example.musicplayer.model.RadioStation

@UnstableApi
class RadioPlayerService : Service() {

    private lateinit var player: ExoPlayer
    private var currentTitle: String? = null
    private var currentUrl: String? = null
    private var androidPlayer: MediaPlayer? = null
    private var stationList: List<RadioStation>? = null
    private var currentIndex: Int = -1
    // Feature flag: disable ICY metadata polling while it's not working on device.
    // Set to `true` to re-enable polling later.
    private val enableIcyMetadataPolling = false

    // Coroutine scope for metadata polling
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var metadataPollingJob: Job? = null

    // New: latest parsed metadata (artist - title or raw stream title)
    companion object {
        private const val CHANNEL_ID = "radio_playback_channel"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY = "com.example.musicplayer.action.PLAY"
        const val ACTION_PAUSE = "com.example.musicplayer.action.PAUSE"
        const val ACTION_STOP = "com.example.musicplayer.action.STOP"
        const val ACTION_PLAY_STATION = "com.example.musicplayer.action.PLAY_STATION"
        const val ACTION_PREV_STATION = "com.example.musicplayer.action.PREV_STATION"
        const val ACTION_NEXT_STATION = "com.example.musicplayer.action.NEXT_STATION"
        const val EXTRA_STATION_URL = "extra_station_url"
        const val EXTRA_STATION_TITLE = "extra_station_title"
        const val EXTRA_STATION_LIST = "extra_station_list"
        const val EXTRA_STATION_INDEX = "extra_station_index"
        const val EXTRA_STATION_FAVICON = "extra_station_favicon"
        const val EXTRA_STATION_TAGS = "extra_station_tags"

        // Expose a volatile status field so UI can read quick debug status
        @JvmStatic
        @Volatile
        var lastStatus: String = "idle"
            set(value) {
                // Log every assignment so we can trace unexpected short values like "t"
                try {
                    Log.d("RadioPlayerService", "lastStatus set -> '${value}' (len=${value?.length ?: 0})")
                    if (value.length <= 1) {
                        // Log a stacktrace to help find the origin of tiny/truncated assignments
                        val trace = Exception("Short lastStatus assignment").stackTraceToString()
                        Log.w("RadioPlayerService", "Short lastStatus assigned: '${value}'. Stack:\n$trace")
                    }
                } catch (_: Throwable) {}
                field = value
            }

        // Expose latest metadata string (Artist - Title) parsed from the stream; updated by service
        @JvmStatic
        @Volatile
        var lastMetadata: String? = null
        @JvmStatic @Volatile var lastStationName: String? = null
        @JvmStatic @Volatile var lastStationFavicon: String? = null
        @JvmStatic @Volatile var lastStationTags: String? = null
    }

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
                lastStatus = if (isPlaying) "PLAYING" else "PAUSED"
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
                // no automatic retry on ENDED — let the player reach ENDED and surface the state
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
                try {
                    if (androidPlayer != null) {
                        // If Android fallback is active, start it on the main thread
                        runOnMain {
                            try { androidPlayer?.start() } catch (_: Throwable) {}
                            lastStatus = "PLAYING"
                            updateNotification(true)
                        }
                    } else {
                        runOnMain {
                            try { stopAndroidMediaPlayer() } catch (_: Throwable) {}
                            try { player.playWhenReady = true } catch (_: Throwable) {}
                            try { player.play() } catch (_: Throwable) {}
                            lastStatus = "PLAYING"
                            updateNotification(true)
                        }
                    }
                } catch (e: Throwable) {
                    Log.w("RadioPlayerService", "Play action failed: ${e.message}")
                }
            }
            ACTION_PAUSE -> {
                Log.d("RadioPlayerService", "ACTION_PAUSE: pause() called")
                lastStatus = "pause_request"
                try {
                    if (androidPlayer != null) {
                        // Pause Android fallback on main thread
                        runOnMain {
                            try { androidPlayer?.pause() } catch (_: Throwable) {}
                            lastStatus = "PAUSED"
                            updateNotification(false)
                        }
                    } else {
                        runOnMain {
                            try { player.playWhenReady = false } catch (_: Throwable) {}
                            try { player.pause() } catch (_: Throwable) {}
                            lastStatus = "PAUSED"
                            updateNotification(false)
                        }
                    }
                } catch (e: Throwable) {
                    Log.w("RadioPlayerService", "Pause action failed: ${e.message}")
                }
            }
            ACTION_STOP -> {
                Log.d("RadioPlayerService", "ACTION_STOP: stopping service")
                lastStatus = "stopped"
                try { stopForeground(true) } catch (_: Throwable) {}
                // Ensure both players are stopped on the main thread to avoid native errors
                runOnMain {
                    try { player.stop() } catch (e: Throwable) { Log.w("RadioPlayerService", "ExoPlayer stop failed: ${e.message}") }
                    try { stopAndroidMediaPlayer() } catch (e: Throwable) { Log.w("RadioPlayerService", "AndroidPlayer stop failed: ${e.message}") }
                    updateNotification(false)
                }
                stopSelf()
            }
            ACTION_PLAY_STATION -> {
                val url = intent.getStringExtra(EXTRA_STATION_URL)
                val title = intent.getStringExtra(EXTRA_STATION_TITLE)
                val fav = intent.getStringExtra(EXTRA_STATION_FAVICON)
                val tags = intent.getStringExtra(EXTRA_STATION_TAGS)
                val list = intent.getParcelableArrayListExtra<RadioStation>(EXTRA_STATION_LIST)
                val idx = intent.getIntExtra(EXTRA_STATION_INDEX, -1)
                if (!list.isNullOrEmpty()) {
                    stationList = list
                    currentIndex = idx.coerceIn(list.indices)
                }
                Log.d("RadioPlayerService", "ACTION_PLAY_STATION url=$url title=$title idx=$idx listSize=${stationList?.size}")
                if (!url.isNullOrBlank()) {
                    currentTitle = title
                    lastStationName = title ?: lastStationName
                    lastStationFavicon = fav ?: lastStationFavicon
                    lastStationTags = tags ?: lastStationTags
                    playUrl(url)
                    startMetadataPolling(url)
                } else if (!stationList.isNullOrEmpty()) {
                    // fallback to current index from list
                    playCurrentFromList(startPlaying = true)
                } else {
                    Log.w("RadioPlayerService", "ACTION_PLAY_STATION: url was blank")
                }
            }
            ACTION_PREV_STATION -> {
                if (!stationList.isNullOrEmpty()) {
                    stepStation(-1)
                } else {
                    Log.d("RadioPlayerService", "No station list for prev")
                }
            }
            ACTION_NEXT_STATION -> {
                if (!stationList.isNullOrEmpty()) {
                    stepStation(1)
                } else {
                    Log.d("RadioPlayerService", "No station list for next")
                }
            }
            else -> {
                Log.d("RadioPlayerService", "onStartCommand: unknown action")
            }
        }
        return START_NOT_STICKY
    }

    private fun playUrl(url: String) {
        // ExoPlayer must be accessed on the main thread. If called from a background
        // dispatcher, forward the work to the main dispatcher.
        if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
            serviceScope.launch(Dispatchers.Main) {
                playUrlInternal(url)
            }
        } else {
            playUrlInternal(url)
        }
    }

    // This function contains the actual ExoPlayer/MediaPlayer interactions and must
    // always be executed on the main thread.
    private fun playUrlInternal(url: String) {
        try {
            lastStatus = "preparing"
            Log.d("RadioPlayerService", "playUrl: preparing $url")
            currentUrl = url
            // Stop Android fallback if active so we don't have two audio pipelines
            try { stopAndroidMediaPlayer() } catch (_: Throwable) {}

            // Reset previous ExoPlayer playback state to avoid conflicts
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

    // Start a background coroutine that polls ICY metadata from the stream URL periodically.
    private fun startMetadataPolling(url: String) {
        // ICY metadata polling is disabled by default because it caused failures on some devices.
        // Keep the function in place so UI can continue to read `lastMetadata` if it's set from elsewhere.
        if (!enableIcyMetadataPolling) {
            Log.d("RadioPlayerService", "startMetadataPolling: ICY polling disabled by feature flag")
            metadataPollingJob?.cancel()
            metadataPollingJob = null
            return
        }

        // cancel previous job if any
        metadataPollingJob?.cancel()
        metadataPollingJob = serviceScope.launch {
            while (isActive) {
                try {
                    val meta = fetchIcyMetadata(url)
                    if (!meta.isNullOrBlank()) {
                        val clean = meta.trim().replace(Regex("[\n\r\t]+"), " ")
                        // ignore obviously-bogus very short metadata (single letters)
                        if (clean.length > 1 && clean != lastMetadata) {
                            lastMetadata = clean
                            currentTitle = clean
                            Log.d("RadioPlayerService", "ICY metadata updated: $clean")
                            updateNotification(player.isPlaying)
                        }
                    }
                } catch (e: Throwable) {
                    Log.w("RadioPlayerService", "Metadata poll failed: ${e.message}")
                }
                // Poll interval — reduced frequency to 30s to be kinder to servers and battery
                delay(30_000L)
            }
        }
    }

    private fun stopMetadataPolling() {
        metadataPollingJob?.cancel()
        metadataPollingJob = null
    }

    // Fetch ICY metadata from the provided stream URL. This makes a short HTTP request
    // that asks for ICY metadata and reads the first metadata block.
    private fun fetchIcyMetadata(streamUrl: String): String? {
        var conn: HttpURLConnection? = null
        var input: InputStream? = null
        try {
            val url = URL(streamUrl)
            conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 6000
                instanceFollowRedirects = true
                // Request ICY metadata
                setRequestProperty("Icy-MetaData", "1")
                setRequestProperty("User-Agent", "MusicPlayer/1.0")
                connect()
            }

            // Some servers expose the meta interval in the header 'icy-metaint'
            val metaIntHeader = conn.getHeaderField("icy-metaint") ?: conn.getHeaderField("Ice-Metaint")
            val metaInt = metaIntHeader?.toIntOrNull() ?: -1

            input = conn.inputStream
            if (metaInt > 0) {
                // Skip `metaInt` bytes of audio data
                var toSkip = metaInt
                val buffer = ByteArray(8192)
                while (toSkip > 0) {
                    val read = input.read(buffer, 0, minOf(buffer.size, toSkip))
                    if (read <= 0) break
                    toSkip -= read
                }

                // Read metadata length byte
                val lenByte = input.read()
                if (lenByte > 0) {
                    val metaLen = lenByte * 16
                    val metaBuf = ByteArray(metaLen)
                    var offset = 0
                    while (offset < metaLen) {
                        val r = input.read(metaBuf, offset, metaLen - offset)
                        if (r <= 0) break
                        offset += r
                    }
                    val meta = String(metaBuf, 0, offset, charset("UTF-8"))
                    // Parse StreamTitle='Artist - Title';
                    val title = parseIcyStreamTitle(meta)
                    if (!title.isNullOrBlank()) return title.trim()
                }
            } else {
                // No meta interval header — attempt to read a small chunk and search for StreamTitle
                val sample = ByteArray(4096)
                val read = input.read(sample)
                if (read > 0) {
                    val txt = String(sample, 0, read, charset("ISO-8859-1"))
                    val title = parseIcyStreamTitle(txt)
                    if (!title.isNullOrBlank()) return title.trim()
                }
            }
        } catch (e: Throwable) {
            Log.w("RadioPlayerService", "fetchIcyMetadata error: ${e.message}")
        } finally {
            try { input?.close() } catch (_: Throwable) {}
            try { conn?.disconnect() } catch (_: Throwable) {}
        }
        return null
    }

    private fun parseIcyStreamTitle(meta: String): String? {
        // Examples: "StreamTitle='Artist - Title';" or StreamTitle="Artist - Title";
        val regex = Regex("StreamTitle=('|\")(?<t>[^'\"]+?)('\"|)\\;?")
        val m = regex.find(meta)
        if (m != null) return m.groups["t"]?.value

        // Fallback: look for StreamTitle=...;
        val idx = meta.indexOf("StreamTitle=", ignoreCase = true)
        if (idx >= 0) {
            val sub = meta.substring(idx + "StreamTitle=".length)
            val end = sub.indexOf(';')
            val raw = if (end >= 0) sub.substring(0, end) else sub
            return raw.trim('"', '\'', ' ', ';')
        }
        return null
    }

    // Start android.media.MediaPlayer as a fallback for streams ExoPlayer can't handle
    private fun startAndroidMediaPlayer(url: String) {
        // Ensure ExoPlayer is stopped on the main thread before starting the Android fallback
        if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
            serviceScope.launch(Dispatchers.Main) { startAndroidMediaPlayer(url) }
            return
        }

        try {
            // Stop ExoPlayer to avoid both players playing simultaneously
            try { player.stop() } catch (_: Throwable) {}
            try { player.clearMediaItems() } catch (_: Throwable) {}
            stopAndroidMediaPlayer()
            lastStatus = "PREPARING"
            androidPlayer = MediaPlayer().apply {
                setDataSource(url)
                setOnPreparedListener { mp ->
                    try { mp.start() } catch (_: Throwable) {}
                    lastStatus = "PLAYING"
                    Log.d("RadioPlayerService", "Android MediaPlayer started for $url")
                    updateNotification(true)
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("RadioPlayerService", "Android MediaPlayer error what=$what extra=$extra")
                    lastStatus = "ERROR:$what"
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e("RadioPlayerService", "Failed android MediaPlayer fallback: ${e.message}", e)
            lastStatus = "ERROR:${e.message}"
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
        val prevIntent = Intent(this, RadioPlayerService::class.java).apply { action = ACTION_PREV_STATION }
        val prevPi = PendingIntent.getService(this, 4, prevIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val nextIntent = Intent(this, RadioPlayerService::class.java).apply { action = ACTION_NEXT_STATION }
        val nextPi = PendingIntent.getService(this, 5, nextIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

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
        if ((stationList?.size ?: 0) > 1) {
            builder.addAction(NotificationCompat.Action(0, "Prev", prevPi))
            builder.addAction(NotificationCompat.Action(0, "Next", nextPi))
        }

        val style = MediaAppNotificationCompat.MediaStyle()
            .setShowActionsInCompactView(0)

        builder.setStyle(style)

        return builder.build()
    }

    override fun onDestroy() {
        try { player.release() } catch (_: Throwable) {}
        try { stopAndroidMediaPlayer() } catch (_: Throwable) {}
        try { stopMetadataPolling() } catch (_: Throwable) {}
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

    // Helper function to ensure code runs on the main thread
    private fun runOnMain(block: () -> Unit) {
        if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
            serviceScope.launch(Dispatchers.Main) {
                block()
            }
        } else {
            block()
        }
    }

    private fun stepStation(delta: Int) {
        val list = stationList
        if (list.isNullOrEmpty()) return
        if (currentIndex !in list.indices) currentIndex = 0
        currentIndex = (currentIndex + delta).floorMod(list.size)
        playCurrentFromList(startPlaying = true)
    }

    private fun playCurrentFromList(startPlaying: Boolean) {
        val list = stationList
        if (list.isNullOrEmpty()) return
        if (currentIndex !in list.indices) currentIndex = 0
        val station = list[currentIndex]
        val url = station.url
        if (url.isNullOrBlank()) {
            Log.w("RadioPlayerService", "playCurrentFromList: blank url for idx=$currentIndex")
            return
        }
        currentTitle = Util.extractQuotedOrOriginal(station.name).ifBlank { station.name }
        lastStationName = currentTitle
        lastStationFavicon = station.favicon
        lastStationTags = station.tags
        stopMetadataPolling()
        startMetadataPolling(url)
        playUrl(url)
    }
}

private fun Int.floorMod(mod: Int): Int {
    if (mod <= 0) return this
    val r = this % mod
    return if (r >= 0) r else r + mod
}

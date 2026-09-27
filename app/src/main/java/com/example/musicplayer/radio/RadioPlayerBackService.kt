package com.example.musicplayer.radio

import com.example.musicplayer.R
import android.os.Looper
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
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
import com.example.musicplayer.util.Util
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import com.example.musicplayer.model.RadioStation
import com.example.musicplayer.service.PlayerForegroundService
import com.example.musicplayer.service.PlayerIntentBuilder

@UnstableApi
class RadioPlayerService : Service() {

    private lateinit var player: ExoPlayer
    private var currentTitle: String? = null
    private var currentUrl: String? = null
    private var androidPlayer: MediaPlayer? = null
    private var stationList: List<RadioStation>? = null
    private var currentIndex: Int = -1
    // Feature flag: disable ICY metadata polling while it's not working on device; set true to re-enable.
    private val enableIcyMetadataPolling = false

    // Coroutine scope for metadata polling
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var metadataPollingJob: Job? = null

    // Lets the system show this as a media player (lock screen, quick settings) and routes its buttons back here.
    private lateinit var mediaSession: MediaSessionCompat
    private var notificationArtUrl: String? = null
    private var notificationArt: Bitmap? = null
    private var lastNotifiedPlaying = false

    // New: latest parsed metadata (artist - title or raw stream title)
    companion object {
        private const val CHANNEL_ID = "radio_playback_channel"
        // Must differ from PlayerNotificationManager.NOTIFICATION_ID (1001), or the two players' notifications replace each other.
        private const val NOTIFICATION_ID = 2001

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
        // Lets the local player pause us only when we're alive, instead of startService() spawning an empty instance.
        @JvmStatic @Volatile var isRunning: Boolean = false
    }

    @SuppressLint("RestrictedApi")
    override fun onCreate() {
        super.onCreate()
        isRunning = true

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
                val errorType = when (error.errorCode) {
                    PlaybackException.ERROR_CODE_IO_UNSPECIFIED -> "IO_UNSPECIFIED - Generic I/O error"
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> "IO_NETWORK_CONNECTION_FAILED - Network unreachable"
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> "IO_NETWORK_CONNECTION_TIMEOUT - Connection timeout"
                    PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE -> "IO_INVALID_HTTP_CONTENT_TYPE - Wrong content type"
                    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> "IO_BAD_HTTP_STATUS - HTTP error response"
                    PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> "IO_FILE_NOT_FOUND - File/stream not found"
                    PlaybackException.ERROR_CODE_IO_NO_PERMISSION -> "IO_NO_PERMISSION - No permission"
                    PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED -> "IO_CLEARTEXT_NOT_PERMITTED - HTTP not allowed"
                    PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE -> "IO_READ_POSITION_OUT_OF_RANGE - Invalid position"
                    PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED -> "PARSING_CONTAINER_MALFORMED - Invalid container format"
                    PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED -> "PARSING_MANIFEST_MALFORMED - Invalid manifest (M3U8/MPD)"
                    PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED -> "PARSING_CONTAINER_UNSUPPORTED - Unsupported format"
                    PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED -> "PARSING_MANIFEST_UNSUPPORTED - Unsupported manifest"
                    else -> "ERROR_CODE_${error.errorCode}"
                }

                Log.e("RadioPlayerService", "════════════════════════════════════════")
                Log.e("RadioPlayerService", "❌ EXOPLAYER ERROR")
                Log.e("RadioPlayerService", "URL: $currentUrl")
                Log.e("RadioPlayerService", "Error Code: ${error.errorCode}")
                Log.e("RadioPlayerService", "Error Type: $errorType")
                Log.e("RadioPlayerService", "Message: ${error.message}")
                Log.e("RadioPlayerService", "Cause: ${error.cause?.javaClass?.simpleName} - ${error.cause?.message}")
                Log.e("RadioPlayerService", "════════════════════════════════════════")

                lastStatus = "error: $errorType"

                // Try fallback to Android MediaPlayer for simple HTTP streams
                currentUrl?.let { url ->
                    Log.w("RadioPlayerService", "⚠ ExoPlayer failed, attempting Android MediaPlayer fallback")
                    startAndroidMediaPlayer(url)
                }
            }
        })

        mediaSession = MediaSessionCompat(this, "RadioPlayerService").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() = sendSelf(ACTION_PLAY)
                override fun onPause() = sendSelf(ACTION_PAUSE)
                override fun onStop() = sendSelf(ACTION_STOP)
                override fun onSkipToNext() = sendSelf(ACTION_NEXT_STATION)
                override fun onSkipToPrevious() = sendSelf(ACTION_PREV_STATION)
            })
            isActive = true
        }

        createNotificationChannel()

        // Start with a foreground notification so the service isn't killed immediately
        val initial = buildNotification(currentTitle ?: getString(R.string.app_name), false)
        startForeground(NOTIFICATION_ID, initial)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d("RadioPlayerService", "onStartCommand action=$action extras=${intent?.extras}")
        when (action) {
            ACTION_PLAY -> {
                Log.d("RadioPlayerService", "ACTION_PLAY: play() called")
                lastStatus = "play_request"
                ensureLocalMusicPaused()
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
                    // Re-selecting the already-playing station shouldn't reconnect the stream from scratch.
                    val alreadyPlayingThisStation = url == currentUrl &&
                        (try { player.isPlaying } catch (_: Throwable) { false } ||
                            try { androidPlayer?.isPlaying == true } catch (_: Throwable) { false })
                    if (alreadyPlayingThisStation) {
                        Log.d("RadioPlayerService", "ACTION_PLAY_STATION: already playing $url, ignoring restart")
                    } else {
                        playUrl(url)
                        startMetadataPolling(url)
                    }
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

    // Explicitly pauses local playback so song/radio never sound at once, regardless of audio-focus timing.
    private fun ensureLocalMusicPaused() {
        if (!PlayerForegroundService.isRunning) return
        try { PlayerIntentBuilder.startPause(this) } catch (_: Throwable) {}
    }

    private fun playUrl(url: String) {
        // ExoPlayer must be accessed on the main thread; forward to it if called from a background dispatcher.
        if (Looper.myLooper() != Looper.getMainLooper()) {
            serviceScope.launch(Dispatchers.Main) {
                playUrlInternal(url)
            }
        } else {
            playUrlInternal(url)
        }
    }

    // Contains the actual ExoPlayer/MediaPlayer interactions; must always run on the main thread.
    private fun playUrlInternal(url: String) {
        try {
            ensureLocalMusicPaused()
            lastStatus = "preparing"
            Log.d("RadioPlayerService", "════════════════════════════════════════")
            Log.d("RadioPlayerService", "playUrl: ATTEMPTING TO PLAY")
            Log.d("RadioPlayerService", "URL: $url")
            Log.d("RadioPlayerService", "URL Length: ${url.length}")
            Log.d("RadioPlayerService", "URL Protocol: ${url.substringBefore("://")}")
            Log.d("RadioPlayerService", "URL Host: ${try { URL(url).host } catch (e: Exception) { "INVALID_URL: ${e.message}" }}")
            Log.d("RadioPlayerService", "════════════════════════════════════════")

            currentUrl = url

            // Validate URL format before attempting playback
            if (url.isBlank()) {
                Log.e("RadioPlayerService", "❌ FAILED: URL is blank")
                lastStatus = "error: blank URL"
                return
            }

            // Check if URL is reachable (optional HEAD request)
            serviceScope.launch(Dispatchers.IO) {
                try {
                    val testUrl = URL(url)
                    val conn = testUrl.openConnection() as? HttpURLConnection
                    conn?.apply {
                        requestMethod = "HEAD"
                        connectTimeout = 5000
                        readTimeout = 5000
                        setRequestProperty("User-Agent", "MusicPlayer/1.0")

                        try {
                            connect()
                            val code = responseCode
                            val contentType = contentType
                            Log.d("RadioPlayerService", "✓ URL REACHABILITY CHECK:")
                            Log.d("RadioPlayerService", "  HTTP Status: $code")
                            Log.d("RadioPlayerService", "  Content-Type: $contentType")
                            Log.d("RadioPlayerService", "  Content-Length: ${contentLength}")

                            if (code !in 200..299) {
                                Log.w("RadioPlayerService", "⚠ URL returned non-2xx status: $code")
                            }
                        } catch (e: Exception) {
                            Log.w("RadioPlayerService", "⚠ URL reachability check failed: ${e.message}")
                        } finally {
                            disconnect()
                        }
                    }
                } catch (e: Exception) {
                    Log.w("RadioPlayerService", "⚠ Could not perform URL check: ${e.message}")
                }
            }

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
            Log.d("RadioPlayerService", "✓ ExoPlayer.play() called successfully")
        } catch (e: Exception) {
            Log.e("RadioPlayerService", "❌ FAILED to play url=$url", e)
            Log.e("RadioPlayerService", "Error Type: ${e.javaClass.simpleName}")
            Log.e("RadioPlayerService", "Error Message: ${e.message}")
            Log.e("RadioPlayerService", "Stack Trace: ${e.stackTraceToString().take(500)}")
            lastStatus = "error: ${e.message}"
            // On primary failure try Android MediaPlayer fallback
            startAndroidMediaPlayer(url)
        }
    }

    // Start a background coroutine that polls ICY metadata from the stream URL periodically.
    private fun startMetadataPolling(url: String) {
        // Disabled by default because it caused failures on some devices; kept in place so UI can still read `lastMetadata` if set elsewhere.
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

    // Makes a short HTTP request asking for ICY metadata and reads the first metadata block.
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
        if (Looper.myLooper() != Looper.getMainLooper()) {
            serviceScope.launch(Dispatchers.Main) { startAndroidMediaPlayer(url) }
            return
        }

        try {
            Log.d("RadioPlayerService", "════════════════════════════════════════")
            Log.d("RadioPlayerService", "FALLBACK: Trying Android MediaPlayer")
            Log.d("RadioPlayerService", "URL: $url")
            Log.d("RadioPlayerService", "════════════════════════════════════════")

            // Stop ExoPlayer to avoid both players playing simultaneously
            try { player.stop() } catch (_: Throwable) {}
            try { player.clearMediaItems() } catch (_: Throwable) {}
            stopAndroidMediaPlayer()
            lastStatus = "PREPARING"

            androidPlayer = MediaPlayer().apply {
                // Set data source with enhanced error handling
                try {
                    setDataSource(url)
                    Log.d("RadioPlayerService", "✓ MediaPlayer.setDataSource() succeeded")
                } catch (e: Exception) {
                    Log.e("RadioPlayerService", "❌ MediaPlayer.setDataSource() FAILED: ${e.message}", e)
                    throw e
                }

                setOnPreparedListener { mp ->
                    Log.d("RadioPlayerService", "✓ MediaPlayer PREPARED successfully")
                    try {
                        mp.start()
                        Log.d("RadioPlayerService", "✓ MediaPlayer STARTED successfully")
                    } catch (e: Throwable) {
                        Log.e("RadioPlayerService", "❌ MediaPlayer.start() FAILED: ${e.message}", e)
                    }
                    lastStatus = "PLAYING"
                    updateNotification(true)
                }

                setOnErrorListener { _, what, extra ->
                    // Decode error codes
                    val whatStr = when (what) {
                        MediaPlayer.MEDIA_ERROR_UNKNOWN -> "MEDIA_ERROR_UNKNOWN (1)"
                        MediaPlayer.MEDIA_ERROR_SERVER_DIED -> "MEDIA_ERROR_SERVER_DIED (100)"
                        else -> "UNKNOWN_ERROR_CODE ($what)"
                    }

                    val extraStr = when (extra) {
                        MediaPlayer.MEDIA_ERROR_IO -> "MEDIA_ERROR_IO (-1004) - I/O error"
                        MediaPlayer.MEDIA_ERROR_MALFORMED -> "MEDIA_ERROR_MALFORMED (-1007) - Bitstream not conforming to spec"
                        MediaPlayer.MEDIA_ERROR_UNSUPPORTED -> "MEDIA_ERROR_UNSUPPORTED (-1010) - Unsupported format"
                        MediaPlayer.MEDIA_ERROR_TIMED_OUT -> "MEDIA_ERROR_TIMED_OUT (-110) - Operation timed out"
                        -1007 -> "MEDIA_ERROR_MALFORMED (-1007) - The stream/file is malformed or invalid"
                        else -> "UNKNOWN_EXTRA_CODE ($extra)"
                    }

                    Log.e("RadioPlayerService", "════════════════════════════════════════")
                    Log.e("RadioPlayerService", "❌ ANDROID MEDIAPLAYER ERROR")
                    Log.e("RadioPlayerService", "URL: $url")
                    Log.e("RadioPlayerService", "WHAT: $whatStr")
                    Log.e("RadioPlayerService", "EXTRA: $extraStr")
                    Log.e("RadioPlayerService", "════════════════════════════════════════")
                    Log.e("RadioPlayerService", "DIAGNOSIS:")
                    Log.e("RadioPlayerService", "  -1007 = Stream format is not recognized or is corrupted")
                    Log.e("RadioPlayerService", "  Common causes:")
                    Log.e("RadioPlayerService", "    • URL requires authentication/cookies")
                    Log.e("RadioPlayerService", "    • URL has expired (session-based URLs)")
                    Log.e("RadioPlayerService", "    • Server expects specific headers")
                    Log.e("RadioPlayerService", "    • Stream format not supported by Android")
                    Log.e("RadioPlayerService", "    • URL returns HTML/error page instead of audio")
                    Log.e("RadioPlayerService", "════════════════════════════════════════")

                    lastStatus = "ERROR: $whatStr / $extraStr"
                    true
                }

                setOnInfoListener { _, what, extra ->
                    val infoStr = when (what) {
                        MediaPlayer.MEDIA_INFO_BUFFERING_START -> "BUFFERING_START"
                        MediaPlayer.MEDIA_INFO_BUFFERING_END -> "BUFFERING_END"
                        MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START -> "VIDEO_RENDERING_START"
                        else -> "INFO_$what"
                    }
                    Log.d("RadioPlayerService", "ℹ MediaPlayer Info: $infoStr (extra=$extra)")
                    false
                }

                Log.d("RadioPlayerService", "⏳ Calling MediaPlayer.prepareAsync()...")
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e("RadioPlayerService", "════════════════════════════════════════")
            Log.e("RadioPlayerService", "❌ ANDROID MEDIAPLAYER SETUP FAILED")
            Log.e("RadioPlayerService", "Error: ${e.javaClass.simpleName}")
            Log.e("RadioPlayerService", "Message: ${e.message}")
            Log.e("RadioPlayerService", "Stack: ${e.stackTraceToString().take(500)}")
            Log.e("RadioPlayerService", "════════════════════════════════════════")
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

    private fun sendSelf(action: String) {
        try { startService(Intent(this, RadioPlayerService::class.java).setAction(action)) } catch (_: Throwable) {}
    }

    private fun updateNotification(isPlaying: Boolean) {
        lastNotifiedPlaying = isPlaying
        val title = currentTitle ?: getString(R.string.app_name)
        loadNotificationArtIfNeeded()
        val notif = buildNotification(title, isPlaying)
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, notif)
    }

    // Station logo for the notification/session artwork; fetched once per favicon URL, then the notification is re-posted with it.
    private fun loadNotificationArtIfNeeded() {
        val raw = lastStationFavicon
        if (raw == notificationArtUrl) return
        notificationArtUrl = raw
        notificationArt = null
        if (raw.isNullOrBlank()) return
        val url = if (raw.startsWith("//")) "https:$raw" else raw
        serviceScope.launch {
            val bitmap = try {
                (URL(url).openConnection() as HttpURLConnection).run {
                    connectTimeout = 8000
                    readTimeout = 8000
                    instanceFollowRedirects = true
                    inputStream.use { BitmapFactory.decodeStream(it) }
                }
            } catch (_: Throwable) { null }
            if (bitmap != null && notificationArtUrl == raw) {
                notificationArt = bitmap
                runOnMain { updateNotification(lastNotifiedPlaying) }
            }
        }
    }

    private fun updateMediaSession(stationName: String, subtitle: String, isPlaying: Boolean, canSkip: Boolean) {
        if (!::mediaSession.isInitialized) return
        var actions = PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE or
            PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_STOP
        if (canSkip) actions = actions or PlaybackStateCompat.ACTION_SKIP_TO_NEXT or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
        try {
            mediaSession.setPlaybackState(
                PlaybackStateCompat.Builder()
                    .setActions(actions)
                    .setState(
                        if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                        PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                        1f
                    )
                    .build()
            )
            mediaSession.setMetadata(
                MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, stationName)
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, subtitle)
                    .apply { notificationArt?.let { putBitmap(MediaMetadataCompat.METADATA_KEY_ART, it) } }
                    .build()
            )
        } catch (_: Throwable) {}
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

        // Station name as the title; the live stream's now-playing text (when known) as the subtitle.
        val stationName = lastStationName?.takeIf { it.isNotBlank() } ?: title
        val subtitle = lastMetadata?.takeIf { it.isNotBlank() && it != stationName } ?: "Live radio"
        val canSkip = (stationList?.size ?: 0) > 1
        updateMediaSession(stationName, subtitle, isPlaying, canSkip)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentTitle(stationName)
            .setContentText(subtitle)
            .setContentIntent(contentIntent)
            .setDeleteIntent(stopPi)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        notificationArt?.let { builder.setLargeIcon(it) }

        // Order: prev, play/pause, next, stop; the compact view shows the first three (or just play/pause without a station list).
        if (canSkip) builder.addAction(NotificationCompat.Action(android.R.drawable.ic_media_previous, "Previous station", prevPi))
        builder.addAction(
            if (isPlaying) NotificationCompat.Action(android.R.drawable.ic_media_pause, "Pause", pausePi)
            else NotificationCompat.Action(android.R.drawable.ic_media_play, "Play", playPi)
        )
        if (canSkip) builder.addAction(NotificationCompat.Action(android.R.drawable.ic_media_next, "Next station", nextPi))
        builder.addAction(NotificationCompat.Action(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPi))

        val compact = if (canSkip) intArrayOf(0, 1, 2) else intArrayOf(0)
        val style = MediaAppNotificationCompat.MediaStyle()
            .setMediaSession(mediaSession.sessionToken)
            .setShowActionsInCompactView(*compact)

        builder.setStyle(style)

        return builder.build()
    }

    override fun onDestroy() {
        isRunning = false
        try { mediaSession.isActive = false; mediaSession.release() } catch (_: Throwable) {}
        try { player.release() } catch (_: Throwable) {}
        try { stopAndroidMediaPlayer() } catch (_: Throwable) {}
        try { stopMetadataPolling() } catch (_: Throwable) {}
        serviceScope.cancel()
        lastStatus = "stopped"
        super.onDestroy()
    }

    // Swiping the app away from Recents stops the radio too, matching the local player.
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        try { stopForeground(STOP_FOREGROUND_REMOVE) } catch (_: Throwable) {}
        stopSelf()
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
        if (Looper.myLooper() != Looper.getMainLooper()) {
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

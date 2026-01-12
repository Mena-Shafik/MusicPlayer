package com.example.musicplayer.radio

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PauseCircleFilled
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import com.example.musicplayer.R
import com.example.musicplayer.Util
import com.example.musicplayer.composable.RadioTagChips
import com.example.musicplayer.model.RadioStation
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.musicplayer.composable.RadioControls

@androidx.annotation.OptIn(UnstableApi::class)
@SuppressLint("ContextCastToActivity")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadioPlayerScreen(
    radioStation: RadioStation,
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // background brush
    var backgroundColor by remember { mutableStateOf(Color.Black) }
    val backgroundBrush = remember(backgroundColor) { Brush.verticalGradient(listOf(backgroundColor, Util.darkerColor(backgroundColor, 0.25f))) }

    val activity = LocalContext.current as? Activity
    LaunchedEffect(backgroundColor) {
        activity?.window?.statusBarColor = backgroundColor.toArgb()
        activity?.window?.let { win ->
            val controller = WindowInsetsControllerCompat(win, win.decorView)
            controller.isAppearanceLightStatusBars = backgroundColor.luminance() > 0.5f
        }
    }

    BackHandler { navController.popBackStack() }

    DisposableEffect(Unit) { onDispose { } }

    // Track service status and derive playing state based on strings
    var svcStatus by remember { mutableStateOf(RadioPlayerService.lastStatus) }
    var currentStationName by remember { mutableStateOf(radioStation.name ?: "Unknown") }
    var currentStationFavicon by remember { mutableStateOf(radioStation.favicon ?: "") }
    var currentStationTags by remember { mutableStateOf(radioStation.tags ?: "") }
    val isPlaying by remember(svcStatus) {
        derivedStateOf {
            val s = svcStatus.lowercase()
            s.contains("playing") || s == "ready" || s.contains("androidplayer_playing")
        }
    }

    // Read latest metadata published by the service and split into title/artist for display
    val stationMeta = try { RadioPlayerService.lastMetadata } catch (_: Throwable) { null }

    fun splitMeta(meta: String?): Pair<String?, String?> {
        if (meta.isNullOrBlank()) return Pair(null, null)
        val clean = meta.trim().replace(Regex("[\n\r\t]+"), " ")
        val seps = listOf(" - ", " — ", " – ", " / ", " | ")
        for (sep in seps) {
            if (clean.contains(sep)) {
                val parts = clean.split(sep, limit = 2)
                val artist = parts.getOrNull(0)?.trim()
                val title = parts.getOrNull(1)?.trim()
                return Pair(title.takeIf { !it.isNullOrBlank() }, artist.takeIf { !it.isNullOrBlank() })
            }
        }
        return Pair(clean.takeIf { it.isNotBlank() }, null)
    }

    val (playingTitle, playingArtist) = splitMeta(stationMeta)

    // Keep polling the service status so UI stays in sync with the actual service.
    LaunchedEffect(Unit) {
        try {
            svcStatus = RadioPlayerService.lastStatus
            currentStationName = RadioPlayerService.lastStationName ?: currentStationName
            currentStationFavicon = RadioPlayerService.lastStationFavicon ?: currentStationFavicon
            currentStationTags = RadioPlayerService.lastStationTags ?: currentStationTags
        } catch (_: Throwable) {}
        while (true) {
            try {
                svcStatus = RadioPlayerService.lastStatus
                currentStationName = RadioPlayerService.lastStationName ?: currentStationName
                currentStationFavicon = RadioPlayerService.lastStationFavicon ?: currentStationFavicon
                currentStationTags = RadioPlayerService.lastStationTags ?: currentStationTags
            } catch (_: Throwable) {}
            kotlinx.coroutines.delay(300L)
        }
    }

    fun togglePlayPause() {
        val ctx = context.applicationContext
        try {
            if (isPlaying) {
                val intent = Intent().apply { action = "com.example.musicplayer.action.PAUSE"; setClassName(ctx.packageName, "com.example.musicplayer.radio.RadioPlayerService") }
                ctx.startService(intent)
                // optimistic update
                svcStatus = "paused"
            } else {
                val url = radioStation.url ?: ""
                if (url.isBlank()) return
                val intent = Intent().apply {
                    action = "com.example.musicplayer.action.PLAY_STATION"
                    putExtra("extra_station_url", url)
                    putExtra("extra_station_title", radioStation.name ?: "")
                    putExtra(RadioPlayerService.EXTRA_STATION_FAVICON, radioStation.favicon)
                    putExtra(RadioPlayerService.EXTRA_STATION_TAGS, radioStation.tags)
                    setClassName(ctx.packageName, "com.example.musicplayer.radio.RadioPlayerService")
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(ctx, intent)
                } else {
                    ctx.startService(intent)
                }
                // optimistic update
                svcStatus = "playing"

                // Poll the service status briefly and show a toast if an error is reported
                scope.launch {
                    var seen = false
                    repeat(6) {
                        val s = RadioPlayerService.lastStatus
                        if (!s.isNullOrBlank()) {
                            seen = true
                            if (s.startsWith("error")) {
                                Toast.makeText(context, "Radio service error: $s", Toast.LENGTH_LONG).show()
                                return@launch
                            }
                            if (s == "READY" || s.equals("ready", true) || s.equals("playing", true) || s.contains("androidplayer_playing")) {
                                // service indicates playing; keep optimistic state
                                return@launch
                            }
                        }
                        kotlinx.coroutines.delay(500)
                    }
                    if (!seen) Toast.makeText(context, "Radio service started, check logs if no audio", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.w("RadioPlayerScreen", "Failed to toggle radio playback: ${e.message}")
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize(), topBar = { CenterAlignedTopAppBar(title = { Text(text = "", color = Color.White) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent), modifier = Modifier.statusBarsPadding()) }, containerColor = backgroundColor) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(modifier = Modifier.fillMaxWidth().fillMaxHeight().background(backgroundBrush), horizontalAlignment = Alignment.CenterHorizontally) {
                // Use the raw station-provided favicon exactly as supplied by the API, but
                // normalize protocol-relative URLs ("//host/...") to "https://host/..." so Coil can load them.
                // StationImage will display the bundled fallback if the favicon is blank or fails to load.
                val favRaw = currentStationFavicon
                val favUrl = when {
                    favRaw.startsWith("//") -> "https:$favRaw"
                    else -> favRaw
                }
                try { Log.d("RadioPlayerScreen", "Loading station favicon: $favUrl") } catch (_: Throwable) {}
                StationImage(path = favUrl, onDominantColor = { extracted -> backgroundColor = extracted })
                Column(modifier = Modifier.size(340.dp, 130.dp).padding(10.dp).align(Alignment.CenterHorizontally)) {
                    Text(text = currentStationName.ifBlank { "Unknown" }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp, textAlign = TextAlign.Center, modifier = Modifier.width(340.dp).padding(10.dp))
                    RadioTagChips(
                        tagsRaw = currentStationTags,
                        modifier = Modifier.width(340.dp),
                        chipBackground = Color.White.copy(alpha = 0.12f),
                        chipContentColor = Color.LightGray
                    )

                    // Show current song metadata (title then artist) when available from the service
                    if (!playingTitle.isNullOrBlank()) {
                        Text(
                            text = playingTitle,
                            color = Color.White.copy(alpha = 0.95f),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(340.dp).padding(top = 6.dp)
                        )
                    }

                    if (!playingArtist.isNullOrBlank()) {
                        Text(
                            text = playingArtist,
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(340.dp).padding(top = 2.dp)
                        )
                    }
                }
                RadioControls(
                    isPlaying = isPlaying,
                    onPlayPause = { togglePlayPause() },
                    onPrevStation = {
                        try {
                            val intent = Intent(context, RadioPlayerService::class.java).apply { action = RadioPlayerService.ACTION_PREV_STATION }
                            context.startService(intent)
                        } catch (_: Throwable) {}
                    },
                    onNextStation = {
                        try {
                            val intent = Intent(context, RadioPlayerService::class.java).apply { action = RadioPlayerService.ACTION_NEXT_STATION }
                            context.startService(intent)
                        } catch (_: Throwable) {}
                    }
                )
                // Separate composable to display now-playing title and artist under the controls
                RadioNowPlayingInfo(title = playingTitle, artist = playingArtist)
            }

            // Show the raw stream/service status as plain text at the bottom center of the screen
            // Keep service status separate from title/artist — always show svcStatus here.
            val statusText = svcStatus.ifBlank { "IDLE" }

            Text(
                text = statusText,
                color = Color.White.copy(alpha = 0.50f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)
            )
        }
    }
}

@Composable
fun StationImage(
    path: String,
    modifier: Modifier = Modifier,
    onDominantColor: (Color) -> Unit = {},
    onAccentColor: (Color) -> Unit = {}
) {
    val context = LocalContext.current

    // If path is blank just show the fallback immediately
    if (path.isBlank()) {
        Image(
            painter = painterResource(id = R.drawable.img),
            contentDescription = "Station Art",
            modifier = modifier
                .width(340.dp)
                .height(340.dp)
                .clip(RoundedCornerShape(5.dp)),
            contentScale = ContentScale.Crop
        )
        // Default colors when we don't have art
        LaunchedEffect(Unit) {
            onDominantColor(Color.Black)
            onAccentColor(Color.White)
        }
        return
    }

    // Use AsyncImage with built-in crossfade for smooth transitions
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(path)
            .crossfade(500)
            .build(),
        contentDescription = "Station Art",
        modifier = modifier
            .width(340.dp)
            .height(340.dp)
            .clip(RoundedCornerShape(5.dp)),
        contentScale = ContentScale.Crop,
        placeholder = painterResource(id = R.drawable.img),
        error = painterResource(id = R.drawable.img)
    )

    // Default colors for now (AsyncImage handles image loading internally)
    LaunchedEffect(path) {
        onDominantColor(Color.Black)
        onAccentColor(Color.White)
    }
}



@Composable
fun RadioNowPlayingInfo(title: String?, artist: String?, modifier: Modifier = Modifier) {
    // Separate composable that displays title and artist in a centered column.
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (!title.isNullOrBlank()) {
            Text(
                text = title,
                color = Color.White.copy(alpha = 0.95f),
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }

        if (!artist.isNullOrBlank()) {
            Text(
                text = artist,
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Suppress("unused")
@Composable
fun SmallAlbumImage(path: String?, size: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val imageBitmap = try {
        Util.getAlbumArt(context, path)
    } catch (_: Throwable) { null }

    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap,
            contentDescription = null,
            modifier = modifier.size(size),
            contentScale = ContentScale.Crop
        )
    } else {
        Image(
            painter = painterResource(id = R.drawable.img),
            contentDescription = null,
            modifier = modifier.size(size),
            contentScale = ContentScale.Crop
        )
    }
}

// Preview: Full Radio Screen (playing)
@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(UnstableApi::class)
@Preview(showBackground = true, showSystemUi = true, name = "RadioScreen (real) Preview", backgroundColor = 0xFF000000)
@Composable
fun RadioScreenPreview() {
    MaterialTheme {
        val context = LocalContext.current
        val navController = remember { androidx.navigation.NavController(context) }
        // single valid sample RadioStation (matches model.RadioStation constructor)
        val sampleStation = RadioStation(
            stationuuid = "custom-virgin-999",
            name = "Virgin 99.9",
            url = "https://18153.live.streamtheworld.com/CKFMFMAAC_SC",
            favicon = "https://provisioning.streamtheworld.com/virgin99.9/logo.png",
            country = "Canada",
            tags = "pop top40",
            bitrate = 128,
            codec = "mp3",
            votes = 0,
            geo_lat = null,
            geo_long = null
        )

        // Provide sample metadata and status for the preview so the UI shows title/artist
        try { RadioPlayerService.lastMetadata = "Ed Sheeran - Shape of You" } catch (_: Throwable) {}
        try { RadioPlayerService.lastStatus = "playing" } catch (_: Throwable) {}

        // Call the real RadioPlayerScreen preview with the sample station
        RadioPlayerScreen(radioStation = sampleStation, navController = navController)
    }
}

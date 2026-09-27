package com.example.musicplayer.radio

import android.os.Build
import androidx.compose.ui.unit.Dp
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Radio
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
import androidx.compose.ui.draw.shadow
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
import com.example.musicplayer.ui.theme.restoreDefaultSystemBars
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import com.example.musicplayer.R
import com.example.musicplayer.util.Util
import com.example.musicplayer.ui.components.radio.RadioTagChips
import com.example.musicplayer.ui.components.common.MultiRadialBackground
import com.example.musicplayer.ui.components.common.RadialSpec
import com.example.musicplayer.model.RadioStation
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.musicplayer.ui.components.common.RadioControls
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

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

    val activity = LocalContext.current as? Activity
    LaunchedEffect(backgroundColor) {
        activity?.window?.statusBarColor = backgroundColor.toArgb()
        activity?.window?.let { win ->
            val controller = WindowInsetsControllerCompat(win, win.decorView)
            controller.isAppearanceLightStatusBars = backgroundColor.luminance() > 0.5f
        }
    }

    DisposableEffect(Unit) { onDispose { activity?.window?.let { restoreDefaultSystemBars(it) } } }

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
            delay(300L.milliseconds)
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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
                        delay(500)
                    }
                    if (!seen) Toast.makeText(context, "Radio service started, check logs if no audio", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.w("RadioPlayerScreen", "Failed to toggle radio playback: ${e.message}")
        }
    }

    // Background drawn full-screen behind the whole Scaffold (including the top app bar), not just the content area below it -- otherwise a "transparent" app bar just shows the Scaffold's own flat containerColor instead of this blur/wash.
    Box(modifier = Modifier.fillMaxSize()) {
        // Fixed dark gradient + soft station-wash glow (not derived per-station -- the favicon palette extraction below is a stub that always returns black/white).
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color(0xFF23252B),
                        0.55f to Color(0xFF111318),
                        1f to Color(0xFF05060A)
                    )
                )
        )
        MultiRadialBackground(
            specs = listOf(
                RadialSpec(fx = 0.30f, fy = 0.18f, radius = 300.dp, colors = listOf(Color(0xFFFFA500), Color.Transparent), alpha = 0.3f),
                RadialSpec(fx = 0.80f, fy = 0.40f, radius = 280.dp, colors = listOf(Color(0xFF7A8296), Color.Transparent), alpha = 0.7f)
            ),
            blurRadius = 80.dp
        )

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "LIVE RADIO",
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.4.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowDown,
                                contentDescription = "Close player",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        Icon(
                            imageVector = Icons.Filled.Radio,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.padding(12.dp)
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    modifier = Modifier.statusBarsPadding()
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                // Uses the raw station favicon as supplied by the API, normalizing protocol-relative URLs ("//host/...") to "https://host/..." so Coil can load them; StationImage falls back to the bundled placeholder if blank/failed.
                val favRaw = currentStationFavicon
                val favUrl = when {
                    favRaw.startsWith("//") -> "https:$favRaw"
                    else -> favRaw
                }
                try { Log.d("RadioPlayerScreen", "Loading station favicon: $favUrl") } catch (_: Throwable) {}
                StationImage(path = favUrl, onDominantColor = { extracted -> backgroundColor = extracted })
                Column(modifier = Modifier.size(340.dp, 130.dp).padding(10.dp).align(Alignment.CenterHorizontally)) {
                    Text(
                        text = currentStationName.ifBlank { "Unknown" },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        lineHeight = 30.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(340.dp).padding(10.dp)
                    )
                    RadioTagChips(
                        tagsRaw = currentStationTags,
                        modifier = Modifier.width(340.dp),
                        chipBackground = Color.White.copy(alpha = 0.12f),
                        chipContentColor = Color.LightGray
                    )

                    // "ON AIR NOW" block, rendered once -- this used to also render again via a separate RadioNowPlayingInfo call below the controls, showing the same text twice.
                    if (!playingTitle.isNullOrBlank()) {
                        Text(
                            text = "ON AIR NOW",
                            color = Color(0xFFFFA500),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.4.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(340.dp).padding(top = 10.dp)
                        )
                        Text(
                            text = playingTitle,
                            color = Color.White,
                            fontSize = 16.sp,
                            lineHeight = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(340.dp).padding(top = 4.dp)
                        )
                        if (!playingArtist.isNullOrBlank()) {
                            Text(
                                text = playingArtist,
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(340.dp)
                            )
                        }
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
            }

            // Raw stream/service status, shown separately from title/artist -- always svcStatus here.
            val statusText = svcStatus.ifBlank { "IDLE" }

            Text(
                text = statusText,
                color = Color.White.copy(alpha = 0.50f),
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.2.sp,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 22.dp)
            )
        }
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
    // Station logos are typically white/light artwork, so unlike album art the tile itself is white with a muted glyph, not a dark placeholder.
    val tileModifier = modifier
        .width(340.dp)
        .height(340.dp)
        .shadow(
            elevation = 24.dp,
            shape = RoundedCornerShape(8.dp),
            ambientColor = Color.Black.copy(alpha = 0.55f),
            spotColor = Color.Black.copy(alpha = 0.55f)
        )
        .clip(RoundedCornerShape(8.dp))
        .background(Color.White)

    // If path is blank just show the fallback immediately
    if (path.isBlank()) {
        Box(modifier = tileModifier, contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Filled.Radio,
                contentDescription = "Station Art",
                tint = Color(0xFF8A8A8A),
                modifier = Modifier.size(100.dp)
            )
        }
        // Default colors when we don't have art
        LaunchedEffect(Unit) {
            onDominantColor(Color.Black)
            onAccentColor(Color.White)
        }
        return
    }

    var loaded by remember(path) { mutableStateOf(false) }
    Box(modifier = tileModifier, contentAlignment = Alignment.Center) {
        if (!loaded) {
            Icon(
                imageVector = Icons.Filled.Radio,
                contentDescription = null,
                tint = Color(0xFF8A8A8A),
                modifier = Modifier.size(100.dp)
            )
        }
        // Use AsyncImage with built-in crossfade for smooth transitions
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(path)
                .crossfade(500)
                .build(),
            contentDescription = "Station Art",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            onSuccess = { loaded = true },
            onError = { loaded = false }
        )
    }

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
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
        }

        if (!artist.isNullOrBlank()) {
            Text(
                text = artist,
                color = Color.White.copy(alpha = 0.75f),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Suppress("unused")
@Composable
fun SmallAlbumImage(path: String?, size: Dp, modifier: Modifier = Modifier) {
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
        val navController = remember { NavController(context) }
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

package com.example.musicplayer.radio

import com.example.musicplayer.music.MusicPlayerViewModel

import android.annotation.SuppressLint
import android.app.Activity
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PauseCircleFilled
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
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
import androidx.compose.ui.graphics.asAndroidBitmap
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import com.example.musicplayer.model.Song
import com.example.musicplayer.R
import com.example.musicplayer.Util
import com.example.musicplayer.model.RadioStation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.let

@SuppressLint("ContextCastToActivity")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadioScreen(
    radioStation: RadioStation,
    navController: NavController,
    viewModel: MusicPlayerViewModel = viewModel()
) {


    //val playlist by viewModel.playlist.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    // background color extracted from album art
    var backgroundColor by remember { mutableStateOf(Color.Black) }
    val backgroundBrush = remember(backgroundColor) {
        Brush.verticalGradient(listOf(backgroundColor, Util.darkerColor(backgroundColor, 0.25f)))
    }

    val activity = LocalContext.current as? Activity

    LaunchedEffect(backgroundColor) {
        activity?.window?.statusBarColor = backgroundColor.toArgb()
        activity?.window?.let { win ->
            val controller = WindowInsetsControllerCompat(win, win.decorView)
            // true = dark icons (for light background), false = light icons (for dark background)
            controller.isAppearanceLightStatusBars = backgroundColor.luminance() > 0.5f
        }
    }

    // When back pressed, simply navigate back (do not pause playback so the mini-player can appear in the list)
    BackHandler {
        navController.popBackStack()
    }

    // Also ensure we pause when the composable is disposed (navigated away)
    DisposableEffect(Unit) {
        onDispose {
            //if (isPlaying) {
            //    viewModel.togglePlayPause(ctx)
            //}
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.statusBarsPadding(),
            )
        },
        containerColor = backgroundColor
    ) { innerPadding ->
        // Use a Box so we can overlay a non-blocking peek bar anchored to the bottom.
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .background(backgroundBrush),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                //Spacer(modifier = Modifier.height(20.dp))


                    // Use placeholders here; the real station data will be provided when navigation supplies it.
                    val ctx = LocalContext.current
                    StationImage(
                        path = "",
                        onDominantColor = { extracted -> backgroundColor = extracted })
                    Column(modifier = Modifier
                        .size(340.dp, 130.dp)
                        .padding(10.dp).align(Alignment.CenterHorizontally),)
                    {
                        Text(
                            text = radioStation.name!!,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .width(340.dp)
                                .padding(10.dp)
                        )
                        RadioTagChips(tagsRaw = radioStation.tags,
                        modifier = Modifier.width(340.dp),
                        chipBackground = Color.White.copy(alpha = 0.12f),
                        chipContentColor =  Color.LightGray)
                    }

                    RadioControls(
                        isPlaying = isPlaying,
                        onPlayPause = { viewModel.togglePlayPause(ctx) }
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
    val albumBitmap = try {
        Util.getAlbumArt(context, path)
    } catch (_: Throwable) {
        null
    }

    if (albumBitmap != null) {
        Image(
            bitmap = albumBitmap,
            contentDescription = "Station Art",
            modifier = modifier
                .width(340.dp)
                .height(340.dp)
                .clip(RoundedCornerShape(5.dp))
        )

        LaunchedEffect(albumBitmap) {
            val (dominantInt, accentInt) = withContext(Dispatchers.Default) {
                try {
                    val palette = Palette.from(albumBitmap.asAndroidBitmap()).generate()
                    val dominant = palette.getDominantColor(android.graphics.Color.BLACK)
                    // prefer vibrant swatch, fallback to dominant
                    val accent = palette.vibrantSwatch?.rgb ?: palette.mutedSwatch?.rgb ?: dominant
                    Pair(dominant, accent)
                } catch (_: Throwable) {
                    Pair(android.graphics.Color.BLACK, android.graphics.Color.WHITE)
                }
            }
            onDominantColor(Color(dominantInt))
            onAccentColor(Color(accentInt))
        }
    } else {
        Image(
            painter = painterResource(id = R.drawable.img),
            contentDescription = "Station Art",
            modifier = modifier
                .width(340.dp)
                .height(340.dp)
                .clip(RoundedCornerShape(5.dp))
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun RadioControls(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    accentColor: Color = Color.White
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
        ) {

            val morphDuration = 320
            val targetSize = if (isPlaying) 96.dp else 96.dp
            val animatedSize by animateDpAsState(
                targetValue = targetSize,
                animationSpec = tween(durationMillis = morphDuration)
            )

            Box(
                modifier = Modifier
                    .size(animatedSize)
                    .background(Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.fillMaxSize()
                ) {
                    AnimatedContent(
                        targetState = isPlaying,
                        transitionSpec = {
                            val spec = tween<Float>(durationMillis = morphDuration)
                            (fadeIn(animationSpec = spec) + scaleIn(
                                initialScale = 1.15f,
                                animationSpec = spec
                            )) togetherWith
                                    (fadeOut(animationSpec = spec) + scaleOut(
                                        targetScale = 1.15f,
                                        animationSpec = spec
                                    ))
                        },
                        contentAlignment = Alignment.Center
                    ) { playing ->
                        val iconModifier = Modifier
                            .fillMaxSize()
                            .padding(5.dp, 0.dp, 5.dp, 0.dp)

                        if (playing) {
                            Icon(
                                imageVector = Icons.Filled.PauseCircleFilled,
                                contentDescription = "Pause",
                                modifier = iconModifier,
                                tint = accentColor
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.PlayCircleFilled,
                                contentDescription = "Play",
                                modifier = iconModifier,
                                tint = accentColor
                            )
                        }
                    }
                }
            }
            /*Icon(
                imageVector = if (isPlaying) Icons.Filled.PauseCircleFilled else Icons.Filled.PlayCircleFilled,
                contentDescription = "play/pause Button",
                modifier = Modifier
                    .size(84.dp)
                    .padding(5.dp)
                    .clickable { onPlayPause() },
                tint = accentColor
            )*/
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





@Preview(showBackground = true, name = "RadioControls Preview - Paused", backgroundColor = 0xFF000000)
@Composable
fun RadioControlsPreview_Paused() {
    MaterialTheme {
        Column(modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            RadioControls(isPlaying = false, onPlayPause = {})
        }
    }
}

@Preview(showBackground = true, name = "RadioScreen (real) Preview", backgroundColor = 0xFF000000, showSystemUi = true)
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

        // Use a plain ViewModel instance for preview; side-effects are inert in the preview sandbox
        val vm = remember { MusicPlayerViewModel() }

        RadioScreen(radioStation = sampleStation, navController = navController, viewModel = vm)
    }
}

// kotlin
package com.example.musicplayer.music

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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PauseCircleFilled
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextOverflow
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.firstOrNull
import kotlin.collections.getOrNull
import kotlin.collections.isNotEmpty
import kotlin.let
import kotlin.ranges.coerceAtLeast
import kotlin.ranges.coerceIn
import kotlin.ranges.rangeTo

@SuppressLint("ContextCastToActivity")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicScreen(
    songId: Int,
    songs: List<Song>,
    navController: NavController,
    viewModel: MusicPlayerViewModel = viewModel()
) {
    // ensure viewModel has the playlist / start index (tell the service via ViewModel)
    val ctx = LocalContext.current
    LaunchedEffect(songs, songId) {
        val current = viewModel.playlist.value
        val currentIdx = viewModel.currentIndex.value
        val incomingSame = if (current.isNotEmpty() && songs.isNotEmpty()) {
            // compare by path and size to detect same collection
            current.size == songs.size && current.getOrNull(0)?.path == songs.getOrNull(0)?.path
        } else {
            false
        }
        // Only set the playlist if we don't already have the same songs or the requested index differs
        if (!incomingSame || currentIdx != songId) {
            viewModel.setPlaylist(ctx, songs, songId)
        }
    }

    //val playlist by viewModel.playlist.collectAsState()
    val currentIndex by viewModel.currentIndex.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val replayEnabled by viewModel.replayEnabled.collectAsState()
    val shuffleEnabled by viewModel.shuffleEnabled.collectAsState()
    val positionMs by viewModel.positionMs.collectAsState()
    val durationMs by viewModel.durationMs.collectAsState()

    var showSheet by remember { mutableStateOf(false) }

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
    val song = songs.getOrNull(currentIndex) ?: songs.firstOrNull()

    // slider local state for user seeking
    var sliderPosition by remember { mutableStateOf(positionMs.toFloat()) }
    var isUserSeeking by remember { mutableStateOf(false) }

    // update sliderPosition when viewModel position changes
    LaunchedEffect(positionMs) {
        if (!isUserSeeking) sliderPosition = positionMs.toFloat()
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

                if (song != null) {
                    AlbumImage(
                        path = song.path,
                        onDominantColor = { extracted -> backgroundColor = extracted })
                    Column(modifier = Modifier
                        .size(340.dp, 130.dp)
                        .padding(10.dp).align(Alignment.CenterHorizontally),)
                    {
                        Text(
                            text = song.title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .width(340.dp)
                                .padding(10.dp)
                        )
                        Text(
                            text = song.artist,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                             modifier = Modifier
                                 .padding(10.dp)
                                 .width(340.dp)
                        )
                    }


                    val effectiveDuration =
                        if (durationMs > 0L) durationMs.toFloat() else song.duration.toFloat()

                    Slider(
                        value = sliderPosition.coerceIn(0f, effectiveDuration),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFFA500), // Orange color for the thumb
                            activeTrackColor = Color(0xFFFFA500), // Orange color for the active track
                            inactiveTrackColor = Color(0xFFFFDAB9), // Lighter orange for the inactive track
                        ),
                        onValueChange = {
                            isUserSeeking = true
                            sliderPosition = it
                        },
                        onValueChangeFinished = {
                            isUserSeeking = false
                            viewModel.seekTo(ctx, sliderPosition.toInt())
                        },
                        valueRange = 0f..effectiveDuration,
                        modifier = Modifier.width(300.dp)
                    )

                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 50.dp)) {
                        Text(
                            text = Util.converter(sliderPosition.toDouble()),
                            color = Color.White,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = Util.converter(effectiveDuration.toDouble()),
                            color = Color.White,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    MusicControls(
                        isPlaying = isPlaying,
                        replayEnabled = replayEnabled,
                        shuffleEnabled = shuffleEnabled,
                        onPlayPause = { viewModel.togglePlayPause(ctx) },
                        onNext = { viewModel.next(ctx) },
                        onPrev = { viewModel.previous(ctx) },
                        onReplayToggle = { viewModel.toggleReplay() },
                        onShuffleToggle = { viewModel.toggleShuffle(it) }
                    )
                }
            }

            // Non-blocking peek bar: visible when the sheet is not shown. It's anchored to the bottom and
            // only occupies a small height so it won't block other UI interactions.
            if (!showSheet) {
                SongsPeekBar(
                    backgroundColor = backgroundColor,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    onExpand = { showSheet = true }
                )
            }

            // Only compose the full BottomSheetScaffold when the user explicitly opens it. That avoids
            // creating any overlay/scrim that might intercept clicks while keeping a visible peek.
            if (showSheet) {
                SongsModalBottomSheet(
                    songs = songs,
                    currentIndex = currentIndex,
                    visible = showSheet,
                    onDismiss = { showSheet = false },
                    onSongSelected = { selectedIdx ->
                        viewModel.setPlaylist(ctx, songs, selectedIdx)
                        if (!isPlaying) viewModel.togglePlayPause(ctx)
                        showSheet = false
                    },
                    backgroundColor = backgroundColor
                )
            }
        }
    }
}

@Composable
fun AlbumImage(
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
            contentDescription = "Album Art",
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
            contentDescription = "Album Art",
            modifier = modifier
                .width(340.dp)
                .height(340.dp)
                .clip(RoundedCornerShape(5.dp))
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MusicControls(
    isPlaying: Boolean,
    replayEnabled: Boolean,
    shuffleEnabled: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onReplayToggle: () -> Unit,
    onShuffleToggle: (Boolean) -> Unit,
    accentColor: Color = Color.White
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            IconButton(
                onClick = onReplayToggle,
                modifier = Modifier
                    .size(45.dp, 45.dp)
                    .padding(5.dp, 0.dp, 5.dp, 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Repeat,
                    contentDescription = if (replayEnabled) "replay on" else "replay off",
                    modifier = Modifier.size(45.dp, 45.dp),
                    tint = if (replayEnabled) accentColor else Util.dim(false)
                )
            }

            IconButton(
                onClick = onPrev,
                modifier = Modifier
                    .size(65.dp, 65.dp)
                    .padding(0.dp, 0.dp, 10.dp, 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = "previous button",
                    modifier = Modifier.size(55.dp, 55.dp),
                    tint = accentColor
                )
            }
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

            IconButton(
                onClick = onNext,
                modifier = Modifier
                    .size(65.dp, 65.dp)
                    .padding(10.dp, 0.dp, 0.dp, 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "next button",
                    modifier = Modifier.size(55.dp, 55.dp),
                    tint = accentColor
                )
            }

            IconButton(
                onClick = { onShuffleToggle(!shuffleEnabled) },
                modifier = Modifier
                    .size(45.dp, 45.dp)
                    .padding(5.dp, 0.dp, 5.dp, 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Shuffle,
                    contentDescription = if (shuffleEnabled) "shuffle on" else "shuffle off",
                    modifier = Modifier.size(45.dp, 45.dp),
                    tint = if (shuffleEnabled) accentColor else Util.dim(false)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SongsModalBottomSheet(
    songs: List<Song>,
    currentIndex: Int,
    visible: Boolean,
    onDismiss: () -> Unit,
    onSongSelected: (index: Int) -> Unit,
    backgroundColor: Color // new param: pass MusicScreen's backgroundColor
) {
    // Use BottomSheetScaffold state so we can expand/collapse programmatically
    val scaffoldState = androidx.compose.material.rememberBottomSheetScaffoldState(
        bottomSheetState = androidx.compose.material.rememberBottomSheetState(
            initialValue = if (visible) androidx.compose.material.BottomSheetValue.Expanded
            else androidx.compose.material.BottomSheetValue.Collapsed
        )
    )

    // remember list state and start with current index centered (or clamped)
    val startIndex = currentIndex.coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)

    val scope = rememberCoroutineScope()

    // keep host `visible` in sync when user dismisses/swipes the sheet
    LaunchedEffect(scaffoldState.bottomSheetState.currentValue) {
        if (scaffoldState.bottomSheetState.currentValue == androidx.compose.material.BottomSheetValue.Collapsed && visible) {
            onDismiss()
        }

        // When sheet is expanded, scroll to the current song (clamp index)
        if (scaffoldState.bottomSheetState.currentValue == androidx.compose.material.BottomSheetValue.Expanded && songs.isNotEmpty()) {
            val target = currentIndex.coerceIn(0, songs.size - 1)
            // animate to item (safe to call even if already visible)
            listState.animateScrollToItem(target)
        }
    }

    // respond to visible changes by expanding/collapsing the sheet
    LaunchedEffect(visible) {
        if (visible) scaffoldState.bottomSheetState.expand() else scaffoldState.bottomSheetState.collapse()
    }

    // derive readable foreground from the provided backgroundColor
    val sheetBg = backgroundColor
    val contentOnBg = if (sheetBg.luminance() > 0.5f) Color.Black else Color.White

    val colors = MaterialTheme.colorScheme
    val primary = colors.primary
    val subtle = contentOnBg.copy(alpha = 0.06f)
    val handleColor = contentOnBg.copy(alpha = 0.12f)
    val currentBg = primary.copy(alpha = 0.12f)

    androidx.compose.material.BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetShape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        sheetElevation = 8.dp,
        sheetPeekHeight = 100.dp,
        sheetBackgroundColor = Color.Transparent,
        // don't draw a white background behind the sheet content — let the host UI show through
        backgroundColor = Color.Transparent,
        sheetContent = {
            // use a semi-transparent background so a light `sheetBg` won't fully cover the screen
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(sheetBg.copy(alpha = 0.90f))
                    .padding(bottom = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .padding(vertical = 10.dp)
                        .size(width = 40.dp, height = 4.dp)
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(2.dp))
                        .background(handleColor)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Up Next",
                        color = contentOnBg,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }

                HorizontalDivider(color = subtle)

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .heightIn(max = 520.dp)
                ) {
                    itemsIndexed(songs) { idx, song ->
                        val isCurrent = idx == currentIndex
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSongSelected(idx)
                                    // also collapse the sheet after selecting
                                    scope.launch { scaffoldState.bottomSheetState.collapse() }
                                }
                                .background(if (isCurrent) currentBg else Color.Transparent)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    color = contentOnBg,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = song.artist,
                                    color = contentOnBg.copy(alpha = 0.75f),
                                    fontSize = 13.sp
                                )
                            }

                            if (isCurrent) {
                                Icon(
                                    imageVector = Icons.Filled.PlayCircleFilled,
                                    contentDescription = null,
                                    tint = primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) {
        // main content placeholder (was Spacer previously)
        Spacer(modifier = Modifier.height(0.dp))
    }
}

// Peek bar composable placed in this file so it uses the same colors and callbacks.
@Composable
fun SongsPeekBar(
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    onExpand: () -> Unit
) {
    val sheetBg = backgroundColor
    val contentOnBg = if (sheetBg.luminance() > 0.5f) Color.Black else Color.White

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(70.dp)
            .clickable(onClick = onExpand)
            .background(Color.Transparent),
        contentAlignment = Alignment.CenterStart
    ) {
        /*Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(contentOnBg.copy(alpha = 0.12f))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = song?.title ?: "Up Next", color = contentOnBg, fontWeight = FontWeight.Bold)
                Text(text = song?.artist ?: "", color = contentOnBg.copy(alpha = 0.85f), fontSize = 12.sp)
            }

            SmallAlbumImage(path = song?.path, size = 40.dp)
        }*/
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Up Next",
                color = contentOnBg.copy(alpha = 0.75f),
                fontSize = 20.sp,
                fontWeight = FontWeight . Bold
            )
        }
    }
}

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

/*@Preview(showBackground = true, name = "MusicScreen Preview (default)", backgroundColor = 0xFF000000)
@Composable
fun MusicScreenPreview() {
    MaterialTheme {
        val context = LocalContext.current
        val navController = remember { NavController(context) }

        // No sample songs provided for preview; pass an empty list
        MusicScreen(
            songId = 0,
            songs = emptyList(),
            navController = navController
        )
    }
}*/

/*@Preview(showBackground = true, name = "MusicScreen Preview (middle song)", backgroundColor = 0xFF000000)
@Composable
fun MusicScreenPreview_Middle() {
    MaterialTheme {
        val context = LocalContext.current
        val navController = remember { NavController(context) }

        // No sample songs provided for preview; pass an empty list
        MusicScreen(
            songId = 1,
            songs = emptyList(),
            navController = navController
        )
    }
}*/

@Preview(
    showBackground = true,
    name = "MusicControls Preview (shuffle on, replay on)",
    backgroundColor = 0xFF000000
)
@Composable
fun MusicControlsPreview_Toggled() {
    MaterialTheme {
        MusicControls(
            isPlaying = true,
            replayEnabled = true,
            shuffleEnabled = true,
            onPlayPause = {},
            onNext = {},
            onPrev = {},
            onReplayToggle = { },
            onShuffleToggle = { _ -> }
        )
    }
}


@Preview(showBackground = true, name = "SongsPeekBar - Dark", backgroundColor = 0xFF000000)
@Composable
fun SongsPeekBarPreview_Dark() {
    MaterialTheme {
        SongsPeekBar(
            backgroundColor = Color.Black,
            onExpand = {}
        )
    }
}

@Preview(showBackground = true, name = "SongsModalBottomSheet - Expanded", backgroundColor = 0xFF000000, showSystemUi = true)
@Composable
fun SongsModalBottomSheetPreview_Expanded() {
    val sampleSongs = listOf(
        Song(0, "Album A","First Song", "Artist A", 180.0, "/storage/emulated/0/Music/first.mp3"),
        Song(1, "Album A","Second Song", "Artist B", 200.0, "/storage/emulated/0/Music/second.mp3"),
        Song(2, "Album A","Third Song", "Artist C", 240.0, "/storage/emulated/0/Music/third.mp3")
    )

    MaterialTheme {
        SongsModalBottomSheet(
            songs = sampleSongs,
            currentIndex = 1,
            visible = true,
            onDismiss = {},
            onSongSelected = {},
            backgroundColor = Color(0xFF121212)
        )
    }
}

@Preview(showBackground = true, name = "SongsModalBottomSheet - Collapsed", backgroundColor = 0xFF000000, showSystemUi = true)
@Composable
fun SongsModalBottomSheetPreview_Collapsed() {
    val sampleSongs = listOf(
        Song(0, "Album A","First Song", "Artist A", 180.0, "/storage/emulated/0/Music/first.mp3"),
        Song(1, "Album A","Second Song", "Artist B", 200.0, "/storage/emulated/0/Music/second.mp3"),
    )

    MaterialTheme {
        SongsModalBottomSheet(
            songs = sampleSongs,
            currentIndex = 0,
            visible = false,
            onDismiss = {},
            onSongSelected = {},
            backgroundColor = Color(0xFF222222)
        )
    }
}

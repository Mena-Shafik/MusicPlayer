// kotlin
package com.example.musicplayer.music

import android.annotation.SuppressLint
import android.app.Activity
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomSheetValue
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.icons.Icons
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
import androidx.compose.material.Tab
import androidx.compose.material.TabPosition
import androidx.compose.material.TabRowDefaults
import androidx.compose.runtime.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.text.TextStyle
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
import com.example.musicplayer.service.PlayerRepository
import com.example.musicplayer.composable.AudioVisualizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material.TabRow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.unit.Dp
import com.example.musicplayer.songlist.SongCardRow


// Lyrics are now cached on the Song instance (fields: lyrics, lyricsFetched). No global cache needed.

@SuppressLint("ContextCastToActivity")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerScreen(
    songId: Int,
    songs: List<Song>,
    navController: NavController,
    viewModel: MusicPlayerViewModel = viewModel()
) {
    // ensure viewModel has the playlist / start index (tell the service via ViewModel)
    val ctx = LocalContext.current
    LaunchedEffect(songs, songId) {
        // Map incoming songId (an identifier) to an index inside the provided `songs` list.
        val requestedIndex = songs.indexOfFirst { it.id == songId }.takeIf { it >= 0 } ?: 0
        // Ensure the view model knows about the playlist and requested start index.
        viewModel.setPlaylist(ctx, songs, requestedIndex)
    }

    //val playlist by viewModel.playlist.collectAsState()
    val currentIndex by viewModel.currentIndex.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val replayEnabled by viewModel.replayEnabled.collectAsState()
    val shuffleEnabled by viewModel.shuffleEnabled.collectAsState()
    val positionMs by viewModel.positionMs.collectAsState()
    val durationMs by viewModel.durationMs.collectAsState()

    // background color target extracted from album art
    var targetBackgroundColor by remember { mutableStateOf(Color.Black) }

    // Animate the background color smoothly when target changes
    val backgroundColor by animateColorAsState(
        targetValue = targetBackgroundColor,
        animationSpec = tween(durationMillis = 800),
        label = "Background color transition"
    )

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
    // Prefer the repository playlist for the currently-playing song so the UI always
    // reflects the actual playback state. Fall back to the provided `songs` parameter
    // if the repository playlist is empty or doesn't contain the expected index.
    val repoPlaylist by viewModel.playlist.collectAsState()
    val song = repoPlaylist.getOrNull(currentIndex) ?: songs.getOrNull(currentIndex) ?: songs.firstOrNull()

    // slider local state for user seeking
    var sliderPosition by remember { mutableStateOf(positionMs.toFloat()) }
    var isUserSeeking by remember { mutableStateOf(false) }

    // update sliderPosition when viewModel position changes
    LaunchedEffect(positionMs) {
        if (!isUserSeeking) sliderPosition = positionMs.toFloat()
    }

    // compute a consistent sheet peek height that includes any navigation bar inset
    // and add a small extra offset depending on navigation mode.
    // Heuristic: when the navigation bar inset is small (<= 20.dp) treat as gesture nav and
    // add a larger visible offset so the sheet headers are comfortably visible; otherwise
    // for 3-button navigation add a smaller extra offset.
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val extraPeek = if (navBarBottom == 24.dp) 30.dp else 8.dp
    val sheetPeekHeight = navBarBottom + extraPeek

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
        // persistent BottomSheetScaffold so the mini-player peek is visible and main content stays interactive
        val bottomSheetScaffoldState = androidx.compose.material.rememberBottomSheetScaffoldState(
            bottomSheetState = androidx.compose.material.rememberBottomSheetState(initialValue = BottomSheetValue.Collapsed)
        )
        val bsScope = rememberCoroutineScope()

        BottomSheetScaffold(
            modifier = Modifier.padding(innerPadding),
            scaffoldState = bottomSheetScaffoldState,
            sheetPeekHeight = sheetPeekHeight,
            sheetShape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
            sheetElevation = 8.dp,
            sheetBackgroundColor = Color.Transparent,
            backgroundColor = Color.Transparent,
            sheetContent = {
                SongsSheetContent(
                    songs = songs,
                    currentIndex = currentIndex,
                    backgroundColor = backgroundColor,
                    onSelect = { idx ->
                        // set playlist and start playing
                        viewModel.setPlaylist(ctx, songs, idx)
                        viewModel.play(ctx)
                        bsScope.launch { bottomSheetScaffoldState.bottomSheetState.collapse() }
                    },
                    onOpenSheet = {
                        // expand the BottomSheetScaffold when a tab is clicked inside the sheet header
                        bsScope.launch { try { bottomSheetScaffoldState.bottomSheetState.expand() } catch (_: Throwable) {} }
                    },
                    showIndicator = (bottomSheetScaffoldState.bottomSheetState.currentValue == BottomSheetValue.Expanded),
                    isExpanded = (bottomSheetScaffoldState.bottomSheetState.currentValue == BottomSheetValue.Expanded)
                )
            },
            content = { paddingValues ->
                // main content — remains interactive while sheet is collapsed
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .background(backgroundBrush)
                        .padding(paddingValues),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (song != null) {
                        AnimatedContent(
                            targetState = song.id,
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(500)) + scaleIn(
                                    initialScale = 0.95f,
                                    animationSpec = tween(500)
                                )) togetherWith
                                        (fadeOut(animationSpec = tween(300)) + scaleOut(
                                            targetScale = 1.05f,
                                            animationSpec = tween(300)
                                        ))
                            },
                            label = "Song transition"
                        ) { songId ->
                            val currentSong = songs.find { it.id == songId }
                            if (currentSong != null) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    AlbumImage(song = currentSong, onDominantColor = { c: Color -> targetBackgroundColor = c })
                                    Column(modifier = Modifier
                                        .size(340.dp, 130.dp)
                                        .padding(10.dp).align(Alignment.CenterHorizontally),) {
                                        Text(text = currentSong.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp, textAlign = TextAlign.Center, modifier = Modifier.width(340.dp).padding(10.dp))
                                        Text(text = currentSong.artist, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.padding(10.dp).width(340.dp))
                                    }
                                }
                            }
                        }


                        val effectiveDuration = if (durationMs > 0L) durationMs.toFloat() else song.duration.toFloat()
                        Slider(value = sliderPosition.coerceIn(0f, effectiveDuration), colors = SliderDefaults.colors(thumbColor = Color(0xFFFFA500), activeTrackColor = Color(0xFFFFA500), inactiveTrackColor = Color(0xFFFFDAB9)), onValueChange = { isUserSeeking = true; sliderPosition = it }, onValueChangeFinished = { isUserSeeking = false; viewModel.seekTo(ctx, sliderPosition.toInt()) }, valueRange = 0f..effectiveDuration, modifier = Modifier.width(300.dp))

                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 50.dp)) {
                            Text(text = Util.converter(sliderPosition.toDouble()), color = Color.White, textAlign = TextAlign.Start, modifier = Modifier.weight(1f))
                            Text(text = Util.converter(effectiveDuration.toDouble()), color = Color.White, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                        }

                        MusicControls(isPlaying = isPlaying, replayEnabled = replayEnabled, shuffleEnabled = shuffleEnabled, onPlayPause = { viewModel.togglePlayPause(ctx) }, onNext = { viewModel.next(ctx) }, onPrev = { viewModel.previous(ctx) }, onReplayToggle = { viewModel.toggleReplay() }, onShuffleToggle = { enabled -> viewModel.toggleShuffle(enabled) })
                    }

                    // small tappable area to expand the sheet
                    //Box(modifier = Modifier.fillMaxWidth().height(32.dp).clickable { bsScope.launch { bottomSheetScaffoldState.bottomSheetState.expand() } })
                }
            }
        )
    }
}

// SongsSheetContent: sheet UI (Up Next + Lyrics) without its own scaffold so it can be used inside a persistent BottomSheetScaffold.
@Composable
fun SongsSheetContent(
    songs: List<Song>,
    currentIndex: Int,
    backgroundColor: Color,
    onSelect: (Int) -> Unit,
    initialSelectedTab: Int = 0, // allow preview to set the starting tab
    onOpenSheet: () -> Unit = {}, // called when a tab is clicked so parent can expand the bottom sheet
    showIndicator: Boolean = true, // when false the tab indicator is hidden (useful when sheet is collapsed)
    isExpanded: Boolean = true, // whether the parent bottom sheet is expanded
    expandedHeight: Dp = 520.dp // fixed expanded height to enforce consistent sheet size
) {
    val sheetBg = backgroundColor
    val contentOnBg = if (sheetBg.luminance() > 0.5f) Color.Black else Color.White
    val subtle = contentOnBg.copy(alpha = 0.06f)
    val handleColor = contentOnBg.copy(alpha = 0.12f)

    val isPlayingSheet by PlayerRepository.isPlaying.collectAsState()
    val startIndex = currentIndex.coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)

    // Auto-scroll to current song when it changes
    LaunchedEffect(currentIndex) {
        listState.animateScrollToItem(startIndex)
    }

    // shared tab data/state (must be declared before we reference it in modifiers)
    val tabs = listOf("Up Next", "Lyrics", "Related")
    var selectedTab by remember { mutableStateOf(initialSelectedTab) }
    val context = LocalContext.current
    var relatedSongs by remember { mutableStateOf<List<Pair<Int, Song>>>(emptyList()) }

    // Populate relatedSongs whenever the Related tab is selected or when the current index/songs change.
    LaunchedEffect(selectedTab, currentIndex, songs) {
        if (selectedTab == 2) {
            try {
                relatedSongs = Util.getRelatedSongs(songs, currentIndex)
            } catch (e: Throwable) {
                // On error just clear related list (avoid crashing the UI)
                relatedSongs = emptyList()
            }
        } else {
            relatedSongs = emptyList()
        }
    }

    // accumulate drag distance between press and release so we can decide a swipe
    var dragAccum by remember { mutableStateOf(0f) }
    // threshold in pixels to be considered a swipe
    val swipeThreshold = 100f

    // swipe modifier: uses detectDragGestures (dragAmount is an Offset) and will switch tabs when threshold exceeded
    val swipeModifier = Modifier.pointerInput(selectedTab) {
        detectDragGestures(
            onDragStart = { dragAccum = 0f },
            onDrag = { change, dragAmount ->
                dragAccum += dragAmount.x
                change.consume()
            },
            onDragEnd = {
                if (dragAccum > swipeThreshold) {
                    // dragged right -> previous tab
                    selectedTab = (selectedTab - 1).coerceAtLeast(0)
                } else if (dragAccum < -swipeThreshold) {
                    // dragged left -> next tab
                    selectedTab = (selectedTab + 1).coerceAtMost(tabs.lastIndex)
                }
                dragAccum = 0f
            },
            onDragCancel = { dragAccum = 0f }
        )
    }

    // Enable swipe left/right across the sheet content area to switch tabs.
    // When collapsed we want the sheet to be transparent except for the tab headers,
    // so the outer column is transparent and the TabRow itself receives the sheet background.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(sheetBg.copy(alpha = 0.80f))
            .padding(bottom = 8.dp)
            .then(if (isExpanded) Modifier.height(expandedHeight) else Modifier)
    ) {
        TabRow(
            modifier = Modifier.fillMaxWidth().height(56.dp), // give extra vertical space so text isn't overlapped
            // When the sheet is collapsed the TabRow should show the sheet background so headers remain visible.
            selectedTabIndex = selectedTab,
            backgroundColor = sheetBg.copy(alpha = 0.80f),
            contentColor = contentOnBg,
            indicator = { tabPositions: List<TabPosition> ->
                // Always provide an Indicator composable so TabRow reserves the same height.
                // When showIndicator is false we render it transparent to hide it visually while
                // preserving layout (avoids jump when expanding).
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    height = 3.dp,
                    color = if (showIndicator) Color(0xFFFFA500) else Color.Transparent
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = selectedTab == index, selectedContentColor = contentOnBg,onClick = {
                    selectedTab = index
                    // Ask parent to open/expand the sheet when a tab is tapped
                    try { onOpenSheet() } catch (_: Throwable) {}
                }, text = {
                    val textColor = if (selectedTab == index) contentOnBg else contentOnBg.copy(alpha = 0.65f);
                    Text(
                        text = title,
                        color = textColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                        fontSize = 16.sp,
                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.SemiBold)
                })
            }
        }

        // Content area: capture horizontal swipes to switch tabs and show tab content.
        Column(modifier = swipeModifier.fillMaxWidth()) {
            if (selectedTab == 0) {
                HorizontalDivider(color = subtle)
                LazyColumn(state = listState, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).heightIn(max = 520.dp)) {
                    itemsIndexed(songs) { idx, s ->
                        val isCurrent = idx == currentIndex
                        // Build modifier on Modifier (so background/padding are applied correctly)
                        val rowMod = Modifier
                            .fillMaxWidth()
                            .clickable { if (idx != currentIndex) onSelect(idx) }
                            .background(if (isCurrent) Color(0xFFFFDAB9).copy(alpha = 0.12f) else Color.Transparent)
                            .padding(horizontal = 16.dp, vertical = 12.dp)

                        Row(modifier = rowMod, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = s.title, color = contentOnBg, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold, fontSize = 16.sp)
                                Text(text = s.artist, color = contentOnBg.copy(alpha = 0.75f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }

                            if (isCurrent) {
                                Box(modifier = Modifier.width(40.dp).height(40.dp).align(Alignment.CenterVertically)) {
                                    AudioVisualizer(audioSessionId = null, isPlaying = isPlayingSheet, modifier = Modifier.fillMaxSize(), barCount = 3, barWidth = 6.dp, heightDp = 24.dp, barColor = contentOnBg, speed = 1.6f)
                                }
                            }
                        }
                    }
                }
            } else if (selectedTab == 1) {
                val currentSong = songs.getOrNull(currentIndex)
                LyricsTab(currentSong = currentSong, contentColor = contentOnBg)
            } else {
                // Related tab UI
                HorizontalDivider(color = subtle)
                if (relatedSongs.isEmpty()) {
                    // show helpful message when no related songs found
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "No related songs found", color = contentOnBg.copy(alpha = 0.85f))
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).heightIn(max = 520.dp)) {
                        items(items = relatedSongs) { pair ->
                            val idx = pair.first
                            val s = pair.second

                            // Reuse the same song row used in the main song list so related items look identical.
                            SongCardRow(
                                song = s,
                                onClick = { onSelect(idx) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AlbumImage(
    song: Song,
    modifier: Modifier = Modifier,
    onDominantColor: (Color) -> Unit = {},
    onAccentColor: (Color) -> Unit = {}
) {
    val context = LocalContext.current
    val albumBitmap = Util.getAlbumArt(context, song.path)

    val imageModifier = modifier
        .width(340.dp)
        .height(340.dp)
        .clip(RoundedCornerShape(5.dp))

    Crossfade(targetState = albumBitmap, animationSpec = tween(500), label = "Album art crossfade") { bitmap ->
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Album Art",
                modifier = imageModifier
            )

            LaunchedEffect(bitmap) {
                val (dominantInt, accentInt) = withContext(Dispatchers.Default) {
                    try {
                        val palette = Palette.from(bitmap.asAndroidBitmap()).generate()
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
                modifier = imageModifier
            )
        }
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
    backgroundColor: Color,
    peekHeight: Dp,
    initialSelectedTab: Int = 0 // allow preview to start with a specific tab
) {
    val scaffoldState = androidx.compose.material.rememberBottomSheetScaffoldState(
        bottomSheetState = androidx.compose.material.rememberBottomSheetState(
            initialValue = if (visible) BottomSheetValue.Expanded else BottomSheetValue.Collapsed
        )
    )

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = currentIndex.coerceAtLeast(0))
    val scope = rememberCoroutineScope()
    val selectionInProgress = remember { mutableStateOf(false) }

    LaunchedEffect(scaffoldState.bottomSheetState.currentValue) {
        if (scaffoldState.bottomSheetState.currentValue == BottomSheetValue.Collapsed && visible) onDismiss()
        if (scaffoldState.bottomSheetState.currentValue == BottomSheetValue.Expanded && songs.isNotEmpty()) {
            listState.animateScrollToItem(currentIndex.coerceIn(0, songs.size - 1))
        }
    }
    LaunchedEffect(visible) { if (visible) scaffoldState.bottomSheetState.expand() else scaffoldState.bottomSheetState.collapse() }

    val sheetBg = backgroundColor
    val contentOnBg = if (sheetBg.luminance() > 0.5f) Color.Black else Color.White
    val subtle = contentOnBg.copy(alpha = 0.06f)

    var selectedTab by remember { mutableStateOf(initialSelectedTab) }
    var related by remember { mutableStateOf<List<Pair<Int, Song>>>(emptyList()) }
    val context = LocalContext.current

    LaunchedEffect(selectedTab, currentIndex, songs) {
        if (selectedTab == 2) {
            related = withContext(Dispatchers.IO) {
                val current = songs.getOrNull(currentIndex)
                val currentAlbum = songs.get(currentIndex).album
                if (currentAlbum.isNullOrBlank()) return@withContext emptyList<Pair<Int, Song>>()
                songs.mapIndexedNotNull { idx, s -> if (idx == currentIndex) null else {
                    val album = s.album
                    if (!album.isNullOrBlank() && album == currentAlbum) Pair(idx, s) else null
                }}
            } //DO NOT REMOVE THIS LINE
        } else {
            related = emptyList()
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetShape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        sheetElevation = 8.dp,
        sheetPeekHeight = peekHeight,
        sheetBackgroundColor = Color.Transparent,
        backgroundColor = Color.Transparent,
        sheetContent = {
            // Reuse the shared SongsSheetContent to avoid duplicated UI code.
            SongsSheetContent(
                songs = songs,
                currentIndex = currentIndex,
                backgroundColor = sheetBg,
                onSelect = { idx ->
                    // Propagate selection to the modal's callback and collapse the sheet.
                    onSongSelected(idx)
                    scope.launch { try { scaffoldState.bottomSheetState.collapse() } catch (_: Throwable) {} }
                },
                initialSelectedTab = initialSelectedTab,
                onOpenSheet = { scope.launch { try { scaffoldState.bottomSheetState.expand() } catch (_: Throwable) {} } },
                showIndicator = (scaffoldState.bottomSheetState.currentValue == BottomSheetValue.Expanded),
                isExpanded = (scaffoldState.bottomSheetState.currentValue == BottomSheetValue.Expanded)
            )
        }
    ) {
        Spacer(modifier = Modifier.height(0.dp))
    }
}

@Composable
fun LyricsTab(currentSong: Song?, modifier: Modifier = Modifier, contentColor: Color = Color.White) {
    var loading by remember { mutableStateOf(false) }
    // Keep lyrics in state so UI updates when cache fills
    var lyrics by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    // Only fetch when the currentSong changes and we don't already have a cached value on the song
    LaunchedEffect(currentSong?.id) {
        val tag = "LyricsTab"
        if (currentSong == null) {
            lyrics = null
            loading = false
            return@LaunchedEffect
        }

        // If the Song instance already has lyricsFetched, use its cached value (may be null)
        if (currentSong.lyricsFetched) {
            lyrics = currentSong.lyrics
            loading = false
            try { Log.d(tag, "Cache hit: lyricsFetched=true length=${currentSong.lyrics?.length ?: 0} for '${currentSong.title}'") } catch (_: Throwable) {}
            return@LaunchedEffect
        }

        loading = true
        try { Log.d(tag, "Begin fetch lyrics for '${currentSong.title}' by '${currentSong.artist}'") } catch (_: Throwable) {}
        // API-first: try online lyrics, then fallback to embedded file lyrics
        val fetched = withContext(Dispatchers.IO) {
            try {
                val apiResult = try { Util.fetchLyricsOnline(currentSong) } catch (_: Throwable) { null }
                if (!apiResult.isNullOrBlank()) {
                    try { Log.d(tag, "Loaded lyrics from API, length=${apiResult.length} title='${currentSong.title}'") } catch (_: Throwable) {}
                    return@withContext apiResult
                } else {
                    try { Log.d(tag, "API returned no lyrics; attempting embedded for '${currentSong.title}'") } catch (_: Throwable) {}
                }

            } catch (t: Throwable) {
                try { Log.w(tag, "Exception while fetching lyrics: ${t.message}", t) } catch (_: Throwable) {}
                null
            }
        }
        // Store on the song instance (may be null) and mark fetched
        currentSong.lyrics = fetched as String?
        currentSong.lyricsFetched = true
        lyrics = fetched
        loading = false
        try { Log.d(tag, "Fetch complete for '${currentSong.title}' length=${fetched?.length ?: 0}") } catch (_: Throwable) {}
    }

    val scrollState = rememberScrollState()

    // Outer container matches the height available for lyrics; we will center only the spinner inside it.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 520.dp)
            .padding(12.dp)
    ) {
        if (loading) {
            // Center only the spinner
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFFFA500))
            }
        } else if (lyrics.isNullOrBlank()) {
            // Show not-available message in normal flow (top-left within the lyrics area)
            Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Text(text = "Lyrics not available", color = contentColor)
            }
        } else {
            // Put lyrics in a vertically-scrollable container so long lyrics are fully visible
            Column(modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
                .verticalScroll(scrollState)
                .padding(8.dp)
            ) {
                // Add extra blank lines after the first N lines to improve readability on small screens
                val spaced = Util.addSpacingToFirstLines(lyrics, firstLines = 5) ?: lyrics ?: ""
                Text(
                    text = spaced,
                    color = contentColor,
                    fontWeight = FontWeight.Bold,
                    style = TextStyle(lineHeight = 15.sp)
                )
            }
        }
    }
}

//@Composable
//fun SongsPeekBar(
//    backgroundColor: Color,
//    modifier: Modifier = Modifier,
//    peekHeight: Dp = 70.dp, // default kept for previews/legacy calls
//    onExpand: () -> Unit
//) {
//    val sheetBg = backgroundColor
//    val contentOnBg = if (sheetBg.luminance() > 0.5f) Color.Black else Color.White
//
//    // Compute an accent color derived from the background. For dark backgrounds pick a
//    // white-ish accent to ensure readability; for light backgrounds use a darker tint of the bg.
//    val accentColor = remember(sheetBg) {
//        if (sheetBg.luminance() < 0.6f) Color.White else Util.darkerColor(sheetBg, 0.6f)
//    }
//
//    Box(
//        modifier = modifier
//            .fillMaxWidth()
//            .height(peekHeight)
//            .clickable { onExpand() }
//            .background(Color.Transparent),
//        contentAlignment = Alignment.CenterStart
//    ) {
//        /*Row(verticalAlignment = Alignment.CenterVertically) {
//            Box(
//                modifier = Modifier
//                    .size(width = 40.dp, height = 4.dp)
//                    .clip(RoundedCornerShape(2.dp))
//                    .background(contentOnBg.copy(alpha = 0.12f))
//            )
//
//            Spacer(modifier = Modifier.width(12.dp))
//
//            Column(modifier = Modifier.weight(1f)) {
//                Text(text = song?.title ?: "Up Next", color = contentOnBg, fontWeight = FontWeight.Bold)
//                Text(text = song?.artist ?: "", color = contentOnBg.copy(alpha = 0.85f), fontSize = 12.sp)
//            }
//
//            SmallAlbumImage(path = song?.path, size = 40.dp)
//        }*/
//        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
//            Text(
//                text = "Up Next",
//                color = accentColor.copy(alpha = 0.95f),
//                fontWeight = FontWeight.Bold,
//                fontSize = 20.sp,
//            )
//        }
//    }
//}


@Suppress("unused")
@Composable
fun SmallAlbumImage(path: String?, size: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val imageBitmap = try {
        Util.getAlbumArt(context, path)
    } catch (_: Throwable) { null }

    Crossfade(targetState = imageBitmap, animationSpec = tween(500), label = "Small album art crossfade") { bitmap ->
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
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
}

// @Preview(showBackground = true, name = "MusicScreen Preview (default)", backgroundColor = 0xFF000000)
// @Composable
// fun MusicScreenPreview() {
//     MaterialTheme {
//         val context = LocalContext.current
//         val navController = remember { NavController(context) }

//         // No sample songs provided for preview; pass an empty list
//         MusicScreen(
//             songId = 0,
//             songs = emptyList(),
//             navController = navController
//         )
//     }
// }

// @Preview(showBackground = true, name = "MusicScreen Preview (middle song)", backgroundColor = 0xFF000000)
// @Composable
// fun MusicScreenPreview_Middle() {
//     MaterialTheme {
//         val context = LocalContext.current
//         val navController = remember { NavController(context) }

//         // No sample songs provided for preview; pass an empty list
//         MusicScreen(


@Preview(showBackground = true, name = "MusicControls Preview (shuffle on, replay on)", backgroundColor = 0xFF000000)
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


@Preview(showBackground = true, showSystemUi = true, name = "MusicScreen (full) Preview", backgroundColor = 0xFF000000)
@Composable
fun MusicPlayerScreenFullPreview() {
    MaterialTheme {
        val context = LocalContext.current
        val navController = remember { androidx.navigation.NavController(context) }
        // create a sample playlist
        val sampleSongs = listOf(
            Song(0, "First Song", "Artist A", 180000.0, ""),
            Song(1, "Second Song", "Artist B", 210000.0, ""),
            Song(2, "Third Song", "Artist C", 240000.0, "")
        )
        // create a plain VM instance for preview; methods may no-op but it's okay for preview
        val vm = remember { MusicPlayerViewModel() }

        // call the real MusicScreen with a sample start song id of 0
        MusicPlayerScreen(songId = 0, songs = sampleSongs, navController = navController, viewModel = vm)
    }
}


@Preview(showBackground = true, showSystemUi = true, name = "SongsModalBottomSheet - Collapsed", backgroundColor = 0xFF000000)
@Composable
fun SongsModalBottomSheetPreview_Collapsed() {
    val sampleSongs = listOf(
        Song(0, "First Song", "Artist A", 180.0, "/storage/emulated/0/Music/first.mp3"),
        Song(1, "Second Song", "Artist B", 200.0, "/storage/emulated/0/Music/second.mp3"),
    )

    MaterialTheme {
        SongsModalBottomSheet(
            songs = sampleSongs,
            currentIndex = 0,
            visible = false,
            onDismiss = {},
            onSongSelected = {},
            backgroundColor = Color(0xFF222222),
            peekHeight = 54.dp
        )
    }
}


@Preview(showBackground = true, showSystemUi = true, name = "SongsModalBottomSheet - Expanded", backgroundColor = 0xFF000000)
@Composable
fun SongsModalBottomSheetPreview_Expanded() {
    val sampleSongs = listOf(
        Song(0, "First Song", "Artist A", 180.0, "/storage/emulated/0/Music/first.mp3"),
        Song(1, "Second Song", "Artist B", 200.0, "/storage/emulated/0/Music/second.mp3"),
        Song(2, "Third Song", "Artist C", 240.0, "/storage/emulated/0/Music/third.mp3")
    )

    MaterialTheme {
        SongsModalBottomSheet(
            songs = sampleSongs,
            currentIndex = 1,
            visible = true,
            onDismiss = {},
            onSongSelected = {},
            backgroundColor = Color(0xFF121212),
            peekHeight = 60.dp
        )
    }
}

// Previews specifically showing the Sheets with Lyrics or Related selected
@Preview(showBackground = true,  name = "SongsModalBottomSheet - Lyrics Selected", backgroundColor = 0xFF000000)
@Composable
fun SongsModalBottomSheetPreview_LyricsSelected() {
    val sampleSongs = listOf(
        Song(0, "First Song", "Artist A", 180000.0, "/storage/emulated/0/Music/first.mp3"),
        Song(1, "Second Song", "Artist B", 200000.0, "/storage/emulated/0/Music/second.mp3"),
        Song(2, "Third Song", "Artist C", 240000.0, "/storage/emulated/0/Music/third.mp3")
    )

    // Provide fake cached lyrics for the preview so LyricsTab shows content without network access
    try {
        // set lyrics on the sample song instance used in the preview
        sampleSongs[1].lyrics = "These are fake preview lyrics.\nLine 2 of the preview lyrics.\nLine 3 - chorus repeats."
        sampleSongs[1].lyricsFetched = true
    } catch (_: Throwable) {}

    MaterialTheme {
        SongsModalBottomSheet(
            songs = sampleSongs,
            currentIndex = 1,
            visible = true,
            onDismiss = {},
            onSongSelected = {},
            backgroundColor = Color(0xFF121212),
            peekHeight = 60.dp,
            initialSelectedTab = 1 // Lyrics
        )
    }
}

@Preview(showBackground = true, name = "SongsModalBottomSheet - Related Selected", backgroundColor = 0xFF000000)
@Composable
fun SongsModalBottomSheetPreview_RelatedSelected() {
    val sampleSongs = listOf(
        Song(0, "First Song", "Artist A", 180000.0, "/storage/emulated/0/Music/first.mp3"),
        Song(1, "Second Song", "Artist B", 200000.0, "/storage/emulated/0/Music/second.mp3"),
        Song(2, "Third Song", "Artist C", 240000.0, "/storage/emulated/0/Music/third.mp3")
    )

    MaterialTheme {
        SongsModalBottomSheet(
            songs = sampleSongs,
            currentIndex = 0,
            visible = true,
            onDismiss = {},
            onSongSelected = {},
            backgroundColor = Color(0xFF121212),
            peekHeight = 60.dp,
            initialSelectedTab = 2 // Related
        )
    }
}


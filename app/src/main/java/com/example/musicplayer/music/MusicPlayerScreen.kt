// kotlin
package com.example.musicplayer.music

import android.annotation.SuppressLint
import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
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
// Slider replaced by custom InteractiveSeekBar
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.foundation.gestures.detectTapGestures
// interaction imports no longer needed for Slider
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.TabRowDefaults.tabIndicatorOffset
import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.basicMarquee
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
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
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import com.example.musicplayer.model.Song
import com.example.musicplayer.R
import com.example.musicplayer.util.Util
import com.example.musicplayer.service.PlayerStateManager
import com.example.musicplayer.ui.components.common.AudioVisualizer
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material.Icon
import androidx.compose.material.TabRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Dp
import com.example.musicplayer.ui.components.background.AuroraBackground
import com.example.musicplayer.ui.components.common.LibraryViewTabs
import com.example.musicplayer.ui.components.common.MusicControls
import com.example.musicplayer.ui.components.song.SongCardRow
import com.example.musicplayer.ui.components.playlist.AddToPlaylistDialog


// Lyrics are now cached on the Song instance (fields: lyrics, lyricsFetched). No global cache needed.

@SuppressLint("ContextCastToActivity")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
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
        // If a playlist is already active and contains this song (e.g., launched from a playlist),
        // keep that playlist and just sync the current index instead of replacing it with the full library.
        val repo = viewModel.playlist.value
        val repoIdx = repo.indexOfFirst { it.id == songId }
        if (repo.isNotEmpty() && repoIdx >= 0) {
            PlayerStateManager.setCurrentIndex(repoIdx)
        } else {
            val requestedIndex = songs.indexOfFirst { it.id == songId }.takeIf { it >= 0 } ?: 0
            viewModel.setPlaylist(ctx, songs, requestedIndex)
        }
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
    // current album bitmap passed to AuroraBackground for palette sampling
    var currentAlbumBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    // collect preference early so we can make Scaffold and system bars transparent when Aurora is enabled
    val useAuroraBackground by com.example.musicplayer.preferences.PreferencesManager.getUseAuroraBackgroundFlow(ctx).collectAsState(initial = false)

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

    LaunchedEffect(backgroundColor, useAuroraBackground) {
        // If aurora background is used we want system bars to be transparent so the aurora shows through.
        try {
            activity?.window?.let { win ->
                if (useAuroraBackground) {
                    try { WindowCompat.setDecorFitsSystemWindows(win, false) } catch (_: Throwable) {}
                    try { win.statusBarColor = android.graphics.Color.TRANSPARENT } catch (_: Throwable) {}
                    // Do not change navigation bar color here — keep system navigation bar color stable
                } else {
                    try { WindowCompat.setDecorFitsSystemWindows(win, true) } catch (_: Throwable) {}
                    try { win.statusBarColor = backgroundColor.toArgb() } catch (_: Throwable) {}
                    // Leave navigation bar color unchanged to respect system theming
                }

                val controller = WindowInsetsControllerCompat(win, win.decorView)
                // Decide light/dark icons based on the target background color
                val light = targetBackgroundColor.luminance() > 0.5f
                controller.isAppearanceLightStatusBars = light
                // Do not modify navigation bar icon appearance here to avoid changing system nav bar visuals
            }
        } catch (_: Throwable) {}
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
    val activeSongs = if (repoPlaylist.isNotEmpty()) repoPlaylist else songs
    val song = activeSongs.getOrNull(currentIndex) ?: songs.getOrNull(currentIndex) ?: songs.firstOrNull()

    // Add-to-playlist dialog state
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var selectedSongIdForPlaylist by remember { mutableStateOf<Int?>(null) }

    // slider local state for user seeking
    var sliderPosition by remember { mutableStateOf(positionMs.toFloat()) }
    var isUserSeeking by remember { mutableStateOf(false) }

    // update sliderPosition when viewModel position changes
    LaunchedEffect(positionMs) {
        if (!isUserSeeking) sliderPosition = positionMs.toFloat()
    }

    // Sheet peek height: just the collapsed header's own content (grab handle + tab pills,
    // ~72dp — see the Box/LibraryViewTabs block at the top of SongsSheetContent). The system
    // navigation bar inset is already reserved by the outer Scaffold's innerPadding (its
    // default contentWindowInsets includes safeDrawing/navigationBars) which is applied to
    // this BottomSheetScaffold via .padding(innerPadding) below — adding it again here would
    // double-count it and inflate the peek past the header into the content underneath.
    val sheetPeekHeight = 87.dp


    val scaffoldContainerColor = if (useAuroraBackground) Color.Transparent else backgroundColor

    // Contrast checkpoints along the screen's own vertical gradient (backgroundColor at the
    // top, fading to a much darker Util.darkerColor(.., 0.25f) at the bottom) — the top bar,
    // the mid-screen title/artist block and the sheet (handled separately, since its own
    // panel colors that region once open) each sit at a different point on that gradient, so
    // a single fixed white/black choice doesn't hold for all three.
    val topOnBg = if (backgroundColor.luminance() > 0.5f) Color.Black else Color.White
    val gradientMiddle = Color(
        red = (backgroundColor.red + Util.darkerColor(backgroundColor, 0.25f).red) / 2f,
        green = (backgroundColor.green + Util.darkerColor(backgroundColor, 0.25f).green) / 2f,
        blue = (backgroundColor.blue + Util.darkerColor(backgroundColor, 0.25f).blue) / 2f
    )
    val middleOnBg = if (gradientMiddle.luminance() > 0.5f) Color.Black else Color.White

    // Draw aurora behind the entire UI (including TopAppBar); keep Scaffold as the primary layout
    Box(modifier = Modifier.fillMaxSize()) {
        if (useAuroraBackground) {
            AuroraBackground(modifier = Modifier.fillMaxSize(), albumCoverBitmap = currentAlbumBitmap)
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "PLAYING FROM LIBRARY",
                            color = topOnBg.copy(alpha = 0.65f),
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
                                tint = topOnBg
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    modifier = Modifier.statusBarsPadding(),
                    actions = {
                        IconButton(onClick = {
                            try {
                                val currentId = song?.id
                                if (currentId != null) {
                                    selectedSongIdForPlaylist = currentId
                                    showAddToPlaylistDialog = true
                                    Log.d("MusicPlayerScreen", "Opening AddToPlaylistDialog for songId=$currentId")
                                } else {
                                    Toast.makeText(ctx, "No song available to add", Toast.LENGTH_SHORT).show()
                                }
                            } catch (_: Throwable) {}
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Add to playlist",
                                tint = topOnBg
                            )
                        }
                    }
                )
            },
            containerColor = scaffoldContainerColor
        ) { innerPadding ->
            // persistent BottomSheetScaffold so the mini-player peek is visible and main content stays interactive
            val bottomSheetScaffoldState = androidx.compose.material.rememberBottomSheetScaffoldState(
                bottomSheetState = androidx.compose.material.rememberBottomSheetState(initialValue = BottomSheetValue.Collapsed)
            )
            val bsScope = rememberCoroutineScope()

            // Live 0f..1f collapsed->expanded fraction driven by the sheet's actual drag
            // position (currentValue/targetValue/progress), not just the discrete settled
            // state — so it tracks continuously while the user is mid-drag, not just at the
            // start/end of the gesture.
            val sheetState = bottomSheetScaffoldState.bottomSheetState
            val sheetOpenFraction = when {
                sheetState.currentValue == BottomSheetValue.Expanded && sheetState.targetValue == BottomSheetValue.Expanded -> 1f
                sheetState.currentValue == BottomSheetValue.Collapsed && sheetState.targetValue == BottomSheetValue.Collapsed -> 0f
                sheetState.targetValue == BottomSheetValue.Expanded -> sheetState.progress
                else -> 1f - sheetState.progress
            }.coerceIn(0f, 1f)

            BottomSheetScaffold(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                scaffoldState = bottomSheetScaffoldState,
                sheetPeekHeight = sheetPeekHeight,
                sheetShape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                sheetElevation = 0.dp,
                sheetBackgroundColor = Color.Transparent,
                backgroundColor = Color.Transparent,
                sheetContent = {
                    SongsSheetContent(
                        songs = activeSongs,
                        currentIndex = currentIndex,
                        backgroundColor = backgroundColor,
                        onSelect = { idx ->
                            // set playlist and start playing
                            viewModel.setPlaylist(ctx, activeSongs, idx)
                            viewModel.play(ctx)
                            bsScope.launch { bottomSheetScaffoldState.bottomSheetState.collapse() }
                        },
                        onOpenSheet = {
                            // expand the BottomSheetScaffold when a tab is clicked inside the sheet header
                            bsScope.launch { try { bottomSheetScaffoldState.bottomSheetState.expand() } catch (_: Throwable) {} }
                        },
                        showIndicator = (bottomSheetScaffoldState.bottomSheetState.currentValue == BottomSheetValue.Expanded),
                        isExpanded = (bottomSheetScaffoldState.bottomSheetState.currentValue == BottomSheetValue.Expanded),
                        openFraction = sheetOpenFraction
                    )
                },
                content = { paddingValues ->
                    // main content — remains interactive while sheet is collapsed

                    val contentModifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .then(if (useAuroraBackground) Modifier else Modifier.background(backgroundBrush))
                        .padding(paddingValues)

                    Column(
                        modifier = contentModifier,
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
                                val currentSong = activeSongs.find { it.id == songId } ?: songs.find { it.id == songId }
                                if (currentSong != null) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        AlbumImage(
                                            song = currentSong,
                                            onDominantColor = { c: Color -> targetBackgroundColor = c },
                                            onBitmap = { bmp -> currentAlbumBitmap = bmp }
                                        )
                                        Column(modifier = Modifier
                                            .size(340.dp, 130.dp)
                                            .padding(10.dp).align(Alignment.CenterHorizontally),) {
                                            Text(
                                                text = currentSong.title,
                                                color = middleOnBg,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 24.sp,
                                                lineHeight = 30.sp,
                                                textAlign = TextAlign.Center,
                                                maxLines = 1,
                                                modifier = Modifier
                                                    .width(340.dp)
                                                    .padding(8.dp)
                                                    .basicMarquee(
                                                        iterations = Int.MAX_VALUE,
                                                        initialDelayMillis = 2000,
                                                        spacing = MarqueeSpacing(50.dp)
                                                    )
                                            )
                                            val artistLineCount = currentSong.artist.split("\n").size
                                            Text(
                                                text = currentSong.artist,
                                                color = middleOnBg.copy(alpha = 0.7f),
                                                textAlign = TextAlign.Center,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = if (artistLineCount > 3) Int.MAX_VALUE else 3,
                                                modifier = Modifier
                                                    .padding(10.dp)
                                                    .width(340.dp)
                                                    .then(
                                                        if (artistLineCount > 3) {
                                                            Modifier.basicMarquee(
                                                                iterations = Int.MAX_VALUE,
                                                                initialDelayMillis = 2000,
                                                                spacing = MarqueeSpacing(50.dp)
                                                            )
                                                        } else {
                                                            Modifier
                                                        }
                                                    )
                                            )
                                        }
                                    }
                                }
                            }


                            val effectiveDuration = if (durationMs > 0L) durationMs.toFloat() else song.duration.toFloat()
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                InteractiveSeekBar(
                                    value = sliderPosition.coerceIn(0f, effectiveDuration),
                                    valueRange = 0f..effectiveDuration,
                                    modifier = Modifier.width(308.dp),
                                    activeColor = Color(0xFFFFA500),
                                    inactiveColor = Color.White.copy(alpha = 0.22f),
                                    onValueChange = { isUserSeeking = true; sliderPosition = it },
                                    onValueChangeFinished = {
                                        isUserSeeking = false
                                        viewModel.seekTo(ctx, sliderPosition.toInt())
                                    }
                                )

                                Row(modifier = Modifier.width(308.dp)) {
                                    Text(
                                        text = Util.converter(sliderPosition.toDouble()),
                                        color = Color.White,
                                        textAlign = TextAlign.Start,
                                        fontSize = 12.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = Util.converter(effectiveDuration.toDouble()),
                                        color = Color.White,
                                        textAlign = TextAlign.End,
                                        fontSize = 12.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
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

    // Render AddToPlaylistDialog when requested
    if (showAddToPlaylistDialog && selectedSongIdForPlaylist != null) {
        AddToPlaylistDialog(
            songId = selectedSongIdForPlaylist!!,
            song = activeSongs.find { it.id == selectedSongIdForPlaylist } ?: songs.find { it.id == selectedSongIdForPlaylist },
            onDismiss = {
                showAddToPlaylistDialog = false
                selectedSongIdForPlaylist = null
            },
            onConfirm = { playlistId ->
                // user selected a playlist and the dialog added the song via PlaylistViewModel
                Toast.makeText(ctx, "Added to playlist", Toast.LENGTH_SHORT).show()
                showAddToPlaylistDialog = false
                selectedSongIdForPlaylist = null
            }
        )
    }
}

// SongsSheetContent: sheet UI (Up Next + Lyrics + Related) without its own scaffold so it can be used inside a persistent BottomSheetScaffold.
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
    expandedHeight: Dp = 520.dp, // fixed expanded height to enforce consistent sheet size
    openFraction: Float = if (isExpanded) 1f else 0f // live collapsed(0)->expanded(1) drag progress
) {
    val sheetBg = backgroundColor
    // The tinted panel reveals in lockstep with openFraction as the sheet is dragged/animated
    // open, rather than snapping in only once fully expanded.
    val liveSheetAlpha = 0.80f * openFraction
    val liveSheetBg = sheetBg.copy(alpha = liveSheetAlpha)
    // What's actually behind the sheet: when fully collapsed this panel is invisible, so the
    // eye sees the screen's own gradient at its darker bottom stop; as the sheet opens, this
    // panel's own (raw, undarkened) color increasingly dominates. Composite the two so the
    // contrast decision below tracks what's really on screen at the current drag position,
    // not just the closed or the open extreme.
    val behindSheet = Util.darkerColor(sheetBg, 0.25f)
    val effectiveSheetBg = Color(
        red = sheetBg.red * liveSheetAlpha + behindSheet.red * (1f - liveSheetAlpha),
        green = sheetBg.green * liveSheetAlpha + behindSheet.green * (1f - liveSheetAlpha),
        blue = sheetBg.blue * liveSheetAlpha + behindSheet.blue * (1f - liveSheetAlpha)
    )
    val contentOnBg by animateColorAsState(
        targetValue = if (effectiveSheetBg.luminance() > 0.5f) Color.Black else Color.White,
        animationSpec = tween(220),
        label = "sheetContentOnBg"
    )
    val subtle = contentOnBg.copy(alpha = 0.06f)
    val handleColor = contentOnBg.copy(alpha = 0.12f)

    val isPlayingSheet by PlayerStateManager.isPlaying.collectAsState()
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
    // When collapsed the sheet should be invisible except for the handle/tab pills floating
    // over the screen behind it (matching the redesign); the tinted panel fades in as the
    // sheet is opened (see liveSheetBg above) rather than appearing only once fully expanded.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(liveSheetBg)
            .padding(bottom = 8.dp)
            // Fixed regardless of isExpanded (which only flips true once the sheet has
            // fully settled) so the sheet's measured content height — and therefore
            // BottomSheetScaffold's collapsed/expanded anchors — never changes mid-gesture.
            // Letting this toggle with isExpanded caused the content to wrap taller while
            // dragging, then snap down to expandedHeight the instant the drag settled,
            // which made the sheet jump and re-settle right after opening.
            .height(expandedHeight)
    ) {
        // Grab handle, matching the redesign's bottom-sheet affordance.
        Box(
            modifier = Modifier
                .padding(top = 10.dp, bottom = 12.dp)
                .width(36.dp)
                .height(4.dp)
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.28f))
        )
        // Pill segmented control replaces the underline TabRow. The tab stays selected
        // underneath while the sheet is collapsed, but the highlight tracks openFraction
        // directly — invisible when closed, fading/sliding in exactly in step with the
        // open drag rather than snapping in once fully expanded.
        LibraryViewTabs(
            labels = tabs,
            selectedIndex = selectedTab,
            openFraction = openFraction,
            unselectedColor = contentOnBg.copy(alpha = 0.75f),
            onSelected = { index ->
                selectedTab = index
                // Ask parent to open/expand the sheet when a tab is tapped
                try { onOpenSheet() } catch (_: Throwable) {}
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))

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
                                Text(
                                    text = s.title,
                                    color = contentOnBg,
                                    style = if (isCurrent) MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                           else MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = s.artist,
                                    color = contentOnBg.copy(alpha = 0.75f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
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
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).heightIn(max = 520.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
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
    onAccentColor: (Color) -> Unit = {},
    onBitmap: (android.graphics.Bitmap?) -> Unit = {}
) {
    val context = LocalContext.current
    var displayBitmap by remember(song.path) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(song.path) {
        displayBitmap = null
        if (song.path.isNotBlank()) {
            displayBitmap = withContext(Dispatchers.IO) {
                try {
                    // First try embedded album art
                    var bitmap = Util.getAlbumArt(context, song.path)

                    // If no embedded art, fetch from web
                    if (bitmap == null) {
                        Log.d("AlbumImage", "No embedded artwork for '${song.title}', fetching from web...")
                        val webUrl = Util.getAlbumArtWebUrl(song)
                        if (webUrl != null) {
                            bitmap = Util.loadBitmapFromUrl(webUrl)
                            if (bitmap != null) {
                                Log.d("AlbumImage", "✓ Loaded web album art for '${song.title}'")
                            }
                        }
                    }
                    bitmap
                } catch (_: Throwable) {
                    null
                }
            }
        }
    }

    val imageModifier = modifier
        .width(308.dp)
        .height(308.dp)
        .shadow(
            elevation = 24.dp,
            shape = RoundedCornerShape(8.dp),
            ambientColor = Color.Black.copy(alpha = 0.55f),
            spotColor = Color.Black.copy(alpha = 0.55f)
        )
        .clip(RoundedCornerShape(8.dp))

    Crossfade(targetState = displayBitmap, animationSpec = tween(500), label = "Album art crossfade") { bitmap ->
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
                // forward the loaded android Bitmap to caller for background sampling
                try { onBitmap(bitmap.asAndroidBitmap()) } catch (_: Throwable) { onBitmap(null) }
            }
        } else {
            Box(
                modifier = imageModifier.background(Color(0xFF2A2A2A)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Album,
                    contentDescription = "Album Art",
                    modifier = Modifier.size(100.dp),
                    tint = Color(0xFF5A5A5A)
                )
            }
            // no bitmap available — inform caller
            LaunchedEffect(Unit) { onBitmap(null) }
        }
    }
}

@Composable
fun InteractiveSeekBar(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    modifier: Modifier = Modifier,
    activeColor: Color = Color(0xFFFFA500),
    inactiveColor: Color = Color.White.copy(alpha = 0.22f),
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit
) {
    // Press state for thickening the track; the thumb is always visible (6dp radius,
    // growing to 8dp while pressed) matching the redesign's always-on seek handle.
    var pressed by remember { mutableStateOf(false) }
    val thumbRadius by animateFloatAsState(targetValue = if (pressed) 8f else 6f, label = "thumbRadius")
    val trackHeightDp by animateFloatAsState(targetValue = if (pressed) 4f else 2f, label = "trackHeight")

    Box(modifier = modifier
        .height(24.dp)
        .pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { offset ->
                    pressed = true
                    // update value immediately on press
                    val w = size.width.toFloat()
                    val x = offset.x.coerceIn(0f, w)
                    val frac = if (w > 0f) x / w else 0f
                    val newValue = (valueRange.start + (valueRange.endInclusive - valueRange.start) * frac)
                    onValueChange(newValue)
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    val x = change.position.x.coerceIn(0f, size.width.toFloat())
                    val frac = if (size.width > 0f) x / size.width else 0f
                    val newValue = (valueRange.start + (valueRange.endInclusive - valueRange.start) * frac)
                    onValueChange(newValue)
                },
                onDragEnd = {
                    pressed = false
                    onValueChangeFinished()
                },
                onDragCancel = {
                    pressed = false
                    onValueChangeFinished()
                }
            )
        }
        // detectDragGestures only fires once a touch moves past the system touch-slop
        // threshold, so a plain tap (press + release, no real movement) never reached
        // onValueChange at all — this is what let you tap a spot on the bar and have
        // nothing happen. Handle plain taps separately so both work.
        .pointerInput(Unit) {
            detectTapGestures(
                onTap = { offset ->
                    val w = size.width.toFloat()
                    val x = offset.x.coerceIn(0f, w)
                    val frac = if (w > 0f) x / w else 0f
                    val newValue = (valueRange.start + (valueRange.endInclusive - valueRange.start) * frac)
                    onValueChange(newValue)
                    onValueChangeFinished()
                }
            )
        }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val trackHeightPx = trackHeightDp.dp.toPx().coerceAtLeast(1f)
            // draw inactive track
            drawRoundRect(
                color = inactiveColor,
                topLeft = androidx.compose.ui.geometry.Offset(0f, (h - trackHeightPx) / 2f),
                size = androidx.compose.ui.geometry.Size(w, trackHeightPx),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackHeightPx / 2f, trackHeightPx / 2f)
            )

            // draw active track
            val frac = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
            val activeW = w * frac
            drawRoundRect(
                color = activeColor,
                topLeft = androidx.compose.ui.geometry.Offset(0f, (h - trackHeightPx) / 2f),
                size = androidx.compose.ui.geometry.Size(activeW, trackHeightPx),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackHeightPx / 2f, trackHeightPx / 2f)
            )

            // draw thumb only when pressed (thumbRadius > 0)
            if (thumbRadius > 0f) {
                val cx = activeW
                val cy = h / 2f
                drawCircle(color = activeColor, radius = thumbRadius, center = androidx.compose.ui.geometry.Offset(cx, cy))
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
                if (current == null) return@withContext emptyList<Pair<Int, Song>>()

                val currentAlbum = current.album
                val currentArtist = current.artist
                val sameAlbumSongs = mutableListOf<Pair<Int, Song>>()
                val sameArtistSongs = mutableListOf<Pair<Int, Song>>()

                songs.forEachIndexed { idx, s ->
                    if (idx == currentIndex) return@forEachIndexed // skip current song

                    val isSameAlbum = !currentAlbum.isNullOrBlank() && s.album == currentAlbum
                    val isSameArtist = !currentArtist.isNullOrBlank() && s.artist == currentArtist

                    when {
                        isSameAlbum -> sameAlbumSongs.add(Pair(idx, s))
                        isSameArtist -> sameArtistSongs.add(Pair(idx, s))
                    }
                }

                // Combine: same album first, then same artist
                (sameAlbumSongs + sameArtistSongs).toList()
            } //DO NOT REMOVE THIS LINE
        } else {
            related = emptyList()
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetShape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        sheetElevation = 0.dp,
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
        val fetched: String? = withContext(Dispatchers.IO) {
            try {
                val apiResult = try {
                    val result = Util.fetchLyricsOnline(currentSong)
                    // Ensure result is a String before using it
                    if (result is String) result else null
                } catch (_: Throwable) {
                    null
                }
                if (!apiResult.isNullOrBlank()) {
                    try { Log.d(tag, "Loaded lyrics from API, length=${apiResult.length} title='${currentSong.title}'") } catch (_: Throwable) {}
                    return@withContext apiResult
                } else {
                    try { Log.d(tag, "API returned no lyrics; attempting embedded for '${currentSong.title}'") } catch (_: Throwable) {}
                }
                null
            } catch (t: Throwable) {
                try { Log.w(tag, "Exception while fetching lyrics: ${t.message}", t) } catch (_: Throwable) {}
                null
            }
        }
        // Store on the song instance (may be null) and mark fetched
        currentSong.lyrics = fetched
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





@Preview(showBackground = true, showSystemUi = true, name = "MusicScreen (full) Preview", backgroundColor = 0xFF000000,
    device = "id:pixel_6"
)
@Composable
fun MusicPlayerScreenFullPreview() {
    MaterialTheme {
        val context = LocalContext.current
        val navController = remember { androidx.navigation.NavController(context) }
        // create a sample playlist
        val sampleSongs = listOf(
            // Use constructor (id, title, artist, duration, path, cover, album?) — omit year in preview
            Song(0, "First Song", "Artist A", 180000.0, "", null, null),
            Song(1, "Second Song", "Artist B", 210000.0, "", null, null),
            Song(2, "Third Song", "Artist C", 240000.0, "", null, null)
        )
        // create a plain VM instance for preview; methods may no-op but it's okay for preview
        val vm = remember { MusicPlayerViewModel() }

        // call the real MusicScreen with a sample start song id of 0
        MusicPlayerScreen(songId = 0, songs = sampleSongs, navController = navController, viewModel = vm)
    }
}


@Preview(showBackground = true, showSystemUi = true, name = "SongsModalBottomSheet - Collapsed", backgroundColor = 0xFF000000,
    device = "id:pixel_6"
)
@Composable
fun SongsModalBottomSheetPreview_Collapsed() {
    val sampleSongs = listOf(
        Song(0, "First Song", "Artist A", 180.0, "/storage/emulated/0/Music/first.mp3", null, null),
        Song(1, "Second Song", "Artist B", 200.0, "/storage/emulated/0/Music/second.mp3", null, null),
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


@Preview(showBackground = true, showSystemUi = true, name = "SongsModalBottomSheet - Expanded", backgroundColor = 0xFF000000,
    device = "id:pixel_6"
)
@Composable
fun SongsModalBottomSheetPreview_Expanded() {
    val sampleSongs = listOf(
        Song(0, "First Song", "Artist A", 180.0, "/storage/emulated/0/Music/first.mp3", null, null),
        Song(1, "Second Song", "Artist B", 200.0, "/storage/emulated/0/Music/second.mp3", null, null),
        Song(2, "Third Song", "Artist C", 240.0, "/storage/emulated/0/Music/third.mp3", null, null)
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
        Song(0, "First Song", "Artist A", 180000.0, "/storage/emulated/0/Music/first.mp3", null, null),
        Song(1, "Second Song", "Artist B", 200000.0, "/storage/emulated/0/Music/second.mp3", null, null),
        Song(2, "Third Song", "Artist C", 240000.0, "/storage/emulated/0/Music/third.mp3", null, null)
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
    // Create songs with the same album so related songs will be shown
    val sampleSongs = listOf(
        Song(0, "First Song", "Artist A", 180000.0, "/storage/emulated/0/Music/first.mp3", null, "Greatest Hits"),
        Song(1, "Second Song", "Artist A", 200000.0, "/storage/emulated/0/Music/second.mp3", null, "Greatest Hits"),
        Song(2, "Third Song", "Artist A", 240000.0, "/storage/emulated/0/Music/third.mp3", null, "Greatest Hits"),
        Song(3, "Fourth Song", "Artist A", 220000.0, "/storage/emulated/0/Music/fourth.mp3", null, "Greatest Hits"),
        Song(4, "Fifth Song", "Artist B", 190000.0, "/storage/emulated/0/Music/fifth.mp3", null, "Different Album")
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




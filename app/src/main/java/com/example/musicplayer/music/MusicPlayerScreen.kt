// kotlin
package com.example.musicplayer.music

import androidx.compose.material.rememberBottomSheetState
import androidx.compose.material.rememberBottomSheetScaffoldState
import com.example.musicplayer.preferences.PreferencesManager
import com.example.musicplayer.util.LibraryPreloadCache
import android.graphics.Bitmap
import kotlin.math.sqrt
import kotlin.math.abs
import android.annotation.SuppressLint
import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomSheetValue
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp as lerpRect
import androidx.compose.ui.unit.lerp as lerpUnit
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.musicplayer.model.Song
import com.example.musicplayer.util.Util
import com.example.musicplayer.service.PlayerStateManager
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.IconButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import com.example.musicplayer.ui.components.background.AuroraBackground
import com.example.musicplayer.ui.components.common.MusicControls
import com.example.musicplayer.ui.components.player.AlbumArt
import com.example.musicplayer.ui.components.player.InteractiveSeekBar
import com.example.musicplayer.ui.components.player.SongsSheetContent
import com.example.musicplayer.ui.components.song.MiniPlayerArtInset
import com.example.musicplayer.ui.components.song.MiniPlayerArtSize
import com.example.musicplayer.ui.components.playlist.AddToPlaylistDialog


// Lyrics are now cached on the Song instance (fields: lyrics, lyricsFetched). No global cache needed.

// The Up Next/Lyrics/Related sheet's three resting stops (see MusicPlayerChrome); not private since previewInitialDetent exposes this type across a public function signature.
enum class SheetDetent { Collapsed, Half, Full }

private val SheetEasing = CubicBezierEasing(0.22f, 0.9f, 0.24f, 1f)
private val SheetTween = tween<Float>(durationMillis = 420, easing = SheetEasing)

private val SHEET_COLLAPSED_HEIGHT = 87.dp
// The title/artist block's original fixed height; whatever it no longer uses goes below the controls instead.
private val TITLE_BLOCK_HEIGHT = 130.dp
private val SHEET_HALF_HEIGHT = 332.dp
// Fallback only, before the content area is measured; the real Full height is derived from the mini-row's position.
private val SHEET_FULL_HEIGHT = 750.dp

// Half/full-detent geometry for the shared album art (see MusicPlayerChrome's sheetProgress doc); the collapsed rect is measured at runtime instead since it must match the collapsed content column's own 340dp placeholder.
private val HALF_ART_HEIGHT = 448.dp
private val FULL_ART_SIZE = 58.dp
private val FULL_ART_CORNER_RADIUS = 8.dp
private val COLLAPSED_ART_CORNER_RADIUS = 14.dp

// The full Now Playing screen, mounted by PersistentPlayerHost only while expanded; the mini bar is a separate component (MiniPlayer).
@SuppressLint("ContextCastToActivity")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun MusicPlayerChrome(
    backgroundColor: Color,
    currentAlbumBitmap: Bitmap?,
    onCollapse: () -> Unit,
    // Drag-down-to-collapse on the top bar, mirroring the mini bar's own drag-up-to-expand in PersistentPlayerHost (which owns dockProgress); delta is raw pointer movement in px (positive = finger moved down).
    onCollapseDragDelta: (Float) -> Unit = {},
    onCollapseDragEnd: (velocity: Float) -> Unit = {},
    // Loaded once by the host and shared with the mini bar, so opening the player never reloads or crossfades it.
    artBitmap: ImageBitmap? = null,
    // Bumped by PersistentPlayerHost on every collapse-to-mini -- snaps the inner sheet back to peek immediately instead of co-fading fully expanded as dockProgress falls, which otherwise reads as "the sheet didn't close".
    collapseSignal: Int = 0,
    // PersistentPlayerHost's 0 (docked) -> 1 (full) progress; morphs the art from the mini bar thumbnail and fades everything else in.
    dockProgress: Float = 1f,
    viewModel: MusicPlayerViewModel = viewModel(),
    // @Preview-only: real callers always start at Collapsed and reach Half/Full via the grab handle; lets previews show each detent directly.
    previewInitialDetent: SheetDetent = SheetDetent.Collapsed
) {
    val ctx = LocalContext.current
    // Reads MainActivity's startup preload (LibraryPreloadCache) rather than an independent MediaStore re-scan, which used to block during this composable's first-expand mount and make the mini bar appear to hang; used only as the Related tab's candidate pool -- the actual queue is viewModel.playlist/PlayerStateManager.
    val songs: List<Song> by LibraryPreloadCache.songs.collectAsState()

    val currentIndex by viewModel.currentIndex.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val replayEnabled by viewModel.replayEnabled.collectAsState()
    val shuffleEnabled by viewModel.shuffleEnabled.collectAsState()
    val positionMs by viewModel.positionMs.collectAsState()
    val durationMs by viewModel.durationMs.collectAsState()

    // collect preference early so we can make Scaffold and system bars transparent when Aurora is enabled
    val useAuroraBackground by PreferencesManager.getUseAuroraBackgroundFlow(ctx).collectAsState(initial = false)

    val backgroundBrush = remember(backgroundColor) {
        Brush.verticalGradient(listOf(backgroundColor, Util.darkerColor(backgroundColor, 0.25f)))
    }

    val activity = LocalContext.current as? Activity

    // Prefer the repository playlist so the UI reflects actual playback state, falling back to `songs` if it's empty or missing the expected index.
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

    // Always transparent now -- the sampled-color gradient backdrop is painted once, behind the shared art, at the outer Box level, and an opaque container here would paint over that art since the Scaffold now sits in front of it.
    val scaffoldContainerColor = Color.Transparent

    // Contrast checkpoint for the top bar/title/artist text, animated (not snapped) so switching tracks crossfades the text color smoothly instead of popping at the 0.5 luminance threshold.
    val topOnBg by animateColorAsState(
        targetValue = if (backgroundColor.luminance() > 0.5f) Color.Black else Color.White,
        animationSpec = tween(durationMillis = 500),
        label = "Chrome text contrast"
    )

    // Draw aurora behind the entire UI (including TopAppBar); keep Scaffold as the primary layout
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    // sheetProgress: 0f (collapsed) / 0.5f (half) / 1f (full), continuous while dragging -- drives the sheet plus the shared album art/mini-row/collapsed content below, not just the sheet panel itself.
    val swipeableState = rememberSwipeableState(
        initialValue = previewInitialDetent,
        animationSpec = SheetTween
    )
    val collapsedAnchorPx = with(density) { SHEET_COLLAPSED_HEIGHT.toPx() }
    val halfAnchorPx = with(density) { SHEET_HALF_HEIGHT.toPx() }
    // Root-window bounds of this composable's outermost Box; the album art is positioned in this same coordinate space.
    var screenBoundsRoot by remember { mutableStateOf<Rect?>(null) }
    // Measured root-relative bounds of the collapsed content column's 340dp art slot and the mini-row's 58dp slot.
    var collapsedArtRect by remember { mutableStateOf<Rect?>(null) }
    var fullArtRect by remember { mutableStateOf<Rect?>(null) }
    // Height of the content area below the top bar (root px), so the title/seek/transport block can be bottom-pinned to the space left above the sheet instead of sitting in fixed flow where the sheet would grow into it.
    var contentBoundsRoot by remember { mutableStateOf<Rect?>(null) }
    // At Full the top bar collapses to just the status bar, so the mini-row sits at statusBar + 6dp; the sheet stops a small gap below it on any screen height.
    val statusBarTopPx = WindowInsets.statusBars.getTop(density)
    val fullAnchorPx = contentBoundsRoot?.let { content ->
        with(density) {
            val miniRowBottom = (screenBoundsRoot?.top ?: 0f) + statusBarTopPx + 6.dp.toPx() + FULL_ART_SIZE.toPx()
            content.bottom - miniRowBottom - 8.dp.toPx()
        }
    } ?: with(density) { SHEET_FULL_HEIGHT.toPx() }
    val sheetAnchors = mapOf(
        collapsedAnchorPx to SheetDetent.Collapsed,
        halfAnchorPx to SheetDetent.Half,
        fullAnchorPx to SheetDetent.Full
    )
    // Continuous 0f..1f across the whole collapsed->full span (0.5f exactly at Half), computed piecewise since the anchors aren't evenly spaced.
    val sheetOffsetPx = swipeableState.offset.value
    val sheetProgress = if (sheetOffsetPx <= halfAnchorPx) {
        0.5f * ((sheetOffsetPx - collapsedAnchorPx) / (halfAnchorPx - collapsedAnchorPx)).coerceIn(0f, 1f)
    } else {
        0.5f + 0.5f * ((sheetOffsetPx - halfAnchorPx) / (fullAnchorPx - halfAnchorPx)).coerceIn(0f, 1f)
    }

    // Top-edge dissolve ramps in over a short initial window so it's already on once the art starts growing.
    val artHandoffT = (sheetProgress / 0.08f).coerceIn(0f, 1f)

    // True as soon as a transition toward Half/Full is underway (keyed off the swipe's target, not sheetProgress) so the window's edge-to-edge property change gets a head start against the 420ms sheet animation instead of lagging visibly behind it.
    val edgeToEdgeForArt = swipeableState.targetValue != SheetDetent.Collapsed

    LaunchedEffect(backgroundColor, useAuroraBackground, edgeToEdgeForArt) {
        // Edge-to-edge whenever Aurora is enabled, or whenever this sheet's shared art has gone full-bleed and needs to extend under the status bar too.
        try {
            activity?.window?.let { win ->
                if (useAuroraBackground || edgeToEdgeForArt) {
                    try { WindowCompat.setDecorFitsSystemWindows(win, false) } catch (_: Throwable) {}
                    try { win.statusBarColor = android.graphics.Color.TRANSPARENT } catch (_: Throwable) {}
                    // Do not change navigation bar color here — keep system navigation bar color stable
                } else {
                    try { WindowCompat.setDecorFitsSystemWindows(win, true) } catch (_: Throwable) {}
                    try { win.statusBarColor = backgroundColor.toArgb() } catch (_: Throwable) {}
                    // Leave navigation bar color unchanged to respect system theming
                }

                val controller = WindowInsetsControllerCompat(win, win.decorView)
                // Decide light/dark icons based on the background color
                val light = backgroundColor.luminance() > 0.5f
                controller.isAppearanceLightStatusBars = light
                // Do not modify navigation bar icon appearance here to avoid changing system nav bar visuals
            }
        } catch (_: Throwable) {}
    }

    // Snap the sheet closed the instant the host starts collapsing, instead of letting it co-fade at full size.
    LaunchedEffect(collapseSignal) {
        if (collapseSignal > 0) {
            try { swipeableState.snapTo(SheetDetent.Collapsed) } catch (_: Throwable) {}
        }
    }

    // Tap only ever toggles Collapsed <-> Half; Half is never a drag target.
    fun stepSheetDetent() {
        val next = when (swipeableState.currentValue) {
            SheetDetent.Collapsed -> SheetDetent.Half
            SheetDetent.Half -> SheetDetent.Collapsed
            SheetDetent.Full -> SheetDetent.Half
        }
        scope.launch { try { swipeableState.animateTo(next) } catch (_: Throwable) {} }
    }
    // Drag settles to the nearest detent (Half included); a fast flick moves to the next detent in its direction.
    val sheetFlingThresholdPx = with(density) { 400.dp.toPx() }
    val sheetDragState = rememberDraggableState { delta -> swipeableState.performDrag(-delta) }
    // Velocity is px/s, positive = finger moving down.
    suspend fun settleSheet(velocity: Float) {
        val offset = swipeableState.offset.value
        val ordered = listOf(
            collapsedAnchorPx to SheetDetent.Collapsed,
            halfAnchorPx to SheetDetent.Half,
            fullAnchorPx to SheetDetent.Full
        )
        val target = when {
            velocity < -sheetFlingThresholdPx -> ordered.firstOrNull { it.first > offset + 1f }?.second ?: SheetDetent.Full
            velocity > sheetFlingThresholdPx -> ordered.lastOrNull { it.first < offset - 1f }?.second ?: SheetDetent.Collapsed
            else -> ordered.minByOrNull { abs(it.first - offset) }!!.second
        }
        try { swipeableState.animateTo(target) } catch (_: Throwable) {}
    }
    // Lets a list scrolled to its top keep dragging the sheet closed, instead of the drag dying at the list's edge.
    val sheetNestedScroll = remember(swipeableState, collapsedAnchorPx, halfAnchorPx, fullAnchorPx) {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput || available.y <= 0f) return Offset.Zero
                return Offset(0f, -swipeableState.performDrag(-available.y))
            }
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Once the sheet is mid-close, an upward drag re-opens it before the list starts scrolling again.
                if (source != NestedScrollSource.UserInput || available.y >= 0f) return Offset.Zero
                if (swipeableState.offset.value >= swipeableState.targetValue.let { sheetAnchors.entries.first { e -> e.value == it }.key } - 1f) return Offset.Zero
                return Offset(0f, -swipeableState.performDrag(-available.y))
            }
            override suspend fun onPreFling(available: Velocity): Velocity {
                val offset = swipeableState.offset.value
                val atAnchor = sheetAnchors.keys.any { abs(it - offset) < 1f }
                if (atAnchor) return Velocity.Zero
                settleSheet(available.y)
                return available
            }
        }
    }
    // Whole-sheet drag (handle, tabs, non-scrolling tab content); scrolling lists hand off through sheetNestedScroll instead.
    val sheetDragModifier = Modifier
        .nestedScroll(sheetNestedScroll)
        .draggable(
            state = sheetDragState,
            orientation = Orientation.Vertical,
            onDragStopped = { velocity -> settleSheet(velocity) }
        )
    val grabHandleModifier = Modifier
        // enabled = false: swipeable only supplies anchors/offset here, sheetDragModifier owns the gesture.
        .swipeable(
            state = swipeableState,
            anchors = sheetAnchors,
            orientation = Orientation.Vertical,
            enabled = false,
            reverseDirection = true
        )
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) { stepSheetDetent() }

    // One shared drag state for every drag-to-minimize surface (top bar, collapsed art area).
    val currentOnCollapseDragDelta by rememberUpdatedState(onCollapseDragDelta)
    val currentOnCollapseDragEnd by rememberUpdatedState(onCollapseDragEnd)
    val collapseDragState = rememberDraggableState { currentOnCollapseDragDelta(it) }


    // Bottom edge pinned to exactly where the sheet's top edge sits at Half (not the design's literal 448dp, sized for its own 780dp mockup) so they line up on taller real devices and the art's dissolve-to-background fade is visible right at the seam instead of hidden behind the sheet; falls back to 448dp only before contentBoundsRoot is measured.
    val halfArtRect = screenBoundsRoot?.let { screen ->
        val content = contentBoundsRoot
        val bottom = if (content != null) {
            content.bottom - halfAnchorPx
        } else {
            screen.top + with(density) { HALF_ART_HEIGHT.toPx() }
        }
        Rect(screen.left, screen.top, screen.right, bottom)
    }

    // Piecewise lerp across three keyframes; fullArtRect isn't measured until the mini-row has mounted once, which shouldn't block the [0, 0.5] sub-range.
    val innerArtRect = if (sheetProgress <= 0.5f) {
        val c = collapsedArtRect
        val h = halfArtRect
        if (c != null && h != null) lerpRect(c, h, sheetProgress / 0.5f) else null
    } else {
        val h = halfArtRect
        val f = fullArtRect
        if (h != null && f != null) lerpRect(h, f, (sheetProgress - 0.5f) / 0.5f) else h
    }
    val innerArtCornerRadius = if (sheetProgress <= 0.5f) {
        lerpUnit(COLLAPSED_ART_CORNER_RADIUS, 0.dp, sheetProgress / 0.5f)
    } else {
        lerpUnit(0.dp, FULL_ART_CORNER_RADIUS, (sheetProgress - 0.5f) / 0.5f)
    }

    // Docked endpoint is the mini bar's thumbnail, which rides the top edge of the sliding panel.
    val dockedArtRect = with(density) {
        val origin = screenBoundsRoot?.topLeft ?: Offset.Zero
        val left = origin.x + MiniPlayerArtInset.x.toPx()
        val top = origin.y + MiniPlayerArtInset.y.toPx()
        Rect(left, top, left + MiniPlayerArtSize.toPx(), top + MiniPlayerArtSize.toPx())
    }
    // Before the detent rects are measured (first frame after mounting), hold the art on the thumbnail rather than dropping it.
    val artRect = when {
        // Eased (p^1.5) so the art is already on the thumbnail spot by the time the mini bar row fades back in.
        dockProgress < 1f && innerArtRect != null -> lerpRect(dockedArtRect, innerArtRect, dockProgress * sqrt(dockProgress))
        innerArtRect != null -> innerArtRect
        else -> dockedArtRect
    }
    val artCornerRadius = lerpUnit(8.dp, innerArtCornerRadius, dockProgress)
    // Starts late so the top bar and text never fade in over the still-small art.
    val dockContentAlpha = ((dockProgress - 0.35f) / 0.5f).coerceIn(0f, 1f)
    // Top bar waits until the art has mostly cleared it; the seek bar is the very last thing to appear.
    val dockTopBarAlpha = ((dockProgress - 0.6f) / 0.4f).coerceIn(0f, 1f)
    val dockSeekAlpha = ((dockProgress - 0.85f) / 0.15f).coerceIn(0f, 1f)

    // Collapsed content stays fully visible across [0, 0.5] (half doesn't drop it, matching the design) and only fades (no resizing) across [0.5, 1]; doesn't collide with the sheet since it's bottom-pinned within the shrinking space above it (availableAboveSheetPx) rather than fixed flow.
    val collapsedChromeAlpha = 1f - ((sheetProgress - 0.5f) / 0.5f).coerceIn(0f, 1f)
    // Space left above the sheet's current top edge (px); the title/seek/transport block is bottom-aligned within a Box of this height so it grows/shrinks with the sheet instead of a fixed position the sheet can grow into.
    val availableAboveSheetPx = ((contentBoundsRoot?.height ?: 0f) - sheetOffsetPx).coerceAtLeast(0f)
    // How far across [0, 0.5] for title/artist/time text-size interpolation -- the one exception to "only art resizes" (buttons/icons stay fixed).
    val halfLerpT = (sheetProgress / 0.5f).coerceIn(0f, 1f)
    // Mini-row (compact art-slot + title + its own play button) fades in only past half.
    val miniRowAlpha = ((sheetProgress - 0.5f) / 0.5f).coerceIn(0f, 1f)
    // Top bar fades out across the first half of the drag so it's fully hidden by Half -- once the art reaches under the status bar the bar's own row reads as redundant clutter over it.
    val topBarAlpha = 1f - (sheetProgress / 0.5f).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { screenBoundsRoot = it.boundsInRoot() }
    ) {
        if (useAuroraBackground) {
            AuroraBackground(modifier = Modifier.fillMaxSize().alpha(dockContentAlpha), albumCoverBitmap = currentAlbumBitmap)
        } else {
            // Sampled-color backdrop moved here (behind the art) rather than on the Scaffold's content Box, which sits in front of the art and would otherwise paint over it.
            Box(modifier = Modifier.fillMaxSize().alpha(dockContentAlpha).background(backgroundBrush))
        }

        // Chrome's own shared/morphing album art -- the one element that moves/resizes across detents (everything else only fades) -- drawn behind the Scaffold so its content renders on top, reaching up through the header since topBar has a transparent container color.
        if (song != null) {
            // Real alpha fade (BlendMode.DstIn against a gradient, not a painted-on overlay) so the art's edges genuinely reveal backgroundBrush behind them; off at Collapsed, top dissolves via artHandoffT and bottom via halfLerpT as the sheet opens, and fadePresence triangles back to 0 by Full so it doesn't stay applied on the tiny mini-row art -- fade is only visible around Half, matching the design.
            val fadePresence = (1f - abs(sheetProgress - 0.5f) * 2f).coerceIn(0f, 1f)
            val topFadeAlpha = 1f - artHandoffT * fadePresence
            val bottomFadeAlpha = 1f - halfLerpT * fadePresence
            // Peak opacity at 35%, fully transparent by 80% -- more gradual than the previous 30%/65% split, still resolving before the seek bar.
            val artFadeBrush = Brush.verticalGradient(
                colorStops = arrayOf(
                    0.00f to Color.Black.copy(alpha = topFadeAlpha),
                    0.35f to Color.Black,
                    0.80f to Color.Black.copy(alpha = bottomFadeAlpha)
                )
            )
            Box(
                modifier = Modifier
                    .offset { IntOffset(artRect.left.roundToInt(), artRect.top.roundToInt()) }
                    .size(
                        with(density) { artRect.width.toDp() },
                        with(density) { artRect.height.toDp() }
                    )
                    .clip(RoundedCornerShape(artCornerRadius))
            ) {
                AlbumArt(
                    bitmap = artBitmap,
                    // CompositingStrategy.Offscreen is required for BlendMode.DstIn to blend against just this image's pixels instead of the whole canvas behind it.
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            drawRect(brush = artFadeBrush, blendMode = BlendMode.DstIn)
                        }
                )
            }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize().alpha(dockContentAlpha),
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
                        IconButton(onClick = onCollapse) {
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowDown,
                                contentDescription = "Close player",
                                tint = topOnBg
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .statusBarsPadding()
                        .alpha(topBarAlpha * dockTopBarAlpha)
                        // Shrinks the *reported* layout height in lockstep with topBarAlpha (measured at normal size but reported as only that fraction tall) so Scaffold's innerPadding -- and the mini-row's position -- shrinks smoothly to 0 by Half instead of snapping in one frame.
                        .layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints)
                            val height = (placeable.height * topBarAlpha).roundToInt()
                            layout(placeable.width, height) { placeable.placeRelative(0, 0) }
                        }
                        .draggable(
                            state = collapseDragState,
                            orientation = Orientation.Vertical,
                            onDragStopped = { velocity -> currentOnCollapseDragEnd(velocity) }
                        ),
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .onGloballyPositioned { contentBoundsRoot = it.boundsInRoot() }
            ) {
                if (song != null) {
                    // Invisible slot the album art measures its collapsed-detent rect from; fixed flow so it stays put regardless of the sheet's height.
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Spacer(
                            modifier = Modifier
                                .size(340.dp)
                                .onGloballyPositioned { collapsedArtRect = it.boundsInRoot() }
                                // Big drag-to-minimize target; off once the sheet opens so it can't fight the sheet's own swipe.
                                .draggable(
                                    state = collapseDragState,
                                    orientation = Orientation.Vertical,
                                    enabled = swipeableState.currentValue == SheetDetent.Collapsed,
                                    onDragStopped = { velocity -> currentOnCollapseDragEnd(velocity) }
                                )
                        )
                    }

                    // Title/artist/seek/full transport, bottom-pinned within the space left above the sheet (availableAboveSheetPx) so it tracks the sheet's growth instead of sitting in fixed flow -- mirrors the design's own margin-top:auto push.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(with(density) { availableAboveSheetPx.toDp() }),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                    // The art's own bottom-edge dissolve (drawWithContent + BlendMode.DstIn above) now provides the darkening this block used to paint separately, sitting directly behind and sharing the same bottom edge, so no separate scrim is needed here.
                    var titleBlockHeightPx by remember { mutableIntStateOf(0) }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        // Wraps its content so title/artist/seek/controls sit tight; the title keeps its old 18dp top inset and the saved height moves below the controls, so the art-to-title gap is unchanged.
                        Column(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .width(340.dp)
                                // Before the padding so the measured height includes it, matching how the old fixed 130dp block was sized.
                                .onSizeChanged { titleBlockHeightPx = it.height }
                                .alpha(collapsedChromeAlpha)
                                .padding(horizontal = 10.dp)
                                .padding(top = 18.dp, bottom = 10.dp)
                        ) {
                            Text(
                                text = song.title,
                                color = topOnBg,
                                fontWeight = FontWeight.Bold,
                                fontSize = lerpUnit(24.sp, 22.sp, halfLerpT),
                                lineHeight = 30.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                modifier = Modifier
                                    .width(340.dp)
                                    .padding(horizontal = 8.dp)
                                    .basicMarquee(
                                        iterations = Int.MAX_VALUE,
                                        initialDelayMillis = 2000,
                                        spacing = MarqueeSpacing(50.dp)
                                    )
                            )
                            Text(
                                text = song.artist.replace("\n", ", "),
                                color = topOnBg.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                fontSize = lerpUnit(14.sp, 13.sp, halfLerpT),
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                modifier = Modifier
                                    .padding(horizontal = 10.dp)
                                    .padding(top = 2.dp)
                                    .width(340.dp)
                                    .basicMarquee(
                                        iterations = Int.MAX_VALUE,
                                        initialDelayMillis = 2000,
                                        spacing = MarqueeSpacing(50.dp)
                                    )
                            )
                        }

                        val effectiveDuration = if (durationMs > 0L) durationMs.toFloat() else song.duration.toFloat()
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.alpha(collapsedChromeAlpha * dockSeekAlpha)
                        ) {
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
                                    fontSize = lerpUnit(12.sp, 11.5.sp, halfLerpT),
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = Util.converter(effectiveDuration.toDouble()),
                                    color = Color.White,
                                    textAlign = TextAlign.End,
                                    fontSize = lerpUnit(12.sp, 11.5.sp, halfLerpT),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Box(modifier = Modifier.alpha(collapsedChromeAlpha)) {
                            MusicControls(
                                isPlaying = isPlaying,
                                replayEnabled = replayEnabled,
                                shuffleEnabled = shuffleEnabled,
                                onPlayPause = { viewModel.togglePlayPause(ctx) },
                                onNext = { viewModel.next(ctx) },
                                onPrev = { viewModel.previous(ctx) },
                                onReplayToggle = { viewModel.toggleReplay() },
                                onShuffleToggle = { enabled -> viewModel.toggleShuffle(enabled) }
                            )
                        }
                        // Collapses to nothing as the sheet opens, so at Half the controls sit right on the sheet.
                        Spacer(Modifier.height((TITLE_BLOCK_HEIGHT - with(density) { titleBlockHeightPx.toDp() }).coerceAtLeast(0.dp) * (1f - halfLerpT)))
                    }
                    }

                    // Compact mini-row -- art-slot placeholder (real art is the shared element via innerArtRect) + title/artist + its own fixed-size play button, fading in only past half per the "only art moves/resizes" rule; always composed (not gated on miniRowAlpha > 0) so its art-slot placeholder is measured into fullArtRect before sheetProgress crosses 0.5 and needs it.
                    run {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(miniRowAlpha)
                                .padding(horizontal = 22.dp)
                                .padding(top = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Spacer(
                                modifier = Modifier
                                    .size(FULL_ART_SIZE)
                                    .onGloballyPositioned { fullArtRect = it.boundsInRoot() }
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 14.dp)
                            ) {
                                Text(
                                    text = song.title,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = song.artist,
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            // Second, independent play button -- fixed 42dp, never resized/moved, only its alpha (via the parent Row) changes; see MusicControls' own button above for the collapsed/half one.
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(21.dp))
                                    .background(Color.White)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { viewModel.togglePlayPause(ctx) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    modifier = Modifier.size(24.dp),
                                    tint = Color(0xFF111111)
                                )
                            }
                        }
                    }
                }

                // Sheet panel -- height driven directly by the same swipeableState offset that also drives sheetProgress, so the panel and every other progress-driven element stay in sync.
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(with(density) { sheetOffsetPx.toDp() })
                        .then(sheetDragModifier)
                ) {
                    SongsSheetContent(
                        songs = activeSongs,
                        currentIndex = currentIndex,
                        backgroundColor = backgroundColor,
                        onSelect = { idx ->
                            viewModel.setPlaylist(ctx, activeSongs, idx)
                            viewModel.play(ctx)
                            scope.launch { try { swipeableState.animateTo(SheetDetent.Collapsed) } catch (_: Throwable) {} }
                        },
                        onTabSelected = {
                            if (swipeableState.currentValue == SheetDetent.Collapsed) {
                                scope.launch { try { swipeableState.animateTo(SheetDetent.Half) } catch (_: Throwable) {} }
                            }
                        },
                        grabHandleModifier = grabHandleModifier,
                        showIndicator = swipeableState.currentValue != SheetDetent.Collapsed,
                        openFraction = sheetProgress,
                        useAuroraBackground = useAuroraBackground
                    )
                }
            }
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

// Static sample playlist so the previews below render standalone.
private val chromePreviewSongs = listOf(
    Song(1, "Afterglow", "Nova Reyes", 238000.0, "", "Neon Parallels"),
    Song(2, "Dust & Gold", "Marla Quinn", 195000.0, "", "Dust & Gold")
)

@Composable
private fun MusicPlayerChromePreview(detent: SheetDetent) {
    MaterialTheme {
        // remember (not LaunchedEffect) so the seed runs synchronously during composition -- a static preview snapshot renders before a LaunchedEffect's coroutine body gets a chance to run, leaving playlist empty and song null on first render.
        remember {
            PlayerStateManager.setPlaylist(chromePreviewSongs, 0)
            PlayerStateManager.setIsPlaying(true)
        }
        MusicPlayerChrome(
            backgroundColor = Color(0xFF8A6D1F),
            currentAlbumBitmap = null,
            onCollapse = {},
            previewInitialDetent = detent
        )
    }
}

@Preview(showSystemUi = true, name = "MusicPlayerChrome — Collapsed", backgroundColor = 0xFF000000, showBackground = true)
@Composable
private fun MusicPlayerChromeCollapsedPreview() {
    MusicPlayerChromePreview(SheetDetent.Collapsed)
}

@Preview(showSystemUi = true, name = "MusicPlayerChrome — Half open", backgroundColor = 0xFF000000, showBackground = true)
@Composable
private fun MusicPlayerChromeHalfPreview() {
    MusicPlayerChromePreview(SheetDetent.Half)
}

@Preview(showSystemUi = true, name = "MusicPlayerChrome — Full open", backgroundColor = 0xFF000000, showBackground = true)
@Composable
private fun MusicPlayerChromeFullPreview() {
    MusicPlayerChromePreview(SheetDetent.Full)
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
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberBottomSheetState(
            initialValue = if (visible) BottomSheetValue.Expanded else BottomSheetValue.Collapsed
        )
    )

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = currentIndex.coerceAtLeast(0))
    val scope = rememberCoroutineScope()

    LaunchedEffect(scaffoldState.bottomSheetState.currentValue) {
        if (scaffoldState.bottomSheetState.currentValue == BottomSheetValue.Collapsed && visible) onDismiss()
        if (scaffoldState.bottomSheetState.currentValue == BottomSheetValue.Expanded && songs.isNotEmpty()) {
            listState.animateScrollToItem(currentIndex.coerceIn(0, songs.size - 1))
        }
    }
    LaunchedEffect(visible) { if (visible) scaffoldState.bottomSheetState.expand() else scaffoldState.bottomSheetState.collapse() }

    val sheetBg = backgroundColor
    var selectedTab by remember { mutableStateOf(initialSelectedTab) }
    var related by remember { mutableStateOf<List<Pair<Int, Song>>>(emptyList()) }

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
                onTabSelected = { scope.launch { try { scaffoldState.bottomSheetState.expand() } catch (_: Throwable) {} } },
                showIndicator = (scaffoldState.bottomSheetState.currentValue == BottomSheetValue.Expanded),
                openFraction = if (scaffoldState.bottomSheetState.currentValue == BottomSheetValue.Expanded) 1f else 0f
            )
        }
    ) {
        Spacer(modifier = Modifier.height(0.dp))
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




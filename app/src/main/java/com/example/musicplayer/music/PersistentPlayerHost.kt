package com.example.musicplayer.music

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.lerp as lerpRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.musicplayer.ui.components.player.AlbumImage
import com.example.musicplayer.service.PlayerDockController
import com.example.musicplayer.service.PlayerStateManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val DOCK_ANIM_MS = 900
// Matches the Aura 2.0 design's own dock/sheet transition curve (gentle decelerate) rather than Compose's default FastOutSlowIn, which reads clipped/mechanical for this gesture-driven transition.
private val DockEasing = CubicBezierEasing(0.22f, 0.9f, 0.24f, 1f)
private val DockTween = tween<Float>(durationMillis = DOCK_ANIM_MS, easing = DockEasing)
// How much vertical drag (dp) fully expands/collapses the mini bar; widened from 260dp, which let a short drag complete the whole gesture via live tracking alone, leaving nothing for the release animation to animate.
private const val DRAG_RANGE_DP = 320f

// Mounted once as a sibling of the NavHost so it's available on every tab; renders nothing when nothing is playing, otherwise docks as mini bar or full player (MusicPlayerChrome) driven by a single dockProgress value, with the album art as the one persistent element that morphs position/size while everything else fades; replaces the old navigate(MusicPlayer)/popBackStack() flow -- callers now call PlayerDockController.requestExpand() instead.
@Composable
fun PersistentPlayerHost(
    modifier: Modifier = Modifier,
    // @Preview-only; real callers always start docked and expand via PlayerDockController.requestExpand().
    initialDockProgress: Float = 0f,
    // @Preview-only: bypasses the "stay hidden until first expand has fully loaded" gate below, which has nothing to wait on outside a real app session.
    previewSkipRevealGate: Boolean = false,
    // True on routes (Settings) that shouldn't show the dock; skips rendering only -- stays mounted so state/measured rects survive the visit and the mini bar reappears instantly on return.
    hideContent: Boolean = false
) {
    val playlist by PlayerStateManager.playlist.collectAsState()
    if (playlist.isEmpty()) return

    val density = LocalDensity.current
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel: MusicPlayerViewModel = viewModel()

    val currentIndex by PlayerStateManager.currentIndex.collectAsState()
    val isPlaying by PlayerStateManager.isPlaying.collectAsState()
    val current = playlist.getOrNull(currentIndex)

    val dockProgress = remember { Animatable(initialDockProgress) }
    // Plain synchronous accumulator, avoids racing the async snapTo below.
    var rawDragFraction by remember { mutableStateOf(initialDockProgress) }

    // Endpoint geometry for the shared album art (root-window px), measured from invisible same-size placeholders in each layer; cached at host level (not per-layer) so it survives layer unmount -- only the very first expand pays a one-frame measurement lag, every expand/collapse after reuses the last known bounds.
    var miniArtBounds by remember { mutableStateOf<Rect?>(null) }
    var fullArtBounds by remember { mutableStateOf<Rect?>(null) }

    // Nothing renders -- not even the mini bar -- until the first expand of the session has fully loaded (Chrome mounted/measured); otherwise tapping the first song would flash the mini bar before popping to full player a beat later, so this routes that first transition straight to full player instead, matching every expand after it.
    var everRevealed by remember { mutableStateOf(previewSkipRevealGate) }

    LaunchedEffect(Unit) {
        PlayerDockController.expandRequests.collect { count ->
            if (count <= 0) return@collect
            if (!everRevealed) {
                // First expand of the session: warm up the measurement at an imperceptible (but nonzero, so Chrome mounts) progress while hidden, then reveal already at full player -- no animated climb from mini, no mini bar shown for this transition.
                dockProgress.snapTo(0.002f)
                snapshotFlow { fullArtBounds != null }.first { it }
                dockProgress.snapTo(1f)
                everRevealed = true
            } else {
                dockProgress.animateTo(1f, DockTween)
            }
            rawDragFraction = 1f
        }
    }

    // Bumped on every collapse-to-mini (chevron, drag-down, back) -- MusicPlayerChrome observes this to snap its own inner sheet back to peek immediately instead of co-fading it fully expanded as dockProgress falls.
    var collapseSheetSignal by remember { mutableStateOf(0) }
    fun collapseToMini() {
        collapseSheetSignal++
        rawDragFraction = 0f
        scope.launch { dockProgress.animateTo(0f, DockTween) }
    }

    BackHandler(enabled = dockProgress.value > 0.5f) {
        collapseToMini()
    }

    val dragRangePx = with(density) { DRAG_RANGE_DP.dp.toPx() }
    // Tracks the finger 1:1 across the full 0..1 range (no artificial clamp band, which used to cause a jump at drag start).
    fun onDockDrag(pxDelta: Float) {
        val delta = -pxDelta / dragRangePx
        rawDragFraction = (rawDragFraction + delta).coerceIn(0f, 1f)
        scope.launch { dockProgress.snapTo(rawDragFraction) }
    }
    fun onDockDragEnd() {
        val target = if (rawDragFraction > 0.5f) 1f else 0f
        if (target == 0f) collapseSheetSignal++
        rawDragFraction = target
        scope.launch { dockProgress.animateTo(target, DockTween) }
    }
    // Shared by the mini bar and the album art overlay; two independent detectors so a plain tap (never exceeds slop, so detectVerticalDragGestures never even calls onDragEnd) reliably reaches [onTap].
    fun Modifier.dockDragGesture(onTap: () -> Unit): Modifier = this
        .pointerInput(Unit) { detectTapGestures(onTap = { onTap() }) }
        .pointerInput(Unit) {
            detectVerticalDragGestures(
                onVerticalDrag = { change, amount -> change.consume(); onDockDrag(amount) },
                onDragEnd = { onDockDragEnd() }
            )
        }

    // Background color sampled from the current album art's palette, same mechanism the old MusicPlayerScreen used -- now owned here since AlbumImage itself is shared.
    var targetBackgroundColor by remember { mutableStateOf(Color.Black) }
    var currentAlbumBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    val backgroundColor by animateColorAsState(
        targetValue = targetBackgroundColor,
        animationSpec = tween(durationMillis = 800),
        label = "Dock background color"
    )

    val artRect = when {
        miniArtBounds != null && fullArtBounds != null -> lerpRect(miniArtBounds!!, fullArtBounds!!, dockProgress.value)
        else -> miniArtBounds ?: fullArtBounds
    }

    // Chrome's Up Next/Lyrics/Related sheet fades its title/seek/transport as it opens (underlayAlpha) so its translucent background doesn't bleed them through; the shared album art is drawn on top by this host, so it needs the same fade applied here -- multiplied by dockProgress (not used alone) so collapsing back to mini with the sheet left open doesn't hide the art the whole way down, only while still deep in full-player mode.
    var innerSheetOpenFraction by remember { mutableStateOf(0f) }
    val artAlpha = 1f - (innerSheetOpenFraction * dockProgress.value)

    if (!hideContent) {
    Box(modifier = modifier.fillMaxSize()) {
        // Full player layer -- only mounted (and able to intercept touches) while at least slightly expanded, so it doesn't eat taps meant for the screen underneath while docked as mini bar; deliberately NOT gated on everRevealed since the pre-warm phase needs exactly this mounted (invisibly, at ~0.002) to measure itself, or the reveal would deadlock forever.
        if (dockProgress.value > 0.001f) {
            Box(Modifier.fillMaxSize().alpha(dockProgress.value)) {
                MusicPlayerChrome(
                    backgroundColor = backgroundColor,
                    currentAlbumBitmap = currentAlbumBitmap,
                    onCollapse = ::collapseToMini,
                    onArtBoundsChanged = { fullArtBounds = it.boundsInRoot() },
                    onCollapseDragDelta = ::onDockDrag,
                    onCollapseDragEnd = ::onDockDragEnd,
                    onSheetOpenFractionChanged = { innerSheetOpenFraction = it },
                    collapseSignal = collapseSheetSignal,
                    viewModel = viewModel
                )
            }
        }

        // Mini bar layer -- symmetrically only mounted while at least slightly docked, so it doesn't intercept touches meant for the full player's sheet once expanded (both layers overlap at the bottom); also gated on everRevealed so it doesn't flash mini before the first expand pops to full player.
        if (everRevealed && dockProgress.value < 0.999f) {
            // Positioning/reservation only -- no pointerInput/background of its own, so the empty space it reserves above BottomNav doesn't swallow taps meant for BottomNav; only the inner Column is interactive.
            Box(
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.BottomCenter)
                    .fillMaxWidth()
                    // Songs/Radio/Playlists each render their own BottomNav as that screen's Scaffold bottomBar, which this host (a NavHost sibling) can't directly measure, so reserve BottomNav's known ~56dp here instead of sitting the mini bar underneath it; screens without a BottomNav just get a small cosmetic gap above the system nav bar instead.
                    .navigationBarsPadding()
                    .padding(bottom = 56.dp)
            ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(1f - dockProgress.value)
                    // Unlike the old in-flow MiniPlayer (which pushed the list up via weight(1f)), this bar floats over the screen without reserving space, so it needs to be near-opaque to keep the list's last row from bleeding through.
                    .background(Color.Black.copy(alpha = 0.94f))
                    // Reports this bar's real measured height so every screen's list (miniPlayerBottomPadding()) reserves exactly this much space instead of a hand-picked guess that can drift out of sync.
                    .onGloballyPositioned { PlayerDockController.reportMiniBarHeightPx(it.size.height.toFloat()) }
                    // No plain clickable() here -- it would eat drag-up before dockDragGesture sees it.
                    .dockDragGesture { PlayerDockController.requestExpand() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .height(44.dp),
                    // Deliberately no tap-to-expand on this Row: a clickable would consume the pointer-down before the parent Column's detectVerticalDragGestures sees it, breaking drag-up-to-expand; the shared album art (separate composable) still handles tap-to-expand.
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    // Invisible placeholder reserving the art's spot -- the real, visible copy is the shared element drawn on top of this whole Box tree, morphing between here and the full player.
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .onGloballyPositioned { miniArtBounds = it.boundsInRoot() }
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp, end = 8.dp)
                    ) {
                        Text(
                            text = current?.title ?: "",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = current?.artist ?: "",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White)
                            .clickable { viewModel.togglePlayPause(ctx) },
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(24.dp),
                            tint = Color(0xFF111111)
                        )
                    }
                }

                // Thin progress bar matching the old MiniPlayer, orange fill on a dim track.
                val positionMs by PlayerStateManager.positionMs.collectAsState()
                val durationMs by PlayerStateManager.durationMs.collectAsState()
                val progress = if (durationMs > 0L) {
                    (positionMs.coerceAtMost(durationMs).toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                } else 0f
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = progress)
                            .background(Color(0xFFFFA500))
                    )
                }
                Box(Modifier.height(6.dp))
            }
            }
        }

        // The album art is the one persistent element across both states, morphing position/size via dockProgress rather than cross-fading; everything else fades with its own layer instead; also gated on everRevealed so it doesn't appear alone during the first expand's pre-warm phase.
        if (everRevealed && current != null && artRect != null) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(artRect.left.roundToInt(), artRect.top.roundToInt()) }
                    .size(
                        with(density) { artRect.width.toDp() },
                        with(density) { artRect.height.toDp() }
                    )
                    .alpha(artAlpha)
                    // Drag-enabled like the rest of the mini bar; omitted entirely once invisible so touches pass through.
                    .then(
                        if (artAlpha > 0.05f) {
                            Modifier.dockDragGesture { PlayerDockController.requestExpand() }
                        } else Modifier
                    )
            ) {
                AlbumImage(
                    song = current,
                    modifier = Modifier.fillMaxSize(),
                    onDominantColor = { targetBackgroundColor = it },
                    onBitmap = { currentAlbumBitmap = it }
                )
            }
        }
    }
    }
}

// Sample playlist shared by both previews below -- static data so they render standalone, matching SongListScreen's own preview pattern.
private val previewSongs = listOf(
    com.example.musicplayer.model.Song(1, "Afterglow", "Nova Reyes", 238000.0, "", "Neon Parallels"),
    com.example.musicplayer.model.Song(2, "Dust & Gold", "Marla Quinn", 195000.0, "", "Dust & Gold")
)

@Preview(showSystemUi = true, name = "PersistentPlayerHost — Mini docked", backgroundColor = 0xFF000000, showBackground = true)
@Composable
private fun PersistentPlayerHostMiniPreview() {
    androidx.compose.material3.MaterialTheme {
        // remember (not LaunchedEffect) so the seed runs synchronously during composition, before this host's own `if (playlist.isEmpty()) return` check.
        remember {
            PlayerStateManager.setPlaylist(previewSongs, 0)
            PlayerStateManager.setIsPlaying(true)
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF14261C))
        ) {
            PersistentPlayerHost(previewSkipRevealGate = true)
        }
    }
}

@Preview(showSystemUi = true, name = "PersistentPlayerHost — Full expanded", backgroundColor = 0xFF000000, showBackground = true)
@Composable
private fun PersistentPlayerHostFullPreview() {
    androidx.compose.material3.MaterialTheme {
        // remember (not LaunchedEffect) so the seed runs synchronously during composition, before this host's own `if (playlist.isEmpty()) return` check.
        remember {
            PlayerStateManager.setPlaylist(previewSongs, 0)
            PlayerStateManager.setIsPlaying(true)
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            PersistentPlayerHost(initialDockProgress = 1f, previewSkipRevealGate = true)
        }
    }
}

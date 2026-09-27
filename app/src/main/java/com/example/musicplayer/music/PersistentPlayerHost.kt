package com.example.musicplayer.music

import androidx.compose.material3.MaterialTheme
import com.example.musicplayer.model.Song
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.musicplayer.service.PlayerDockController
import com.example.musicplayer.service.PlayerStateManager
import com.example.musicplayer.ui.components.player.albumPaletteColors
import com.example.musicplayer.ui.components.player.rememberAlbumBitmap
import com.example.musicplayer.ui.components.song.MiniPlayer
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val DOCK_ANIM_MS = 750
// Standard ease-in-out spreads the motion over the full duration; the old (0.22, 0.9, 0.24, 1) curve did ~90% of it in the first third.
private val DockEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
private val DockTween = tween<Float>(durationMillis = DOCK_ANIM_MS, easing = DockEasing)
private const val FLING_VELOCITY_DP = 400f

// The tap animation continued from the point where it would already be at `covered` (0..1 of the way to the target), so a flick keeps its speed instead of restarting the curve from rest.
private fun remainingDockTween(covered: Float): TweenSpec<Float> {
    val start = covered.coerceIn(0f, 1f)
    if (start >= 0.999f) return tween(durationMillis = 0)
    var lo = 0f
    var hi = 1f
    repeat(24) {
        val mid = (lo + hi) / 2f
        if (DockEasing.transform(mid) < start) lo = mid else hi = mid
    }
    val t0 = lo
    val eased0 = DockEasing.transform(t0)
    return tween(
        durationMillis = (DOCK_ANIM_MS * (1f - t0)).roundToInt().coerceAtLeast(1),
        easing = Easing { x -> ((DockEasing.transform(t0 + x * (1f - t0)) - eased0) / (1f - eased0)).coerceIn(0f, 1f) }
    )
}
// Used only before the mini bar has been measured once (first expand of a session).
private val MINI_FALLBACK_HEIGHT = 65.dp
// Matches MiniPlayer's own background so the panel's top edge reads as the bar itself at rest.
private val MiniBarColor = Color.Black

// YouTube Music-style dock: the full player is one panel sliding up from the mini bar, the mini bar riding its top edge and fading while only the album art morphs.
@Composable
fun PersistentPlayerHost(
    modifier: Modifier = Modifier,
    // @Preview-only; real callers start docked and expand via PlayerDockController.requestExpand().
    initialDockProgress: Float = 0f,
    // True on routes that shouldn't show the player; stays mounted so state survives and it reappears instantly.
    hideContent: Boolean = false
) {
    val playlist by PlayerStateManager.playlist.collectAsState()
    if (playlist.isEmpty()) return

    val density = LocalDensity.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel: MusicPlayerViewModel = viewModel()

    val currentIndex by PlayerStateManager.currentIndex.collectAsState()
    val isPlaying by PlayerStateManager.isPlaying.collectAsState()
    val positionMs by PlayerStateManager.positionMs.collectAsState()
    val durationMs by PlayerStateManager.durationMs.collectAsState()
    val current = playlist.getOrNull(currentIndex)

    // 0f = docked mini bar, 1f = full player.
    val dockProgress = remember { Animatable(initialDockProgress) }
    // Written straight from each touch event while dragging (no per-event coroutine), so the panel tracks the finger on the same frame.
    var dragFraction by remember { mutableFloatStateOf(initialDockProgress) }

    // Root-space bounds; the host isn't transformed, so subtracting its origin gives host-local coords.
    var hostBoundsRoot by remember { mutableStateOf<Rect?>(null) }
    var miniBoundsRoot by remember { mutableStateOf<Rect?>(null) }
    val fallbackMiniTopPx = with(density) {
        (hostBoundsRoot?.height ?: 0f) - WindowInsets.navigationBars.getBottom(this) - 56.dp.toPx() - MINI_FALLBACK_HEIGHT.toPx()
    }
    val host = hostBoundsRoot
    val miniRoot = miniBoundsRoot
    // Panel edges when docked; the panel grows from exactly the mini bar's rect to full screen.
    val miniTopPx = if (host != null && miniRoot != null) miniRoot.top - host.top else fallbackMiniTopPx
    val miniBottomPx = if (host != null && miniRoot != null) miniRoot.bottom - host.top else fallbackMiniTopPx + with(density) { MINI_FALLBACK_HEIGHT.toPx() }

    // Mounted by the very tap that expands, so there was no mini bar yet; keep it hidden rather than flashing it before the slide-up.
    var suppressMini by remember { mutableStateOf(PlayerDockController.pendingExpand.value) }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) {
        PlayerDockController.pendingExpand.collect { pending ->
            if (pending && PlayerDockController.consumeExpand()) {
                // A pick from search would otherwise leave the keyboard up over the full player.
                keyboardController?.hide()
                focusManager.clearFocus()
                try { dockProgress.animateTo(1f, DockTween) } finally { suppressMini = false }
            }
        }
    }

    fun collapseToMini() {
        scope.launch { dockProgress.animateTo(0f, DockTween) }
    }

    BackHandler(enabled = !hideContent && dockProgress.value > 0.5f) {
        collapseToMini()
    }

    val flingThresholdPx = with(density) { FLING_VELOCITY_DP.dp.toPx() }
    var dragging by remember { mutableStateOf(false) }
    // Only a drag that started on the mini bar keeps it composed past full, so it can never linger invisibly over the sheet.
    var miniDragging by remember { mutableStateOf(false) }
    // Panel travel equals the finger's travel, so the panel stays glued under the finger.
    fun onDockDrag(pxDelta: Float) {
        if (!dragging) {
            dragging = true
            dragFraction = dockProgress.value
            scope.launch { dockProgress.stop() }
        }
        val range = miniTopPx.coerceAtLeast(1f)
        dragFraction = (dragFraction - pxDelta / range).coerceIn(0f, 1f)
    }
    // A fast flick wins over distance; the release continues the tap's own curve rather than a different spring/curve.
    fun onDockDragEnd(velocity: Float) {
        miniDragging = false
        if (!dragging) return
        val target = when {
            velocity < -flingThresholdPx -> 1f
            velocity > flingThresholdPx -> 0f
            dragFraction > 0.5f -> 1f
            else -> 0f
        }
        scope.launch {
            // Hand off only once the animatable holds the finger's position, so there's no one-frame jump back.
            val from = dragFraction
            dockProgress.snapTo(from)
            dragging = false
            dockProgress.animateTo(target, remainingDockTween(if (target == 1f) from else 1f - from))
        }
    }

    var targetBackgroundColor by remember { mutableStateOf(Color.Black) }
    var currentAlbumBitmap by remember { mutableStateOf<Bitmap?>(null) }
    // Loaded once here and handed to both layers, so expanding never reloads or crossfades the cover mid-slide.
    val artBitmap = rememberAlbumBitmap(current)
    LaunchedEffect(artBitmap) {
        val bitmap = artBitmap
        if (bitmap == null) {
            currentAlbumBitmap = null
            return@LaunchedEffect
        }
        targetBackgroundColor = albumPaletteColors(bitmap).first
        currentAlbumBitmap = try { bitmap.asAndroidBitmap() } catch (_: Throwable) { null }
    }
    val backgroundColor by animateColorAsState(
        targetValue = targetBackgroundColor,
        animationSpec = tween(durationMillis = 800),
        label = "Dock background color"
    )

    val p = if (dragging) dragFraction else dockProgress.value
    SideEffect { PlayerDockController.reportDockProgress(if (hideContent) 0f else p) }

    if (hideContent) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { hostBoundsRoot = it.boundsInRoot() }
    ) {
        val panelTopPx = miniTopPx * (1f - p)
        val panelBottomFraction = p

        if (p > 0.001f || dragging) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // Draw-only translate (not graphicsLayer) so touch coords and MusicPlayerChrome's boundsInRoot stay untransformed.
                    .drawWithContent {
                        val panelBottomPx = miniBottomPx + (size.height - miniBottomPx) * panelBottomFraction
                        drawRect(
                            color = lerpColor(MiniBarColor, backgroundColor, p),
                            topLeft = Offset(0f, panelTopPx),
                            size = Size(size.width, panelBottomPx - panelTopPx)
                        )
                        clipRect(top = panelTopPx, bottom = panelBottomPx) {
                            translate(top = panelTopPx) { this@drawWithContent.drawContent() }
                        }
                    }
            ) {
                MusicPlayerChrome(
                    backgroundColor = backgroundColor,
                    currentAlbumBitmap = currentAlbumBitmap,
                    onCollapse = ::collapseToMini,
                    onCollapseDragDelta = ::onDockDrag,
                    onCollapseDragEnd = ::onDockDragEnd,
                    artBitmap = artBitmap,
                    dockProgress = p,
                    viewModel = viewModel
                )
            }
        }

        // Positioning only -- no pointerInput/background of its own, so the space it reserves above BottomNav doesn't eat BottomNav taps.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 56.dp)
                // Measured on this untransformed slot, not the bar, since the bar is drawn shifted while the panel slides.
                .onGloballyPositioned { if (it.size.height > 0) miniBoundsRoot = it.boundsInRoot() }
        ) {
            if (current != null && !suppressMini && (p < 0.999f || miniDragging)) {
                val progress = if (durationMs > 0L) {
                    positionMs.coerceAtMost(durationMs).toFloat() / durationMs.toFloat()
                } else 0f
                MiniPlayer(
                    song = current,
                    isPlaying = isPlaying,
                    progress = progress,
                    onPlayPause = { viewModel.togglePlayPause(ctx) },
                    onExpand = { PlayerDockController.requestExpand() },
                    onDrag = { miniDragging = true; onDockDrag(it) },
                    onDragStopped = ::onDockDragEnd,
                    artBitmap = artBitmap,
                    // The full player's art takes over the thumbnail as soon as the panel starts moving.
                    artAlpha = if (p > 0.001f) 0f else 1f,
                    backgroundAlpha = if (p > 0.001f) 0f else 1f,
                    modifier = Modifier
                        // Rides the panel's top edge; draw-only so the drag doesn't chase its own moving hit area, and outside the alpha layer so that layer doesn't clip the shifted bar.
                        .drawWithContent {
                            translate(top = panelTopPx - miniTopPx) { this@drawWithContent.drawContent() }
                        }
                        // Gone quickly so its row never shows while the art has already drifted off its thumbnail spot.
                        .graphicsLayer { alpha = 1f - (p / 0.15f).coerceIn(0f, 1f) }
                        .onGloballyPositioned {
                            PlayerDockController.reportMiniBarHeightPx(it.size.height.toFloat())
                        }
                )
            }
        }
    }
}

private val previewSongs = listOf(
    Song(1, "Afterglow", "Nova Reyes", 238000.0, "", "Neon Parallels"),
    Song(2, "Dust & Gold", "Marla Quinn", 195000.0, "", "Dust & Gold")
)

@Composable
private fun PersistentPlayerHostPreview(progress: Float) {
    MaterialTheme {
        // remember (not LaunchedEffect) so the seed runs before the host's playlist.isEmpty() check.
        remember {
            PlayerStateManager.setPlaylist(previewSongs, 0)
            PlayerStateManager.setIsPlaying(true)
        }
        Box(Modifier.fillMaxSize().background(Color(0xFF14261C))) {
            PersistentPlayerHost(initialDockProgress = progress)
        }
    }
}

@Preview(showSystemUi = true, name = "PersistentPlayerHost — Mini docked")
@Composable
private fun PersistentPlayerHostMiniPreview() = PersistentPlayerHostPreview(0f)

@Preview(showSystemUi = true, name = "PersistentPlayerHost — Sliding (40%)")
@Composable
private fun PersistentPlayerHostMidPreview() = PersistentPlayerHostPreview(0.4f)

@Preview(showSystemUi = true, name = "PersistentPlayerHost — Full expanded")
@Composable
private fun PersistentPlayerHostFullPreview() = PersistentPlayerHostPreview(1f)

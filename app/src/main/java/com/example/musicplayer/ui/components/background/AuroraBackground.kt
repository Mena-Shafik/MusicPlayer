package com.example.musicplayer.ui.components.background

// ...existing code...
import androidx.compose.animation.core.*

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.LifecycleEventObserver
import android.os.PowerManager
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.lerp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.graphics.Bitmap
import androidx.palette.graphics.Palette

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.tooling.preview.Preview
import kotlin.math.*

/**
 * Simple aurora-like animated background.
 * - Draws several drifting ribbons with soft colors.
 * - Applies RenderEffect blur on supported API levels for a soft appearance.
 */
@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    albumCoverBitmap: Bitmap? = null,
    speed: Float = 0.8f,
    intensity: Float = 0.95f,
) {
    // Use a continuously increasing time value (0..1) updated each frame so the
    // animation never "jumps" when the underlying animation restarts.
    // We compute a period in seconds similar to the previous duration (20000ms / speed).
    val tState = remember { mutableStateOf(0f) }

    // Lifecycle & power mode awareness to reduce battery usage when the screen is off
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val isPowerSave = remember { mutableStateOf(false) }
    val isActive = remember { mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) }

    DisposableEffect(lifecycleOwner) {
        // observe lifecycle to pause animation when not visible
        val observer = LifecycleEventObserver { _, _ ->
            isActive.value = lifecycleOwner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        // initial power save state
        try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            isPowerSave.value = pm.isPowerSaveMode
        } catch (_: Throwable) {}

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    LaunchedEffect(speed, isActive.value, isPowerSave.value) {
        // compute a continuous cycle count (elapsed / period). We DO NOT modulo it so the
        // underlying sin/cos calls remain continuous across cycle boundaries.
        val periodSec = (12f / max(0.1f, speed)) // base 12s / speed -> faster default cycle
        val startNanos = System.nanoTime()
        while (true) {
            if (isActive.value && !isPowerSave.value) {
                val now = System.nanoTime()
                val elapsedSec = (now - startNanos) / 1_000_000_000f
                // continuous cycles (may exceed 1.0) — sin/cos remain continuous, no abrupt wrap
                tState.value = (elapsedSec / periodSec)
                // aim for ~60fps update; use a small delay to yield
                kotlinx.coroutines.delay(16L)
            } else {
                // when not active or power saver on, slow down updates to save CPU/GPU
                kotlinx.coroutines.delay(1000L)
            }
        }
    }
    val t by remember { tState }

    // Sample colors from the album cover (async) and fall back to baseColors.
    // Keep the last successful palette so we don't flash back to default/base colors
    // during quick song switches where album bitmaps may be temporarily null.
    val lastPalette = remember { mutableStateOf<List<Color>?>(null) }
    val bmpState = rememberUpdatedState(albumCoverBitmap)
    // Fade-in control: start transparent until we have a sampled palette
    val alphaAnim = remember { Animatable(0f) }

    LaunchedEffect(bmpState.value) {
        val bmp = bmpState.value
        if (bmp != null) {
            // run Palette generation off the main thread
            val ordered = withContext(Dispatchers.Default) {
                val p = Palette.from(bmp).maximumColorCount(8).generate()

                // dominant (majority) color from the album art (fallback black)
                val dominantInt = p.getDominantColor(android.graphics.Color.BLACK)

                // Convert dominant to HSL and produce a small range of variants around it
                val hsl = FloatArray(3)
                try {
                    // androidx.core.graphics.ColorUtils provides HSL helpers
                    androidx.core.graphics.ColorUtils.colorToHSL(dominantInt, hsl)
                } catch (_: Throwable) {
                    // fallback if ColorUtils not available — approximate neutral HSL
                    hsl[0] = 0f; hsl[1] = 0f; hsl[2] = 0.5f
                }

                fun hslToColor(h: Float, s: Float, l: Float): Color {
                    val arr = floatArrayOf(h, s.coerceIn(0f,1f), l.coerceIn(0f,1f))
                    return Color(androidx.core.graphics.ColorUtils.HSLToColor(arr))
                }

                val dominantColor = Color(dominantInt)

                // create a small majority-range palette from dominant (lighter/darker, saturated variants)
                val range = listOf(
                    dominantColor,
                    // slightly more saturated/brighter variant
                    hslToColor(hsl[0], (hsl[1] * 1.25f).coerceAtMost(1f), (hsl[2] * 1.08f).coerceAtMost(1f)),
                    // slightly desaturated/darker variant
                    hslToColor(hsl[0], (hsl[1] * 0.78f).coerceAtLeast(0f), (hsl[2] * 0.78f).coerceAtLeast(0f)),
                    // small hue shifts for variety
                    hslToColor((hsl[0] + 18f) % 360f, hsl[1], (hsl[2] * 1.04f).coerceAtMost(1f)),
                    hslToColor((hsl[0] - 18f + 360f) % 360f, hsl[1], (hsl[2] * 0.92f).coerceAtLeast(0f))
                ).distinct()

                // collect other useful swatches (vibrant/muted variants)
                val others = listOfNotNull(
                    p.vibrantSwatch?.rgb,
                    p.darkVibrantSwatch?.rgb,
                    p.lightVibrantSwatch?.rgb,
                    p.mutedSwatch?.rgb,
                    p.darkMutedSwatch?.rgb,
                    p.lightMutedSwatch?.rgb
                ).map { Color(it) }

                // merge: prefer range (majority) first, then distinct other swatches
                val result = mutableListOf<Color>()
                for (c in range) if (!result.any { it == c }) result.add(c)
                for (c in others) if (!result.any { it == c }) result.add(c)

                // Ensure at least one color
                if (result.isEmpty()) listOf(Color.Black) else result.toList()
            }

            // Only update the stored palette when we successfully extracted colors
            if (ordered.isNotEmpty()) lastPalette.value = ordered
        }
        // If bmp is null, do not clear lastPalette — keep previous colors until new palette is ready.
    }

    // When new palette becomes available, fade in if needed
    LaunchedEffect(lastPalette.value) {
        if (lastPalette.value != null) {
            try { alphaAnim.animateTo(1f, animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)) } catch (_: Throwable) {}
        }
    }

    // Smooth transition between palettes when album art (or base colors) change.
    // Use a fixed palette size so node mapping remains stable and we don't reposition colors abruptly.
    val paletteSize = 6

    fun normalizePalette(input: List<Color>, size: Int): List<Color> {
        // If no palette available, return fully-transparent colors so nothing visible is drawn
        if (input.isEmpty()) return List(size) { Color.Black.copy(alpha = 0f) }
        if (input.size == size) return input.toList()
        // Sample from input across its length to build a size-length list
        return List(size) { i ->
            val idx = (i * input.size) / size
            input[idx % input.size]
        }
    }

    val targetColorsRaw = lastPalette.value ?: emptyList()
    val targetColors = normalizePalette(targetColorsRaw, paletteSize)

    val prevColorsState = remember { mutableStateOf(normalizePalette(targetColorsRaw, paletteSize)) }
    val currentTargetState = remember { mutableStateOf(normalizePalette(targetColorsRaw, paletteSize)) }
    val progress = remember { Animatable(1f) }



    // When targetColors changes, animate progress from 0 -> 1 blending prev -> target
    LaunchedEffect(targetColors) {
        // compare by values to avoid object reference differences
        val old = currentTargetState.value
        if (!old.zip(targetColors).all { (a, b) -> a == b }) {
            prevColorsState.value = old.toList()
            currentTargetState.value = targetColors.toList()
            // start from 0 and animate to 1 for a smooth crossfade
            progress.snapTo(0f)
            try {
                progress.animateTo(1f, animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing))
            } catch (_: Throwable) {}
        }
    }

    // If no palette yet, render fully-transparent colors (no default/base colors shown).
    val colorsToUse = run {
        if (lastPalette.value == null) {
            List(paletteSize) { Color.Black.copy(alpha = 0f) }
        } else {
            val prev = prevColorsState.value
            val target = currentTargetState.value
            val p = progress.value
            List(paletteSize) { i ->
                val c = lerp(prev[i], target[i], p)
                // apply overall alpha fade so initial draw is invisible until palette ready
                c.copy(alpha = c.alpha * alphaAnim.value)
            }
        }
    }

    Box(modifier = modifier.drawBehind {
        drawMeshGradient(size.width, size.height, t, colorsToUse, speed, intensity)
    })
}

// Draw a mesh-like gradient by placing a handful of animated radial gradients and blending them
private fun DrawScope.drawMeshGradient(
    width: Float,
    height: Float,
    t: Float,
    colors: List<Color>,
    speed: Float,
    intensity: Float
) {
    val maxDim = max(width, height)

    // number of gradient nodes (more -> richer mesh)
    // increase nodes for a richer, more pronounced aurora
    val nodes = max(4, colors.size * 3)

    for (i in 0 until nodes) {
        // choose a base color cycling through provided colors
        val col = colors[i % colors.size]

        // animated offset using sin/cos for organic motion
        // use i/n instead of adding a raw integer offset so phase spacing is a fraction of a cycle
        val frac = i.toFloat() / nodes
        val angle = t * 2f * PI.toFloat() * (0.25f + frac)
        val radiusFactor = 0.35f + (i % 3) * 0.1f
        val cx = width * (0.5f + cos(angle) * 0.38f * (0.6f + speed * 0.4f))
        // bias the aurora upward so motion appears to originate from the top
        val cy = height * (0.28f + sin(angle * 1.2f) * 0.26f * (0.6f + speed * 0.4f))
        val center = Offset(cx, cy)

        val radius = maxDim * (radiusFactor * 0.9f + 0.12f * sin(t * 2f * PI.toFloat() * (0.5f + frac)))

        // stronger per-node alpha so aurora is more visible; intensity scales overall strength
        val alpha = (0.35f + 0.55f * intensity) * (0.88f + 0.22f * (i % 2))

        val brush = Brush.radialGradient(
            colors = listOf(col.copy(alpha = alpha), col.copy(alpha = 0.0f)),
            center = center,
            radius = radius,
            tileMode = TileMode.Clamp
        )

        // draw with soft blending by drawing semi-transparent circles using the radial brush
        drawCircle(brush = brush, radius = radius, center = center)
    }

    // Add a top source tint so the aurora appears to originate from the top,
    // and keep a bottom vignette to darken the UI area.
    if (colors.isNotEmpty()) {
        val base = colors[0]
        // Top radial source near the top-center
        val topAlpha = (0.20f * intensity).coerceIn(0f, 0.6f)
        val topBrush = Brush.radialGradient(
            colors = listOf(base.copy(alpha = topAlpha), base.copy(alpha = 0f)),
            center = Offset(width * 0.5f, height * 0.16f),
            radius = maxDim * 0.7f,
            tileMode = TileMode.Clamp
        )
        drawCircle(brush = topBrush, radius = maxDim * 0.7f, center = Offset(width * 0.5f, height * 0.16f))

        // Bottom darkening vignette - transparent near center, dark at bottom
        val bottomAlpha = (0.55f * intensity).coerceIn(0f, 0.95f)
        val bottomBrush = Brush.verticalGradient(
            colors = listOf(Color.Transparent, Color.Black.copy(alpha = bottomAlpha)),
            startY = height * 0.45f,
            endY = height,
            tileMode = TileMode.Clamp
        )
        drawRect(brush = bottomBrush, size = size, topLeft = Offset.Zero)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, apiLevel = 36)
@Composable
fun AuroraBackgroundPreview() {
    Box(modifier = Modifier.fillMaxSize()) {
        AuroraBackground(
            speed = 0.6f,
            intensity = 0.8f
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, name = "Aurora Background (sampled preview)", apiLevel = 36, showSystemUi = true)
@Composable
fun AuroraBackgroundPreviewSample() {
    // Previews can't supply a Bitmap easily, so demonstrate with a sampled-like palette
    Box(modifier = Modifier.fillMaxSize()) {
        AuroraBackground(
            speed = 0.5f,
            intensity = 0.9f
        )
    }
}

// helper removed (not needed with Compose drawing APIs)

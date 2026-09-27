package com.example.musicplayer.ui.components.background

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import com.example.musicplayer.model.Song
import com.example.musicplayer.util.Util
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import kotlin.math.tan

// "2.0" redesign background: drifting, skewed color ribbons anchored to the top, blurred and faded under a dark scrim, ported from the "MusicPlayer 2.0 UI" mockup's auroraRibbons() generator; a lighter-weight companion to the mesh-gradient AuroraBackground (unchanged, still drives Now Playing) meant for list-style screens instead.
object AuroraRibbonPalette {
    private fun rgb(r: Int, g: Int, b: Int) = Color(r, g, b)

    /** Default mockup palette ("Northern" in the design source). */
    val Northern = listOf(
        Color(0xFF50F0AA), Color(0xFF8CFFC8), Color(0xFF46E6B4),
        Color(0xFFA078FF), Color(0xFF5AEBB9), Color(0xFF3CDCD2),
        Color(0xFF78FFBE), Color(0xFF966EFF), Color(0xFF46C8DC)
    )

    val Brand = listOf(
        rgb(255, 170, 60), rgb(255, 190, 90), rgb(90, 140, 255),
        rgb(110, 150, 255), rgb(255, 150, 40), rgb(70, 120, 240),
        rgb(255, 205, 120), rgb(120, 160, 255), rgb(255, 140, 30)
    )
    val Solar = listOf(
        rgb(255, 120, 40), rgb(255, 80, 60), rgb(255, 180, 70),
        rgb(230, 60, 90), rgb(255, 140, 30), rgb(250, 200, 110),
        rgb(220, 50, 70), rgb(255, 160, 50), rgb(240, 90, 40)
    )
    val Ultraviolet = listOf(
        rgb(170, 80, 255), rgb(120, 90, 255), rgb(220, 90, 220),
        rgb(90, 120, 255), rgb(190, 60, 240), rgb(140, 60, 255),
        rgb(240, 120, 200), rgb(100, 150, 255), rgb(200, 90, 255)
    )
    val Aura = listOf(
        rgb(229, 57, 53), rgb(244, 81, 30), rgb(255, 193, 7),
        rgb(76, 175, 80), rgb(0, 150, 136), rgb(31, 83, 162),
        rgb(20, 46, 140), rgb(123, 31, 162), rgb(181, 52, 25)
    )
    val Rainbow = listOf(
        rgb(255, 45, 45), rgb(255, 140, 0), rgb(255, 215, 0),
        rgb(60, 220, 80), rgb(0, 190, 190), rgb(40, 110, 240),
        rgb(130, 60, 230), rgb(230, 60, 200), rgb(255, 90, 60)
    )
    val MutedRainbow = listOf(
        rgb(196, 104, 98), rgb(201, 140, 88), rgb(196, 178, 104),
        rgb(124, 166, 124), rgb(104, 160, 158), rgb(104, 132, 182),
        rgb(140, 116, 176), rgb(178, 116, 158), rgb(186, 124, 112)
    )
    val Dusk = listOf(
        rgb(92, 120, 180), rgb(128, 124, 186), rgb(168, 128, 172),
        rgb(200, 136, 150), rgb(214, 152, 138), rgb(176, 140, 164),
        rgb(132, 132, 190), rgb(104, 128, 186), rgb(86, 112, 168)
    )
    val Moss = listOf(
        rgb(104, 142, 110), rgb(132, 158, 104), rgb(160, 170, 112),
        rgb(118, 152, 132), rgb(96, 138, 128), rgb(142, 164, 118),
        rgb(110, 148, 116), rgb(86, 130, 118), rgb(126, 156, 108)
    )
    val SlateRose = listOf(
        rgb(112, 124, 140), rgb(142, 132, 142), rgb(172, 140, 144),
        rgb(196, 150, 150), rgb(164, 138, 148), rgb(134, 130, 148),
        rgb(110, 122, 142), rgb(150, 136, 146), rgb(184, 146, 148)
    )
    val AshGold = listOf(
        rgb(150, 140, 124), rgb(178, 158, 120), rgb(200, 176, 124),
        rgb(164, 148, 124), rgb(136, 130, 120), rgb(190, 168, 128),
        rgb(156, 144, 124), rgb(172, 152, 122), rgb(142, 134, 122)
    )
    val InkCyan = listOf(
        rgb(68, 104, 140), rgb(76, 132, 156), rgb(92, 158, 168),
        rgb(70, 118, 150), rgb(60, 96, 132), rgb(86, 146, 164),
        rgb(74, 126, 154), rgb(64, 110, 144), rgb(96, 164, 172)
    )
    val PlumSage = listOf(
        rgb(142, 116, 158), rgb(156, 132, 150), rgb(140, 148, 138),
        rgb(120, 150, 132), rgb(132, 128, 156), rgb(152, 124, 152),
        rgb(128, 144, 140), rgb(146, 136, 148), rgb(118, 140, 136)
    )
    val Spectrum = listOf(
        rgb(70, 235, 180), rgb(60, 200, 235), rgb(90, 140, 255),
        rgb(160, 110, 255), rgb(235, 90, 210), rgb(255, 110, 120),
        rgb(255, 165, 60), rgb(210, 235, 90), rgb(70, 225, 150)
    )
    val Tropic = listOf(
        rgb(0, 220, 190), rgb(60, 240, 140), rgb(255, 210, 60),
        rgb(255, 130, 90), rgb(0, 180, 220), rgb(130, 240, 110),
        rgb(255, 170, 50), rgb(40, 210, 235), rgb(190, 245, 90)
    )
    val Candy = listOf(
        rgb(255, 120, 190), rgb(255, 160, 120), rgb(180, 140, 255),
        rgb(120, 220, 255), rgb(255, 110, 150), rgb(210, 130, 255),
        rgb(255, 200, 120), rgb(140, 190, 255), rgb(255, 140, 220)
    )
    val Ice = listOf(
        rgb(150, 230, 255), rgb(110, 180, 255), rgb(200, 220, 255),
        rgb(80, 220, 235), rgb(130, 150, 255), rgb(180, 250, 240),
        rgb(100, 200, 255), rgb(220, 200, 255), rgb(90, 240, 215)
    )
    val Sunset = listOf(
        rgb(255, 90, 60), rgb(255, 140, 70), rgb(255, 60, 120),
        rgb(200, 70, 180), rgb(255, 180, 80), rgb(160, 60, 190),
        rgb(255, 110, 90), rgb(230, 50, 140), rgb(255, 160, 110)
    )
    val TealMagenta = listOf(
        rgb(0, 215, 205), rgb(235, 60, 170), rgb(60, 240, 220),
        rgb(190, 50, 200), rgb(0, 180, 190), rgb(255, 90, 190),
        rgb(90, 250, 230), rgb(220, 70, 150), rgb(40, 200, 215)
    )
    val LimeViolet = listOf(
        rgb(190, 245, 70), rgb(140, 90, 255), rgb(220, 255, 110),
        rgb(110, 60, 230), rgb(160, 230, 60), rgb(180, 120, 255),
        rgb(230, 250, 140), rgb(90, 70, 220), rgb(200, 240, 90)
    )
    val Copper = listOf(
        rgb(255, 150, 70), rgb(210, 90, 50), rgb(255, 200, 120),
        rgb(170, 70, 40), rgb(255, 120, 50), rgb(230, 170, 90),
        rgb(190, 80, 45), rgb(255, 175, 95), rgb(205, 110, 60)
    )
    val DeepSea = listOf(
        rgb(20, 140, 200), rgb(0, 200, 190), rgb(60, 90, 220),
        rgb(10, 180, 230), rgb(40, 70, 190), rgb(0, 220, 215),
        rgb(80, 120, 240), rgb(30, 160, 210), rgb(0, 100, 180)
    )
    val NeonNoir = listOf(
        rgb(255, 20, 120), rgb(0, 240, 255), rgb(180, 0, 255),
        rgb(255, 90, 0), rgb(60, 255, 180), rgb(255, 0, 200),
        rgb(120, 80, 255), rgb(0, 200, 255), rgb(255, 60, 60)
    )

    /** Display name -> palette, in the same order as the design source's picker. */
    val byName: Map<String, List<Color>> = linkedMapOf(
        "Aura" to Aura,
        "Northern" to Northern,
        "Brand" to Brand,
        "Rainbow" to Rainbow,
        "Spectrum" to Spectrum,
        "Solar" to Solar,
        "Ultraviolet" to Ultraviolet,
        "Tropic" to Tropic,
        "Candy" to Candy,
        "Ice" to Ice,
        "Sunset" to Sunset,
        "Teal_Magenta" to TealMagenta,
        "Lime_Violet" to LimeViolet,
        "Copper" to Copper,
        "Deep_Sea" to DeepSea,
        "Neon_Noir" to NeonNoir,
        "Muted_Rainbow" to MutedRainbow,
        "Dusk" to Dusk,
        "Moss" to Moss,
        "Slate_Rose" to SlateRose,
        "Ash_Gold" to AshGold,
        "Ink_Cyan" to InkCyan,
        "Plum_Sage" to PlumSage
    )

    /** Display names in picker order, for a Settings list. */
    val names: List<String> = byName.keys.toList()

    fun forName(name: String?): List<Color> = byName[name] ?: Northern
}

/** Canvas base color used behind the ribbon band and scrim, matching the mockup's `#03050c`. */
val AuroraRibbonDefaultBaseColor = Color(0xFF03050C)

private enum class RibbonMotion { A, B, C }

// Position/motion only -- deliberately palette-independent so a palette switch (Settings or the scroll-driven sampler) never rebuilds this and restarts each ribbon's phase, which used to make ribbons visibly jump to a new location on every palette change.
private data class RibbonSpec(
    val leftFraction: Float,
    val widthFraction: Float,
    val motion: RibbonMotion,
    val periodMillis: Int,
    val startOffsetMillis: Int
)

private data class RibbonKeyState(val translateXFraction: Float, val skewDeg: Float, val scaleY: Float, val alpha: Float)

// Ported from the mockup's aurA/aurB/aurC @keyframes: each motion oscillates between its 0%/100% state and its 50% (peak) state.
private val motionKeyStates: Map<RibbonMotion, Pair<RibbonKeyState, RibbonKeyState>> = mapOf(
    RibbonMotion.A to (RibbonKeyState(-0.08f, -14f, 1.00f, 0.75f) to RibbonKeyState(0.10f, 6f, 1.18f, 1.00f)),
    RibbonMotion.B to (RibbonKeyState(0.06f, 10f, 1.12f, 0.90f) to RibbonKeyState(-0.09f, -8f, 0.94f, 0.60f)),
    RibbonMotion.C to (RibbonKeyState(0.00f, 4f, 0.95f, 0.50f) to RibbonKeyState(-0.12f, -16f, 1.20f, 0.95f)),
)

private fun lerp(a: Float, b: Float, p: Float) = a + (b - a) * p

// Remembers the last palette name any screen has read from DataStore, across screen instances.
private object AuroraPaletteCache {
    @Volatile var lastKnown: String = "Northern"
}

@Composable
fun AuroraRibbonBackground(
    modifier: Modifier = Modifier,
    ribbonCount: Int = 6,
    // null = read the user's chosen palette from Settings; pass an explicit list (e.g. one sampled live from album art) to override it for this call site only.
    palette: List<Color>? = null,
    speed: Float = 1f,
    intensity: Float = 1f,
    bandHeightFraction: Float = 0.5f,
    baseColor: Color = AuroraRibbonDefaultBaseColor,
) {
    val resolvedPalette = palette ?: run {
        val context = LocalContext.current
        // Seeded from the last value any screen has already read (AuroraPaletteCache) rather than a hardcoded collectAsState(initial = "Northern"), so only the very first cold read of the whole app can flash that placeholder before the real DataStore value arrives.
        var paletteName by remember { mutableStateOf(AuroraPaletteCache.lastKnown) }
        LaunchedEffect(context) {
            com.example.musicplayer.preferences.PreferencesManager.getAuroraPaletteFlow(context).collect { name ->
                paletteName = name
                AuroraPaletteCache.lastKnown = name
            }
        }
        AuroraRibbonPalette.forName(paletteName)
    }

    val n = ribbonCount.coerceIn(3, 9)
    val step = 380f / n
    val blurDp = (22 + n * 1.6f).roundToInt().dp

    // Keyed only on n/speed -- a palette change must never recreate this list, or each ribbon's animateFloat phase restarts and it visibly jumps location instead of just recoloring.
    val specs = remember(n, speed) {
        List(n) { i ->
            val width = (step * 1.5f + (i % 3) * 14f) / 360f
            val left = (-40f + i * step + if (i % 2 == 1) 10f else -6f) / 360f
            val motion = RibbonMotion.entries[i % 3]
            val period = ((13f + i * 2.6f) / speed * 1000f).roundToInt().coerceAtLeast(200)
            val startOffset = (-i * 1.7f * 1000f).roundToInt()
            RibbonSpec(left, width, motion, period, startOffset)
        }
    }

    Box(modifier = modifier.fillMaxSize().background(baseColor)) {
        RibbonBand(
            specs = specs,
            colors = resolvedPalette,
            intensity = intensity,
            bandHeightFraction = bandHeightFraction,
            // Fills the whole area (not just the band) so the blur dissipates smoothly instead of a hard cutoff at the band's edge -- each ribbon already fades to transparent internally before reaching it.
            modifier = Modifier
                .fillMaxSize()
                .blur(blurDp)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to baseColor.copy(alpha = 0.45f),
                        0.18f to baseColor.copy(alpha = 0.12f),
                        0.58f to baseColor.copy(alpha = 0.66f),
                        1f to baseColor.copy(alpha = 0.94f)
                    )
                )
        )
    }
}

// AuroraRibbonBackground variant for scrollable song lists: colors sampled from the album art of the song sampleOffset rows below the viewport top, re-sampled as scrolling brings a new song there, falling back to the user's Settings palette until the first sample loads.
@Composable
fun DynamicAuroraRibbonBackground(
    listState: LazyListState,
    songs: List<Song>,
    modifier: Modifier = Modifier,
    sampleOffset: Int = 6,
    ribbonCount: Int = 6,
    speed: Float = 1f,
    intensity: Float = 1f,
    bandHeightFraction: Float = 0.5f,
    baseColor: Color = AuroraRibbonDefaultBaseColor,
) {
    val context = LocalContext.current

    val targetIndex by remember(songs) {
        derivedStateOf {
            if (songs.isEmpty()) -1
            else (listState.firstVisibleItemIndex + sampleOffset).coerceIn(0, songs.lastIndex)
        }
    }

    // Keeps the previous sample visible (rather than snapping back to the fallback palette) while the next one loads, so the background doesn't flicker between songs.
    var samplePalette by remember { mutableStateOf<List<Color>?>(null) }

    LaunchedEffect(targetIndex, songs) {
        if (targetIndex < 0) return@LaunchedEffect
        val song = songs.getOrNull(targetIndex) ?: return@LaunchedEffect
        val extracted = withContext(Dispatchers.IO) {
            try {
                val bitmap = Util.getAlbumArt(context, song.path)?.asAndroidBitmap() ?: return@withContext null
                val palette = Palette.from(bitmap).maximumColorCount(8).generate()
                val dominant = Color(palette.getDominantColor(android.graphics.Color.BLACK))
                val swatches = listOfNotNull(
                    palette.vibrantSwatch, palette.lightVibrantSwatch, palette.darkVibrantSwatch,
                    palette.mutedSwatch, palette.lightMutedSwatch, palette.darkMutedSwatch
                ).map { Color(it.rgb) }
                (listOf(dominant) + swatches).distinct()
            } catch (_: Throwable) {
                null
            }
        }
        if (extracted != null && extracted.size >= 2) samplePalette = extracted
    }

    AuroraRibbonBackground(
        modifier = modifier,
        ribbonCount = ribbonCount,
        palette = samplePalette,
        speed = speed,
        intensity = intensity,
        bandHeightFraction = bandHeightFraction,
        baseColor = baseColor
    )
}

@Composable
private fun RibbonBand(
    specs: List<RibbonSpec>,
    colors: List<Color>,
    intensity: Float,
    bandHeightFraction: Float,
    modifier: Modifier = Modifier
) {
    // Animates each ribbon's color toward its target rather than snapping, so palette switches crossfade instead of popping; keyed on `specs.indices`/colors only (not `specs` itself) to stay independent of the position/motion state below.
    val animatedColors = specs.indices.map { i ->
        animateColorAsState(
            targetValue = colors[i % colors.size],
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            label = "ribbonColor"
        )
    }

    val transition = rememberInfiniteTransition(label = "auroraRibbons")
    val progresses = specs.map { spec ->
        val startOffset = ((spec.startOffsetMillis % spec.periodMillis) + spec.periodMillis) % spec.periodMillis
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = spec.periodMillis / 2, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
                initialStartOffset = StartOffset(startOffset, StartOffsetType.FastForward)
            ),
            label = "ribbon"
        )
    }

    Canvas(modifier = modifier) {
        val w = size.width
        // Logical band height the ribbons taper within; the canvas itself is full-size so blur can bleed past that point without hitting a clipped edge.
        val bandH = size.height * bandHeightFraction
        specs.forEachIndexed { i, spec ->
            val (state0, state1) = motionKeyStates.getValue(spec.motion)
            val p = progresses[i].value
            val tx = lerp(state0.translateXFraction, state1.translateXFraction, p)
            val skewDeg = lerp(state0.skewDeg, state1.skewDeg, p)
            val scaleY = lerp(state0.scaleY, state1.scaleY, p)
            val alphaFactor = lerp(state0.alpha, state1.alpha, p) * intensity

            val ribbonWidth = spec.widthFraction * w
            val left = spec.leftFraction * w
            val cx = left + ribbonWidth / 2f
            val topY = 0f
            val bottomY = bandH * scaleY
            val dx = (bottomY * tan(Math.toRadians(skewDeg.toDouble()))).toFloat()
            val translatePx = tx * ribbonWidth

            val path = Path().apply {
                moveTo(cx - ribbonWidth / 2f + translatePx, topY)
                lineTo(cx + ribbonWidth / 2f + translatePx, topY)
                lineTo(cx + ribbonWidth / 2f + dx + translatePx, bottomY)
                lineTo(cx - ribbonWidth / 2f + dx + translatePx, bottomY)
                close()
            }

            val ribbonColor = animatedColors[i].value
            val brush = Brush.verticalGradient(
                0f to ribbonColor.copy(alpha = 0f),
                0.16f to ribbonColor.copy(alpha = (0.82f * alphaFactor).coerceIn(0f, 1f)),
                0.55f to ribbonColor.copy(alpha = (0.42f * alphaFactor).coerceIn(0f, 1f)),
                1f to ribbonColor.copy(alpha = 0f),
                startY = topY,
                endY = bottomY.coerceAtLeast(topY + 1f)
            )
            drawPath(path = path, brush = brush)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, apiLevel = 36)
@Composable
private fun AuroraRibbonBackgroundPreview() {
    Box(modifier = Modifier.fillMaxSize()) {
        AuroraRibbonBackground()
    }
}

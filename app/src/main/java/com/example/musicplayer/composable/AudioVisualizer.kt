package com.example.musicplayer.composable

import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.rem
import kotlin.text.toLong
import kotlin.times


@Composable
fun AudioVisualizer(
    audioSessionId: Int? = null,
    isPlaying: Boolean = true,
    barCount: Int = 3,
    // tuned defaults so 3 bars comfortably fit inside 50x50
    barWidth: Dp = 14.dp,
    heightDp: Dp = 40.dp,
    barColor: Color = Color.White,
    modifier: Modifier = Modifier.size(50.dp),
    speed: Float = 1.5f // >1 speeds up, <1 slows down
) {
    // Ensure speed is positive
    val s = (if (speed <= 0f) 1f else speed)

    // Use Animatable per-bar so we have explicit control
    val animatables = remember { List(barCount) { Animatable(0.15f) } }

    // Launch animation coroutines for each bar to create staggered opposite-phase loop
    animatables.forEachIndexed { index, anim ->
        LaunchedEffect(isPlaying, index, s) {
            if (!isPlaying) {
                anim.snapTo(0f)
                return@LaunchedEffect
            }

            // initial opposite-phase setup: even indices low, odd indices high
            if (index % 2 == 0) anim.snapTo(0.15f) else anim.snapTo(0.95f)

            // Stagger start slightly (smaller delay -> faster wave)
            delay((index * (40L / s)).toLong())

            while (isActive && isPlaying) {
                val base = 700
                val perIndex = 150
                val duration = ((base + index * perIndex) / s).toInt().coerceAtLeast(80)

                if (index % 2 == 0) {
                    // bars 1 and 3 (indices 0 and 2) go up then down
                    anim.animateTo(0.95f, animationSpec = tween(durationMillis = duration, easing = LinearEasing))
                    anim.animateTo(0.15f, animationSpec = tween(durationMillis = (duration * 0.6f).toInt(), easing = LinearEasing))
                } else {
                    // middle bar goes down then up (opposite phase)
                    anim.animateTo(0.15f, animationSpec = tween(durationMillis = (duration * 0.6f).toInt(), easing = LinearEasing))
                    anim.animateTo(0.95f, animationSpec = tween(durationMillis = duration, easing = LinearEasing))
                }
                // smaller pause between cycles to increase perceived speed
                delay((40L / s).toLong())
            }

            if (!isActive || !isPlaying) anim.snapTo(0f)
        }
    }

    // UI: horizontal row of bars aligned to bottom
    Row(
        modifier = modifier.height(50.dp).width(50.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        animatables.forEach { anim ->
            // respect isPlaying: when paused, target amplitude is 0
            val rawAmp = if (isPlaying) anim.value else 0f
            // faster smoothing when starting/stopping
            val animValue by animateFloatAsState(targetValue = rawAmp, animationSpec = tween(durationMillis = (80L / s).toInt().coerceAtLeast(40)))

            Box(
                modifier = Modifier
                    .width(barWidth)
                    .height(heightDp)
                    .clip(RoundedCornerShape(30))
                    .background(Color.Transparent),
                contentAlignment = Alignment.BottomCenter
            ) {
                val barHeight = (heightDp.value * animValue).dp
                Box(
                    modifier = Modifier
                        .width(barWidth)
                        .height(barHeight)
                        .clip(RoundedCornerShape(30))
                        .background(barColor)
                )
            }
        }
    }
}

@Preview(name = "AudioVisualizer - Playing", backgroundColor = 0xFF000000, showBackground = true)
@Composable
fun AudioVisualizerPreviewPlaying() {
    MaterialTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            AudioVisualizer(audioSessionId = null, isPlaying = true, speed = 1.8f)
        }
    }
}

@Preview(name = "AudioVisualizer - Paused", backgroundColor = 0xFF000000, showBackground = true)
@Composable
fun AudioVisualizerPreviewPaused() {
    MaterialTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            AudioVisualizer(audioSessionId = null, isPlaying = false, speed = 1.8f)
        }
    }
}

package com.example.musicplayer.ui.components.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

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
    // Press state for thickening the track; the thumb is always visible (6dp radius, growing to 8dp while pressed) matching the redesign's always-on seek handle.
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
        // detectDragGestures only fires once a touch moves past the system touch-slop threshold, so a plain tap never reached onValueChange -- handle plain taps separately so both work.
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
                topLeft = Offset(0f, (h - trackHeightPx) / 2f),
                size = Size(w, trackHeightPx),
                cornerRadius = CornerRadius(trackHeightPx / 2f, trackHeightPx / 2f)
            )

            // draw active track
            val frac = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
            val activeW = w * frac
            drawRoundRect(
                color = activeColor,
                topLeft = Offset(0f, (h - trackHeightPx) / 2f),
                size = Size(activeW, trackHeightPx),
                cornerRadius = CornerRadius(trackHeightPx / 2f, trackHeightPx / 2f)
            )

            // draw thumb only when pressed (thumbRadius > 0)
            if (thumbRadius > 0f) {
                val cx = activeW
                val cy = h / 2f
                drawCircle(color = activeColor, radius = thumbRadius, center = Offset(cx, cy))
            }
        }
    }
}

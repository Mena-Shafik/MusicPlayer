package com.example.musicplayer.ui.components.common

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** A dashed rounded-rect outline — used for "empty" placeholder tiles in the redesign
 * (no playlist cover yet, no playlists yet) instead of a solid border. */
fun Modifier.dashedBorder(
    widthDp: Dp,
    color: Color,
    cornerRadiusDp: Dp,
    dashLengthDp: Dp = 6.dp,
    gapLengthDp: Dp = 4.dp
): Modifier = this.drawBehind {
    val strokeWidthPx = widthDp.toPx()
    val cornerRadiusPx = cornerRadiusDp.toPx()
    drawRoundRect(
        color = color,
        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
        style = Stroke(
            width = strokeWidthPx,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashLengthDp.toPx(), gapLengthDp.toPx()), 0f)
        )
    )
}

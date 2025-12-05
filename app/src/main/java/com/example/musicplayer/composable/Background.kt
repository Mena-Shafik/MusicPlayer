package com.example.musicplayer.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class RadialSpec(
    val fx: Float, // fractional x center (0-1)
    val fy: Float, // fractional y center (0-1)
    val radius: Dp,
    val colors: List<Color>,
    val alpha: Float = 1.0f
)

@Preview()
@Composable
fun MainBackground(){
    MultiRadialBackground(
        specs = listOf(
            RadialSpec(fx = 0.6f, fy = -0.1f, radius = 320.dp, colors = listOf(Color(0xFFB53419), Color(0xFF5A1E10), Color.Transparent), alpha = 0.20f),
            RadialSpec(fx = 1.0f, fy = 0.0f, radius = 220.dp,colors = listOf(Color(0xFFFFC107), Color(0xFF856832), Color.Transparent), alpha = 0.2f),
            RadialSpec(fx = 0.2f, fy = 0.3f, radius = 120.dp, colors = listOf(Color(0xFF7B1FA2), Color(0xFF4A148C), Color.Transparent), alpha = 0.2f),
            RadialSpec(fx = 0.8f, fy = 0.2f, radius = 180.dp, colors = listOf(Color(0xFF4CAF50), Color(0xFF009688), Color.Transparent), alpha = 0.2f),
            RadialSpec(fx = 0.3f, fy = 0.2f, radius = 180.dp, colors = listOf(Color(0xFFE53935), Color(0xFFF4511E), Color.Transparent), alpha = 0.2f),
            RadialSpec(fx = 0.0f, fy = 0.0f, radius = 220.dp, colors = listOf(Color(0xFF1F53A2), Color(0xFF142E8C), Color.Transparent), alpha = 0.3f),
        ),
        blurRadius = 90.dp // slightly less blur so smaller spots stay focused
    )
    // Black vertical gradient overlay (top -> bottom)
    Box(modifier = Modifier.fillMaxSize().background(
        Brush.verticalGradient(
            // make fully black by 50% of the screen then remain black
            0.0f to Color.Black.copy(alpha = 0.0f),
            0.7f to Color.Black.copy(alpha =.7f),
            1.0f to Color.Black.copy(alpha = 1.0f)
        )
    ))

}

@Composable
fun MultiRadialBackground(specs: List<RadialSpec> = emptyList(), blurRadius: Dp = 0.dp) {
    // Provide a reasonable default when called as a preview without params
    val defaultSpecs = listOf(
        RadialSpec(0.5f, 0.22f, 450.dp, listOf(Color(0xFFB53419), Color(0xFF5A1E10), Color.Black), alpha = 0.95f),
        RadialSpec(0.82f, 0.18f, 320.dp, listOf(Color(0xFFFFC107), Color(0xFF856832), Color.Black), alpha = 0.7f)
    )
    val drawSpecs = if (specs.isEmpty()) defaultSpecs else specs

    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                // Draw each radial gradient on the full canvas, using fractional centers
                drawSpecs.forEach { spec ->
                    val center = Offset(size.width * spec.fx, size.height * spec.fy)
                    val radiusPx = with(density) { spec.radius.toPx() }
                    val brush = Brush.radialGradient(
                        colors = spec.colors,
                        center = center,
                        radius = radiusPx
                    )
                    drawRect(brush, alpha = spec.alpha)
                }
            }
            .blur(blurRadius)
    )
}
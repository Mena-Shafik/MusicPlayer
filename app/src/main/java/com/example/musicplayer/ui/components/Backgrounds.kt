package com.example.musicplayer.ui.components

import android.graphics.RenderEffect
import android.graphics.Shader
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Restored simple DynamicBlurredBackground (no Palette/artwork):
 * - Two blurred layers (soft wash + tighter blobs)
 * - Colors derived from Material dynamic color scheme when available
 * - Bottom fade to black for content contrast
 */
@Composable
fun DynamicBlurredBackground(
    modifier: Modifier = Modifier,
    washBlur: Dp = 120.dp,
    blobBlur: Dp = 48.dp,
    topFraction: Float = 0.6f,
    bottomFadeStart: Float = 0.72f,
) {
    val context = LocalContext.current
    val colorScheme = runCatching { dynamicLightColorScheme(context) }.getOrNull() ?: lightColorScheme()

    val colA = colorScheme.primary
    val colB = colorScheme.secondary
    val colC = colorScheme.primaryContainer
    val colD = colorScheme.secondaryContainer

    val density = LocalDensity.current
    val washPx = with(density) { washBlur.toPx() }
    val blobPx = with(density) { blobBlur.toPx() }

    Box(modifier = modifier.fillMaxSize()) {
        // base background color: dark to match music app aesthetic
        Box(modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background))

        // Soft large wash (heavy blur)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    renderEffect = RenderEffect.createBlurEffect(washPx, washPx, Shader.TileMode.CLAMP)
                        .asComposeRenderEffect()
                }
                .align(Alignment.TopCenter)
                .drawWithContent {
                    val w = size.width
                    val h = size.height * topFraction

                    fun large(cxF: Float, cyF: Float, rF: Float, color: Color, a: Float) {
                        val cx = w * cxF
                        val cy = h * cyF
                        val r = (w.coerceAtLeast(h)) * rF
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(color.copy(alpha = a), color.copy(alpha = a * 0.6f), Color.Transparent),
                                center = Offset(cx, cy),
                                radius = r
                            ),
                            radius = r,
                            center = Offset(cx, cy)
                        )
                    }

                    large(0.12f, 0.2f, 0.9f, colA, 0.8f)
                    large(0.84f, 0.16f, 0.7f, colB, 0.75f)
                    large(0.5f, 0.36f, 1.0f, colC, 0.65f)
                }
        )

        // Tighter blobs for color punch (moderate blur)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    renderEffect = RenderEffect.createBlurEffect(blobPx, blobPx, Shader.TileMode.CLAMP)
                        .asComposeRenderEffect()
                }
                .align(Alignment.TopCenter)
                .drawWithContent {
                    val w = size.width
                    val h = size.height * topFraction

                    fun blob(cxF: Float, cyF: Float, rF: Float, color: Color, aCenter: Float, aMid: Float) {
                        val cx = w * cxF
                        val cy = h * cyF
                        val r = (w.coerceAtLeast(h)) * rF
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(color.copy(alpha = aCenter), color.copy(alpha = aMid), Color.Transparent),
                                center = Offset(cx, cy),
                                radius = r
                            ),
                            radius = r,
                            center = Offset(cx, cy)
                        )
                    }

                    blob(0.12f, 0.20f, 0.48f, colA, 0.98f, 0.86f)
                    blob(0.82f, 0.18f, 0.44f, colB, 0.96f, 0.84f)
                    blob(0.50f, 0.40f, 0.56f, colC, 0.92f, 0.80f)
                    blob(0.94f, 0.62f, 0.28f, colD, 0.88f, 0.76f)
                }
        )

        // Bottom fade to black for contrast with UI
        Box(modifier = Modifier
            .fillMaxSize()
            .drawWithContent {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black),
                        startY = size.height * bottomFadeStart,
                        endY = size.height
                    )
                )
            }
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
fun PreviewDynamicBlurredBackground() {
    MaterialTheme {
        Surface {
            DynamicBlurredBackground()
        }
    }
}

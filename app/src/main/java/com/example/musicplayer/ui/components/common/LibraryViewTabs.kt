package com.example.musicplayer.ui.components.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text

/**
 * Pill-shaped segmented tab switcher from the "2.0" design language, e.g. for
 * Songs / Albums / Artists (/ Eras) and the music player's Up Next/Lyrics/Related sheet.
 *
 * [openFraction] is the live, externally-driven collapsed(0)->expanded(1) progress of
 * whatever container this sits in (a drag gesture, not an internal timer) — the
 * highlight's visibility and the selected label's color track it directly (1:1, no extra
 * animateXAsState wrapper) so a tab reads as "selected but not shown" while collapsed and
 * fades/slides in exactly in step with the open gesture, rather than popping in on a
 * fixed-duration timer once some boolean flips. Switching tabs while already open (a
 * discrete action, not a drag) still animates on its own via [animateDpAsState] once so
 * the highlight slides shows correctly regardless of the two motions coming from a click.
 */
@Composable
fun LibraryViewTabs(
    labels: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    openFraction: Float = 1f,
    unselectedColor: Color = Color.White.copy(alpha = 0.75f)
) {
    val outerPadding = 4.dp
    val spacing = 6.dp
    val n = labels.size

    var tabHeight by remember { mutableStateOf(34.dp) }
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.08f))
    ) {
        val contentWidth = maxWidth - outerPadding * 2
        val slotWidth = (contentWidth - spacing * (n - 1)) / n

        val targetIndex = selectedIndex.coerceIn(0, (n - 1).coerceAtLeast(0))
        val indicatorOffsetX by animateDpAsState(
            targetValue = outerPadding + (slotWidth + spacing) * targetIndex,
            animationSpec = tween(260),
            label = "tabIndicatorOffset"
        )

        Box(
            modifier = Modifier
                .offset(x = indicatorOffsetX, y = outerPadding)
                .width(slotWidth)
                .height(tabHeight)
                .scale(0.85f + 0.15f * openFraction)
                .alpha(openFraction)
                .clip(RoundedCornerShape(7.dp))
                .background(Color.White)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(outerPadding),
            horizontalArrangement = Arrangement.spacedBy(spacing)
        ) {
            labels.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                // Where this tab's label lands *if* the sheet were fully open — blended
                // against openFraction below so it only actually reads as selected once
                // the sheet has (partly) opened.
                val openSelectedColor by animateColorAsState(
                    targetValue = if (selected) Color(0xFF111111) else unselectedColor,
                    animationSpec = tween(220),
                    label = "tabPillLabel"
                )
                val labelColor = lerp(unselectedColor, openSelectedColor, openFraction)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .onSizeChanged { size ->
                            if (index == 0) tabHeight = with(density) { size.height.toDp() }
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelected(index) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = labelColor
                    )
                }
            }
        }
    }
}

@Preview(name = "LibraryViewTabs Preview", backgroundColor = 0xFF000000, showBackground = true)
@Composable
private fun LibraryViewTabsPreview() {
    MaterialTheme {
        LibraryViewTabs(
            labels = listOf("Songs", "Albums", "Artists"),
            selectedIndex = 0,
            onSelected = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

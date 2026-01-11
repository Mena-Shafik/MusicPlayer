package com.example.musicplayer.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicplayer.Util

/**
 * Render radio station tags as a horizontal scrollable list of chips.
 * Accepts either a raw tags string (comma/space separated) or a RadioStation.
 */
@Composable
fun RadioTagChips(tagsRaw: String?, modifier: Modifier = Modifier, chipBackground: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), chipContentColor: Color = MaterialTheme.colorScheme.onPrimary
) {
    val tags = Util.parseTags(tagsRaw)
    val scroll = rememberScrollState()

    if (tags.isEmpty()) return

    Row(
        modifier = modifier
            .horizontalScroll(scroll)
            .background(Color.Transparent)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (t in tags) {
            AssistChip(
                onClick = { /* maybe filter by tag in future */ },
                label = { Text(text = t, fontWeight = FontWeight.SemiBold) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = chipBackground,
                    labelColor = chipContentColor
                ),
                modifier = Modifier
            )
        }
    }
}



@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun RadioTagChipsPreview() {
    val sample = "pop rock, top40 \"classic hits\" dance"
    MaterialTheme {
        RadioTagChips(tagsRaw = sample)
    }
}

/** Convenience overload: accept an explicit list of tag strings. */
@Composable
fun RadioTagChips(
    tags: List<String>,
    modifier: Modifier = Modifier,
    chipBackground: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
    chipContentColor: Color = MaterialTheme.colorScheme.onPrimary
) {
    if (tags.isEmpty()) return
    val scroll = rememberScrollState()
    Row(
        modifier = modifier
            .horizontalScroll(scroll)
            .background(Color.Transparent)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (t in tags) {
            AssistChip(
                onClick = {},
                label = { Text(text = t, fontWeight = FontWeight.SemiBold) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = chipBackground,
                    labelColor = chipContentColor
                ),
                modifier = Modifier
            )
        }
    }
}

/**
 * Compact version of RadioTagChips with smaller size for dense layouts.
 * Uses plain Surface instead of AssistChip for a more minimal look.
 */
@Composable
fun CompactRadioTagChips(
    tagsRaw: String?,
    modifier: Modifier = Modifier,
    chipBackground: Color = Color.White.copy(alpha = 0.2f),
    chipContentColor: Color = Color.White
) {
    val tags = Util.parseTags(tagsRaw)
    val scroll = rememberScrollState()

    if (tags.isEmpty()) return

    Row(
        modifier = modifier
            .horizontalScroll(scroll),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        for (t in tags) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = chipBackground,
                modifier = Modifier
            ) {
                Text(
                    text = t,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = chipContentColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

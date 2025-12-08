package com.example.musicplayer.radio

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Render radio station tags as a horizontal scrollable list of chips.
 * Accepts either a raw tags string (comma/space separated) or a RadioStation.
 */
@Composable
fun RadioTagChips(
    tagsRaw: String?,
    modifier: Modifier = Modifier,
    chipBackground: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
    chipContentColor: Color = MaterialTheme.colorScheme.onPrimary
) {
    val tags = parseTags(tagsRaw)
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

/**
 * Parse tags from Radio Browser's tags field (space or comma separated).
 * Keep quoted segments intact if provided (e.g. "classic rock").
 */
fun parseTags(raw: String?): List<String> {
    if (raw.isNullOrBlank()) return emptyList()
    // radio-browser tags often are space-separated or comma-separated
    // Normalize commas to spaces, then split on whitespace, but keep quoted groups
    val regex = Regex("\"([^\"]+)\"|'([^']+)'|([^,\\s]+)")
    val matches = regex.findAll(raw)
    val out = matches.mapNotNull { m ->
        val g1 = m.groups[1]?.value
        val g2 = m.groups[2]?.value
        val g3 = m.groups[3]?.value
        (g1 ?: g2 ?: g3)?.trim()?.takeIf { it.isNotEmpty() }
    }.toList()
    return out
}

@Preview(showBackground = true)
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

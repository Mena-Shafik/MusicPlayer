package com.example.musicplayer.ui.components.radio

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Horizontal pill row for filtering the radio station list by genre — "All" plus
 * the distinct genres seen across the current station list. Selected pill is
 * white/black to match the library filter chips; unselected pills sit at 10%
 * white on the aurora background.
 */
@Composable
fun RadioGenreFilterRow(
    genres: List<String>,
    selectedGenre: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            GenreFilterChip(label = "All", selected = selectedGenre == null, onClick = { onSelect(null) })
        }
        items(genres) { genre ->
            GenreFilterChip(label = genre, selected = selectedGenre == genre, onClick = { onSelect(genre) })
        }
    }
}

@Composable
private fun GenreFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Color.White else Color.White.copy(alpha = 0.10f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            color = if (selected) Color(0xFF111111) else Color.White.copy(alpha = 0.8f),
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF03050C, name = "RadioGenreFilterRow Preview")
@Composable
fun RadioGenreFilterRowPreview() {
    MaterialTheme {
        RadioGenreFilterRow(
            genres = listOf("Pop", "Jazz", "Talk"),
            selectedGenre = null,
            onSelect = {}
        )
    }
}

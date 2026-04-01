package com.example.musicplayer.ui.components.song

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.scale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import com.example.musicplayer.model.Song
import com.example.musicplayer.ui.theme.MusicPlayerTheme
import kotlinx.coroutines.launch

private fun eraForYear(year: Int): String {
    return when (year) {
        in 1950..1959 -> "1950s"
        in 1960..1969 -> "1960s"
        in 1970..1979 -> "1970s"
        in 1980..1989 -> "1980s"
        in 1990..1999 -> "1990s"
        in 2000..2009 -> "2000s"
        in 2010..2019 -> "2010s"
        in 2020..2099 -> "2020s"
        else -> "Other"
    }
}

@Composable
fun EraSongList(
    songs: List<Song>,
    modifier: Modifier = Modifier,
    onSongClick: (Song) -> Unit = {},
    showOtherAsSingles: Boolean = true,
    onAddToPlaylist: (Int) -> Unit = {},
    startCollapsed: Boolean = false // optional start closed
) {
    val lightText = Color.White
    val faintText = Color(0xFFB0B0B0)
    val accentColor = Color(0xFFFFA500)

    // Group songs by era and sort by eraOrder so the most recent decade appears first
    val grouped = remember(songs) {
        val eraOrder = listOf("2020s", "2010s", "2000s", "1990s", "1980s", "1970s", "1960s", "1950s", "Other")
        songs.groupBy { eraForYear(it.year ?: 0) }
            .toSortedMap(compareBy { key ->
                val idx = eraOrder.indexOf(key)
                if (idx >= 0) idx else eraOrder.size // unknown keys go to the end
            })
    }

    // Era keys excluding 'Other' for the horizontal picker
    val eraKeys = remember(grouped) { grouped.keys.filter { it != "Other" } }
    var selectedEra by remember { mutableStateOf(eraKeys.firstOrNull() ?: "Other") }

    // Songs categorized as Other (no year or out of range)
    val otherSongs = remember(grouped) { grouped["Other"] ?: emptyList() }

    val scope = rememberCoroutineScope()
    // allow collapsing the era song list (hide/show the Crossfade only). Singles remain visible.
    var isEraCollapsed by remember { mutableStateOf(startCollapsed) }

    Column(modifier = modifier) {
        // Horizontal era cards
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            eraKeys.forEach { era ->
                EraCardItem(
                    eraName = era,
                    songCount = grouped[era]?.size ?: 0,
                    isSelected = era == selectedEra,
                    accentColor = accentColor,
                    lightText = lightText,
                    faintText = faintText,
                    onSelect = {
                        // clicking the selected era toggles collapse, selecting another era expands
                        scope.launch {
                            if (selectedEra == era) {
                                isEraCollapsed = !isEraCollapsed
                            } else {
                                selectedEra = era
                                isEraCollapsed = false
                            }
                        }
                    }
                )
            }
        }

        HorizontalDivider(thickness = 1.dp, color = Color(0xFF2A2A2A))

        // Selected era's song list with slide+fade when showing/hiding and crossfade when switching eras
        AnimatedVisibility(
            visible = !isEraCollapsed,
            enter = fadeIn(animationSpec = tween(220)) + expandVertically(animationSpec = tween(260)),
            exit = shrinkVertically(animationSpec = tween(200)) + fadeOut(animationSpec = tween(180))
        ) {
            Crossfade(targetState = selectedEra, animationSpec = tween(durationMillis = 300)) { era ->
                val itemsToShow = grouped[era] ?: emptyList()
                if (itemsToShow.isNotEmpty()) {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(itemsToShow) { song ->
                            SongCardRow(
                                song = song,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 0.dp, vertical = 6.dp),
                                onClick = { onSongClick(song) },
                                onAddToPlaylist = { onAddToPlaylist(it) }
                            )
                        }
                    }
                }
            }
        }

        // Singles section (songs without a decade/year) - always shown regardless of era collapse
        if (showOtherAsSingles && otherSongs.isNotEmpty()) {
            HorizontalDivider(thickness = 1.dp, color = Color(0xFF2A2A2A))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Singles", style = MaterialTheme.typography.titleMedium, color = lightText)
                Text(text = "${otherSongs.size}", style = MaterialTheme.typography.bodySmall, color = faintText)
            }

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(otherSongs) { song ->
                    SongCardRow(
                        song = song,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 0.dp, vertical = 6.dp),
                        onClick = { onSongClick(song) },
                        onAddToPlaylist = { onAddToPlaylist(it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EraCardItem(
    eraName: String,
    songCount: Int,
    isSelected: Boolean,
    accentColor: Color,
    lightText: Color,
    faintText: Color,
    onSelect: () -> Unit
) {
    // animated selection scale and background color
    val targetScale = if (isSelected) 1.03f else 1.0f
    val scale by animateFloatAsState(targetValue = targetScale, animationSpec = tween(200))
    val targetBg = if (isSelected) Color(0xFF1F1F1F) else Color(0xFF0F0F0F)
    val bgColor by animateColorAsState(targetValue = targetBg, animationSpec = tween(200))

    Box(
        modifier = Modifier
            .width(100.dp)
            .scale(scale)
            .background(
                color = bgColor,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onSelect() }
            .padding(12.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = eraName,
                style = MaterialTheme.typography.labelMedium,
                color = lightText,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "$songCount song${if (songCount == 1) "" else "s"}",
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) accentColor else faintText
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun EraSongListPreview() {
    MusicPlayerTheme {
                    val sample: List<Song> = listOf(
                    Song(1, null, "Great Balls of Fire", "Jerry Lee Lewis", 177000.0, "", null, 1957),
                    Song(2, null, "Purple Haze", "Jimi Hendrix", 170000.0, "", null, 1967),
                    Song(3, null, "Hotel California", "Eagles", 390000.0, "", null, 1977),
                    Song(4, null, "Take On Me", "a-ha", 225000.0, "", null, 1985),
                    Song(5, null, "Smells Like Teen Spirit", "Nirvana", 301000.0, "", null, 1991),
                    Song(6, null, "Blue (Da Ba Dee)", "Eiffel 65", 223000.0, "", null, 1999),
                    Song(7, null, "In Da Club", "50 Cent", 241000.0, "", null, 2003),
                    Song(8, null, "Rolling in the Deep", "Adele", 228000.0, "", null, 2010),
                    Song(9, null, "Blinding Lights", "The Weeknd", 200000.0, "", null, 2020)
                )
        EraSongList(songs = sample, onSongClick = {})
    }
}

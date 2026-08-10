package com.example.musicplayer.history

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.musicplayer.model.Song
import com.example.musicplayer.ui.components.background.AuroraRibbonBackground

@Composable
fun HistoryContent(
    plays: List<HistoryPlay>,
    onSongClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        if (plays.isEmpty()) {
            EmptyHistory(
                modifier = Modifier.fillMaxSize()
            )
        } else {
            HistoryList(
                plays = plays,
                onSongClick = onSongClick,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, name = "HistoryContent - Empty")
@Composable
private fun HistoryContentEmptyPreview() {
    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            AuroraRibbonBackground()
            HistoryContent(
                plays = emptyList(),
                onSongClick = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, name = "HistoryContent - With Songs")
@Composable
private fun HistoryContentWithSongsPreview() {
    val now = System.currentTimeMillis()
    val samplePlays = listOf(
        HistoryPlay(Song(101, null, "Numb", "Linkin Park", 185000.0, "", null, 2003), now - 10 * 60 * 1000),
        HistoryPlay(Song(102, null, "Viva La Vida", "Coldplay", 242000.0, "", null, 2008), now - 5 * 60 * 60 * 1000),
        HistoryPlay(Song(103, null, "Levitating", "Dua Lipa", 203000.0, "", null, 2020), now - 2L * 24 * 60 * 60 * 1000)
    )
    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            AuroraRibbonBackground()
            HistoryContent(
                plays = samplePlays,
                onSongClick = {}
            )
        }
    }
}

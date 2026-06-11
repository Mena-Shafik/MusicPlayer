package com.example.musicplayer.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.example.musicplayer.model.Song
import com.example.musicplayer.ui.components.common.MainBackground

@Composable
fun HistoryContent(
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        if (songs.isEmpty()) {
            EmptyHistory(
                modifier = Modifier.fillMaxSize()
            )
        } else {
            HistoryList(
                songs = songs,
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            MainBackground()
            HistoryContent(
                songs = emptyList(),
                onSongClick = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, name = "HistoryContent - With Songs")
@Composable
private fun HistoryContentWithSongsPreview() {
    val sampleSongs = listOf(
        Song(101, null, "Numb", "Linkin Park", 185000.0, "", null, 2003),
        Song(102, null, "Viva La Vida", "Coldplay", 242000.0, "", null, 2008),
        Song(103, null, "Levitating", "Dua Lipa", 203000.0, "", null, 2020)
    )
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            MainBackground()
            HistoryContent(
                songs = sampleSongs,
                onSongClick = {}
            )
        }
    }
}

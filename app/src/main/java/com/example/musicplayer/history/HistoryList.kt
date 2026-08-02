package com.example.musicplayer.history

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.musicplayer.model.Song
import com.example.musicplayer.ui.components.common.MainBackground

@Composable
fun HistoryList(
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        itemsIndexed(items = songs, key = { index, song -> "${song.id}_$index" }) { _, song ->
            HistoryItem(
                song = song,
                onSongClick = onSongClick
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, name = "HistoryList")
@Composable
private fun HistoryListPreview() {
    val sampleSongs = listOf(
        Song(1, null, "Call Me Maybe", "Carly Rae Jepsen", 193000.0, "", null, 2012),
        Song(2, null, "Heat Waves", "Glass Animals", 238000.0, "", null, 2020),
        Song(3, null, "Blinding Lights", "The Weeknd", 200000.0, "", null, 2019)
    )

    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp)
        ) {
            MainBackground()
            HistoryList(
                songs = sampleSongs,
                onSongClick = {}
            )
        }
    }
}

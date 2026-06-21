package com.example.musicplayer.history

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.musicplayer.model.Song
import com.example.musicplayer.ui.components.song.SongCardRow

@Composable
fun HistoryItem(
    song: Song,
    onSongClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    SongCardRow(
        song = song,
        onClick = { onSongClick(song) },
        modifier = modifier,
        showDuration = false
    )
}

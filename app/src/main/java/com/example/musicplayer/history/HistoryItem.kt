package com.example.musicplayer.history

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.musicplayer.ui.components.song.SongCardRow
import java.util.concurrent.TimeUnit

@Composable
fun HistoryItem(
    play: HistoryPlay,
    onSongClick: (com.example.musicplayer.model.Song) -> Unit,
    modifier: Modifier = Modifier
) {
    SongCardRow(
        song = play.song,
        onClick = { onSongClick(play.song) },
        modifier = modifier,
        showDuration = false,
        metaText = formatRelativeTime(play.playedAt)
    )
}

/** "Just now" / "12m ago" / "3h ago" / "5d ago", falling back to a plain day count. */
private fun formatRelativeTime(playedAt: Long): String {
    val elapsedMs = (System.currentTimeMillis() - playedAt).coerceAtLeast(0L)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(elapsedMs)
    val hours = TimeUnit.MILLISECONDS.toHours(elapsedMs)
    val days = TimeUnit.MILLISECONDS.toDays(elapsedMs)
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        else -> "${days}d ago"
    }
}

package com.example.musicplayer.history

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicplayer.model.Song
import com.example.musicplayer.ui.components.background.AuroraRibbonBackground
import java.util.Calendar

@Composable
fun HistoryList(
    plays: List<HistoryPlay>,
    onSongClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = mutableListOf<HistoryPlay>()
    val thisWeek = mutableListOf<HistoryPlay>()
    val earlier = mutableListOf<HistoryPlay>()
    for (play in plays) {
        when (dayBucket(play.playedAt)) {
            DayBucket.TODAY -> today.add(play)
            DayBucket.THIS_WEEK -> thisWeek.add(play)
            DayBucket.EARLIER -> earlier.add(play)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        if (today.isNotEmpty()) {
            item { SectionHeader("TODAY") }
            items(items = today, key = { it.song.id to it.playedAt }) { play ->
                HistoryItem(play = play, onSongClick = onSongClick)
            }
        }
        if (thisWeek.isNotEmpty()) {
            item { SectionHeader("EARLIER THIS WEEK") }
            items(items = thisWeek, key = { it.song.id to it.playedAt }) { play ->
                HistoryItem(play = play, onSongClick = onSongClick)
            }
        }
        if (earlier.isNotEmpty()) {
            item { SectionHeader("EARLIER") }
            items(items = earlier, key = { it.song.id to it.playedAt }) { play ->
                HistoryItem(play = play, onSongClick = onSongClick)
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        color = Color.White.copy(alpha = 0.5f),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
    )
}

private enum class DayBucket { TODAY, THIS_WEEK, EARLIER }

private fun dayBucket(playedAt: Long): DayBucket {
    val startOfToday = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val startOfWeek = startOfToday - 6L * 24 * 60 * 60 * 1000
    return when {
        playedAt >= startOfToday -> DayBucket.TODAY
        playedAt >= startOfWeek -> DayBucket.THIS_WEEK
        else -> DayBucket.EARLIER
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, name = "HistoryList")
@Composable
private fun HistoryListPreview() {
    val now = System.currentTimeMillis()
    val samplePlays = listOf(
        HistoryPlay(Song(1, null, "Call Me Maybe", "Carly Rae Jepsen", 193000.0, "", null, 2012), now - 10 * 60 * 1000),
        HistoryPlay(Song(2, null, "Heat Waves", "Glass Animals", 238000.0, "", null, 2020), now - 3 * 60 * 60 * 1000),
        HistoryPlay(Song(3, null, "Blinding Lights", "The Weeknd", 200000.0, "", null, 2019), now - 3L * 24 * 60 * 60 * 1000)
    )

    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp)
        ) {
            AuroraRibbonBackground()
            HistoryList(
                plays = samplePlays,
                onSongClick = {}
            )
        }
    }
}

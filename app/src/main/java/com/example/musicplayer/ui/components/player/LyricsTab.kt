package com.example.musicplayer.ui.components.player

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicplayer.model.Song
import com.example.musicplayer.util.Util
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun LyricsTab(currentSong: Song?, modifier: Modifier = Modifier, contentColor: Color = Color.White) {
    var loading by remember { mutableStateOf(false) }
    // Keep lyrics in state so UI updates when cache fills
    var lyrics by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    // Only fetch when the currentSong changes and we don't already have a cached value on the song
    LaunchedEffect(currentSong?.id) {
        val tag = "LyricsTab"
        if (currentSong == null) {
            lyrics = null
            loading = false
            return@LaunchedEffect
        }

        // If the Song instance already has lyricsFetched, use its cached value (may be null)
        if (currentSong.lyricsFetched) {
            lyrics = currentSong.lyrics
            loading = false
            try { Log.d(tag, "Cache hit: lyricsFetched=true length=${currentSong.lyrics?.length ?: 0} for '${currentSong.title}'") } catch (_: Throwable) {}
            return@LaunchedEffect
        }

        loading = true
        try { Log.d(tag, "Begin fetch lyrics for '${currentSong.title}' by '${currentSong.artist}'") } catch (_: Throwable) {}
        // API-first: try online lyrics, then fallback to embedded file lyrics
        val fetched: String? = withContext(Dispatchers.IO) {
            try {
                val apiResult = try {
                    val result = Util.fetchLyricsOnline(currentSong)
                    // Ensure result is a String before using it
                    if (result is String) result else null
                } catch (_: Throwable) {
                    null
                }
                if (!apiResult.isNullOrBlank()) {
                    try { Log.d(tag, "Loaded lyrics from API, length=${apiResult.length} title='${currentSong.title}'") } catch (_: Throwable) {}
                    return@withContext apiResult
                } else {
                    try { Log.d(tag, "API returned no lyrics; attempting embedded for '${currentSong.title}'") } catch (_: Throwable) {}
                }
                null
            } catch (t: Throwable) {
                try { Log.w(tag, "Exception while fetching lyrics: ${t.message}", t) } catch (_: Throwable) {}
                null
            }
        }
        // Store on the song instance (may be null) and mark fetched
        currentSong.lyrics = fetched
        currentSong.lyricsFetched = true
        lyrics = fetched
        loading = false
        try { Log.d(tag, "Fetch complete for '${currentSong.title}' length=${fetched?.length ?: 0}") } catch (_: Throwable) {}
    }

    val scrollState = rememberScrollState()

    // Outer container matches the height available for lyrics; we will center only the spinner inside it.
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        if (loading) {
            // Center only the spinner
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFFFA500))
            }
        } else if (lyrics.isNullOrBlank()) {
            // Show not-available message in normal flow (top-left within the lyrics area)
            Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Text(text = "Lyrics not available", color = contentColor)
            }
        } else {
            // Put lyrics in a vertically-scrollable container so long lyrics are fully visible
            Column(modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(8.dp)
            ) {
                // Add extra blank lines after the first N lines to improve readability on small screens
                val spaced = Util.addSpacingToFirstLines(lyrics, firstLines = 5) ?: lyrics ?: ""
                Text(
                    text = spaced,
                    color = contentColor,
                    fontWeight = FontWeight.Bold,
                    style = TextStyle(lineHeight = 15.sp)
                )
            }
        }
    }
}

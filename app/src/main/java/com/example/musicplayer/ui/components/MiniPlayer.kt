package com.example.musicplayer.ui.components

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.musicplayer.R
import com.example.musicplayer.util.Util
import com.example.musicplayer.model.Song
import com.example.musicplayer.service.PlayerIntentBuilder
import com.example.musicplayer.service.PlayerRepository

@Composable
fun MiniPlayer(
    modifier: Modifier = Modifier,
    onOpenPlayer: (Song?) -> Unit = {}
) {
    val playlist by PlayerRepository.playlist.collectAsState()
    val currentIndex by PlayerRepository.currentIndex.collectAsState()
    val isPlaying by PlayerRepository.isPlaying.collectAsState()
    val positionMs by PlayerRepository.positionMs.collectAsState()
    val durationMs by PlayerRepository.durationMs.collectAsState()
    val current = playlist.getOrNull(currentIndex)
    val context = LocalContext.current
    // read preview mode inside a composable context
    val isPreviewMode = LocalInspectionMode.current

    // If there's no playlist and no current song, don't show the mini player
    if (playlist.isEmpty() && current == null) return

    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .fillMaxWidth()
                .height(72.dp)
                .background(Color(0xFF0F0F0F).copy(alpha = 0.95f))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // album art (left) - tappable to open full player
            val art = remember(current?.path) {
                try { current?.path?.let { Util.getAlbumArt(context, it) } } catch (_: Throwable) { null }
            }

            val imageModifier = Modifier
                .width(56.dp)
                .height(56.dp)
                .clip(RoundedCornerShape(6.dp))

            if (art != null) {
                Image(
                    bitmap = art,
                    contentDescription = "Album art",
                    modifier = imageModifier.clickable { onOpenPlayer(current) },
                    contentScale = ContentScale.Crop
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.ic_album),
                    contentDescription = "Album art",
                    modifier = imageModifier.clickable { onOpenPlayer(current) },
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = current?.title ?: "",
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = current?.artist ?: "",
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            IconButton(onClick = {
                val appCtx = context.applicationContext
                Log.d("MiniPlayer", "play/pause clicked isPlaying=$isPlaying appCtx=$appCtx")
                if (isPreviewMode) {
                    // in preview toggle repository state only
                    PlayerRepository.setIsPlaying(!PlayerRepository.isPlaying.value)
                } else {
                    // Optimistically update UI state so the button feels responsive, then send intent to service.
                    PlayerRepository.setIsPlaying(!isPlaying)
                    if (isPlaying) PlayerIntentBuilder.startPause(appCtx) else PlayerIntentBuilder.startPlay(appCtx)
                }
            }) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.PauseCircle else Icons.Filled.PlayCircle,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    Modifier.size(60.dp,60.dp),
                    tint = Color.White
                )
            }
        }

        // Progress indicator (determinate) — use LinearProgressIndicator instead of a slider
        val duration = durationMs
        val position = positionMs.coerceAtMost(duration)
        val progress = remember(position, duration) {
            if (duration > 0L) {
                (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
            } else 0f
        }

        Column(modifier = Modifier
            .fillMaxWidth()) {
            // determinate progress bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(2.dp, 0.dp, 2.dp, 0.dp),
                color = Color(0xFFFFA500),
                trackColor = Color(0xFFFFDAB9)
            )
        }
    }
}


@Preview(showBackground = true, name = "MiniPlayer Preview", backgroundColor = 0xFF000000)
@Composable
private fun MiniPlayerPreview() {
    // Prepare a small sample playlist with empty paths so placeholder art is used in preview
    val sampleSongs = listOf(
        Song(id = 1, title = "Preview Song", artist = "Preview Artist", duration = 180000.0, path = ""),
        Song(id = 2, title = "Another Track", artist = "Artist Two", duration = 200000.0, path = "")
    )

    // populate PlayerRepository with sample data for preview
    LaunchedEffect(Unit) {
        PlayerRepository.setPlaylist(sampleSongs, 0)
        PlayerRepository.setIsPlaying(false)
    }

    MaterialTheme {
        Box(modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)) {
            MiniPlayer(modifier = Modifier.align(Alignment.Center))
        }
    }
}
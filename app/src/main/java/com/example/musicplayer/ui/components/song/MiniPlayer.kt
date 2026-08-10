package com.example.musicplayer.ui.components.song

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicplayer.R
import com.example.musicplayer.util.Util
import com.example.musicplayer.model.Song
import com.example.musicplayer.service.PlayerIntentBuilder
import com.example.musicplayer.service.PlayerStateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun MiniPlayer(
    modifier: Modifier = Modifier,
    onOpenPlayer: (Song?) -> Unit = {}
) {
    val playlist by PlayerStateManager.playlist.collectAsState()
    val currentIndex by PlayerStateManager.currentIndex.collectAsState()
    val isPlaying by PlayerStateManager.isPlaying.collectAsState()
    val positionMs by PlayerStateManager.positionMs.collectAsState()
    val durationMs by PlayerStateManager.durationMs.collectAsState()
    val current = playlist.getOrNull(currentIndex)
    val context = LocalContext.current
    // read preview mode inside a composable context
    val isPreviewMode = LocalInspectionMode.current

    // If there's no playlist and no current song, don't show the mini player
    if (playlist.isEmpty() && current == null) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.45f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // album art (left) - tappable to open full player
            var displayArt by remember(current?.path) { mutableStateOf<ImageBitmap?>(null) }

            LaunchedEffect(current?.path) {
                displayArt = null
                if (current?.path?.isNotBlank() == true) {
                    displayArt = withContext(Dispatchers.IO) {
                        try {
                            // First try embedded album art
                            var bitmap = Util.getAlbumArt(context, current.path)

                            // If no embedded art, fetch from web
                            if (bitmap == null) {
                                Log.d("MiniPlayer", "No embedded artwork for '${current.title}', fetching from web...")
                                val webUrl = Util.getAlbumArtWebUrl(current)
                                if (webUrl != null) {
                                    bitmap = Util.loadBitmapFromUrl(webUrl)
                                    if (bitmap != null) {
                                        Log.d("MiniPlayer", "✓ Loaded web album art for '${current.title}'")
                                    }
                                }
                            }
                            bitmap
                        } catch (_: Throwable) {
                            null
                        }
                    }
                }
            }

            val imageModifier = Modifier
                .width(44.dp)
                .height(44.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF2A2A2A))
                .clickable { onOpenPlayer(current) }

            if (displayArt != null) {
                Image(
                    bitmap = displayArt!!,
                    contentDescription = "Album art",
                    modifier = imageModifier,
                    contentScale = ContentScale.Crop
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.ic_album),
                    contentDescription = "Album art",
                    modifier = imageModifier.padding(10.dp),
                    contentScale = ContentScale.Crop,
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color(0xFF5A5A5A))
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = current?.title ?: "",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = current?.artist ?: "",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .clickable {
                        val appCtx = context.applicationContext
                        Log.d("MiniPlayer", "play/pause clicked isPlaying=$isPlaying appCtx=$appCtx")
                        if (isPreviewMode) {
                            // in preview toggle repository state only
                            PlayerStateManager.setIsPlaying(!PlayerStateManager.isPlaying.value)
                        } else {
                            // Optimistically update UI state so the button feels responsive, then send intent to service.
                            PlayerStateManager.setIsPlaying(!isPlaying)
                            if (isPlaying) PlayerIntentBuilder.startPause(appCtx) else PlayerIntentBuilder.startPlay(appCtx)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(24.dp),
                    tint = Color(0xFF111111)
                )
            }
        }

        // Thin progress bar matching the redesign (track + orange fill), not Material's
        // default LinearProgressIndicator styling.
        val duration = durationMs
        val position = positionMs.coerceAtMost(duration)
        val progress = remember(position, duration) {
            if (duration > 0L) {
                (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
            } else 0f
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(1.5.dp))
                .background(Color.White.copy(alpha = 0.15f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = progress)
                    .background(Color(0xFFFFA500))
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}


@Preview(showBackground = true, name = "MiniPlayer Preview", backgroundColor = 0xFF000000)
@Composable
private fun MiniPlayerPreview() {
    // Prepare a small sample playlist with empty paths so placeholder art is used in preview
    val sampleSongs = listOf(
        Song(id = 1, null, title = "Preview Song", artist = "Preview Artist", duration = 180000.0, path = "",album = null,2000),
        Song(id = 2, null, title = "Another Track", artist = "Artist Two", duration = 200000.0, path = "",album = null,2000)
    )

    // populate PlayerRepository with sample data for preview
    LaunchedEffect(Unit) {
        PlayerStateManager.setPlaylist(sampleSongs, 0)
        PlayerStateManager.setIsPlaying(false)
    }

    MaterialTheme {
        Box(modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)) {
            MiniPlayer(modifier = Modifier.align(Alignment.Center))
        }
    }
}

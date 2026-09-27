package com.example.musicplayer.ui.components.song

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicplayer.model.Song
import androidx.compose.ui.graphics.ImageBitmap
import com.example.musicplayer.ui.components.player.AlbumArt

// Thumbnail geometry, shared with MusicPlayerChrome so its art can morph out of this exact spot.
val MiniPlayerArtInset = DpOffset(14.dp, 6.dp)
val MiniPlayerArtSize = 44.dp

// Stateless docked bar; tap or drag up expands; drag deltas and release velocity are px (positive = downward).
@Composable
fun MiniPlayer(
    song: Song,
    isPlaying: Boolean,
    progress: Float,
    onPlayPause: () -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
    onDrag: (Float) -> Unit = {},
    onDragStopped: (velocity: Float) -> Unit = {},
    // Loaded once by the host and shared with the full player, so neither reloads it.
    artBitmap: ImageBitmap? = null,
    // Hidden while the full player's own art is morphing out of this spot.
    artAlpha: Float = 1f,
    // Dropped to 0 while the host's panel (same color) is drawn behind it, so the morphing art isn't dimmed through the bar.
    backgroundAlpha: Float = 1f
) {
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnDragStopped by rememberUpdatedState(onDragStopped)
    // draggable + clickable arbitrate with each other (a drag past slop cancels the click), unlike two raw pointerInput detectors.
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.94f * backgroundAlpha))
            .draggable(
                state = rememberDraggableState { currentOnDrag(it) },
                orientation = Orientation.Vertical,
                onDragStopped = { velocity -> currentOnDragStopped(velocity) }
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onExpand
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MiniPlayerArtInset.x, vertical = MiniPlayerArtInset.y)
                .height(MiniPlayerArtSize),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AlbumArt(
                bitmap = artBitmap,
                modifier = Modifier.size(MiniPlayerArtSize).graphicsLayer { alpha = artAlpha }
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, end = 8.dp)
            ) {
                Text(
                    text = song.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .clickable { onPlayPause() },
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
                    .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                    .background(Color(0xFFFFA500))
            )
        }
        Box(Modifier.height(6.dp))
    }
}

@Preview(name = "MiniPlayer", backgroundColor = 0xFF14261C, showBackground = true, widthDp = 400)
@Composable
private fun MiniPlayerPreview() {
    MaterialTheme {
        Box(Modifier.fillMaxSize()) {
            MiniPlayer(
                song = Song(1, "Afterglow", "Nova Reyes", 238000.0, "", "Neon Parallels"),
                isPlaying = true,
                progress = 0.4f,
                onPlayPause = {},
                onExpand = {},
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

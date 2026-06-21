package com.example.musicplayer.ui.components.song

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.util.Log
import com.example.musicplayer.R
import com.example.musicplayer.model.Song
import com.example.musicplayer.util.Util
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SongCardRow(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onAddToPlaylist: (songId: Int) -> Unit = {},
    onRemoveFromPlaylist: (songId: Int) -> Unit = {},
    isInPlaylist: Boolean = false,
    showDuration: Boolean = true
) {
    val context = LocalContext.current
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(song.path) {
        imageBitmap = null  // reset when song changes
        val hasValidSongFilePath = song.path.isNotBlank() && song.path != "-"
        if (hasValidSongFilePath) {
            imageBitmap = withContext(Dispatchers.IO) {
                try {
                    // First try to get embedded album art from the song file.
                    var bitmap = Util.getAlbumArt(context, song.path)

                    // Only use web lookup as a fallback when the song file has no embedded art.
                    if (bitmap == null) {
                        Log.d("SongCardRow", "No embedded artwork for '${song.title}', fetching from web...")
                        val webUrl = Util.getAlbumArtWebUrl(song)
                        if (webUrl != null) {
                            bitmap = Util.loadBitmapFromUrl(webUrl)
                            if (bitmap != null) {
                                Log.d("SongCardRow", "✓ Loaded web album art for '${song.title}'")
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

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            //.background(Color.Black) // removed so list items are semi-transparent over the background
            .clickable(onClick = onClick)
            .padding(10.dp,0.dp,0.dp,0.dp)
    ) {
        val imgModifier = Modifier
            .width(60.dp)
            .height(60.dp)
            .clip(RoundedCornerShape(2.dp))

        Crossfade(
            targetState = imageBitmap,
            animationSpec = tween(500),
            label = "Album art crossfade"
        ) { bitmap ->
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "Album art",
                    contentScale = ContentScale.Crop,
                    modifier = imgModifier
                )
            } else {
                // Show placeholder while loading or if no album art found
                Image(
                    painter = painterResource(id = R.drawable.ic_album),
                    contentDescription = "Placeholder image",
                    contentScale = ContentScale.Crop,
                    modifier = imgModifier
                )
            }
        }

        Column(Modifier
            .padding(start = 12.dp)
            .weight(1f)) {
            Text(
                text = song.title,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1
            )
            Text(
                text = song.artist,
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1
            )
        }

        if (showDuration) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Util.converter(song.duration),
                    modifier = Modifier
                        .width(80.dp)
                        .padding(10.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.End
                )
            }
        }

        // Menu button for Add to Playlist
        IconButton(onClick = { menuExpanded = true }) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Open menu",
                tint = Color.White
            )
        }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            if (isInPlaylist) {
                DropdownMenuItem(
                    text = { Text("Remove from Playlist") },
                    onClick = {
                        onRemoveFromPlaylist(song.id)
                        menuExpanded = false
                    }
                )
            } else {
                DropdownMenuItem(
                    text = { Text("Add to Playlist") },
                    onClick = {
                        onAddToPlaylist(song.id)
                        menuExpanded = false
                    }
                )
            }
        }
    }
}

@Preview( name = "SongCardRow Preview", backgroundColor = 0xFF000000)
@Composable
fun CardPreview() {
    // Use the real SongCardRow so preview shows the same UI and placeholder logic
    MaterialTheme {
        Surface(color = Color.Black) {
            SongCardRow(
                song = Song(id = 1, null, title = "Title", artist = "Artist", duration = 260000.0, path = "",album = null,2000),
                onClick = {},
                isInPlaylist = false
            )
        }
    }
}

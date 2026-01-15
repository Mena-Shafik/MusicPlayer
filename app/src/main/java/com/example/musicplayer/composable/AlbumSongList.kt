package com.example.musicplayer.composable

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.example.musicplayer.model.Song
import com.example.musicplayer.Util

/**
 * Displays songs organized by album. Horizontal scrollable album cards on top,
 * and a list of songs from the selected album below.
 */
@Composable
fun AlbumSongList(
    songs: List<Song>,
    modifier: Modifier = Modifier,
    onSongClick: (Song) -> Unit = {},
    showSingles: Boolean = true,
) {
    val lightText = Color.White
    val dividerColor = Color(0xFF2A2A2A)
    val faintText = Color(0xFFB0B0B0)
    val accentColor = Color(0xFF1DB954) // Spotify green for selected album
    val context = LocalContext.current

    // Group songs by album
    val grouped = remember(songs) {
        songs.groupBy { (it.album?.takeIf { a -> a.isNotBlank() } ?: "Unknown Album") }
            .toSortedMap(String.CASE_INSENSITIVE_ORDER)
    }

    // Partition into albums and singles
    val albumsMulti = grouped.filter { (_, list) -> list.size >= 2 }
    val singlesList = grouped.filter { (album, list) -> list.size == 1 || album == "Unknown Album" }
        .flatMap { it.value }

    // Track selected album
    val allAlbumNames = remember(albumsMulti) {
        albumsMulti.keys.toList()
    }
    val (selectedAlbum, setSelectedAlbum) = remember { mutableStateOf(allAlbumNames.firstOrNull() ?: "") }
    val selectedSongs = remember(selectedAlbum, albumsMulti) {
        albumsMulti[selectedAlbum] ?: emptyList()
    }

    Surface(
        modifier = modifier,
        color = Color.Black,
        contentColor = lightText
    ) {
        Column(modifier = Modifier.background(Color.Black)) {
            // Horizontal scrollable album cards with covers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                albumsMulti.keys.forEach { albumName ->
                    val firstSongInAlbum = albumsMulti[albumName]?.firstOrNull()
                    val albumArtBitmap = firstSongInAlbum?.let { Util.getAlbumArt(context, it.path) }
                    AlbumCardItem(
                        albumName = albumName,
                        songCount = albumsMulti[albumName]?.size ?: 0,
                        isSelected = albumName == selectedAlbum,
                        accentColor = accentColor,
                        lightText = lightText,
                        faintText = faintText,
                        albumArtBitmap = albumArtBitmap,
                        onSelect = { setSelectedAlbum(albumName) }
                    )
                }
            }

            HorizontalDivider(thickness = 1.dp, color = dividerColor)

            // Song list for selected album
            if (selectedSongs.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    items(selectedSongs) { song ->
                        SongCardRow(
                            song = song,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 0.dp, vertical = 6.dp),
                            onClick = { onSongClick(song) }
                        )
                    }
                }
            }

            // Singles section (optional)
            if (showSingles && singlesList.isNotEmpty()) {
                HorizontalDivider(thickness = 1.dp, color = dividerColor)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Singles",
                        style = MaterialTheme.typography.titleMedium,
                        color = lightText
                    )
                    Text(
                        text = "${singlesList.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = faintText
                    )
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                ) {
                    items(singlesList) { song ->
                        SongCardRow(
                            song = song,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 0.dp, vertical = 4.dp),
                            onClick = { onSongClick(song) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlbumCardItem(
    albumName: String,
    songCount: Int,
    isSelected: Boolean,
    accentColor: Color,
    lightText: Color,
    faintText: Color,
    albumArtBitmap: androidx.compose.ui.graphics.ImageBitmap?,
    onSelect: () -> Unit,
) {
    Box(
        modifier = Modifier
            .width(160.dp)
            .background(
                color = if (isSelected) Color(0xFF1A1A1A) else Color(0xFF0F0F0F),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onSelect() }
            .padding(8.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Album cover image
            if (albumArtBitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = albumArtBitmap,
                    contentDescription = albumName,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Placeholder when no cover art
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(Color(0xFF2A2A2A), shape = RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "♪",
                        style = MaterialTheme.typography.headlineLarge,
                        color = faintText
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Album name and song count
            Text(
                text = albumName,
                style = MaterialTheme.typography.labelMedium,
                color = lightText,
                maxLines = 1,
                overflow = TextOverflow.Visible,
                modifier = Modifier
                    .fillMaxWidth()
                    .basicMarquee(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "$songCount song${if (songCount == 1) "" else "s"}",
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) accentColor else faintText
            )
        }
    }
}

@Preview
@Composable
private fun AlbumSongListPreview() {
    val demo = listOf(
        Song(1, "Hello", "A", 100.0, "p1", album = "Alpha"),
        Song(2, "World", "B", 100.0, "p2", album = "Alpha"),
        Song(3, "Other", "C", 100.0, "p3", album = "Beta"),
        Song(4, "Another", "A", 100.0, "p4", album = "Beta"),
        Song(5, "Single", "D", 100.0, "p5", album = null),
    )
    Surface { AlbumSongList(songs = demo) }
}

@Preview
@Composable
private fun AlbumSongListPreview_MultiAlbum() {
    val demo = listOf(
        Song(1, "Track 1", "A", 100.0, "p1", album = "Alpha"),
        Song(2, "Track 2", "B", 100.0, "p2", album = "Alpha"),
        Song(3, "Track 3", "C", 100.0, "p3", album = "Beta"),
        Song(4, "Track 4", "D", 100.0, "p4", album = "Beta"),
        Song(5, "Track 5", "E", 100.0, "p5", album = "Gamma"),
        Song(6, "Track 6", "F", 100.0, "p6", album = "Gamma"),
    )
    Surface { AlbumSongList(songs = demo) }
}

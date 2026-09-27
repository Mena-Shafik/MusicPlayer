package com.example.musicplayer.ui.components.song

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.util.Log
import com.example.musicplayer.R
import com.example.musicplayer.model.Song
import com.example.musicplayer.service.PlayerStateManager
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
    showDuration: Boolean = true,
    // When set, shown instead of the duration (e.g. a relative "2h ago" in History).
    metaText: String? = null,
    // When set, a leading track-number label (e.g. playlist detail's numbered tracks).
    indexLabel: String? = null,
    // Trailing icon glyph -- defaults to "add" (library rows); playlist detail passes the overflow-menu glyph since the dropdown here means "remove from playlist", not "add".
    trailingIcon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Filled.Add,
    // Shows "on device" (check_circle) / "streamable from server" (cloud_download) next to the duration; only meaningful once Navidrome is connected -- callers pass this off the connected pref, not off `song` alone, so it stays off everywhere else.
    showAvailabilityBadge: Boolean = false,
    // When false, trailingIcon renders as a plain static glyph (no IconButton/DropdownMenu) -- e.g. the Up Next queue's drag_handle icon, a visual affordance only, not a clickable add/remove action like every other caller's trailing icon.
    trailingIconClickable: Boolean = true
) {
    val context = LocalContext.current
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }

    val currentPlaylist by PlayerStateManager.playlist.collectAsState()
    val currentIndex by PlayerStateManager.currentIndex.collectAsState()
    val isPlaying by PlayerStateManager.isPlaying.collectAsState()
    val isCurrentlyPlaying = isPlaying && currentPlaylist.getOrNull(currentIndex)?.id == song.id

    LaunchedEffect(song.path, song.isRemote, song.remoteCoverArtUrl) {
        imageBitmap = null  // reset when song changes
        if (song.isRemote) {
            // Remote rows load cover art from Subsonic's dedicated getCoverArt endpoint -- pulling embedded tags out of the full audio stream (the local-file path below) would mean downloading the whole track just to read a picture.
            val url = song.remoteCoverArtUrl
            if (url != null) {
                imageBitmap = withContext(Dispatchers.IO) {
                    try { Util.loadBitmapFromUrl(url) } catch (_: Throwable) { null }
                }
            }
        } else {
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
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        if (indexLabel != null) {
            Text(
                text = indexLabel,
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.widthIn(min = 20.dp)
            )
        }

        val imgModifier = Modifier
            .width(52.dp)
            .height(52.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF2A2A2A))

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
                    modifier = imgModifier.padding(13.dp),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color(0xFF5A5A5A))
                )
            }
        }

        Column(Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = Color.White,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (isCurrentlyPlaying) {
            PlayingEqualizer()
        }

        if (showAvailabilityBadge) {
            if (song.isRemote) {
                Icon(
                    imageVector = Icons.Filled.CloudDownload,
                    contentDescription = "Streamable from server",
                    tint = Color.White.copy(alpha = 0.45f),
                    modifier = Modifier.size(19.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "On this device",
                    tint = Color(0xFFFFA500),
                    modifier = Modifier.size(19.dp)
                )
            }
        }

        if (metaText != null) {
            Text(
                text = metaText,
                modifier = Modifier.widthIn(min = 34.dp),
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 12.sp,
                textAlign = TextAlign.End
            )
        } else if (showDuration) {
            Text(
                text = Util.converter(song.duration),
                modifier = Modifier.widthIn(min = 34.dp),
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                textAlign = TextAlign.End
            )
        }

        if (trailingIconClickable) {
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = trailingIcon,
                        contentDescription = if (isInPlaylist) "Playlist options" else "Add to playlist",
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.width(20.dp)
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
        } else {
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.45f),
                modifier = Modifier.width(20.dp)
            )
        }
    }
}

/** Three animated bars indicating the row's song is the one currently playing. */
@Composable
private fun PlayingEqualizer() {
    val transition = rememberInfiniteTransition(label = "eq")
    val periodsMs = listOf(747, 907, 1067)
    val heights = periodsMs.map { period ->
        transition.animateFloat(
            initialValue = 0.15f,
            targetValue = 0.95f,
            animationSpec = infiniteRepeatable(
                animation = tween(period, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "eqBar"
        )
    }
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.height(18.dp)
    ) {
        heights.forEach { h ->
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeightFraction(h.value)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFFFA500))
            )
        }
    }
}

private fun Modifier.fillMaxHeightFraction(fraction: Float): Modifier =
    this.height((18.dp) * fraction)

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

@Preview(name = "SongCardRow — availability badges", backgroundColor = 0xFF000000)
@Composable
private fun CardAvailabilityBadgePreview() {
    val localSong = Song(id = 1, null, title = "Afterglow", artist = "Nova Reyes", duration = 238000.0, path = "", album = "Neon Parallels", 2003)
    val remoteSong = Song(id = -2, null, title = "Bright Static", artist = "Kaya Mott", duration = 224000.0, path = "https://music.example.com/rest/stream.view", album = "Bright Static", 2019).apply {
        isRemote = true
    }
    MaterialTheme {
        Surface(color = Color.Black) {
            Column {
                SongCardRow(song = localSong, onClick = {}, showAvailabilityBadge = true)
                SongCardRow(song = remoteSong, onClick = {}, showAvailabilityBadge = true)
            }
        }
    }
}

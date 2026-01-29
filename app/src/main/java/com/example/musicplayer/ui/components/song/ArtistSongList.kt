package com.example.musicplayer.ui.components.song

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import android.util.Log
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import com.example.musicplayer.model.Artist
import com.example.musicplayer.util.ArtistUtil
import com.example.musicplayer.model.Song

private fun splitArtists(raw: String): List<String> {
    val base = raw.ifBlank { "Unknown Artist" }

    Log.d("splitArtists", "════ PROCESSING: '$base' ════")

    // Exception list: band names that should NOT be split even if they contain separators
    val bandExceptions = setOf(
        "crosby, stills & nash",
        "crosby, stills, nash & young",
        "csn",
        "csny",
        "king & queen"
    )

    val baseLower = base.lowercase().trim()

    // Check band exceptions FIRST - if matched, return as-is without splitting
    for (exception in bandExceptions) {
        if (baseLower == exception) {
            Log.d("splitArtists", "✓ BAND EXCEPTION: '$base' -> keeping as single artist")
            return listOf(base.trim())
        }
    }

    // Normalize ampersands, plus signs, slashes, and 'x' to commas for consistency
    val normalized = base
        .replace(" & ", ", ")
        .replace("&", ",")
        .replace(" + ", ", ")
        .replace("+", ",")
        .replace(" / ", ", ")
        .replace("/", ",")
        .replace(Regex("\\s+x\\s+"), ", ")  // Handle "artist x artist" format (case-insensitive)

    Log.d("splitArtists", "  → After normalization: '$normalized'")

    // First, remove any featuring/feat/ft artists (everything after feat/featuring/ft)
    val withoutFeaturing = normalized
        .split(Regex("\\s+(feat|featuring|ft)(?:\\.)?\\s+", RegexOption.IGNORE_CASE), limit = 2)[0]
        .trim()

    Log.d("splitArtists", "  → After removing featuring: '$withoutFeaturing'")

    // Then split on commas to get individual artists
    val artists = withoutFeaturing
        .split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    Log.d("splitArtists", "  → RESULT: $artists")
    return if (artists.isNotEmpty()) artists else listOf("Unknown Artist")
}

private fun normalizeArtistKey(name: String): String {
    val trimmed = name.trim().lowercase()
    val noThe = if (trimmed.startsWith("the ")) trimmed.removePrefix("the ") else trimmed
    // remove simple punctuation around words
    return noThe.replace(Regex("^[^a-z0-9]+|[^a-z0-9]+$"), "")
}

@Composable
fun ArtistSongList(
    songs: List<Song>,
    modifier: Modifier = Modifier,
    onSongClick: (Song) -> Unit = {}
) {
    // Build artist map: normalized key -> Artist object + songs
    val artistMap = remember(songs) {
        val map = linkedMapOf<String, Artist>()
        val songsByArtist = linkedMapOf<String, MutableList<Song>>()

        for (s in songs) {
            val names = splitArtists(s.artist)
            for (name in names) {
                val normalizedKey = normalizeArtistKey(name.ifBlank { "Unknown Artist" })

                // Create Artist object if not exists
                if (!map.containsKey(normalizedKey)) {
                    map[normalizedKey] = Artist(
                        name = name.trim(),
                        normalizedKey = normalizedKey,
                        songCount = 0
                    )
                }

                // Add song to this artist's list
                songsByArtist.getOrPut(normalizedKey) { mutableListOf() }.add(s)
            }
        }

        // Update song counts - always update regardless of count
        map.forEach { (key, artist) ->
            val count = songsByArtist[key]?.size ?: 0
            map[key] = artist.withSongCount(count)
            Log.d("ArtistSongList", "ARTIST: '${artist.name}' | KEY: '$key' | SONGS: $count")
        }

        Pair(map, songsByArtist.mapValues { it.value.toList() })
    }

    val artists = remember(artistMap) { artistMap.first.values.toList().sortedBy { it.name } }
    val songsByArtistKey = remember(artistMap) { artistMap.second }

    var selectedArtistKey by remember(artists) {
        mutableStateOf(artists.firstOrNull()?.normalizedKey ?: normalizeArtistKey("Unknown Artist"))
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Horizontal artist cards with images
        if (artists.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(artists, key = { it.normalizedKey }) { artist ->
                    val isSelected = artist.normalizedKey == selectedArtistKey
                    ArtistCard(
                        artist = artist,
                        selected = isSelected,
                        onClick = { selectedArtistKey = artist.normalizedKey }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Crossfade the currently selected artist's content
        Crossfade(targetState = selectedArtistKey, animationSpec = tween(300), label = "Artist switch") { key ->
            val selectedArtist = artists.find { it.normalizedKey == key }
            val artistSongs = songsByArtistKey[key] ?: emptyList()
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item(key = "header_$key") {
                    if (selectedArtist != null) {
                        Text(
                            text = selectedArtist.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
                itemsIndexed(artistSongs, key = { _, s -> s.id }) { _, song ->
                    SongCardRow(
                        song = song,
                        onClick = { onSongClick(song) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistCard(
    artist: Artist,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: () -> Unit = {}
) {
    // Fetch image asynchronously with Coil
    var imageUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(artist.name) {
        Log.i("ArtistCard", "━━━ FETCHING IMAGE FOR: '${artist.name}' ━━━")
        try {
            // artist.name is already the primary artist (extracted by splitArtists)
            // No need to extract again
            imageUrl = ArtistUtil.getArtistImageUrl(artist.name)
            if (imageUrl != null) {
                Log.d("ArtistCard", "✓ IMAGE FOUND: ${imageUrl!!.take(60)}...")
            } else {
                Log.e("ArtistCard", "✗ NO IMAGE FOUND for '${artist.name}'")
            }
        } catch (e: Exception) {
            Log.e("ArtistCard", "✗ ERROR: ${e.message}")
            imageUrl = null
        }
    }

    // Debug logging for song count
    LaunchedEffect(artist.name, artist.songCount) {
        Log.d("ArtistCard", "DISPLAY: '${artist.name}' | SONGS: ${artist.songCount}")
    }

    Card(
        modifier = modifier
            .size(width = 120.dp, height = 150.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Background image or gradient
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(300)
                        .build(),
                    contentDescription = artist.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop,
                    onLoading = { Log.d("AsyncImage", "⏳ LOADING: '${artist.name}'") },
                    onSuccess = { Log.d("AsyncImage", "✓ LOADED: '${artist.name}'") },
                    onError = { Log.e("AsyncImage", "✗ FAILED: '${artist.name}' | URL: $imageUrl") }
                )
            } else {
                // Fallback gradient background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF2A2A2A),
                                    Color(0xFF1A1A1A)
                                )
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    // Initials in center for no-image fallback
                    val initials = remember(artist.name) {
                        artist.name.split(" ", limit = 2)
                            .map { it.firstOrNull()?.uppercase() ?: "" }
                            .joinToString("")
                    }
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.displayMedium,
                        color = Color.White.copy(alpha = 0.3f),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            // Gradient overlay at bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.8f)
                            )
                        )
                    )
            )

            // Text content at bottom
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = artist.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "${artist.songCount} ${if (artist.songCount == 1) "song" else "songs"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) Color(0xFFFFA500) else Color.White.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Selected indicator border
            if (selected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Transparent)
                        .border(
                            width = 3.dp,
                            color = Color(0xFFFFA500),
                            shape = RoundedCornerShape(12.dp)
                        )
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun ArtistSongListPreview() {
    val sample: List<Song> = listOf(
        Song(id = 1, title = "Song A", artist = "Artist One", duration = 180000.0, path = "/demo/a.mp3", album = "Album X"),
        Song(id = 2, title = "Song B", artist = "Artist One", duration = 200000.0, path = "/demo/b.mp3", album = "Album X"),
        Song(id = 3, title = "Song C", artist = "Artist Two", duration = 210000.0, path = "/demo/c.mp3", album = "Album Y")
    )
    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        ArtistSongList(songs = sample)
    }
}

package com.example.musicplayer.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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

    // Exception list: band names that should NOT be split even if they contain separators
    val bandExceptions = setOf(
        "crosby, stills & nash",
        "crosby, stills, nash & young",
        "csn",
        "csny"
    )

    val baseLower = base.lowercase()
    if (bandExceptions.any { baseLower.contains(it) }) {
        return listOf(base.trim())
    }

    // Common separators: comma, ampersand, slash, "feat." or "ft." and "with"
    val parts = base
        .replace("feat.", ",", ignoreCase = true)
        .replace("ft.", ",", ignoreCase = true)
        .replace(" with ", ",", ignoreCase = true)
        .split(',', '&', '/', '+')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
    return if (parts.isEmpty()) listOf("Unknown Artist") else parts
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

        // Update song counts
        map.forEach { (key, artist) ->
            val count = songsByArtist[key]?.size ?: 0
            if (count > 0) {
                map[key] = artist.withSongCount(count)
            }
        }

        Pair(map, songsByArtist.mapValues { it.value.toList() })
    }

    val artists = remember(artistMap) { artistMap.first.values.toList() }
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
    Card(
        modifier = modifier
            .size(120.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = if (selected) Color(0xFF2A2A2A) else Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // Fetch image asynchronously with Coil
            var imageUrl by remember { mutableStateOf<String?>(null) }
            var isLoading by remember { mutableStateOf(true) }

            LaunchedEffect(artist.name) {
                Log.d("ArtistCard", "LaunchedEffect triggered for artist: ${artist.name}")
                isLoading = true
                try {
                    imageUrl = ArtistUtil.getArtistImageUrl(artist.name)
                    Log.d("ArtistCard", "Image URL for ${artist.name}: ${imageUrl?.take(80) ?: "NULL"}")
                } catch (e: Exception) {
                    Log.e("ArtistCard", "Error fetching image for ${artist.name}: ${e.message}")
                    imageUrl = null
                } finally {
                    isLoading = false
                }
            }

            if (!imageUrl.isNullOrBlank()) {
                Log.d("ArtistCard", "Rendering AsyncImage for ${artist.name}")
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(300)
                        .transformations(CircleCropTransformation())
                        .build(),
                    contentDescription = artist.name,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(40.dp)),
                    contentScale = ContentScale.Crop,
                    onLoading = { Log.d("ArtistCard", "Loading image for ${artist.name}") },
                    onSuccess = { Log.d("ArtistCard", "Successfully loaded image for ${artist.name}") },
                    onError = { Log.e("ArtistCard", "Failed to load image URL for ${artist.name}: $imageUrl") }
                )
            } else {
                if (!isLoading) {
                    Log.d("ArtistCard", "No image URL for ${artist.name}, showing initials")
                }
                // Fallback: initials circle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(40.dp))
                        .background(Color(0xFF2A2A2A))
                ) {
                    val initials = remember(artist.name) {
                        artist.name.split(" ", limit = 2).map { it.firstOrNull()?.uppercase() ?: "" }.joinToString("")
                    }
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = artist.name,
                style = MaterialTheme.typography.bodySmall,
                color = if (selected) Color.White else Color.White.copy(alpha = 0.85f),
                maxLines = 1
            )
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

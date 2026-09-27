package com.example.musicplayer.songlist

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicplayer.model.Song
import com.example.musicplayer.ui.components.song.SongCardRow

private data class ArtistMatch(val name: String, val songCount: Int)

private fun matchingArtists(allSongs: List<Song>, query: String): List<ArtistMatch> {
    val q = query.trim().lowercase()
    if (q.isBlank()) return emptyList()
    val counts = linkedMapOf<String, Int>()
    for (song in allSongs) {
        val name = song.artist.trim()
        if (name.isBlank()) continue
        if (name.lowercase().contains(q)) {
            counts[name] = (counts[name] ?: 0) + 1
        }
    }
    return counts.map { (name, count) -> ArtistMatch(name, count) }.sortedBy { it.name.lowercase() }
}

// Dedicated full-screen search takeover (replaces the old inline search-bar-in-app-bar): own search field, kind filter chips, results grouped into SONGS/ARTISTS with a live count, and a dashed-border "no results" state; local-only, no server/catalogue distinction.
@Composable
fun SearchResultsScreen(
    query: String,
    onQueryChange: (String) -> Unit,
    onCancel: () -> Unit,
    allSongs: List<Song>,
    matchedSongs: List<Song>,
    onSongClick: (Song) -> Unit,
    onArtistClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var kindFilter by remember { mutableIntStateOf(0) } // 0=All 1=Songs 2=Artists
    val artists = remember(allSongs, query) { matchingArtists(allSongs, query) }
    val showSongs = kindFilter == 0 || kindFilter == 1
    val showArtists = kindFilter == 0 || kindFilter == 2
    val hasQuery = query.isNotBlank()
    val hasResults = matchedSongs.isNotEmpty() || artists.isNotEmpty()

    Column(modifier = modifier.fillMaxSize()) {
        // Search field + Cancel
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .padding(top = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.10f))
                    .border(1.5.dp, Color(0xFFFFA500), RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = "Songs, albums, artists",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 15.sp
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium),
                        cursorBrush = SolidColor(Color(0xFFFFA500)),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (query.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Clear",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(19.dp)
                            .clickable { onQueryChange("") }
                    )
                }
            }
            Text(
                text = "Cancel",
                color = Color(0xFFFFA500),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onCancel)
            )
        }

        // Kind filter chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 14.dp)
        ) {
            listOf("All", "Songs", "Artists").forEachIndexed { index, label ->
                val selected = kindFilter == index
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(15.dp))
                        .background(if (selected) Color.White else Color.White.copy(alpha = 0.10f))
                        .clickable { kindFilter = index }
                        .padding(horizontal = 13.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = label,
                        color = if (selected) Color(0xFF111111) else Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                    )
                }
            }
        }

        if (!hasQuery) {
            return@Column
        }

        if (!hasResults) {
            NoResultsState(query = query, onBrowseAll = onCancel)
            return@Column
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (showSongs && matchedSongs.isNotEmpty()) {
                item {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(top = 18.dp, bottom = 8.dp)
                    ) {
                        Text(
                            text = "SONGS",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = "${matchedSongs.size}",
                            color = Color(0xFFFFA500),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                items(items = matchedSongs, key = { "song_${it.id}" }) { song ->
                    SongCardRow(song = song, onClick = { onSongClick(song) }, showDuration = true)
                }
            }
            if (showArtists && artists.isNotEmpty()) {
                item {
                    Text(
                        text = "ARTISTS",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(top = 16.dp, bottom = 8.dp)
                    )
                }
                items(items = artists, key = { "artist_${it.name}" }) { artist ->
                    ArtistResultRow(artist = artist, onClick = { onArtistClick(artist.name) })
                }
            }
        }
    }
}

@Composable
private fun ArtistResultRow(artist: ArtistMatch, onClick: () -> Unit) {
    val initials = remember(artist.name) {
        artist.name.split(" ", limit = 2).map { it.firstOrNull()?.uppercase() ?: "" }.joinToString("")
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color.White.copy(alpha = 0.09f))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(22.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = initials, color = Color.White.copy(alpha = 0.75f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = artist.name,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${artist.songCount} ${if (artist.songCount == 1) "song" else "songs"}",
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 12.sp
            )
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.4f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun NoResultsState(query: String, onBrowseAll: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 26.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(16.dp))
            .padding(20.dp)
    ) {
        Text(
            text = "NO RESULTS",
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp
        )
        Text(
            text = "Nothing for “$query”",
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp)
        )
        Text(
            text = "Check the spelling, or search by artist instead.",
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 13.sp,
            lineHeight = 19.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            modifier = Modifier
                .padding(top = 14.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(1.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                .clickable(onClick = onBrowseAll)
                .padding(horizontal = 18.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.LibraryMusic,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Text(text = "Browse all songs", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

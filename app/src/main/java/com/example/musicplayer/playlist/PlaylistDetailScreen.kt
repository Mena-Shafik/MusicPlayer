package com.example.musicplayer.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.musicplayer.model.Playlist
import com.example.musicplayer.model.Song
import com.example.musicplayer.music.MusicPlayerViewModel
import com.example.musicplayer.navidrome.NavidromeRepository
import com.example.musicplayer.navigation.NavRoutes
import com.example.musicplayer.preferences.PreferencesManager
import com.example.musicplayer.service.PlayerStateManager
import com.example.musicplayer.service.PlayerDockController
import com.example.musicplayer.ui.components.background.AuroraRibbonBackground
import com.example.musicplayer.ui.components.common.MainBackground
import com.example.musicplayer.ui.components.common.dashedBorder
import com.example.musicplayer.ui.components.song.SongCardRow
import com.example.musicplayer.util.Util
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Local device songs plus the Navidrome catalogue (if connected), so playlists can resolve/display remote songIds the same way the song list's "All" tab does.
private suspend fun loadAllSongsIncludingCatalogue(context: android.content.Context): List<Song> {
    val local = withContext(Dispatchers.IO) { Util.getAllAudioFromDevice(context) }
    val connected = PreferencesManager.getNavidromeConnectedFlow(context).first()
    if (!connected) return local
    val remote = try {
        NavidromeRepository(context).listCatalogueSongs()
    } catch (e: Exception) {
        emptyList()
    }
    return local + remote
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    navController: NavHostController,
    playlist: Playlist,
    allSongs: List<Song> = emptyList()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val viewModel: PlaylistViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return PlaylistViewModel(context) as T
            }
        }
    )

    val playerVm: MusicPlayerViewModel = viewModel()

    var currentPlaylist by remember { mutableStateOf(playlist) }
    var songs by remember { mutableStateOf(allSongs) }
    val playlistSongs: List<Song> = remember(currentPlaylist.songIds, songs) {
        songs.filter { it.id in currentPlaylist.songIds }
    }

    // Load all songs on first composition
    LaunchedEffect(context) {
        scope.launch {
            songs = loadAllSongsIncludingCatalogue(context)
        }
    }

    val shuffleEnabled by PlayerStateManager.shuffleEnabled.collectAsState()
    val totalMinutes = remember(playlistSongs) { (playlistSongs.sumOf { it.duration } / 60000).toInt() }

    // Background drawn full-screen behind the whole Scaffold (including the top bar), not just the content area below it -- otherwise a "transparent" bar just shows the Scaffold's own flat containerColor instead of this blur.
    Box(modifier = Modifier.fillMaxSize()) {
    AuroraRibbonBackground()
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        navController.navigate(NavRoutes.PlaylistAddSongs.createRoute(currentPlaylist.id))
                    }) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = "Add Songs", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Cover + identity header
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                ) {
                    if (playlistSongs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(124.dp)
                                .dashedBorder(1.5.dp, Color.White.copy(alpha = 0.22f), 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(124.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF2A2A2A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = null,
                                tint = Color(0xFF5A5A5A),
                                modifier = Modifier.size(52.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f).padding(bottom = 2.dp)) {
                        Text(
                            text = "PLAYLIST",
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.4.sp
                        )
                        Text(
                            text = currentPlaylist.name,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            lineHeight = 32.sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                        Text(
                            text = if (playlistSongs.isEmpty()) "Empty · 0 min" else "${playlistSongs.size} songs · $totalMinutes min",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }

                if (currentPlaylist.description.isNotBlank()) {
                    Text(
                        text = currentPlaylist.description,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        modifier = Modifier.padding(horizontal = 20.dp).padding(top = 16.dp)
                    )
                } else if (playlistSongs.isEmpty()) {
                    Text(
                        text = "Nothing in here yet. Add a few songs and it will start filling out.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        modifier = Modifier.padding(horizontal = 20.dp).padding(top = 16.dp)
                    )
                }

                // Actions
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    if (playlistSongs.isEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color.White)
                                .clickable { navController.navigate(NavRoutes.PlaylistAddSongs.createRoute(currentPlaylist.id)) }
                        ) {
                            Icon(imageVector = Icons.Filled.Add, contentDescription = null, tint = Color(0xFF111111))
                            Text("Add songs", color = Color(0xFF111111), fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(start = 8.dp))
                        }
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .border(1.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
                                .clickable { navController.navigate(NavRoutes.PlaylistAddSongs.createRoute(currentPlaylist.id)) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Filled.Search, contentDescription = "Find songs", tint = Color.White.copy(alpha = 0.8f))
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color.White)
                                .clickable {
                                    playerVm.setPlaylist(context, playlistSongs, 0)
                                    playerVm.play(context)
                                }
                        ) {
                            Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null, tint = Color(0xFF111111))
                            Text("Play", color = Color(0xFF111111), fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(start = 8.dp))
                        }
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .border(1.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
                                .clickable { PlayerStateManager.toggleShuffle(!shuffleEnabled) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Shuffle,
                                contentDescription = "Shuffle",
                                tint = if (shuffleEnabled) Color(0xFFFFA500) else Color.White.copy(alpha = 0.8f)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .border(1.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
                                .clickable { navController.navigate(NavRoutes.PlaylistAddSongs.createRoute(currentPlaylist.id)) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Filled.Add, contentDescription = "Add songs", tint = Color.White.copy(alpha = 0.8f))
                        }
                    }
                }

                if (playlistSongs.isNotEmpty()) {
                    Text(
                        text = "TRACKS",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 8.dp)
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        itemsIndexed(playlistSongs) { index, song ->
                            SongCardRow(
                                song = song,
                                indexLabel = "${index + 1}",
                                trailingIcon = Icons.Filled.MoreVert,
                                onClick = {
                                    // Play the song from this playlist
                                    playerVm.setPlaylist(context, playlistSongs, index)
                                    PlayerStateManager.setCurrentIndex(index)
                                    playerVm.play(context)
                                    PlayerDockController.requestExpand()
                                },
                                isInPlaylist = true,
                                onRemoveFromPlaylist = { songId ->
                                    val updated = currentPlaylist.copy(
                                        songIds = currentPlaylist.songIds.filter { it != songId }
                                    )
                                    currentPlaylist = updated
                                    viewModel.updatePlaylist(updated)
                                }
                            )
                        }
                    }
                } else {
                    // Suggested songs from the library so the next tap is on this screen.
                    val suggested = remember(songs, currentPlaylist.songIds) {
                        songs.filterNot { it.id in currentPlaylist.songIds }.take(6)
                    }
                    if (suggested.isNotEmpty()) {
                        Text(
                            text = "SUGGESTED FROM YOUR LIBRARY",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.2.sp,
                            modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 8.dp)
                        )
                        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                            itemsIndexed(suggested) { _, song ->
                                SongCardRow(
                                    song = song,
                                    showDuration = false,
                                    onClick = {
                                        val updated = currentPlaylist.copy(songIds = currentPlaylist.songIds + song.id)
                                        currentPlaylist = updated
                                        viewModel.updatePlaylist(updated)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistAddSongsScreen(
    navController: NavHostController,
    playlistId: Long,
    allSongs: List<Song> = emptyList()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val viewModel: PlaylistViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return PlaylistViewModel(context) as T
            }
        }
    )

    val playlists by viewModel.playlists.collectAsState()
    val currentPlaylist = remember(playlistId, playlists) {
        playlists.find { it.id == playlistId }
    }

    var songs by remember { mutableStateOf(allSongs) }
    var selectedSongs by remember { mutableStateOf(setOf<Int>()) }

    // Load all songs on first composition
    LaunchedEffect(context) {
        scope.launch {
            songs = loadAllSongsIncludingCatalogue(context)
            // Initialize with songs already in playlist
            currentPlaylist?.let {
                selectedSongs = it.songIds.toSet()
            }
        }
    }

    // Background drawn full-screen behind the whole Scaffold (including the top bar), not just the content area below it -- otherwise a "transparent" bar just shows the Scaffold's own flat containerColor instead of this blur.
    Box(modifier = Modifier.fillMaxSize()) {
    AuroraRibbonBackground()
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Add Songs",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            currentPlaylist?.let { playlist ->
                                val updatedPlaylist = playlist.copy(songIds = selectedSongs.toList())
                                viewModel.updatePlaylist(updatedPlaylist)
                                navController.navigateUp()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Save playlist",
                            tint = Color(0xFFFFA500)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                itemsIndexed(songs) { _, song ->
                    val isSelected = song.id in selectedSongs

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedSongs = if (isSelected) selectedSongs - song.id else selectedSongs + song.id
                            }
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF2A2A2A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Album,
                                contentDescription = null,
                                tint = Color(0xFF5A5A5A),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title ?: "Unknown",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                            Text(
                                text = song.artist ?: "Unknown Artist",
                                color = Color.White.copy(alpha = 0.65f),
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(if (isSelected) Color(0xFFFFA500) else Color.Transparent)
                                .border(2.dp, if (isSelected) Color(0xFFFFA500) else Color.White.copy(alpha = 0.3f), RoundedCornerShape(13.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Selected",
                                    tint = Color(0xFF111111),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    }
}

// Previews
@OptIn(ExperimentalMaterial3Api::class)
@androidx.compose.ui.tooling.preview.Preview(showSystemUi = true, backgroundColor = 0xFF000000, showBackground = true)
@Composable
fun PlaylistDetailScreenPreview() {
    val testPlaylist = Playlist(
        id = 1234567890L,
        name = "My Favorites",
        description = "Songs I love",
        songIds = listOf(1, 2, 3),
        createdAt = System.currentTimeMillis()
    )

    val testSongs = listOf(
        // Use outer Song constructor: (id, track, title, artist, duration, path, album?, year)
        Song(1, null, "Song One", "Artist A", 240000.0, "", null, 2000),
        Song(2, null, "Song Two", "Artist B", 180000.0, "", null, 2001),
        Song(3, null, "Song Three", "Artist C", 200000.0, "", null, 2002)
    )

    MaterialTheme {
        PlaylistDetailScreen(
            navController = androidx.navigation.compose.rememberNavController(),
            playlist = testPlaylist,
            allSongs = testSongs
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@androidx.compose.ui.tooling.preview.Preview(showSystemUi = true, backgroundColor = 0xFF000000, showBackground = true)
@Composable
fun PlaylistDetailScreenEmptyPreview() {
    val testPlaylist = Playlist(
        id = 1234567890L,
        name = "Empty Playlist",
        description = "No songs yet",
        songIds = emptyList(),
        createdAt = System.currentTimeMillis()
    )

    MaterialTheme {
        PlaylistDetailScreen(
            navController = androidx.navigation.compose.rememberNavController(),
            playlist = testPlaylist,
            allSongs = emptyList()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@androidx.compose.ui.tooling.preview.Preview(showSystemUi = true, backgroundColor = 0xFF000000, showBackground = true)
@Composable
fun PlaylistAddSongsScreenPreview() {
    val testPlaylist = Playlist(
        id = 1234567890L,
        name = "My Playlist",
        songIds = listOf(1, 3),
        createdAt = System.currentTimeMillis()
    )

    val testSongs = listOf(
        Song(1, null, "Song One", "Artist A", 240000.0, "", null, 2000),
        Song(2, null, "Song Two", "Artist B", 180000.0, "", null, 2001),
        Song(3, null, "Song Three", "Artist C", 200000.0, "", null, 2002),
        Song(4, null, "Song Four", "Artist D", 210000.0, "", null, 2003),
        Song(5, null, "Song Five", "Artist E", 190000.0, "", null, 2004)
    )

    MaterialTheme {
        PlaylistAddSongsScreen(
            navController = rememberNavController(),
            playlistId = testPlaylist.id,
            allSongs = testSongs
        )
    }
}

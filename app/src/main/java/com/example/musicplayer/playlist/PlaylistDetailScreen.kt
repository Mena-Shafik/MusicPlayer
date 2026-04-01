package com.example.musicplayer.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.musicplayer.model.Playlist
import com.example.musicplayer.model.Song
import com.example.musicplayer.music.MusicPlayerViewModel
import com.example.musicplayer.navigation.NavRoutes
import com.example.musicplayer.service.PlayerStateManager
import com.example.musicplayer.ui.components.common.MainBackground
import com.example.musicplayer.ui.components.song.SongCardRow
import com.example.musicplayer.util.Util
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
            val all = withContext(Dispatchers.IO) { Util.getAllAudioFromDevice(context) }
            songs = all
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = currentPlaylist.name,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${playlistSongs.size} songs",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
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
                    IconButton(onClick = {
                        navController.navigate(NavRoutes.PlaylistAddSongs.createRoute(currentPlaylist.id))
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add Songs",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            MainBackground()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (playlistSongs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "No songs in this playlist",
                                color = Color.Gray,
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Button(
                                onClick = {
                                    navController.navigate(NavRoutes.PlaylistAddSongs.createRoute(currentPlaylist.id))
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFFA500)
                                ),
                                modifier = Modifier.padding(top = 16.dp)
                            ) {
                                Text("Add Songs", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        itemsIndexed(playlistSongs) { index, song ->
                            SongCardRow(
                                song = song,
                                onClick = {
                                    // Play the song from this playlist
                                    playerVm.setPlaylist(context, playlistSongs, index)
                                    PlayerStateManager.setCurrentIndex(index)
                                    playerVm.play(context)
                                    navController.navigate(NavRoutes.MusicPlayer.createRoute(song.id))
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
            val all = withContext(Dispatchers.IO) { Util.getAllAudioFromDevice(context) }
            songs = all
            // Initialize with songs already in playlist
            currentPlaylist?.let {
                selectedSongs = it.songIds.toSet()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Add Songs to Playlist",
                        fontWeight = FontWeight.Bold,
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.background(Color.Black)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            MainBackground()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    itemsIndexed(songs) { _, song ->
                        val isSelected = song.id in selectedSongs

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isSelected) Color.DarkGray.copy(alpha = 0.5f) else Color.Transparent)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title ?: "Unknown",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = song.artist ?: "Unknown Artist",
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            IconButton(
                                onClick = {
                                    selectedSongs = if (isSelected) {
                                        selectedSongs - song.id
                                    } else {
                                        selectedSongs + song.id
                                    }
                                },
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSelected) Icons.Filled.Check else Icons.Filled.Add,
                                    contentDescription = if (isSelected) "Remove from selection" else "Add to playlist",
                                    tint = if (isSelected) Color(0xFFFFA500) else Color.White
                                )
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

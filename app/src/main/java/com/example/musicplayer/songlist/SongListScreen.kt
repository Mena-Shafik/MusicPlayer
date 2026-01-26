package com.example.musicplayer.songlist

import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.musicplayer.util.Util
import com.example.musicplayer.ui.components.AlbumSongList
import com.example.musicplayer.ui.components.ArtistSongList
import com.example.musicplayer.ui.components.MainAppBar
import com.example.musicplayer.ui.components.MainBackground
import com.example.musicplayer.ui.components.MiniPlayer
import com.example.musicplayer.ui.components.RadioCardRow
import com.example.musicplayer.ui.components.SongCardRow
import com.example.musicplayer.ui.components.AddToPlaylistDialog
import com.example.musicplayer.model.Song
import com.example.musicplayer.music.MusicPlayerViewModel
import com.example.musicplayer.navigation.NavRoutes
import com.example.musicplayer.radio.RadioPlayerService
import com.example.musicplayer.service.PlayerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import kotlin.collections.getOrNull

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun ListSongsScreen(
    navController: NavHostController,
    // New params: allow parent to control whether the top app bar / search UI is shown.
    showTopBar: Boolean = true,
    showSearch: Boolean = false,
    onToggleSearch: () -> Unit = {},
    queryExternal: String? = null,
    onQueryChangeExternal: (String) -> Unit = {},
    onSearchedClickedExternal: (String) -> Unit = {}
) {
    // Get context first for passing to ViewModel
    val context = LocalContext.current
    val viewModel: SongListViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return SongListViewModel(context = context) as T
            }
        }
    )

    // If the parent doesn't manage search visibility (the common case), keep local state
    var searchVisible by remember { mutableStateOf(showSearch) }
    val toggleSearch: () -> Unit = {
        try { onToggleSearch() } catch (_: Throwable) {}
        searchVisible = !searchVisible
    }
    // album view state comes from viewModel

    // Use persistent radio selection stored in the SongListViewModel so selection
    // survives navigation (e.g., returning from RadioPlayerScreen)
    val isRadioSelected by viewModel.isRadioSelected.collectAsState()
    val toggleRadio: () -> Unit = { viewModel.toggleRadioSelected() }
    val isAlbumView by viewModel.isAlbumView.collectAsState()
    val isArtistView by viewModel.isArtistView.collectAsState()

    // removed local showSearch state; parent may control it via the new params

    // load and filter songs
    /*val view = LocalView.current
    val activity = LocalContext.current as? Activity
    val isPreviewMode = LocalInspectionMode.current

    SideEffect {
        if (!isPreviewMode && activity != null) {
            // Set a dark background so white status text is visible; use Transparent if you prefer
            activity.window.statusBarColor = Color.Black.toArgb()
            // Ensure status bar icons/text are *not* the "light" variant (i.e. force white icons/text)
            WindowCompat.getInsetsController(activity.window, view)?.isAppearanceLightStatusBars = false
        }
    }*/

    LaunchedEffect(context) {
        val all = withContext(Dispatchers.IO) { Util.getAllAudioFromDevice(context) }
        viewModel.load(all)
    }

    val songs by viewModel.filteredSongs.collectAsState()
    val queryLocal by viewModel.query.collectAsState()
    // prefer external query if provided (keeps parent and vm in sync)
    val query = queryExternal ?: queryLocal

    // when external change provided, update viewModel as well
    val onQueryChange: (String) -> Unit = { new ->
        try { onQueryChangeExternal(new) } catch (_: Throwable) {}
        viewModel.setQuery(new)
    }
    val onSearchedClicked: (String) -> Unit = { text ->
        try { onSearchedClickedExternal(text) } catch (_: Throwable) {}
        viewModel.setQuery(text)
    }

    // playback state used to decide whether to show the mini player
    val isPlaying by PlayerRepository.isPlaying.collectAsState()
    val positionMs by PlayerRepository.positionMs.collectAsState()
    // show mini when we have a playlist and playback has actually started (either playing, or paused with a non-zero position)
    val showMini = (isPlaying || positionMs > 0L)

    // Debug logging
    LaunchedEffect(isPlaying, positionMs, showMini) {
        Log.d("SongListScreen", "isPlaying=$isPlaying positionMs=$positionMs showMini=$showMini")
    }

    // Pull-to-refresh state (re-enabled)
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val pullRefreshState = rememberPullRefreshState(isRefreshing, onRefresh = {
        scope.launch {
            isRefreshing = true
            try {
                val all = withContext(Dispatchers.IO) { Util.getAllAudioFromDevice(context) }
                viewModel.load(all)
            } catch (_: Throwable) {
                // ignore refresh errors
            } finally {
                isRefreshing = false
            }
        }
    })

    // Add to Playlist dialog state
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var selectedSongIdForPlaylist by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            if (showTopBar) {
                MainAppBar(
                    showSearch = searchVisible,
                    onToggleSearch = toggleSearch,
                    isRadio = isRadioSelected,
                    onToggleRadio = toggleRadio,
                    query = query,
                    onQueryChange = { onQueryChange(it) },
                    onSearchedClicked = { onSearchedClicked(it) },
                    onOpenSettings = { navController.navigate(NavRoutes.Settings.route) },
                    onOpenPlaylists = { navController.navigate(NavRoutes.Playlists.route) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pullRefresh(pullRefreshState)
                .background(Color.Black)
        ) {
            MainBackground()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Use a dedicated MusicPlayerViewModel to start playback so setPlaylist + startPlay are atomic
                val playerVm: MusicPlayerViewModel = viewModel()

                // Song/Radio list takes remaining space above mini player
                Box(modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()) {
                    if (isRadioSelected) {
                        // When radio is selected, show the radio stations list UI
                        DisplayListRadioStations(navController = navController, viewModel = viewModel)
                    } else {
                        when {
                            isAlbumView -> {
                                // Sort by album: horizontal album cards + song list
                                AlbumSongList(
                                    songs = songs,
                                    modifier = Modifier.fillMaxSize(),
                                    onSongClick = { song ->
                                        // Find index for playback selection
                                        val index = songs.indexOfFirst { it.id == song.id }
                                        if (index >= 0) {
                                            playerVm.setPlaylist(context, songs, index)
                                            PlayerRepository.setCurrentIndex(index)
                                            playerVm.play(context)
                                            navController.navigate(NavRoutes.MusicPlayer.createRoute(song.id))
                                        }
                                    }
                                )
                            }
                            isArtistView -> {
                                // Sort by artist: grouped list by artist name
                                ArtistSongList(
                                    songs = songs,
                                    modifier = Modifier.fillMaxSize(),
                                    onSongClick = { song ->
                                        // Find index for playback selection
                                        val index = songs.indexOfFirst { it.id == song.id }
                                        if (index >= 0) {
                                            playerVm.setPlaylist(context, songs, index)
                                            PlayerRepository.setCurrentIndex(index)
                                            playerVm.play(context)
                                            navController.navigate(NavRoutes.MusicPlayer.createRoute(song.id))
                                        }
                                    }
                                )
                            }
                            else -> {
                                DisplayListSongs(
                                    songs = songs,
                                    modifier = Modifier.fillMaxSize(),
                                    onSongClicked = { index ->
                                        val selected = songs.getOrNull(index)
                                        if (selected != null) {
                                            playerVm.setPlaylist(context, songs, index)
                                            PlayerRepository.setCurrentIndex(index)
                                            playerVm.play(context)
                                            navController.navigate(NavRoutes.MusicPlayer.createRoute(selected.id))
                                        }
                                    },
                                    onAddToPlaylist = { songId ->
                                        selectedSongIdForPlaylist = songId
                                        showAddToPlaylistDialog = true
                                    }
                                )
                            }
                        }
                    }
                }

                // show the mini player only when playback is active so it doesn't take layout space while idle
                if (showMini) {
                    MiniPlayer(
                        modifier = Modifier.fillMaxWidth(),
                        onOpenPlayer = { selectedSong ->
                            selectedSong?.let { navController.navigate(NavRoutes.MusicPlayer.createRoute(it.id)) }
                        }
                    )
                }
            }

            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                contentColor = Color(0xFFFFA500),
                backgroundColor = Color.Black.copy(alpha = 0.7f)
            )

        }
    }

    // Show Add to Playlist dialog when triggered
    if (showAddToPlaylistDialog && selectedSongIdForPlaylist != null) {
        AddToPlaylistDialog(
            songId = selectedSongIdForPlaylist!!,
            onDismiss = {
                @Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
                showAddToPlaylistDialog = false
                @Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
                selectedSongIdForPlaylist = null
            },
            onConfirm = { _ ->
                // Dialog confirmed, clearing state
                @Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
                showAddToPlaylistDialog = false
                @Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
                selectedSongIdForPlaylist = null
            }
        )
    }
}

/*@RequiresApi(Build.VERSION_CODES.M)
@Preview(showBackground = true)
@Composable
fun SAP() {
    androidx.compose.material.MaterialTheme {
        Surface {
            SearchBar("test", {}, {}, {})
        }
    }

}*/


/*
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppBar(
    showSearch: Boolean,
    onToggleSearch: () -> Unit,
    onToggleRadio: () -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchedClicked: (String) -> Unit
) {
    if (showSearch) {
        TopAppBar(
            title = {
                SearchBar(
                    text = query,
                    onTextChange = onQueryChange,
                    onCloseClicked = {
                        onQueryChange("")
                        onToggleSearch()
                    },
                    onSearchedClicked = {
                        onSearchedClicked(it)
                    }
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        )
    } else {
        TopAppBar(
            title = {
                Text(
                    text = "Songs",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            actions = {
                IconButton(onClick = onToggleSearch) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search Icon",
                        tint = Color.White
                    )
                }

                IconButton(onClick = onToggleRadio) {
                    Icon(
                        imageVector = Icons.Filled.Radio,
                        contentDescription = "Switch to Radio or Search",
                        tint = Color.White
                    )
                }
            },

            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            modifier = Modifier.statusBarsPadding()
        )
    }
}*/

@Composable
fun DisplayListSongs(
    songs: List<Song>,
    modifier: Modifier = Modifier,
    onSongClicked: (Int) -> Unit = {},
    onAddToPlaylist: (Int) -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth(), // removed background(Color.Black)
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            itemsIndexed(songs) { index, song ->
                SongCardRow(
                    song = song,
                    onClick = { onSongClicked(index) },
                    onAddToPlaylist = onAddToPlaylist
                )
            }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun DisplayListRadioStations(modifier: Modifier = Modifier, navController: NavHostController, viewModel: SongListViewModel = viewModel()) {
    val context = LocalContext.current

    // Use built-in default stations provided by the ViewModel
    val stations by viewModel.userStations.collectAsState()
    // Keep the radio loading/error flows for compatibility, but UI shows defaults
    val loading by viewModel.radioLoading.collectAsState()
    val error by viewModel.radioError.collectAsState()

    // Load the default (hard-coded) stations when this composable enters composition
    LaunchedEffect(Unit) { viewModel.loadDefaultUserStations() }

    Column(modifier = modifier.fillMaxWidth()) {
        when {
            loading -> {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            error != null -> {
                Text(text = error ?: "Unknown error", color = Color.White, modifier = Modifier.padding(12.dp))
            }

            else -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    itemsIndexed(stations) { idx, station ->
                        val displayName = Util.extractQuotedOrOriginal(station.name).ifBlank { station.name ?: "Unknown" }

                        RadioCardRow(
                            station = station,
                            displayName = displayName,
                            modifier = Modifier.fillMaxWidth(),
                            onPlay = {
                                val url = station.url ?: ""
                                if (url.isBlank()) {
                                    Toast.makeText(context, "No stream URL for $displayName", Toast.LENGTH_SHORT).show()
                                    return@RadioCardRow
                                }

                                try {
                                    val svcIntent = Intent().apply {
                                        action = RadioPlayerService.ACTION_PLAY_STATION
                                        putExtra(RadioPlayerService.EXTRA_STATION_URL, url)
                                        putExtra(RadioPlayerService.EXTRA_STATION_TITLE, displayName)
                                        putExtra(RadioPlayerService.EXTRA_STATION_FAVICON, station.favicon)
                                        putExtra(RadioPlayerService.EXTRA_STATION_TAGS, station.tags)
                                        putParcelableArrayListExtra(RadioPlayerService.EXTRA_STATION_LIST, ArrayList(stations))
                                        putExtra(RadioPlayerService.EXTRA_STATION_INDEX, idx)
                                        setClassName(context.packageName, "com.example.musicplayer.radio.RadioPlayerService")
                                    }
                                    ContextCompat.startForegroundService(context, svcIntent)
                                    Log.d("DisplayListRadioStations", "Started RadioPlayerService for $displayName -> $url index=$idx size=${stations.size}")
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Failed to start radio service: ${e.message}", Toast.LENGTH_SHORT).show()
                                }

                                try { viewModel.setRadioSelected(true) } catch (_: Throwable) {}
                                val favicon = station.favicon ?: ""
                                val tagsRaw = station.tags ?: ""
                                try { Log.d("DisplayListRadioStations", "Navigating to player: name=$displayName url=$url favicon=$favicon tags=$tagsRaw") } catch (_: Throwable) {}
                                navController.navigate(NavRoutes.RadioPlayer.createRoute(displayName, url, favicon, tagsRaw))
                            }
                        )
                    }
                }
            }
        }
    }
}













@Preview(showSystemUi = true, name = "DisplayList Preview", backgroundColor = 0xFF000000, showBackground = true)
@Composable
fun DisplayListPreview() {
    MaterialTheme {
        val sampleSongs = listOf(
            Song(id = 1, title = "Preview Song", artist = "Preview Artist", duration = 180000.0, path = ""),
            Song(id = 2, title = "Another Track", artist = "Artist Two", duration = 200000.0, path = ""),
            Song(id = 3, title = "Another Track", artist = "Artist Three", duration = 200000.0, path = ""),
            Song(id = 4, title = "Another Track", artist = "Artist Four", duration = 200000.0, path = "")
        )
        Scaffold(
            topBar = {
                MainAppBar(
                    showSearch = false,
                    onToggleSearch = {},
                    isRadio = false,
                    onToggleRadio = {},
                    query = "",
                    onQueryChange = {},
                    onSearchedClicked = {},
                    onOpenSettings = {},
                    onOpenPlaylists = {}
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            )
            {
                MainBackground()

                Column(modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)) {
                    DisplayListSongs(
                        songs = sampleSongs,
                        onSongClicked = {},
                        modifier = Modifier.fillMaxSize(),
                    )
                    // populate PlayerRepository with sample data for preview
                    LaunchedEffect(Unit) {
                        PlayerRepository.setPlaylist(sampleSongs, 0)
                        PlayerRepository.setIsPlaying(false)
                    }
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black)) {
                        MiniPlayer(modifier = Modifier.align(Alignment.Center))
                    }
                }
            }
        }
    }
}

// Add DisplayListRadioStations preview
@Preview(showBackground = true, name = "DisplayListRadioStations Preview", backgroundColor = 0xFF000000, showSystemUi = true)
@Composable
fun DisplayListRadioStationsPreview() {
    MaterialTheme {
        val navController = rememberNavController()

        // Hardcoded sample stations for preview
        val sampleStations = listOf(
            com.example.musicplayer.model.RadioStation(stationuuid = "cidc-z103", name = "Z103.5", url = "https://21363.live.streamtheworld.com/CIDC_FM.mp3", favicon = "https://cdn-profiles.tunein.com/s12366/images/logod.png?t=637554031500000000", country = "Canada", tags = "Top40, Euro, Pop, Hip-Hop, Reggae", bitrate = 128),
            com.example.musicplayer.model.RadioStation(stationuuid = "virgin-999", name = "Virgin 99.9", url = "https://18153.live.streamtheworld.com/CKFMFMAAC_SC", favicon = "https://archive.org/services/img/ckfm_20230202", country = "Canada", tags = "Pop, Top40", bitrate = 128),
            com.example.musicplayer.model.RadioStation(stationuuid = "kiss-925", name = "KISS 92.5", url = "https://21323.live.streamtheworld.com/CKIS_FM.mp3", favicon = "https://cdn-radiotime-logos.tunein.com/s31199d.png", country = "Canada", tags = "Top 40, Pop, Hip-Hop, R&B, Dance", bitrate = 0),
            com.example.musicplayer.model.RadioStation(stationuuid = "chum-1045", name = "CHUM 104.5", url = "https://26293.live.streamtheworld.com/CHUMFMAAC_SC", favicon = "https://cdn-profiles.tunein.com/s31180/images/logod.png?t=637400097550000000", country = "Canada", tags = "Classic, Rock, Pop", bitrate = 0),
            com.example.musicplayer.model.RadioStation(stationuuid = "chfi-981", name = "CHFI 98.1", url = "https://21253.live.streamtheworld.com/CHFIFM.mp3", favicon = "https://www.seekyoursounds.com/wp-content/uploads/2024/06/Seekr-RadioCover-CHFI-981-1-300x300.png", country = "Canada", tags = "easy listening, adult contemporary", bitrate = 0),
            com.example.musicplayer.model.RadioStation(stationuuid = "Boom-973", name = "Boom 97.3", url = "https://21323.live.streamtheworld.com/CHBM_FM.mp3", favicon = "https://cdn-radiotime-logos.tunein.com/s31212d.png", country = "Canada", tags = "70's, 80's, 90's, Pop, Rock, Soul, R&B", bitrate = 0),
            com.example.musicplayer.model.RadioStation(stationuuid = "Flow-987", name = "Flow 98.7", url = "https://ice64.securenetsystems.net/CKFG", favicon = "https://cdn-profiles.tunein.com/s142066/images/logod.jpg?t=637808074610000000", country = "Canada", tags = "Hip-Hop, Pop, Afrobeat, Reggae, Soul, Soca, R&B", bitrate = 0),
            com.example.musicplayer.model.RadioStation(stationuuid = "Fresh-931", name = "Fresh 93.1", url = "https://live.leanstream.co/CHAYFM-MP3?args=tunein", favicon = "https://cdn-profiles.tunein.com/s31156/images/logod.png?t=155144", country = "Canada", tags = "classic, rock", bitrate = 0)
        )

        val vm = remember { SongListViewModel(userStationsInitial = sampleStations) }

        Scaffold(
            topBar = {
                MainAppBar(
                    showSearch = false,
                    onToggleSearch = {},
                    isRadio = true,
                    onToggleRadio = {},
                    query = "",
                    onQueryChange = {},
                    onSearchedClicked = {},
                    onOpenSettings = {},
                    onOpenPlaylists = {}
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
                    DisplayListRadioStations(
                        navController = navController,
                        viewModel = vm,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

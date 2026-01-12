package com.example.musicplayer.songlist

import android.app.Activity
import android.os.Build //keep
import android.util.Log
import androidx.annotation.RequiresApi  //keep
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.tween
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import com.example.musicplayer.model.Song
import com.example.musicplayer.R
import com.example.musicplayer.Util
import androidx.navigation.NavHostController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.musicplayer.music.MusicPlayerViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.core.view.WindowCompat
import com.example.musicplayer.service.PlayerRepository
import com.example.musicplayer.service.PlayerIntentBuilder
import kotlin.collections.getOrNull
import kotlin.text.isNotEmpty
import androidx.compose.runtime.SideEffect
import com.example.musicplayer.composable.MainBackground
import com.example.musicplayer.composable.RadioTagChips
import com.example.musicplayer.composable.CompactRadioTagChips
import coil.compose.AsyncImage
import coil.request.ImageRequest
import android.widget.Toast
import com.example.musicplayer.composable.MainAppBar
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import androidx.navigation.compose.rememberNavController
import com.example.musicplayer.composable.MiniPlayer
import com.example.musicplayer.composable.SongCardRow
import com.example.musicplayer.radio.RadioPlayerService

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun ListSongsScreen(
    navController: NavHostController,
    viewModel: SongListViewModel = viewModel(),
    // New params: allow parent to control whether the top app bar / search UI is shown.
    showTopBar: Boolean = true,
    showSearch: Boolean = false,
    onToggleSearch: () -> Unit = {},
    queryExternal: String? = null,
    onQueryChangeExternal: (String) -> Unit = {},
    onSearchedClickedExternal: (String) -> Unit = {}
) {
    // If the parent doesn't manage search visibility (the common case), keep local state
    var searchVisible by remember { mutableStateOf(showSearch) }
    val toggleSearch: () -> Unit = {
        try { onToggleSearch() } catch (_: Throwable) {}
        searchVisible = !searchVisible
    }

    // Use persistent radio selection stored in the SongListViewModel so selection
    // survives navigation (e.g., returning from RadioPlayerScreen)
    val isRadioSelected by viewModel.isRadioSelected.collectAsState()
    val toggleRadio: () -> Unit = { viewModel.toggleRadioSelected() }

    // removed local showSearch state; parent may control it via the new params

    // load and filter songs
    val context = LocalContext.current
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
                    onToggleAlbumView = {}
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
                    // leave space at the bottom so the mini player doesn't cover list items
                    .padding(innerPadding)
            ) {
                // Use a dedicated MusicPlayerViewModel to start playback so setPlaylist + startPlay are atomic
                val playerVm: MusicPlayerViewModel = viewModel()
                if (isRadioSelected) {
                    // When radio is selected, show the radio stations list UI
                    Box(modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()) {
                        DisplayListRadioStations(navController = navController, viewModel = viewModel)
                    }
                } else {
                    DisplayListSongs(
                        songs = songs,
                        onSongClicked = { index ->
                            val selected = songs.getOrNull(index)
                            if (selected != null) {
                                // use the player VM which calls PlayerRepository.setPlaylist and starts the service
                                playerVm.setPlaylist(context, songs, index)
                                // Ensure repository current index is set immediately to the selected index
                                // so the UI reflects the selection even if setPlaylist coalesced the update.
                                PlayerRepository.setCurrentIndex(index)
                                // Ensure playback is explicitly requested for the selected index.
                                // This covers the case where setPlaylist returns `false` because the
                                // repository considers the playlist identical; calling `startPlay`
                                // forces the service to start the requested index.
                                playerVm.play(context)
                                // navigate to music screen UI
                                val songId = selected.id.toString()
                                navController.navigate("musicScreen/$songId")
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                }
                // show the mini player only when playback is active so it doesn't take layout space while idle
                if (showMini) {
                    MiniPlayer(
                        modifier = Modifier.fillMaxWidth(),
                        onOpenPlayer = { selectedSong ->
                            selectedSong?.let { navController.navigate("musicScreen/${it.id}") }
                        }
                    )
                }
            }

            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                contentColor = Color.White
            )

        }
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
    onSongClicked: (Int) -> Unit = {},
    modifier: Modifier = Modifier
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
                    onClick = { onSongClicked(index) }
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
                        // Prefer quoted name (e.g. "CIDC-FM") when present for UI display
                        val displayName = Util.extractQuotedOrOriginal(station.name).ifBlank { station.name ?: "Unknown" }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val imageUrl = Util.getStationImageUrl(station).ifBlank { null }
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(imageUrl)
                                    .crossfade(500)
                                    .build(),
                                contentDescription = displayName,
                                modifier = Modifier
                                    .width(56.dp)
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White),
                                contentScale = ContentScale.Crop,
                                placeholder = painterResource(id = R.drawable.ic_radio),
                                error = painterResource(id = R.drawable.ic_radio)
                            )

                            Column(modifier = Modifier
                                .padding(start = 10.dp)
                                .weight(1f)) {
                                Text(
                                    text = displayName,
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                // Add compact chip tags below the station name
                                CompactRadioTagChips(
                                    tagsRaw = station.tags,
                                    modifier = Modifier.padding(top = 4.dp),
                                    chipBackground = Color.White.copy(alpha = 0.2f),
                                    chipContentColor = Color.White
                                )
                            }

                            IconButton(
                                modifier = Modifier.size(60.dp),
                                onClick = {
                                // Start the RadioPlayerService to play the stream URL, then navigate to RadioPlayerScreen
                                val url = station.url ?: ""
                                if (url.isBlank()) {
                                    Toast.makeText(context, "No stream URL for $displayName", Toast.LENGTH_SHORT).show()
                                    return@IconButton
                                }

                                try {
                                    // start service with play-station action so playback begins in background
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
                                    // Use startForegroundService on O+ so the service can enter foreground mode
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        ContextCompat.startForegroundService(context, svcIntent)
                                    } else {
                                        context.startService(svcIntent)
                                    }
                                    Log.d("DisplayListRadioStations", "Started RadioPlayerService for $displayName -> $url index=$idx size=${stations.size}")
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Failed to start radio service: ${e.message}", Toast.LENGTH_SHORT).show()
                                }

                                // mark radio view selected so returning from player shows the radio list
                                try { viewModel.setRadioSelected(true) } catch (_: Throwable) {}
                                // navigate to radio player screen with encoded name/url path segments
                                val encName = Uri.encode(displayName)
                                val encUrl = Uri.encode(url)
                                val favicon = station.favicon ?: ""
                                val encFav = Uri.encode(favicon)
                                val tagsRaw = station.tags ?: ""
                                val encTags = Uri.encode(tagsRaw)
                                try { Log.d("DisplayListRadioStations", "Navigating to player: name=$displayName url=$url favicon=$favicon tags=$tagsRaw") } catch (_: Throwable) {}
                                navController.navigate("radioPlayer/$encName/$encUrl/$encFav/$encTags")
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.PlayCircle,
                                    contentDescription = "Play",
                                    modifier = Modifier.size(50.dp),
                                    tint = Color.White
                                )
                            }
                        }
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
                    onToggleAlbumView = {}
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
        // Create a view model instance for preview with pre-populated default stations
        val sampleStations = Util.getDefaultUserStations()
        val vm = remember { SongListViewModel(userStationsInitial = sampleStations) }
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
                    onToggleAlbumView = {}
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

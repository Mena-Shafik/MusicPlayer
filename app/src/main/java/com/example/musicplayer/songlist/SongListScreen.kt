package com.example.musicplayer.songlist


import MainAppBar
import android.app.Activity
import android.os.Build //keep
import android.util.Log
import androidx.annotation.RequiresApi  //keep
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.LinearProgressIndicator
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
import com.example.musicplayer.composable.Tabs
import com.example.musicplayer.navigation.Destination

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

    // determine destinations and the index that corresponds to the SONGS tab
    val destinations = com.example.musicplayer.navigation.Destination.entries.take(3).toList()
    val songsIndex = destinations.indexOfFirst { it == Destination.SONGS }.takeIf { it >= 0 } ?: 0
    val albumIndex = destinations.indexOfFirst { it == Destination.ALBUMS }.takeIf { it >= 0 } ?: -1

    // hoisted selected tab state
    var selectedTab by remember { mutableStateOf(songsIndex) }

    Scaffold( containerColor = Color.Transparent,
        topBar = {
            if (showTopBar) {
                MainAppBar(
                    showSearch = searchVisible,
                    onToggleSearch = toggleSearch,
                    query = query,
                    onQueryChange = { onQueryChange(it) },
                    onSearchedClicked = { onSearchedClicked(it) }
                )
            }
        }
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pullRefresh(pullRefreshState)
                .background(Color.Transparent)
        ) {
            MainBackground()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    // leave space at the bottom so the mini player doesn't cover list items
                    .padding(innerPadding)
            ) {
                Tabs(
                    navController = navController,
                    destinations = destinations,
                    selectedIndex = selectedTab,
                    onSelectedIndexChange = { selectedTab = it }
                )
                // show content depending on the selected tab
                when (selectedTab) {
                    songsIndex -> {
                        DisplayListSongs(
                            songs = songs,
                            onSongClicked = { index ->
                                val selected = songs.getOrNull(index)
                                if (selected != null) {
                                    PlayerRepository.setPlaylist(songs, index)
                                    val appCtx = context.applicationContext
                                    PlayerIntentBuilder.startPlay(appCtx)
                                    navController.navigate("musicScreen/${selected.id}")
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )
                    }
                    albumIndex -> {
                        DisplayAlbumList(
                            songs = songs,
                            onAlbumClicked = { albumName ->
                                // navigate to an album screen if you have one, or filter songs by album
                                // here we navigate to a placeholder route; adjust as needed
                                navController.navigate("albumScreen/${albumName.encodeURIPathComponent()}")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )
                    }
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

// Helper extension for safe navigation encoding (very small helper)
private fun String.encodeURIPathComponent(): String =
    java.net.URLEncoder.encode(this, "utf-8")
@Composable
fun DisplayAlbumList(
    songs: List<Song>,
    onAlbumClicked: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val albums = remember(songs) { songs.map { it.artist }.distinct() }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            itemsIndexed(albums) { _, album ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAlbumClicked(album) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = album, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
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
                    artist = song.artist,
                    title = song.title,
                    duration = song.duration,
                    path = song.path,
                    onClick = { onSongClicked(index) }
                )
            }
        }
    }
}

@Composable
fun SongCardRow(
    artist: String,
    title: String,
    duration: Double,
    path: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageBitmap = Util.getAlbumArt(context, path)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            //.background(Color.Black) // removed so list items are semi-transparent over the background
            .clickable(onClick = onClick)
    ) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = "Artist image",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(60.dp)
                    .height(60.dp)
                    .clip(RoundedCornerShape(2.dp))
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.ic_album),
                contentDescription = "Placeholder image",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(60.dp)
                    .height(60.dp)
                    .clip(RoundedCornerShape(2.dp))
            )
        }

        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = artist,
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 1
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = Util.converter(duration),
                Modifier.width(80.dp).padding(10.dp),
                color = Color.White,
                textAlign = TextAlign.End,

            )
        }
    }
}



@Composable
fun MiniPlayer(
    modifier: Modifier = Modifier,
    onOpenPlayer: (Song?) -> Unit = {}
) {
    val playlist by PlayerRepository.playlist.collectAsState()
    val currentIndex by PlayerRepository.currentIndex.collectAsState()
    val isPlaying by PlayerRepository.isPlaying.collectAsState()
    val positionMs by PlayerRepository.positionMs.collectAsState()
    val durationMs by PlayerRepository.durationMs.collectAsState()
    val current = playlist.getOrNull(currentIndex)
    val context = LocalContext.current
    // read preview mode inside a composable context
    val isPreviewMode = LocalInspectionMode.current

    // If there's no playlist and no current song, don't show the mini player
    if (playlist.isEmpty() && current == null) return

    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .fillMaxWidth()
                .height(72.dp)
                .background(Color(0xFF0F0F0F).copy(alpha = 0.95f))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // album art (left) - tappable to open full player
            val art = remember(current?.path) {
                try { current?.path?.let { Util.getAlbumArt(context, it) } } catch (_: Throwable) { null }
            }

            val imageModifier = Modifier
                .width(56.dp)
                .height(56.dp)
                .clip(RoundedCornerShape(6.dp))

            if (art != null) {
                Image(
                    bitmap = art,
                    contentDescription = "Album art",
                    modifier = imageModifier.clickable { onOpenPlayer(current) },
                    contentScale = ContentScale.Crop
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.ic_album),
                    contentDescription = "Album art",
                    modifier = imageModifier.clickable { onOpenPlayer(current) },
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = current?.title ?: "",
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = current?.artist ?: "",
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp
                )
            }

            IconButton(onClick = {
                val appCtx = context.applicationContext
                Log.d("MiniPlayer", "play/pause clicked isPlaying=$isPlaying appCtx=$appCtx")
                if (isPreviewMode) {
                    // in preview toggle repository state only
                    PlayerRepository.setIsPlaying(!PlayerRepository.isPlaying.value)
                } else {
                    // Optimistically update UI state so the button feels responsive, then send intent to service.
                    PlayerRepository.setIsPlaying(!isPlaying)
                    if (isPlaying) PlayerIntentBuilder.startPause(appCtx) else PlayerIntentBuilder.startPlay(appCtx)
                }
            }) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.PauseCircle else Icons.Filled.PlayCircle,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    Modifier.size(60.dp,60.dp),
                    tint = Color.White
                )
            }
        }

        // Progress indicator (determinate) — use LinearProgressIndicator instead of a slider
        val duration = durationMs
        val position = positionMs.coerceAtMost(duration)
        val progress = remember(position, duration) {
            if (duration > 0L) {
                (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
            } else 0f
        }

        Column(modifier = Modifier
            .fillMaxWidth()) {
            // determinate progress bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().padding(2.dp,0.dp,2.dp,0.dp),
                color = Color(0xFFFFA500),
                trackColor = Color(0xFFFFDAB9)
            )
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
                artist = "Artist",
                title = "Title",
                duration = 260000.0,
                path = "", // empty path triggers vinyl placeholder
                onClick = {}
            )
        }
    }
}



@Preview(showBackground = true, name = "MiniPlayer Preview", backgroundColor = 0xFF000000)
@Composable
private fun MiniPlayerPreview() {
    // Prepare a small sample playlist with empty paths so placeholder art is used in preview
    val sampleSongs = listOf(
        Song(id = 1, "Album A",title = "Preview Song", artist = "Preview Artist", duration = 180000.0, path = ""),
        Song(id = 2, "Album A",title = "Another Track", artist = "Artist Two", duration = 200000.0, path = "")
    )

    // populate PlayerRepository with sample data for preview
    LaunchedEffect(Unit) {
        PlayerRepository.setPlaylist(sampleSongs, 0)
        PlayerRepository.setIsPlaying(false)
    }

    MaterialTheme {
        Box(modifier = Modifier.fillMaxWidth().background(Color.Black)) {
            MiniPlayer(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Preview(showSystemUi = true, name = "DisplayList Preview", backgroundColor = 0xFF000000, showBackground = true)
@Composable
fun DisplayListPreview() {
    MaterialTheme {
        val sampleSongs = listOf(
            Song(
                id = 1,
                "Album A",
                title = "Preview Song",
                artist = "Preview Artist",
                duration = 180000.0,
                path = ""
            ),
            Song(
                id = 2,
                "Album A",
                title = "Another Track",
                artist = "Artist Two",
                duration = 200000.0,
                path = ""
            ),
            Song(
                id = 3,
                "Album A",
                title = "Another Track",
                artist = "Artist Three",
                duration = 200000.0,
                path = ""
            ),
            Song(
                id = 4,
                "Album A",
                title = "Another Track",
                artist = "Artist Four",
                duration = 200000.0,
                path = ""
            )
        )
        Scaffold(
            topBar = {
                MainAppBar(
                    showSearch = false,
                    onToggleSearch = {},
                    query = "",
                    onQueryChange = {},
                    onSearchedClicked = {}
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

                Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    Tabs()
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
                    Box(modifier = Modifier.fillMaxWidth().background(Color.Black)) {
                        MiniPlayer(modifier = Modifier.align(Alignment.Center))
                    }
                }
            }
        }
    }
}

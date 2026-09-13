package com.example.musicplayer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.collectAsState
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.musicplayer.model.Song
import com.example.musicplayer.model.RadioStation
import com.example.musicplayer.music.MusicPlayerViewModel
import com.example.musicplayer.music.PersistentPlayerHost
import com.example.musicplayer.service.PlayerDockController
import com.example.musicplayer.radio.RadioPlayerScreen
import com.example.musicplayer.songlist.ListSongsScreen
import com.example.musicplayer.settings.SettingsScreen
import com.example.musicplayer.history.HistoryScreen
import com.example.musicplayer.ui.theme.MusicPlayerTheme
import com.example.musicplayer.navigation.NavRoutes
import com.example.musicplayer.playlist.PlaylistScreen
import com.example.musicplayer.playlist.PlaylistDetailScreen
import com.example.musicplayer.playlist.PlaylistAddSongsScreen
import com.example.musicplayer.navidrome.NavidromeRepository
import com.example.musicplayer.preferences.PreferencesManager
import com.example.musicplayer.util.Util
import com.example.musicplayer.util.ArtistUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private val REQUESTCODE: Int = 99
    private val viewModel: MainViewModel by viewModels()

    @androidx.annotation.OptIn(UnstableApi::class)
    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val splash = installSplashScreen()
        var keepSplashOn = true
        viewModel.isLoading.observe(this) { loading ->
            keepSplashOn = loading == true
        }
        // Hold for at least one full icon rotation (matches rotate_icon.xml's 3600ms cycle) even if the media scan finishes almost instantly, otherwise a fast/empty library dismisses the splash mid-spin.
        val splashStartElapsedMs = android.os.SystemClock.elapsedRealtime()
        val minSplashDurationMs = 1500L
        splash.setKeepOnScreenCondition {
            keepSplashOn || (android.os.SystemClock.elapsedRealtime() - splashStartElapsedMs) < minSplashDurationMs
        }

        // Preloads songs on a background thread; result shared via LibraryPreloadCache so ListSongsScreen doesn't re-scan MediaStore the moment the splash dismisses.
        lifecycleScope.launch(Dispatchers.IO) {
            val loaded = try {
                Util.getAllAudioFromDevice(this@MainActivity)
            } catch (e: Exception) {
                Log.w("MainActivity", "Failed to preload songs: ${e.message}")
                emptyList()
            }
            com.example.musicplayer.util.LibraryPreloadCache.set(loaded)
            withContext(Dispatchers.Main) {
                viewModel.setLoadingComplete()
            }
        }

        enableEdgeToEdge()
        setupPermissions()

        setContent {
            MusicPlayerTheme {
                val navController = rememberNavController()

                // No top bar -- full screen content. PersistentPlayerHost is a sibling of the NavHost (not a pushed destination) so it stays mounted/visible across every tab once something is playing.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = NavRoutes.Home.route,
                        // Smooth crossfade between destinations (tab switching plus any pushed screen) instead of the previous instant cut.
                        enterTransition = { fadeIn(animationSpec = tween(220)) },
                        exitTransition = { fadeOut(animationSpec = tween(180)) },
                        popEnterTransition = { fadeIn(animationSpec = tween(220)) },
                        popExitTransition = { fadeOut(animationSpec = tween(180)) }
                    ) {
                    // Home route shows the songs list directly
                    composable(NavRoutes.Home.route) {
                        ListSongsScreen(navController = navController)
                    }
                    composable(NavRoutes.Settings.route) {
                        SettingsScreen(navController = navController)
                    }

                    composable(NavRoutes.History.route) {
                        val context = LocalContext.current
                        var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
                        LaunchedEffect(context) {
                            val local = withContext(Dispatchers.IO) { Util.getAllAudioFromDevice(context) }
                            val connected = PreferencesManager.getNavidromeConnectedFlow(context).first()
                            val remote = if (connected) {
                                try {
                                    NavidromeRepository(context).listCatalogueSongs()
                                } catch (e: Exception) {
                                    emptyList()
                                }
                            } else {
                                emptyList()
                            }
                            songs = local + remote
                        }
                        val playerVm: MusicPlayerViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                        HistoryScreen(
                            navController = navController,
                            onSongClick = { song ->
                                val index = songs.indexOfFirst { it.id == song.id }
                                if (index >= 0) {
                                    playerVm.setPlaylist(context, songs, index)
                                    PlayerDockController.requestExpand()
                                }
                            }
                        )
                    }

                    composable(NavRoutes.Playlists.route) {
                        PlaylistScreen(navController = navController, onPlaylistSelected = { playlist ->
                            navController.navigate(NavRoutes.PlaylistDetail.createRoute(playlist.id))
                        })
                    }

                    composable(NavRoutes.Radio.route) {
                        com.example.musicplayer.songlist.RadioScreen(navController = navController)
                    }

                    composable(NavRoutes.Navidrome.route) {
                        com.example.musicplayer.navidrome.NavidromeScreen(navController = navController)
                    }

                    composable(
                        NavRoutes.PlaylistDetail.route,
                        arguments = listOf(navArgument("playlistId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: return@composable
                        val context = LocalContext.current
                        val songs: List<Song> = remember(context) { Util.getAllAudioFromDevice(context) }

                        // Find the playlist from PlaylistRepository
                        val playlistVm: com.example.musicplayer.playlist.PlaylistViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                            factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                                @Suppress("UNCHECKED_CAST")
                                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                    return com.example.musicplayer.playlist.PlaylistViewModel(context) as T
                                }
                            }
                        )
                        val playlists by playlistVm.playlists.collectAsState(initial = emptyList())
                        val currentPlaylist = remember(playlistId, playlists) {
                            playlists.find { it.id == playlistId }
                        }

                        if (currentPlaylist != null) {
                            PlaylistDetailScreen(navController = navController, playlist = currentPlaylist, allSongs = songs)
                        }
                    }

                    composable(
                        NavRoutes.PlaylistAddSongs.route,
                        arguments = listOf(navArgument("playlistId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: return@composable
                        val context = LocalContext.current
                        val songs: List<Song> = remember(context) { Util.getAllAudioFromDevice(context) }

                        // Find the playlist from PlaylistRepository
                        val playlistVm: com.example.musicplayer.playlist.PlaylistViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                            factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                                @Suppress("UNCHECKED_CAST")
                                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                    return com.example.musicplayer.playlist.PlaylistViewModel(context) as T
                                }
                            }
                        )
                        val playlists by playlistVm.playlists.collectAsState(initial = emptyList())
                        val currentPlaylist = remember(playlistId, playlists) {
                            playlists.find { it.id == playlistId }
                        }

                        if (currentPlaylist != null) {
                            PlaylistAddSongsScreen(navController = navController, playlistId = playlistId, allSongs = songs)
                        }
                    }

                    // Radio player route: optionally pass a Serializable RadioStation object
                    composable(
                        "radioPlayer",
                        arguments = listOf(navArgument("station") {
                            type = NavType.ParcelableType(RadioStation::class.java)
                            nullable = true
                        })
                    ) { backStackEntry ->
                        val station = backStackEntry.arguments?.getParcelable<RadioStation>("station")
                        if (station != null) {
                            RadioPlayerScreen(radioStation = station, navController = navController)
                        } else {
                            // No station object provided; show placeholder player
                            RadioPlayerScreen(radioStation = RadioStation(null, "Unknown Station", null), navController = navController)
                        }
                    }

                    // Backwards-compatible route: encoded name/url path (simplified to avoid URL length issues)
                    composable(
                        NavRoutes.RadioPlayer.route,
                        arguments = listOf(
                            navArgument("name") { type = NavType.StringType },
                            navArgument("url") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val nameEnc = backStackEntry.arguments?.getString("name")
                        val urlEnc = backStackEntry.arguments?.getString("url")
                        val decodedName = try { if (nameEnc != null) java.net.URLDecoder.decode(nameEnc, "UTF-8") else "Unknown" } catch (_: Exception) { nameEnc ?: "Unknown" }
                        val decodedUrl = try { if (urlEnc != null) java.net.URLDecoder.decode(urlEnc, "UTF-8") else null } catch (_: Exception) { urlEnc }
                        try { Log.d("MainActivity", "Decoded radio args: name=$decodedName url=$decodedUrl") } catch (_: Throwable) {}
                        val stationFromPath = RadioStation(stationuuid = null, name = decodedName, url = decodedUrl)
                        RadioPlayerScreen(radioStation = stationFromPath, navController = navController)
                    }
                }
                    }
                    // Only shown on the Home tab; stays mounted (hideContent) so it reappears instantly on return.
                    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
                    // Root tabs (Home stays as the permanent nav-stack anchor for saveState/restoreState) exit the app on back instead of popping to a previous tab; registered before PersistentPlayerHost so its own collapse-on-back still wins while the full player is open.
                    androidx.activity.compose.BackHandler(
                        enabled = currentRoute == NavRoutes.Home.route || currentRoute == NavRoutes.Radio.route || currentRoute == NavRoutes.Playlists.route
                    ) {
                        (this@MainActivity).finish()
                    }
                    PersistentPlayerHost(hideContent = currentRoute != NavRoutes.Home.route)
                }
            }
        }
    }

    /*private fun setupPermissions() {
        // Use READ_MEDIA_AUDIO (Android 13+) for this project; the project's min sdk ensures availability.
        val readPermission = Manifest.permission.READ_MEDIA_AUDIO

        val permRead = ContextCompat.checkSelfPermission(this, readPermission)
        val permNotify = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)

        if (permRead != PackageManager.PERMISSION_GRANTED || permNotify != PackageManager.PERMISSION_GRANTED) {
            Log.i("MainActivity", "Requesting required permissions")
            makeRequest()
        } else {
            Log.i("MainActivity", "All required permissions already granted")
        }
    }*/


    private fun setupPermissions() {
        val required = mutableListOf<String>()
        required += Manifest.permission.READ_MEDIA_AUDIO
        required += Manifest.permission.POST_NOTIFICATIONS

        // Filter only permissions not yet granted
        val toRequest = required.distinct().filter { perm ->
            ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED
        }

        if (toRequest.isNotEmpty()) {
            Log.i("MainActivity", "Requesting permissions: ${toRequest.joinToString()}")
            ActivityCompat.requestPermissions(this, toRequest.toTypedArray(), REQUESTCODE)
        } else {
            Log.i("MainActivity", "All required permissions already granted")
        }
    }

    private fun makeRequest() {
        val perms = arrayOf(
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.RECORD_AUDIO // include only if you use microphone/song recognition
        )
        ActivityCompat.requestPermissions(this, perms, REQUESTCODE)
    }

    override fun onPause() {
        super.onPause()
        // App is going to background - disable caching
        ArtistUtil.onAppBackground()
        Log.d("MainActivity", "onPause: Artist image caching disabled")
    }

    override fun onResume() {
        super.onResume()
        // App is coming to foreground - enable caching
        ArtistUtil.onAppForeground()
        Log.d("MainActivity", "onResume: Artist image caching enabled")
    }

    override fun onDestroy() {
        // If the activity is finishing (user closed the app), stop the playback service so audio stops.
        try {
            if (isFinishing) {
                stopService(Intent(this, com.example.musicplayer.service.PlayerForegroundService::class.java))
                // Clear cache on app exit
                ArtistUtil.clearCache()
                Log.d("MainActivity", "onDestroy: Cache cleared on app exit")
            }
        } catch (_: Throwable) {
            // best-effort; do not crash the app
        }
        super.onDestroy()
    }

}

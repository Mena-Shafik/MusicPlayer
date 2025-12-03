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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.musicplayer.model.Song
import com.example.musicplayer.music.MusicScreen
import com.example.musicplayer.songlist.ListSongsScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.graphics.Color as AndroidColor

class MainActivity : ComponentActivity() {

    private val REQUESTCODE: Int = 99
    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val splash = installSplashScreen()
        var keepSplashOn = true
        viewModel.isLoading.observe(this) { loading ->
            keepSplashOn = loading == true
        }
        splash.setKeepOnScreenCondition { keepSplashOn }

        // Start preloading songs on a background thread; hide splash when done.
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // load all audio from device (may require permissions)
                Util.getAllAudioFromDevice(this@MainActivity)
            } catch (e: Exception) {
                Log.w("MainActivity", "Failed to preload songs: ${e.message}")
            } finally {
                withContext(Dispatchers.Main) {
                    viewModel.setLoadingComplete()
                }
            }
        }

        enableEdgeToEdge()
        setupPermissions()

        setContent {
            val navController = rememberNavController()

            // No top bar — show content full screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                NavHost(navController = navController, startDestination = "home") {
                    // Home route shows the songs list directly
                    composable("home") {
                        ListSongsScreen(navController = navController)
                    }

                    composable(
                        "musicScreen/{songId}",
                        arguments = listOf(navArgument("songId") { type = NavType.IntType })
                    ) { backStackEntry ->
                        val songId = backStackEntry.arguments?.getInt("songId")
                        val context = LocalContext.current
                        val songs: List<Song> = remember(context) { Util.getAllAudioFromDevice(context) }
                        val song = songId?.let { id -> songs.find { it.id == id } }
                        song?.let {
                            MusicScreen(
                                songId = songId,
                                songs = songs,
                                navController = navController
                            )
                        }
                    }
                }
            }
        }
    }

    private fun setupPermissions() {
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
    }

    private fun makeRequest() {
        val perms = arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
        ActivityCompat.requestPermissions(this, perms, REQUESTCODE)
    }

    override fun onDestroy() {
        // If the activity is finishing (user closed the app), stop the playback service so audio stops.
        try {
            if (isFinishing) {
                stopService(Intent(this, com.example.musicplayer.service.PlayerForegroundService::class.java))
            }
        } catch (_: Throwable) {
            // best-effort; do not crash the app
        }
        super.onDestroy()
    }

}

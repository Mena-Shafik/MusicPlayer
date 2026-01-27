package com.example.musicplayer.songlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.musicplayer.navigation.NavRoutes
import com.example.musicplayer.ui.components.BottomNav
import com.example.musicplayer.ui.components.MainAppBar
import com.example.musicplayer.ui.components.MainBackground
import com.example.musicplayer.util.Util
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun RadioScreen(navController: NavHostController) {
    val context = LocalContext.current
    val viewModel: SongListViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return SongListViewModel(context = context) as T
            }
        }
    )

    // Refresh support
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val pullRefreshState = rememberPullRefreshState(isRefreshing, onRefresh = {
        scope.launch {
            isRefreshing = true
            try {
                val all = withContext(Dispatchers.IO) { Util.getAllAudioFromDevice(context) }
                viewModel.load(all)
            } catch (_: Throwable) {
            } finally {
                isRefreshing = false
            }
        }
    })

    // Keep radio selected in view model
    val isRadioSelected by viewModel.isRadioSelected.collectAsState()
    LaunchedEffect(Unit) { viewModel.setRadioSelected(true) }

    Scaffold(
        topBar = {
            MainAppBar(
                showSearch = false,
                onToggleSearch = {},
                query = "",
                onQueryChange = {},
                onSearchedClicked = {},
                onOpenSettings = { navController.navigate(NavRoutes.Settings.route) },
                onOpenPlaylists = { navController.navigate(NavRoutes.Playlists.route) },
                title = "Radio",
                searchEnabled = false
            )
        },
        bottomBar = {
            BottomNav(
                selectedIndex = 1,
                onSelected = { idx ->
                    when (idx) {
                        0 -> navController.navigate(NavRoutes.Home.route) { launchSingleTop = true }
                        1 -> { /* already here */ }
                        2 -> navController.navigate(NavRoutes.Playlists.route) { launchSingleTop = true }
                    }
                }
            )
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
                DisplayListRadioStations(navController = navController, viewModel = viewModel, modifier = Modifier.fillMaxSize())
            }

            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(androidx.compose.ui.Alignment.TopCenter),
                contentColor = Color(0xFFFFA500),
                backgroundColor = Color.Black.copy(alpha = 0.7f)
            )
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
                Column {
                    MainAppBar(
                        showSearch = false,
                        onToggleSearch = {},
                        query = "",
                        onQueryChange = {},
                        onSearchedClicked = {},
                        onOpenSettings = {},
                        onOpenPlaylists = {},
                        title = "Radio"
                    )
                }
            },
            bottomBar = {
                BottomNav(selectedIndex = 1, onSelected = { /* no-op in preview */ })
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

package com.example.musicplayer.playlist

import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.musicplayer.model.Playlist
import com.example.musicplayer.navigation.NavRoutes
import com.example.musicplayer.ui.components.BottomNav
import com.example.musicplayer.ui.components.MainAppBar
import com.example.musicplayer.ui.components.MainBackground
import com.example.musicplayer.ui.components.PlaylistCard
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.musicplayer.ui.components.CreatePlaylistDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    navController: NavHostController,
    onPlaylistSelected: (Playlist) -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: PlaylistViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return PlaylistViewModel(context) as T
            }
        }
    )

    val playlists by viewModel.playlists.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            MainAppBar(
                showSearch = false,
                onToggleSearch = {},
                query = "",
                onQueryChange = {},
                onSearchedClicked = {},
                onOpenSettings = {},
                onOpenPlaylists = {},
                title = "Playlists",
                searchEnabled = false,
                onAddPlaylist = { showCreateDialog = true }
            )
        },
        bottomBar = {
            BottomNav(
                selectedIndex = 2,
                onSelected = { idx ->
                    when (idx) {
                        0 -> navController.navigate(NavRoutes.Home.route) { launchSingleTop = true }
                        1 -> navController.navigate(NavRoutes.Radio.route) { launchSingleTop = true }
                        2 -> {} // already here
                    }
                }
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
                AnimatedContent(
                    targetState = playlists.isEmpty(),
                    transitionSpec = {
                        fadeIn(animationSpec = tween(400)) + scaleIn(
                            initialScale = 0.95f,
                            animationSpec = tween(400)
                        ) togetherWith fadeOut(animationSpec = tween(300)) + scaleOut(
                            targetScale = 1.05f,
                            animationSpec = tween(300)
                        )
                    },
                    label = "Playlist content transition"
                ) { isEmpty ->
                    if (isEmpty) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "No playlists yet",
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                Text(
                                    text = "Create one to get started",
                                    color = Color.Gray,
                                    modifier = Modifier.padding(top = 8.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(playlists) { playlist ->
                                PlaylistCard(
                                    playlist = playlist,
                                    onClick = { onPlaylistSelected(playlist) },
                                    onDelete = { viewModel.deletePlaylist(playlist.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            showDialog = showCreateDialog,
            onDismiss = { showCreateDialog = false },
            onCreatePlaylist = { name, description ->
                viewModel.createPlaylist(name, description)
                showCreateDialog = false
            }
        )
    }
}

@Preview(showSystemUi = true, showBackground = true, backgroundColor = 0xFF000000, name = "PlaylistScreen sample")
@Composable
private fun PlaylistScreenPreview() {
    val navController = rememberNavController()
    MaterialTheme {
        Scaffold(
            topBar = {
                MainAppBar(
                    showSearch = false,
                    onToggleSearch = {},
                    query = "",
                    onQueryChange = {},
                    onSearchedClicked = {},
                    onOpenSettings = {},
                    onOpenPlaylists = {},
                    searchEnabled = false,
                    onAddPlaylist = {},
                    title = "Playlists"
                )
            },
            bottomBar = {
                BottomNav(selectedIndex = 2, onSelected = { })
            }
        ) { innerPadding ->
            PlaylistScreen(navController = navController, onPlaylistSelected = {})
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showSystemUi = true, showBackground = true, backgroundColor = 0xFF000000, name = "PlaylistScreen sample (fake data)")
@Composable
private fun PlaylistScreenPreviewWithData() {
    val samplePlaylists = listOf(
        Playlist(id = 1, name = "Favorites", description = "Hand-picked", songIds = listOf(1, 2, 3)),
        Playlist(id = 2, name = "Road Trip", description = "Driving jams", songIds = listOf(4, 5)),
        Playlist(id = 3, name = "Chill", description = "Easy listening", songIds = listOf(6))
    )
    val navController = rememberNavController()
    MaterialTheme {
        Scaffold(
            topBar = {
                MainAppBar(
                    showSearch = false,
                    onToggleSearch = {},
                    query = "",
                    onQueryChange = {},
                    onSearchedClicked = {},
                    onOpenSettings = {},
                    onOpenPlaylists = {},
                    title = "Playlists"
                )
            },
            bottomBar = {
                BottomNav(selectedIndex = 2, onSelected = { })
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
                    .padding(innerPadding)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    MainBackground()
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(samplePlaylists) { playlist ->
                            PlaylistCard(
                                playlist = playlist,
                                onClick = {},
                                onDelete = {}
                            )
                        }
                    }
                }
            }
        }
    }
}

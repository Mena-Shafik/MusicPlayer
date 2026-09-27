package com.example.musicplayer.playlist

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicplayer.model.Playlist
import com.example.musicplayer.navigation.NavRoutes
import com.example.musicplayer.navigation.navigateToTab
import com.example.musicplayer.ui.components.background.AuroraRibbonBackground
import com.example.musicplayer.ui.components.common.BottomNav
import com.example.musicplayer.ui.components.common.dashedBorder
import com.example.musicplayer.ui.components.playlist.PlaylistCard
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.musicplayer.ui.components.playlist.CreatePlaylistDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    navController: NavHostController,
    onPlaylistSelected: (Playlist) -> Unit = {},
    modifier: Modifier = Modifier
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

    // Background drawn full-screen behind the whole Scaffold (including the top/bottom bars), not just the content area between them -- otherwise a "transparent" bar just shows the Scaffold's own flat containerColor instead of this blur.
    Box(modifier = Modifier.fillMaxSize().then(modifier)) {
    AuroraRibbonBackground()
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Playlists",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        lineHeight = 34.sp
                    )
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White)
                            .clickable { showCreateDialog = true }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            tint = Color(0xFF111111),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(text = "New", color = Color(0xFF111111), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            BottomNav(
                selectedIndex = 2,
                onSelected = { idx ->
                    when (idx) {
                        0 -> navController.navigateToTab(NavRoutes.Home.route)
                        1 -> navController.navigateToTab(NavRoutes.Radio.route)
                        2 -> {} // already here
                    }
                }
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
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
                                .fillMaxSize()
                                .padding(horizontal = 34.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(96.dp)
                                        .dashedBorder(
                                            widthDp = 1.5.dp,
                                            color = Color.White.copy(alpha = 0.22f),
                                            cornerRadiusDp = 20.dp
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.35f),
                                        modifier = Modifier.size(44.dp)
                                    )
                                }
                                Text(
                                    text = "No playlists yet",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 21.sp,
                                    modifier = Modifier.padding(top = 22.dp)
                                )
                                Text(
                                    text = "Group the songs you keep coming back to — a playlist takes about ten seconds to make.",
                                    color = Color.White.copy(alpha = 0.55f),
                                    fontSize = 14.sp,
                                    lineHeight = 21.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .padding(top = 24.dp)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(Color.White)
                                        .clickable { showCreateDialog = true }
                                        .padding(horizontal = 26.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = null,
                                        tint = Color(0xFF111111),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(text = "Create playlist", color = Color(0xFF111111), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                                // Shrinks this list's measured height so it stops above PersistentPlayerHost's mini bar instead of running full screen with its last row underneath the bar.
                                .padding(bottom = com.example.musicplayer.ui.components.common.miniPlayerBottomPadding()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
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

@Preview(showSystemUi = true, showBackground = true, backgroundColor = 0xFF000000, name = "PlaylistScreen - Empty")
@Composable
private fun PlaylistScreenPreview() {
    val navController = rememberNavController()
    MaterialTheme {
        PlaylistScreen(navController = navController, onPlaylistSelected = {})
    }
}

// Mirrors PlaylistScreen's real header/list/bottom-nav chrome (minus the ViewModel, which needs a real Context/Room DB) so this preview stays accurate as that screen evolves.
@OptIn(ExperimentalMaterial3Api::class)
@Preview(showSystemUi = true, showBackground = true, backgroundColor = 0xFF000000, name = "PlaylistScreen - With Data")
@Composable
private fun PlaylistScreenPreviewWithData() {
    val samplePlaylists = listOf(
        Playlist(id = 1, name = "Favorites", description = "Hand-picked", songIds = listOf(1, 2, 3)),
        Playlist(id = 2, name = "Road Trip", description = "Driving jams", songIds = listOf(4, 5)),
        Playlist(id = 3, name = "Late drives", description = "", songIds = emptyList()),
        Playlist(id = 4, name = "Chill", description = "Easy listening", songIds = listOf(6))
    )
    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            AuroraRibbonBackground()
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = "Playlists",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp,
                                lineHeight = 34.sp
                            )
                        },
                        actions = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color.White)
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Icon(imageVector = Icons.Filled.Add, contentDescription = null, tint = Color(0xFF111111), modifier = Modifier.size(18.dp))
                                Text(text = "New", color = Color(0xFF111111), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                    )
                },
                bottomBar = { BottomNav(selectedIndex = 2, onSelected = {}) },
                containerColor = Color.Transparent
            ) { innerPadding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(innerPadding)
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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

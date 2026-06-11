package com.example.musicplayer.history

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.padding
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.musicplayer.model.Song
import com.example.musicplayer.ui.components.common.MainBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    navController: NavHostController,
    onSongClick: (Song) -> Unit
) {
    val context = LocalContext.current
    val viewModel: HistoryViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HistoryViewModel(context = context) as T
            }
        }
    )

    val history by viewModel.history.collectAsState(initial = emptyList())

    HistoryScreenContent(
        songs = history,
        onBackClick = { navController.popBackStack() },
        onClearHistory = { viewModel.clearHistory() },
        onSongClick = onSongClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryScreenContent(
    songs: List<Song>,
    onBackClick: () -> Unit,
    onClearHistory: () -> Unit,
    onSongClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        MainBackground()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "History",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        if (songs.isNotEmpty()) {
                            IconButton(onClick = onClearHistory) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Clear history",
                                    tint = Color.White
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    )
                )
            }
        ) { innerPadding ->
            HistoryContent(
                songs = songs,
                onSongClick = onSongClick,
                modifier = modifier.padding(innerPadding)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, showSystemUi = true, name = "HistoryScreen")
@Composable
private fun HistoryScreenPreview() {
    val sampleSongs = listOf(
        Song(101, null, "Numb", "Linkin Park", 185000.0, "", null, 2003),
        Song(102, null, "Viva La Vida", "Coldplay", 242000.0, "", null, 2008),
        Song(103, null, "Levitating", "Dua Lipa", 203000.0, "", null, 2020)
    )
    MaterialTheme {
        HistoryScreenContent(
            songs = sampleSongs,
            onBackClick = {},
            onClearHistory = {},
            onSongClick = {}
        )
    }
}

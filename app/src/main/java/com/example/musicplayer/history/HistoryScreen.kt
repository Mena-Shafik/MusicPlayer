package com.example.musicplayer.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.musicplayer.model.Song
import com.example.musicplayer.ui.components.background.AuroraRibbonBackground

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
        plays = history,
        onBackClick = { navController.popBackStack() },
        onClearHistory = { viewModel.clearHistory() },
        onSongClick = onSongClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryScreenContent(
    plays: List<HistoryPlay>,
    onBackClick: () -> Unit,
    onClearHistory: () -> Unit,
    onSongClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AuroraRibbonBackground()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "History",
                            color = Color.White,
                            fontSize = 28.sp,
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
                        if (plays.isNotEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .padding(end = 20.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .border(
                                        BorderStroke(1.5.dp, Color.White.copy(alpha = 0.22f)),
                                        RoundedCornerShape(18.dp)
                                    )
                                    .clickable(onClick = onClearHistory)
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Clear",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
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
                plays = plays,
                onSongClick = onSongClick,
                modifier = modifier.padding(innerPadding)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, showSystemUi = true, name = "HistoryScreen")
@Composable
private fun HistoryScreenPreview() {
    val now = System.currentTimeMillis()
    val samplePlays = listOf(
        HistoryPlay(Song(101, null, "Numb", "Linkin Park", 185000.0, "", null, 2003), now - 10 * 60 * 1000),
        HistoryPlay(Song(102, null, "Viva La Vida", "Coldplay", 242000.0, "", null, 2008), now - 5 * 60 * 60 * 1000),
        HistoryPlay(Song(103, null, "Levitating", "Dua Lipa", 203000.0, "", null, 2020), now - 2L * 24 * 60 * 60 * 1000)
    )
    MaterialTheme {
        HistoryScreenContent(
            plays = samplePlays,
            onBackClick = {},
            onClearHistory = {},
            onSongClick = {}
        )
    }
}

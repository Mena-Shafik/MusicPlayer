package com.example.musicplayer.ui.components.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.musicplayer.model.Playlist
import com.example.musicplayer.model.Song
import com.example.musicplayer.playlist.PlaylistViewModel

@Composable
fun AddToPlaylistDialog(
    songId: Int,
    onDismiss: () -> Unit,
    onConfirm: (playlistId: Long) -> Unit = {},
    // Optional — only used to show the "adding this song" mini-header in the sheet.
    song: Song? = null
) {
    val context = LocalContext.current
    val viewModel: PlaylistViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PlaylistViewModel(context) as T
            }
        }
    )

    val playlists by viewModel.playlists.collectAsState()
    AddToPlaylistDialogContent(
        playlists = playlists,
        song = song,
        onDismiss = onDismiss,
        onConfirmInternal = { playlistId ->
            viewModel.addSongToPlaylist(playlistId, songId)
            onConfirm(playlistId)
        },
        onCreatePlaylist = { name, description -> viewModel.createPlaylist(name, description) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddToPlaylistDialogContent(
    playlists: List<Playlist>,
    song: Song?,
    onDismiss: () -> Unit,
    onConfirmInternal: (playlistId: Long) -> Unit,
    onCreatePlaylist: (name: String, description: String) -> Unit = { _, _ -> }
) {
    var selectedPlaylistId by remember { mutableStateOf<Long?>(null) }
    var showCreateSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF12141C),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        scrimColor = Color(0xFF03050C).copy(alpha = 0.72f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 18.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.22f))
            )
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                Text(text = "Add to playlist", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)

                if (song != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(top = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF2A2A2A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Filled.Album, contentDescription = null, tint = Color(0xFF5A5A5A), modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text(text = song.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(text = song.artist, color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            if (playlists.isEmpty()) {
                Text(
                    text = "No playlists created yet. Create one first!",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)
                )
            } else {
                Column(modifier = Modifier.padding(top = 18.dp)) {
                    playlists.forEach { playlist ->
                        val selected = selectedPlaylistId == playlist.id
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPlaylistId = if (selected) null else playlist.id }
                                .padding(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF2A2A2A)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null, tint = Color(0xFF5A5A5A), modifier = Modifier.size(24.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = playlist.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    text = "${playlist.songIds.size} ${if (playlist.songIds.size == 1) "song" else "songs"}",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 12.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(RoundedCornerShape(13.dp))
                                    .background(if (selected) Color(0xFFFFA500) else Color.Transparent)
                                    .border(2.dp, if (selected) Color(0xFFFFA500) else Color.White.copy(alpha = 0.3f), RoundedCornerShape(13.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) {
                                    Icon(imageVector = Icons.Filled.Check, contentDescription = null, tint = Color(0xFF111111), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(top = 8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCreateSheet = true }
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.5.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = null, tint = Color.White.copy(alpha = 0.7f))
                    }
                    Text(text = "New playlist", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 14.dp)
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (selectedPlaylistId != null) Color.White else Color.White.copy(alpha = 0.3f))
                        .clickable(enabled = selectedPlaylistId != null) {
                            selectedPlaylistId?.let { onConfirmInternal(it) }
                            onDismiss()
                        }
                ) {
                    Text(text = "Add", color = Color(0xFF111111), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }

    if (showCreateSheet) {
        CreatePlaylistDialog(
            showDialog = true,
            onDismiss = { showCreateSheet = false },
            onCreatePlaylist = { name, description ->
                onCreatePlaylist(name, description)
                showCreateSheet = false
            }
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF03050C)
@Composable
private fun AddToPlaylistDialogPreview_Empty() {
    MaterialTheme {
        AddToPlaylistDialogContent(
            playlists = emptyList(),
            song = Song(1, null, "Afterglow", "Nova Reyes", 210000.0, "", null, 2021),
            onDismiss = {},
            onConfirmInternal = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF03050C)
@Composable
private fun AddToPlaylistDialogPreview_WithPlaylists() {
    val sample = listOf(
        Playlist(id = 1, name = "Workout", songIds = listOf(1, 2, 3)),
        Playlist(id = 2, name = "Chill", songIds = listOf(4, 5)),
        Playlist(id = 3, name = "Roadtrip", songIds = listOf(6))
    )
    MaterialTheme {
        AddToPlaylistDialogContent(
            playlists = sample,
            song = Song(1, null, "Afterglow", "Nova Reyes", 210000.0, "", null, 2021),
            onDismiss = {},
            onConfirmInternal = {}
        )
    }
}

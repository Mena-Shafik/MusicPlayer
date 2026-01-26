package com.example.musicplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.musicplayer.model.Playlist
import com.example.musicplayer.playlist.PlaylistViewModel

@Composable
fun AddToPlaylistDialog(
    songId: Int,
    onDismiss: () -> Unit,
    onConfirm: (playlistId: Long) -> Unit = {}
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
    AddToPlaylistDialogContent(
        playlists = playlists,
        onDismiss = onDismiss,
        onConfirmInternal = { playlistId ->
            viewModel.addSongToPlaylist(playlistId, songId)
            onConfirm(playlistId)
        }
    )
}

@Composable
private fun AddToPlaylistDialogContent(
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onConfirmInternal: (playlistId: Long) -> Unit
) {
    var selectedPlaylistId by remember { mutableStateOf<Long?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Add to Playlist", color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                if (playlists.isEmpty()) {
                    Text(
                        "No playlists created yet. Create one first!",
                        color = Color.Gray,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    LazyColumn {
                        items(playlists) { playlist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedPlaylistId = if (selectedPlaylistId == playlist.id) null else playlist.id
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = selectedPlaylistId == playlist.id,
                                    onCheckedChange = {
                                        selectedPlaylistId = if (it) playlist.id else null
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Color(0xFFFFA500),
                                        uncheckedColor = Color.Gray
                                    )
                                )
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 12.dp)
                                ) {
                                    Text(
                                        text = playlist.name,
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "${playlist.songIds.size} songs",
                                        color = Color.Gray,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            Divider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedPlaylistId?.let { playlistId ->
                        onConfirmInternal(playlistId)
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFA500),
                    disabledContainerColor = Color.Gray
                ),
                enabled = selectedPlaylistId != null
            ) {
                Text("Add", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
            ) {
                Text("Cancel", color = Color.White)
            }
        },
        containerColor = Color.DarkGray,
        modifier = Modifier
            .background(Color.DarkGray, RoundedCornerShape(8.dp))
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun AddToPlaylistDialogPreview_Empty() {
    AddToPlaylistDialogContent(
        playlists = emptyList(),
        onDismiss = {},
        onConfirmInternal = {}
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun AddToPlaylistDialogPreview_WithPlaylists() {
    val sample = listOf(
        Playlist(id = 1, name = "Workout", songIds = listOf(1, 2, 3)),
        Playlist(id = 2, name = "Chill", songIds = listOf(4, 5)),
        Playlist(id = 3, name = "Roadtrip", songIds = listOf(6))
    )
    AddToPlaylistDialogContent(
        playlists = sample,
        onDismiss = {},
        onConfirmInternal = {}
    )
}

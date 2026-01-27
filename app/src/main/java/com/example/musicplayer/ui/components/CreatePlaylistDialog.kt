package com.example.musicplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun CreatePlaylistDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onCreatePlaylist: (name: String, description: String) -> Unit
) {
    var playlistName by remember { mutableStateOf("") }
    var playlistDescription by remember { mutableStateOf("") }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                onDismiss()
                playlistName = ""
                playlistDescription = ""
            },
            title = { Text("Create New Playlist", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = playlistName,
                        onValueChange = { playlistName = it },
                        label = { Text("Playlist Name") },
                        colors = TextFieldDefaults.colors(
                            unfocusedTextColor = Color.White,
                            focusedTextColor = Color.White,
                            unfocusedLabelColor = Color.Gray,
                            focusedLabelColor = Color(0xFFFFA500),
                            unfocusedContainerColor = Color.DarkGray,
                            focusedContainerColor = Color.DarkGray,
                            cursorColor = Color(0xFFFFA500)
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextField(
                        value = playlistDescription,
                        onValueChange = { playlistDescription = it },
                        label = { Text("Description (Optional)") },
                        colors = TextFieldDefaults.colors(
                            unfocusedTextColor = Color.White,
                            focusedTextColor = Color.White,
                            unfocusedLabelColor = Color.Gray,
                            focusedLabelColor = Color(0xFFFFA500),
                            unfocusedContainerColor = Color.DarkGray,
                            focusedContainerColor = Color.DarkGray,
                            cursorColor = Color(0xFFFFA500)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (playlistName.isNotBlank()) {
                            onCreatePlaylist(playlistName, playlistDescription)
                            playlistName = ""
                            playlistDescription = ""
                        }
                    },

                ) {
                    Text("Create", color = Color.Black)
                }
            },
            dismissButton = {
                Button(onClick = {
                    onDismiss()
                    playlistName = ""
                    playlistDescription = ""
                }) {
                    Text("Cancel", color = Color.Black)
                }
            },
            containerColor = Color.DarkGray
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF2C2C2C, name = "CreatePlaylistDialog Preview")
@Composable
fun CreatePlaylistDialogPreview() {
    MaterialTheme {
        CreatePlaylistDialog(
            showDialog = true,
            onDismiss = { },
            onCreatePlaylist = { name, description -> }
        )
    }
}

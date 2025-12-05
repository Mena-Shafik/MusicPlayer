package com.example.musicplayer.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Radio
import androidx.compose.ui.graphics.vector.ImageVector

enum class Destination(val route: String, val label: String, val icon: ImageVector) {
    SONGS("songs", "Songs", Icons.Filled.MusicNote),
    ALBUMS("albums", "Albums", Icons.Filled.LibraryMusic),
    RADIO("radio", "Radio", Icons.Filled.Radio),
    PLAYER("player", "Player", Icons.Filled.MusicNote)
}
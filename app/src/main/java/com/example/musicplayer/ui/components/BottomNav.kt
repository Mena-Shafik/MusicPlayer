package com.example.musicplayer.ui.components

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BottomNav(
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    containerColor: Color = Color.Transparent,
    contentColor: Color = Color.White,
    selectedColor: Color = Color(0xFFFFA500)
) {
    NavigationBar(
        containerColor = containerColor,
        modifier = Modifier.height(110.dp).navigationBarsPadding()
    ) {
        NavigationBarItem(
            selected = selectedIndex == 0,
            onClick = { onSelected(0) },
            icon = { Icon(Icons.Filled.LibraryMusic, contentDescription = "Songs") },
            label = { Text("Songs") },
            alwaysShowLabel = true,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = selectedColor,
                unselectedIconColor = contentColor,
                selectedTextColor = selectedColor,
                unselectedTextColor = contentColor,
                indicatorColor = Color.Transparent
            )
        )
        NavigationBarItem(
            selected = selectedIndex == 1,
            onClick = { onSelected(1) },
            icon = { Icon(Icons.Filled.Radio, contentDescription = "Radio") },
            label = { Text("Radio") },
            alwaysShowLabel = true,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = selectedColor,
                unselectedIconColor = contentColor,
                selectedTextColor = selectedColor,
                unselectedTextColor = contentColor,
                indicatorColor = Color.Transparent
            )
        )
        NavigationBarItem(
            selected = selectedIndex == 2,
            onClick = { onSelected(2) },
            icon = { Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = "Playlists") },
            label = { Text("Playlists") },
            alwaysShowLabel = true,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = selectedColor,
                unselectedIconColor = contentColor,
                selectedTextColor = selectedColor,
                unselectedTextColor = contentColor,
                indicatorColor = Color.Transparent
            )
        )
    }
}

@Preview(name = "BottomNav Preview", backgroundColor = 0xFF000000, showBackground = true)
@Composable
fun BottomNavPreview() {
    MaterialTheme {
        BottomNav(selectedIndex = 0, onSelected = { /* no-op in preview */ })
    }
}

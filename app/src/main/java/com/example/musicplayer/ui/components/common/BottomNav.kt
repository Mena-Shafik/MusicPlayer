package com.example.musicplayer.ui.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicplayer.R

private data class NavTab(val selectedIcon: Int, val unselectedIcon: Int, val label: String)

private val navTabs = listOf(
    NavTab(R.drawable.ic_nav_songs_selected, R.drawable.ic_nav_songs_unselected, "Songs"),
    NavTab(R.drawable.ic_nav_radio_selected, R.drawable.ic_nav_radio_unselected, "Radio"),
    NavTab(R.drawable.ic_nav_playlists_selected, R.drawable.ic_nav_playlists_unselected, "Playlists"),
)

/**
 * Flat, blurred-dock bottom nav in the "2.0" design language: duotone line icons
 * (orange accent for the selected tab) on a translucent dark bar with a hairline top
 * border, instead of Material3's default filled indicator pill.
 */
@Composable
fun BottomNav(
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    containerColor: Color = Color.Black.copy(alpha = 0.45f),
    contentColor: Color = Color.White.copy(alpha = 0.7f),
    selectedColor: Color = Color(0xFFFFA500)
) {
    Column(modifier = Modifier.fillMaxWidth().background(containerColor)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.08f))
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(top = 10.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            navTabs.forEachIndexed { index, tab ->
                val selected = index == selectedIndex
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelected(index) }
                ) {
                    Icon(
                        painter = painterResource(if (selected) tab.selectedIcon else tab.unselectedIcon),
                        contentDescription = tab.label,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = tab.label,
                        color = if (selected) selectedColor else contentColor,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Preview(name = "BottomNav Preview", backgroundColor = 0xFF000000, showBackground = true)
@Composable
fun BottomNavPreview() {
    MaterialTheme {
        BottomNav(selectedIndex = 0, onSelected = { /* no-op in preview */ })
    }
}

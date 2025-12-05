package com.example.musicplayer.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.musicplayer.navigation.Destination

@Composable
fun Tabs(
    modifier: Modifier = Modifier,
    navController: NavHostController? = null,
    destinations: List<Destination> = Destination.entries.take(3).toList(),
    selectedIndex: Int = destinations.indexOfFirst { it == Destination.SONGS }.takeIf { it >= 0 } ?: 0,
    onSelectedIndexChange: (Int) -> Unit = {}
) {
    // keep stable reference for indicator
    val tabList = remember(destinations) { destinations }

    PrimaryTabRow(
        selectedTabIndex = selectedIndex,
        modifier = modifier.background(Color.Transparent),
        containerColor = Color.Transparent,
        divider = {},
        indicator = {
            // apply the default tab indicator offset so width/position matches the active tab
            TabRowDefaults.Indicator(
                modifier = Modifier.tabIndicatorOffset(
                    matchContentSize = true,
                    selectedTabIndex = selectedIndex
                ),
                color = Color(0xFFFFA500)
            )
        }
    ) {
        tabList.forEachIndexed { index, destination ->
            Tab(
                modifier = Modifier.background(Color.Transparent),
                selected = selectedIndex == index,
                selectedContentColor = Color(0xFFFFA500),
                unselectedContentColor = Color(0xFFEEB65D),
                onClick = {
                    onSelectedIndexChange(index)
                    navController?.navigate(destination.route)
                },
                icon = {
                    Icon(imageVector = destination.icon, contentDescription = destination.label)
                },
                /*text = {
                    Text(
                        text = destination.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }*/
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun TabsPreview() {
    Tabs()
}

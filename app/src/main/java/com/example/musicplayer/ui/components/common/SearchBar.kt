package com.example.musicplayer.ui.components.common

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppBar(
    showSearch: Boolean,
    onToggleSearch: () -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchedClicked: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPlaylists: () -> Unit = {},
    title: String = "Songs",
    searchEnabled: Boolean = true,
    onAddPlaylist: (() -> Unit)? = null
) {
    AnimatedContent(
        targetState = showSearch && searchEnabled,
        transitionSpec = {
            if (targetState) {
                // Entering search mode: fade in + scale in
                fadeIn(animationSpec = tween(400)) + scaleIn(
                    initialScale = 0.95f,
                    animationSpec = tween(400)
                ) togetherWith fadeOut(animationSpec = tween(200)) + scaleOut(
                    targetScale = 1.05f,
                    animationSpec = tween(200)
                )
            } else {
                // Exiting search mode: fade out + scale out
                fadeIn(animationSpec = tween(300)) + scaleIn(
                    initialScale = 0.95f,
                    animationSpec = tween(300)
                ) togetherWith fadeOut(animationSpec = tween(400)) + scaleOut(
                    targetScale = 1.05f,
                    animationSpec = tween(400)
                )
            }
        },
        label = "Search bar transition"
    ) { isSearchVisible ->
        if (isSearchVisible) {
            TopAppBar(
                title = {
                    SearchBar(
                        text = query,
                        onTextChange = onQueryChange,
                        onCloseClicked = {
                            onQueryChange("")
                            onToggleSearch()
                        },
                        onSearchedClicked = {
                            onSearchedClicked(it)
                        }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        } else {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                actions = {
                    if (onAddPlaylist != null) {
                        IconButton(onClick = onAddPlaylist) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Add Playlist",
                                tint = Color.White
                            )
                        }
                    }
                    if (searchEnabled) {
                        IconButton(onClick = onToggleSearch) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Search Icon",
                                tint = Color.White
                            )
                        }
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Open settings",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.statusBarsPadding()
            )
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
//@RequiresApi(Build.VERSION_CODES.M) //keep
@Composable
fun SearchBar(
    text: String,
    onTextChange: (String) -> Unit,
    onCloseClicked: () -> Unit,
    onSearchedClicked: (String) -> Unit
) {

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .statusBarsPadding(), // ensure SearchBar sits below the status bar
        //elevation = AppBarDefaults.TopAppBarElevation,
        //elevation= AppBarDefaults.
        color = Color.Transparent
    ) {
        TextField(
            modifier = Modifier.fillMaxWidth(),
            value = text,
            onValueChange = { onTextChange(it) },
            placeholder = {
                Text(
                    modifier = Modifier.alpha(0.6f),
                    text = "Search",
                    color = Color.White
                )
            },
            textStyle = TextStyle(
                fontSize = MaterialTheme.typography.bodySmall.fontSize,
                color = Color.White
            ),
            singleLine = true,
            leadingIcon = {
                IconButton(
                    modifier = Modifier.alpha(0.6f),
                    onClick = { onSearchedClicked(text) } // perform search when user taps the icon
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color.White
                    )
                }
            },
            trailingIcon = {
                IconButton(
                    onClick = {
                        if (text.isNotEmpty()) {
                            onTextChange("")
                        } else {
                            onCloseClicked()
                        }
                    }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Search",
                        tint = Color.White
                    )
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearchedClicked(text) }),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                cursorColor = Color(0xFFFFA500),
                focusedIndicatorColor = Color(0xFFFFA500)
            )
        )
    }
}

@Preview(name = "MainAppBar - Songs On", backgroundColor = 0xFF000000)
@Composable
fun MainAppBarSongPreview() {
    MaterialTheme {
        MainAppBar(
            showSearch = false,
            onToggleSearch = {},
            query = "",
            onQueryChange = {},
            onSearchedClicked = {},
            onOpenSettings = {},
            onOpenPlaylists = {},
            title = "Songs"
        )
    }
}

@Preview(name = "MainAppBar - Radio On", backgroundColor = 0xFF000000)
@Composable
fun MainAppBarRadioPreview() {
    MaterialTheme {
        MainAppBar(
            showSearch = false,
            onToggleSearch = {},
            query = "",
            onQueryChange = {},
            onSearchedClicked = {},
            onOpenSettings = {},
            onOpenPlaylists = {},
            title = "Radio"
        )
    }
}

@Preview(name = "MainAppBar - Search", backgroundColor = 0xFF000000)
@Composable
fun MainAppBarSearchPreview() {
    MaterialTheme {
        MainAppBar(
            showSearch = true,
            onToggleSearch = {},
            query = "Search text",
            onQueryChange = {},
            onSearchedClicked = {},
            onOpenSettings = {},
            onOpenPlaylists = {},
            title = "Songs"
        )
    }
}
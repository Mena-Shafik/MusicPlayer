package com.example.musicplayer.navigation

/**
 * Route definitions for the app navigation.
 *
 * This file only defines route strings and helpers. To wire navigation, add a NavHost
 * in your Activity or a dedicated NavHost composable using `androidx.navigation:navigation-compose`.
 */
sealed class Route(val route: String) {
    object Splash : Route("splash")
    object SongList : Route("song_list")
    object MusicPlayer : Route("music_player/{songId}") {
        fun createRoute(songId: Int) = "music_player/$songId"
    }
}

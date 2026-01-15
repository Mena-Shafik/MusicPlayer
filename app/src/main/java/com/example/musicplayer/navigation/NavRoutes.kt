package com.example.musicplayer.navigation

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Route definitions for the app navigation.
 *
 * This sealed class defines all navigation routes with type-safe helpers.
 */
sealed class NavRoutes(val route: String) {
    object Splash : NavRoutes("splash")
    object Home : NavRoutes("home")
    object SongList : NavRoutes("song_list")
    object Library : NavRoutes("library")
    object Search : NavRoutes("search")
    object Settings : NavRoutes("settings")

    object MusicPlayer : NavRoutes("musicScreen/{songId}") {
        fun createRoute(songId: Int) = "musicScreen/$songId"
    }

    object RadioPlayer : NavRoutes("radioPlayer/{name}/{url}/{favicon}/{tags}") {
        fun createRoute(name: String, url: String, favicon: String, tags: String): String {
            val encodedName = java.net.URLEncoder.encode(name, "UTF-8")
            val encodedUrl = java.net.URLEncoder.encode(url, "UTF-8")
            val encodedFavicon = java.net.URLEncoder.encode(favicon, "UTF-8")
            val encodedTags = java.net.URLEncoder.encode(tags, "UTF-8")
            return "radioPlayer/$encodedName/$encodedUrl/$encodedFavicon/$encodedTags"
        }
    }

    object Playlist : NavRoutes("playlist/{playlistId}") {
        fun createRoute(playlistId: String) = "playlist/$playlistId"
    }

    object Album : NavRoutes("album/{albumId}") {
        fun createRoute(albumId: String) = "album/$albumId"
    }

    object Artist : NavRoutes("artist/{artistId}") {
        fun createRoute(artistId: String) = "artist/$artistId"
    }
}

/**
 * Navigation preferences to persist the current route across app restarts.
 */
private val Context.navDataStore by preferencesDataStore(name = "nav_preferences")

object NavigationPreferences {
    private val CURRENT_NAV_ROUTE_KEY = stringPreferencesKey("current_nav_route")

    fun getCurrentNavRouteFlow(context: Context): Flow<String?> =
        context.navDataStore.data.map { preferences ->
            preferences[CURRENT_NAV_ROUTE_KEY]
        }

    suspend fun setCurrentNavRoute(context: Context, route: String) {
        context.navDataStore.edit { preferences ->
            preferences[CURRENT_NAV_ROUTE_KEY] = route
        }
    }
}

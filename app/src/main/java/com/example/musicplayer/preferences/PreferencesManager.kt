package com.example.musicplayer.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "app_preferences")

object PreferencesManager {
    private val ALBUM_VIEW_KEY = booleanPreferencesKey("album_view_enabled")
    private val RADIO_SELECTED_KEY = booleanPreferencesKey("radio_selected")
    private val ARTIST_VIEW_KEY = booleanPreferencesKey("artist_view_enabled")
    private val USE_DEFAULT_RADIO_LIST_KEY = booleanPreferencesKey("use_default_radio_list")
    private val ERA_VIEW_KEY = booleanPreferencesKey("era_view_enabled")
    private val USE_AURORA_BG_KEY = booleanPreferencesKey("use_aurora_background")
    private val AURORA_PALETTE_KEY = stringPreferencesKey("aurora_palette")
    private val USE_ALBUM_PALETTE_KEY = booleanPreferencesKey("use_album_palette_in_songlist")

    private val NAVIDROME_CONNECTED_KEY = booleanPreferencesKey("navidrome_connected")
    private val NAVIDROME_SERVER_VERSION_KEY = stringPreferencesKey("navidrome_server_version")
    private val NAVIDROME_SONG_COUNT_KEY = intPreferencesKey("navidrome_song_count")
    private val NAVIDROME_ALBUM_COUNT_KEY = intPreferencesKey("navidrome_album_count")
    private val NAVIDROME_LAST_SYNCED_AT_KEY = longPreferencesKey("navidrome_last_synced_at")
    private val NAVIDROME_ALLOW_SELF_SIGNED_KEY = booleanPreferencesKey("navidrome_allow_self_signed")

    fun getAlbumViewFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[ALBUM_VIEW_KEY] ?: false
        }

    fun getEraViewFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[ERA_VIEW_KEY] ?: false
        }

    suspend fun setEraView(context: Context, isEraView: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ERA_VIEW_KEY] = isEraView
        }
    }

    suspend fun setAlbumView(context: Context, isAlbumView: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ALBUM_VIEW_KEY] = isAlbumView
        }
    }

    fun getArtistViewFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[ARTIST_VIEW_KEY] ?: false
        }

    suspend fun setArtistView(context: Context, isArtistView: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ARTIST_VIEW_KEY] = isArtistView
        }
    }

    fun getRadioSelectedFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[RADIO_SELECTED_KEY] ?: false
        }

    suspend fun setRadioSelected(context: Context, isRadioSelected: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[RADIO_SELECTED_KEY] = isRadioSelected
        }
    }

    fun getUseDefaultRadioListFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[USE_DEFAULT_RADIO_LIST_KEY] ?: true
        }

    suspend fun setUseDefaultRadioList(context: Context, useDefault: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[USE_DEFAULT_RADIO_LIST_KEY] = useDefault
        }
    }

    fun getUseAuroraBackgroundFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[USE_AURORA_BG_KEY] ?: false
        }

    suspend fun setUseAuroraBackground(context: Context, useAurora: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[USE_AURORA_BG_KEY] = useAurora
        }
    }

    fun getAuroraPaletteFlow(context: Context): Flow<String> =
        context.dataStore.data.map { preferences ->
            preferences[AURORA_PALETTE_KEY] ?: "Northern"
        }

    suspend fun setAuroraPalette(context: Context, paletteName: String) {
        context.dataStore.edit { preferences ->
            preferences[AURORA_PALETTE_KEY] = paletteName
        }
    }

    // Off by default: the song list's background otherwise follows the fixed Aurora
    // palette above, same as every other screen — this opts a single screen into
    // sampling live colors from album art instead.
    fun getUseAlbumPaletteFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[USE_ALBUM_PALETTE_KEY] ?: false
        }

    suspend fun setUseAlbumPalette(context: Context, useAlbumPalette: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[USE_ALBUM_PALETTE_KEY] = useAlbumPalette
        }
    }

    // Non-secret Navidrome connection state. Credentials themselves live in
    // NavidromeCredentialsStore (encrypted), not here.

    fun getNavidromeConnectedFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { preferences -> preferences[NAVIDROME_CONNECTED_KEY] ?: false }

    suspend fun setNavidromeConnected(context: Context, connected: Boolean) {
        context.dataStore.edit { preferences -> preferences[NAVIDROME_CONNECTED_KEY] = connected }
    }

    fun getNavidromeServerVersionFlow(context: Context): Flow<String?> =
        context.dataStore.data.map { preferences -> preferences[NAVIDROME_SERVER_VERSION_KEY] }

    suspend fun setNavidromeServerVersion(context: Context, version: String?) {
        context.dataStore.edit { preferences ->
            if (version != null) preferences[NAVIDROME_SERVER_VERSION_KEY] = version
            else preferences.remove(NAVIDROME_SERVER_VERSION_KEY)
        }
    }

    fun getNavidromeSongCountFlow(context: Context): Flow<Int> =
        context.dataStore.data.map { preferences -> preferences[NAVIDROME_SONG_COUNT_KEY] ?: 0 }

    fun getNavidromeAlbumCountFlow(context: Context): Flow<Int> =
        context.dataStore.data.map { preferences -> preferences[NAVIDROME_ALBUM_COUNT_KEY] ?: 0 }

    suspend fun setNavidromeLibraryCounts(context: Context, songCount: Int, albumCount: Int) {
        context.dataStore.edit { preferences ->
            preferences[NAVIDROME_SONG_COUNT_KEY] = songCount
            preferences[NAVIDROME_ALBUM_COUNT_KEY] = albumCount
        }
    }

    fun getNavidromeLastSyncedAtFlow(context: Context): Flow<Long> =
        context.dataStore.data.map { preferences -> preferences[NAVIDROME_LAST_SYNCED_AT_KEY] ?: 0L }

    suspend fun setNavidromeLastSyncedAt(context: Context, timestampMs: Long) {
        context.dataStore.edit { preferences -> preferences[NAVIDROME_LAST_SYNCED_AT_KEY] = timestampMs }
    }

    fun getNavidromeAllowSelfSignedFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { preferences -> preferences[NAVIDROME_ALLOW_SELF_SIGNED_KEY] ?: false }

    suspend fun setNavidromeAllowSelfSigned(context: Context, allow: Boolean) {
        context.dataStore.edit { preferences -> preferences[NAVIDROME_ALLOW_SELF_SIGNED_KEY] = allow }
    }

    /** Clears every Navidrome-related preference on disconnect. Credentials are cleared
     * separately via NavidromeCredentialsStore.clear(). */
    suspend fun clearNavidromeState(context: Context) {
        context.dataStore.edit { preferences ->
            preferences.remove(NAVIDROME_CONNECTED_KEY)
            preferences.remove(NAVIDROME_SERVER_VERSION_KEY)
            preferences.remove(NAVIDROME_SONG_COUNT_KEY)
            preferences.remove(NAVIDROME_ALBUM_COUNT_KEY)
            preferences.remove(NAVIDROME_LAST_SYNCED_AT_KEY)
        }
    }
}

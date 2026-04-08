package com.example.musicplayer.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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
    private val GENRE_VIEW_KEY = booleanPreferencesKey("genre_view_enabled")

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

    fun getGenreViewFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[GENRE_VIEW_KEY] ?: false
        }

    suspend fun setArtistView(context: Context, isArtistView: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ARTIST_VIEW_KEY] = isArtistView
        }
    }

    suspend fun setGenreView(context: Context, isGenreView: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[GENRE_VIEW_KEY] = isGenreView
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
}

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

    fun getAlbumViewFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[ALBUM_VIEW_KEY] ?: false
        }

    suspend fun setAlbumView(context: Context, isAlbumView: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ALBUM_VIEW_KEY] = isAlbumView
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
}


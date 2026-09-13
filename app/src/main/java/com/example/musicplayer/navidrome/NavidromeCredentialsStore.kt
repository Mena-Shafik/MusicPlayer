package com.example.musicplayer.navidrome

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

data class NavidromeCredentials(
    val serverUrl: String,
    val username: String,
    val password: String
)

// Server URL/username/password are the only secrets in this app, so unlike PreferencesManager's plaintext DataStore these are kept in a Keystore-backed EncryptedSharedPreferences file.
object NavidromeCredentialsStore {
    private const val PREFS_NAME = "navidrome_credentials"
    private const val KEY_SERVER_URL = "server_url"
    private const val KEY_USERNAME = "username"
    private const val KEY_PASSWORD = "password"

    private fun prefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun get(context: Context): NavidromeCredentials? {
        val p = prefs(context)
        val url = p.getString(KEY_SERVER_URL, null) ?: return null
        val username = p.getString(KEY_USERNAME, null) ?: return null
        val password = p.getString(KEY_PASSWORD, null) ?: return null
        return NavidromeCredentials(url, username, password)
    }

    fun save(context: Context, credentials: NavidromeCredentials) {
        prefs(context).edit()
            .putString(KEY_SERVER_URL, credentials.serverUrl)
            .putString(KEY_USERNAME, credentials.username)
            .putString(KEY_PASSWORD, credentials.password)
            .apply()
    }

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }
}

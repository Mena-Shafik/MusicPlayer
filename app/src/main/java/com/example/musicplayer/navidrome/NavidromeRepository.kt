package com.example.musicplayer.navidrome

import android.content.Context
import android.util.Log
import com.example.musicplayer.model.Song
import com.example.musicplayer.preferences.PreferencesManager
import com.example.musicplayer.util.Util
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import kotlin.math.abs

sealed class ReachabilityResult {
    data class Reachable(val tookMs: Long) : ReachabilityResult()
    data class Unreachable(val message: String) : ReachabilityResult()
}

sealed class AuthResult {
    data class Success(val serverType: String?, val serverVersion: String?) : AuthResult()
    data class Failure(val message: String) : AuthResult()
}

data class LibraryScanProgress(val songCount: Int, val albumCount: Int)

// Wraps the Subsonic/Navidrome connection flow: reachability probe, credentialed ping, paginated library count -- not a full catalogue sync.
class NavidromeRepository(private val context: Context) {
    private val TAG = "NavidromeRepository"
    private fun safeLog(message: String) {
        try { Log.d(TAG, message) } catch (_: Throwable) {}
    }

    suspend fun checkReachability(serverUrl: String, trustSelfSigned: Boolean): ReachabilityResult =
        withContext(Dispatchers.IO) {
            val normalizedUrl = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
            val builder = OkHttpClient.Builder()
            if (trustSelfSigned) NavidromeApiService.applyTrustAllCerts(builder)
            val client = builder.build()
            val startNs = System.nanoTime()
            try {
                val request = Request.Builder().url(normalizedUrl).head().build()
                client.newCall(request).execute().use { response ->
                    val tookMs = (System.nanoTime() - startNs) / 1_000_000
                    // Any HTTP response at all (including 404s from a bare server root) means the host/TLS handshake succeeded -- that's all "reachable" claims here.
                    safeLog("Reachability check: HTTP ${response.code} in ${tookMs}ms")
                    ReachabilityResult.Reachable(tookMs)
                }
            } catch (e: IOException) {
                safeLog("Reachability check failed: ${e.message}")
                ReachabilityResult.Unreachable(describeConnectionError(e))
            }
        }

    suspend fun authenticate(
        serverUrl: String,
        username: String,
        password: String,
        trustSelfSigned: Boolean
    ): AuthResult = withContext(Dispatchers.IO) {
        try {
            val api = NavidromeApiService.create(serverUrl, username, password, trustSelfSigned)
            val wrapper = api.ping()
            val response = wrapper.response
            when {
                response == null -> AuthResult.Failure("Server returned an unexpected response")
                response.isOk -> AuthResult.Success(response.type, response.serverVersion)
                else -> AuthResult.Failure(describeSubsonicError(response.error))
            }
        } catch (e: Exception) {
            safeLog("Authentication failed: ${e.message}")
            AuthResult.Failure(describeConnectionError(e))
        }
    }

    // Pages through getAlbumList2 summing songCount/album totals, reporting progress after each page for a live "N songs so far" count; doesn't fetch/cache individual songs -- that's the streaming-playback phase.
    suspend fun scanLibrary(
        serverUrl: String,
        username: String,
        password: String,
        trustSelfSigned: Boolean,
        onProgress: (LibraryScanProgress) -> Unit
    ): LibraryScanProgress = withContext(Dispatchers.IO) {
        val api = NavidromeApiService.create(serverUrl, username, password, trustSelfSigned)
        scanLibraryViaApi(api, onProgress)
    }

    // Informational count for the setup screen only -- no actual de-duplication/merge happens against this yet.
    suspend fun localSongCount(): Int = withContext(Dispatchers.IO) {
        Util.getAllAudioFromDevice(context).size
    }

    // Fetches the full remote catalogue as playable Songs (stream URL already baked into `path`, so they play through the existing local pipeline unmodified); returns empty if not connected -- callers should already gate on the connected pref.
    suspend fun listCatalogueSongs(): List<Song> = withContext(Dispatchers.IO) {
        val credentials = NavidromeCredentialsStore.get(context) ?: return@withContext emptyList()
        val allowSelfSigned = PreferencesManager.getNavidromeAllowSelfSignedFlow(context).first()
        val api = NavidromeApiService.create(credentials.serverUrl, credentials.username, credentials.password, allowSelfSigned)
        listCatalogueSongsViaApi(api, credentials.serverUrl, credentials.username, credentials.password)
    }

    suspend fun disconnect() {
        NavidromeCredentialsStore.clear(context)
        PreferencesManager.clearNavidromeState(context)
        PreferencesManager.setNavidromeConnected(context, false)
    }

    private fun describeConnectionError(e: Exception): String = when {
        e.message?.contains("timeout", ignoreCase = true) == true -> "Connection timed out"
        e.message?.contains("Unable to resolve host", ignoreCase = true) == true -> "Couldn't find that server address"
        e.message?.contains("CertPathValidatorException", ignoreCase = true) == true ||
            e.message?.contains("SSLHandshakeException", ignoreCase = true) == true ->
            "Certificate not trusted -- enable \"Allow self-signed certificate\" if this is a private server"
        else -> e.message ?: "Couldn't reach the server"
    }

    private fun describeSubsonicError(error: SubsonicError?): String {
        if (error == null) return "The server rejected the request"
        return when (error.code) {
            40, 41 -> "Wrong username or password"
            50 -> "This account isn't authorized for that action"
            0 -> error.message ?: "A generic server error occurred"
            else -> error.message ?: "The server rejected the request"
        }
    }
}

// Pages through getAlbumList2 summing songCount/album totals; a top-level function (not private inside NavidromeRepository) so it's unit-testable against a fake NavidromeApiService without Retrofit/OkHttp.
suspend fun scanLibraryViaApi(
    api: NavidromeApiService,
    onProgress: (LibraryScanProgress) -> Unit = {}
): LibraryScanProgress {
    var songCount = 0
    var albumCount = 0
    var offset = 0
    val pageSize = 500
    while (true) {
        val wrapper = api.getAlbumList2(size = pageSize, offset = offset)
        val albums = wrapper.response?.albumList2?.album.orEmpty()
        if (albums.isEmpty()) break
        albumCount += albums.size
        songCount += albums.sumOf { it.songCount ?: 0 }
        onProgress(LibraryScanProgress(songCount, albumCount))
        if (albums.size < pageSize) break
        offset += pageSize
    }
    return LibraryScanProgress(songCount, albumCount)
}

// Deterministic synthetic id for a remote song: local MediaStore ids are always >= 0, so keeping every remote id negative makes local/remote collisions impossible regardless of hash quality.
internal fun syntheticRemoteSongId(remoteId: String): Int =
    -(1 + (abs(remoteId.hashCode().toLong()) % Int.MAX_VALUE).toInt())

private fun SubsonicSong.toSongOrNull(baseUrl: String, username: String, password: String): Song? {
    val remoteSongId = id ?: return null
    val streamUrl = NavidromeApiService.buildStreamUrl(baseUrl, username, password, remoteSongId)
    val song = Song(
        syntheticRemoteSongId(remoteSongId),
        track,
        title ?: "Unknown",
        artist ?: "Unknown",
        (duration ?: 0) * 1000.0,
        streamUrl,
        album,
        year ?: 0
    )
    song.isRemote = true
    song.remoteId = remoteSongId
    song.remoteCoverArtUrl = coverArt?.let { NavidromeApiService.buildCoverArtUrl(baseUrl, username, password, it) }
    return song
}

// Pages through search3 mapping every result to a playable Song (top-level for testability, same reasoning as scanLibraryViaApi); capped at 20 pages (10,000 songs) as a defensive limit for huge libraries.
suspend fun listCatalogueSongsViaApi(
    api: NavidromeApiService,
    baseUrl: String,
    username: String,
    password: String
): List<Song> {
    val songs = mutableListOf<Song>()
    var offset = 0
    val pageSize = 500
    var page = 0
    val maxPages = 20
    while (page < maxPages) {
        val wrapper = api.search3(query = "", songCount = pageSize, songOffset = offset)
        val pageSongs = wrapper.response?.searchResult3?.song.orEmpty()
        if (pageSongs.isEmpty()) break
        songs += pageSongs.mapNotNull { it.toSongOrNull(baseUrl, username, password) }
        if (pageSongs.size < pageSize) break
        offset += pageSize
        page++
    }
    return songs
}

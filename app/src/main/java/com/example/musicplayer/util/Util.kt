package com.example.musicplayer.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.net.toUri
import com.example.musicplayer.R
import com.example.musicplayer.model.RadioStation
import com.example.musicplayer.model.Song
import com.example.musicplayer.radio.RadioApiService
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import com.google.gson.reflect.TypeToken

import java.io.File
import java.net.ConnectException
import java.net.SocketTimeoutException

import java.util.ArrayList
import java.util.Locale

class Util {

    companion object {
        private const val TAG = "Util"
        // Preferences key and gson instance for user stations persistence
        private const val PREFS_NAME = "user_radio_prefs"
        private const val KEY_USER_STATIONS = "user_stations_json"
        private val gson = Gson()

        fun getAllAudioFromDevice(context: Context): List<Song> {
            val tempAudioList: MutableList<Song> = ArrayList()
            val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            //String path = "/storage/emulated/0/Music/";
            Environment.getExternalStorageDirectory().toString() + "/Music/*"
            // what data to grab
            val projection = arrayOf(
                MediaStore.Audio.Media._ID, // Use stable MediaStore ID
                MediaStore.Audio.AudioColumns.DATA,
                MediaStore.Audio.AudioColumns.TITLE,
                MediaStore.Audio.ArtistColumns.ARTIST,
                MediaStore.Audio.AudioColumns.TRACK,
                MediaStore.Audio.AlbumColumns.ALBUM,
                MediaStore.Audio.AudioColumns.DURATION,
                MediaStore.Audio.Media.YEAR
            )
            // check if it is a song
            val where = MediaStore.Audio.Media.IS_MUSIC + "=1"
            val c = context.contentResolver.query(uri, projection, where, null, "title")
            if (c != null) {
                while (c.moveToNext()) {
                    val mediaStoreId = c.getInt(0) // Get MediaStore ID (stable across scans)
                    val tempPath = c.getString(1)
                    val path = tempPath.toUri()
                    // Skip entries without a valid path to avoid passing empty strings to MediaMetadataRetriever
                    if (path.toString().isBlank()) continue
                    val title = c.getString(2) ?: "Unknown"
                    val artist = c.getString(3) ?: "Unknown"
                    // TRACK may be 0 if unknown — treat <=0 as null
                    val rawTrack = try { c.getInt(4) } catch (_: Exception) { 0 }
                    val trackNullable: Int? = if (rawTrack <= 0) null else rawTrack
                    val album = c.getString(5) ?: "Unknown"
                    val duration = c.getDouble(6)
                    val year = c.getInt(7)
                    val song = Song(mediaStoreId, trackNullable, title, artist, duration, path.toString(), album, year)
                    tempAudioList.add(song)

                    //val msg = "Album id: ${song.id} | Title: ${song.title} | Artist: ${song.artist} | Album: ${song.album ?: "-"} | Year: ${song.year ?: "-"} | Path: ${song.path} | Duration: ${Util.converter(song.duration)} | rawTrack: $rawTrack | track: ${trackNullable ?: "-"}"
                    // Keep the existing formatted table row log for compatibility and also log the raw msg with rawTrack
                    //Log.i("data", formatSongRow(song))
                    //Log.i("data", msg)
                }
                c.close()
            }
            return tempAudioList
        }

        private fun padOrTruncate(s: String?, width: Int): String {
            val str = s ?: "Unknown"
            return if (str.length <= width) str.padEnd(width) else str.take(width - 3) + "..."
        }

        // Returns the first quoted substring (double or single quotes) if present, e.g. "Z103.5 \"CIDC-FM\" Live" -> CIDC-FM, otherwise the trimmed original.
        fun extractQuotedOrOriginal(s: String?): String {
             if (s.isNullOrBlank()) return ""
             val regex = Regex("\"([^\"]+)\"|'([^']+)'")
             val match = regex.find(s)
             return if (match != null) {
                 (match.groups[1]?.value ?: match.groups[2]?.value ?: "").trim()
             } else {
                 s.trim()
             }
         }

        // Prefers a quoted substring (extractQuotedOrOriginal), falls back to the raw name trimmed.
        fun formatStation(st: RadioStation?): String {
            if (st == null) return ""
            val raw = st.name ?: ""
            val extracted = extractQuotedOrOriginal(raw).ifBlank { raw }
            return extracted.trim()
        }

        fun formatSongTableHeader(): String {
            // Columns: ID, Title, Artist, Album, Track, Year, Path, Duration
            return String.format(
                Locale.US, "%-10s %-30s %-20s %-20s %-6s %-6s %-40s %8s",
                "ID", "Title", "Artist", "Album", "Track", "Year", "Path", "Duration")
        }

        fun formatSongRow(song: Song, rawTrack: Int? = null): String {
            val id = song.id.toString()
            val title = padOrTruncate(song.title.trim(), 30)
            val artist = padOrTruncate(song.artist.trim(), 20)
            val album = padOrTruncate(song.album?.trim(), 20)
            // Prefer the raw track value when available; otherwise use the normalized song.track
            val trackStr = when {
                rawTrack != null && rawTrack > 0 -> rawTrack.toString()
                song.track != null -> song.track.toString()
                else -> "-"
            }
            val year = song.year?.toString() ?: "-"
            val path = padOrTruncate(song.path, 40)
            // Use human-friendly duration (m:ss) and truncate if needed
            val durationStr = padOrTruncate(Util.converter(song.duration), 8)
            return String.format(Locale.US, "%-10s %-30s %-20s %-20s %-6s %-6s %-40s %8s", id, title, artist, album, trackStr, year, path, durationStr)
        }

        fun converter(time: Double): String {
            var elapsedTime: String?
            val minutes = (time / 1000 / 60).toInt()
            val seconds = (time / 1000 % 60).toInt()
            elapsedTime = "$minutes:"
            if (seconds < 10) elapsedTime += "0"
            elapsedTime += seconds
            return elapsedTime
        }

        suspend fun getAlbumArtAsync(context: Context, uri: String?): ImageBitmap? {
            return withContext(Dispatchers.IO) {
                try {
                    getAlbumArt(context, uri)
                } catch (_: Throwable) {
                    null
                }
            }
        }

        fun getAlbumArt(context: Context, uri: String?): ImageBitmap? {
            if (uri.isNullOrBlank()) return null

            val retriever = MediaMetadataRetriever()
            return try {
                // Try to set data source - handle both content:// URIs and file paths
                val parsedUri = Uri.parse(uri)
                when {
                    parsedUri.scheme != null -> retriever.setDataSource(context, parsedUri)
                    File(uri).exists() -> retriever.setDataSource(uri)
                    else -> return null
                }

                // Extract and decode embedded picture
                retriever.embeddedPicture?.let { art ->
                    BitmapFactory.decodeByteArray(art, 0, art.size)?.asImageBitmap()
                }
            } catch (e: Exception) {
                Log.w(TAG, "getAlbumArt failed for uri=$uri: ${e.message}")
                null
            } finally {
                try { retriever.release() } catch (_: Exception) {}
            }
        }

        // Fetches album artwork URL from the web via iTunes Search API.
        suspend fun getAlbumArtWebUrl(song: Song?): String? {
            if (song == null || song.title.isBlank() || song.artist.isBlank()) return null

            return withContext(Dispatchers.IO) {
                try {
                    val artist = URLEncoder.encode(song.artist.trim(), "UTF-8")
                    val album = URLEncoder.encode(song.album?.trim() ?: song.title.trim(), "UTF-8")

                    // Use iTunes Search API (free, no auth required)
                    val apiUrl = "https://itunes.apple.com/search?term=$artist+$album&media=music&entity=album&limit=1"
                    Log.d(TAG, "Fetching album art from web: $apiUrl")

                    val url = URL(apiUrl)
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.connectTimeout = 10000
                    conn.readTimeout = 10000
                    conn.setRequestProperty("User-Agent", "MusicPlayer/1.0")

                    return@withContext if (conn.responseCode == 200) {
                        val response = conn.inputStream.bufferedReader().use { it.readText() }
                        val jsonObj = JSONObject(response)
                        val results = jsonObj.optJSONArray("results")

                        if (results != null && results.length() > 0) {
                            val album = results.getJSONObject(0)
                            val artworkUrl = album.optString("artworkUrl600", "")
                                .ifBlank { album.optString("artworkUrl100", "") }

                            if (artworkUrl.isNotBlank()) {
                                Log.d(TAG, "✓ Found album art from web: ${artworkUrl.take(80)}")
                                artworkUrl
                            } else {
                                Log.d(TAG, "No artwork found in iTunes API response")
                                null
                            }
                        } else {
                            Log.d(TAG, "No results from iTunes API for: ${song.artist} - ${song.album}")
                            null
                        }
                    } else {
                        Log.w(TAG, "iTunes API returned ${conn.responseCode}")
                        null
                    }.also { conn.disconnect() }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to fetch album art from web: ${e.message}")
                    null
                }
            }
        }

        // Downloads and decodes album artwork from a URL string.
        suspend fun loadBitmapFromUrl(url: String): ImageBitmap? {
            return withContext(Dispatchers.IO) {
                try {
                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000

                    return@withContext if (connection.responseCode == 200) {
                        val bitmap = BitmapFactory.decodeStream(connection.inputStream)
                        bitmap?.asImageBitmap()
                    } else {
                        Log.w(TAG, "Failed to load bitmap from URL: HTTP ${connection.responseCode}")
                        null
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to load bitmap from URL: ${e.message}")
                    null
                }
            }
        }

        suspend fun fetchLyricsOnline(song: Song?): String? {
            if (song == null) return null

            return withContext(Dispatchers.IO) {
                try {
                    // Validate song has artist and/or title
                    val artist = song.artist.ifBlank { "" }
                    val title = song.title.ifBlank { "" }
                    if (artist.isBlank() && title.isBlank()) return@withContext null

                    // Build API URL
                    val encArtist = URLEncoder.encode(artist, "UTF-8")
                    val encTitle = URLEncoder.encode(title, "UTF-8")
                    val urlStr = "https://api.lyrics.ovh/v1/$encArtist/$encTitle"

                    Log.d(TAG, "Fetching lyrics for '${song.title}' by '${song.artist}'")

                    // Make HTTP request
                    val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 6000
                        readTimeout = 6000
                    }

                    try {
                        val code = conn.responseCode
                        val text = conn.inputStream.bufferedReader().use { it.readText() }

                        if (code !in 200..299) {
                            Log.w(TAG, "Lyrics API returned code $code for '${song.title}'")
                            return@withContext null
                        }

                        // Parse JSON response
                        val lyrics = JSONObject(text).optString("lyrics", "").trim()

                        if (lyrics.isNotBlank()) {
                            Log.d(TAG, "Found lyrics (${lyrics.length} chars) for '${song.title}'")
                            lyrics
                        } else {
                            Log.d(TAG, "No lyrics found for '${song.title}'")
                            null
                        }
                    } finally {
                        conn.disconnect()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to fetch lyrics for '${song.title}': ${e.message}")
                    null
                }
            }
        }

        // Tests a radio station URL's reachability/format, returning a diagnostic string.
        suspend fun testRadioUrl(url: String): String {
            return withContext(Dispatchers.IO) {
                try {
                    if (url.isBlank()) return@withContext "❌ URL is blank"

                    val result = StringBuilder()
                    result.appendLine("🔍 Testing URL: $url")
                    result.appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                    // Parse URL
                    val parsedUrl = try {
                        URL(url)
                    } catch (e: Exception) {
                        result.appendLine("❌ INVALID URL FORMAT")
                        result.appendLine("Error: ${e.message}")
                        return@withContext result.toString()
                    }

                    result.appendLine("Protocol: ${parsedUrl.protocol}")
                    result.appendLine("Host: ${parsedUrl.host}")
                    result.appendLine("Port: ${if (parsedUrl.port == -1) "default" else parsedUrl.port}")
                    result.appendLine("Path: ${parsedUrl.path.ifBlank { "/" }}")
                    result.appendLine("")

                    // Test connection
                    val conn = parsedUrl.openConnection() as? HttpURLConnection
                    if (conn == null) {
                        result.appendLine("❌ Could not create HTTP connection")
                        return@withContext result.toString()
                    }

                    conn.apply {
                        requestMethod = "HEAD"
                        connectTimeout = 10000
                        readTimeout = 10000
                        instanceFollowRedirects = true
                        setRequestProperty("User-Agent", "MusicPlayer/1.0")
                        setRequestProperty("Icy-MetaData", "1")

                        try {
                            connect()
                            val code = responseCode

                            result.appendLine("HTTP Response Code: $code")

                            when (code) {
                                200 -> result.appendLine("✓ SUCCESS - Server responded OK")
                                301, 302, 303, 307, 308 -> {
                                    result.appendLine("⚠ REDIRECT - URL redirects to another location")
                                    val location = getHeaderField("Location")
                                    result.appendLine("Redirect to: $location")
                                }

                                403 -> result.appendLine("❌ FORBIDDEN - Server denies access")
                                404 -> result.appendLine("❌ NOT FOUND - Stream doesn't exist")
                                500, 502, 503 -> result.appendLine("❌ SERVER ERROR - Server is down/misconfigured")
                                else -> result.appendLine("⚠ Unexpected response code")
                            }

                            result.appendLine("")
                            result.appendLine("Headers:")
                            result.appendLine("  Content-Type: ${contentType ?: "none"}")
                            result.appendLine("  Content-Length: ${if (contentLength > 0) contentLength else "unknown"}")
                            result.appendLine("  ICY-Name: ${getHeaderField("icy-name") ?: "none"}")
                            result.appendLine("  ICY-MetaInt: ${getHeaderField("icy-metaint") ?: "none"}")
                            result.appendLine("  ICY-BR: ${getHeaderField("icy-br") ?: "none"}")
                            result.appendLine("  Server: ${getHeaderField("Server") ?: "unknown"}")

                            result.appendLine("")
                            result.appendLine("Analysis:")

                            // Analyze content type
                            val ct = contentType?.lowercase() ?: ""
                            when {
                                ct.contains("audio/") -> result.appendLine("✓ Valid audio stream")
                                ct.contains("application/vnd.apple.mpegurl") ||
                                        ct.contains("application/x-mpegurl") -> result.appendLine("✓ HLS playlist (M3U8)")

                                ct.contains("text/html") -> {
                                    result.appendLine("❌ PROBLEM: Server returned HTML instead of audio")
                                    result.appendLine("   This usually means:")
                                    result.appendLine("   • URL requires authentication")
                                    result.appendLine("   • Session-based URL has expired")
                                    result.appendLine("   • URL points to a webpage, not a stream")
                                }

                                ct.contains("text/") -> result.appendLine("⚠ WARNING: Returned text content, not audio")
                                else -> result.appendLine("⚠ Unknown content type - may not be playable")
                            }

                            // Check for session-based URLs
                            if (url.contains("listeningSessionID", ignoreCase = true) ||
                                url.contains("sessionId", ignoreCase = true) ||
                                url.contains("token", ignoreCase = true)
                            ) {
                                result.appendLine("⚠ WARNING: URL appears to be session-based")
                                result.appendLine("   Session-based URLs expire and need to be refreshed")
                            }

                        } catch (e: ConnectException) {
                            result.appendLine("❌ CONNECTION FAILED")
                            result.appendLine("Error: ${e.message}")
                            result.appendLine("Server may be offline or blocking connections")
                        } catch (e: SocketTimeoutException) {
                            result.appendLine("❌ TIMEOUT")
                            result.appendLine("Server took too long to respond")
                        } catch (e: Exception) {
                            result.appendLine("❌ ERROR: ${e.javaClass.simpleName}")
                            result.appendLine("Message: ${e.message}")
                        } finally {
                            disconnect()
                        }
                    }

                    result.appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    result.toString()

                } catch (e: Exception) {
                    "❌ Test failed: ${e.message}"
                }
            }
        }


        // Splits a collaboration string on ",", "feat.", "featuring", "ft.", "&", "+", "and", "with", "x" and normalizes each artist (strips "The" prefix, trims, lowercases), e.g. "Black Eyed Peas feat. Shakira" -> ["black eyed peas", "shakira"].
        private fun extractAllArtists(artist: String?): List<String> {
            if (artist.isNullOrBlank()) return emptyList()

            val bandExceptions = setOf(
                "crosby, stills & nash",
                "crosby, stills, nash & young",
                "csn",
                "csny",
                "king & queen"
            )
            val artistLower = artist.lowercase(Locale.getDefault())
            if (bandExceptions.any { artistLower.contains(it) }) {
                return listOf(artist.trim().lowercase(Locale.getDefault()))
            }

            // Split by collaboration separators (including comma and +)
            val collaborationRegex = Regex(
                "[,\\s]+(feat\\.?|featuring|ft\\.?|and|&|\\+|with|x)[,\\s]*|[,]",
                RegexOption.IGNORE_CASE
            )
            val artistNames = collaborationRegex.split(artist)

            // Normalize each extracted artist name
            val result = artistNames
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .map { name ->
                    name.removePrefix("The ")
                        .removePrefix("the ")
                        .trim()
                        .lowercase(Locale.getDefault())
                }
                .filter { it.isNotBlank() }
                .distinct() // remove duplicates
                .take(1) // Only keep the primary artist (ignore any "featuring" artists)

            Log.d(TAG, "extractAllArtists input: '$artist' -> output: $result")
            return result
        }

        // Songs sharing the same album or artist as songs[currentIndex] (same album first, then same artist or any collaborator, via extractAllArtists); suspend, runs IO work.
        suspend fun getRelatedSongs(songs: List<Song>, currentIndex: Int): List<Pair<Int, Song>> {
            return withContext(Dispatchers.IO) {
                if (currentIndex < 0 || currentIndex >= songs.size) return@withContext emptyList()
                val current = songs[currentIndex]
                val currentAlbum = current.album

                // Extract all artists from current song (including collaborators)
                val currentArtists = extractAllArtists(current.artist)

                val sameAlbumSongs = mutableListOf<Pair<Int, Song>>()
                val sameArtistSongs = mutableListOf<Pair<Int, Song>>()

                songs.forEachIndexed { idx, s ->
                    if (idx == currentIndex) return@forEachIndexed // skip current song

                    val isSameAlbum = !currentAlbum.isNullOrBlank() && s.album == currentAlbum

                    // Extract all artists from the song being compared
                    val songArtists = extractAllArtists(s.artist)

                    // Bidirectional match: either song's artists can appear in the other's.
                    val isSameArtist = currentArtists.isNotEmpty() && songArtists.isNotEmpty() &&
                        (songArtists.any { it in currentArtists } ||
                         currentArtists.any { it in songArtists })

                    when {
                        isSameAlbum -> {
                            sameAlbumSongs.add(Pair(idx, s))
                        }
                        isSameArtist -> {
                            sameArtistSongs.add(Pair(idx, s))
                        }
                    }
                }

                // Combine: same album first, then same artist
                (sameAlbumSongs + sameArtistSongs).toList()
            }
        }


        // Parses tags from Radio Browser's tags field (space or comma separated), keeping quoted segments intact (e.g. "classic rock").
        fun parseTags(raw: String?): List<String> {
            if (raw.isNullOrBlank()) return emptyList()
            // radio-browser tags are often space- or comma-separated; normalize commas to spaces then split on whitespace, keeping quoted groups intact.
            val regex = Regex("\"([^\"]+)\"|'([^']+)'|([^,\\s]+)")
            val matches = regex.findAll(raw)
            val out = matches.mapNotNull { m ->
                val g1 = m.groups[1]?.value
                val g2 = m.groups[2]?.value
                val g3 = m.groups[3]?.value
                (g1 ?: g2 ?: g3)?.trim()?.takeIf { it.isNotEmpty() }
            }.toList()
            return out
        }


        /*fun converter(time: Float): Float {
            var elapsedTime: String
            val minutes = (time / 1000 / 60).toInt()
            val seconds = (time / 1000 % 60).toInt()
            elapsedTime = "$minutes:"
            if (seconds < 10) elapsedTime += "0"
            elapsedTime += seconds
            return elapsedTime.toFloat()
        }*/

        fun converter(time: Int): String {
            var elapsedTime: String?
            val minutes = (time / 1000 / 60)
            val seconds = (time / 1000 % 60)
            elapsedTime = "$minutes:"
            if (seconds < 10) elapsedTime += "0"
            elapsedTime += seconds
            return elapsedTime
        }

        fun darkerColor(color: Color, factor: Float = 0.5f): Color {
            return Color(
                red = (color.red * factor).coerceIn(0f, 1f),
                green = (color.green * factor).coerceIn(0f, 1f),
                blue = (color.blue * factor).coerceIn(0f, 1f),
                alpha = color.alpha
            )
        }

        // Inserts an extra blank line after each of the first firstLines lines of lyrics text, preserving newline style; returns null if the input is null.
        fun addSpacingToFirstLines(lyrics: String?, firstLines: Int = 5): String? {
            if (lyrics == null) return null
            if (lyrics.isBlank()) return lyrics

            // Normalize to \n, then split.
            val normalized = lyrics.replace("\r\n", "\n").replace("\r", "\n")
            val parts = normalized.split("\n")
            if (parts.size <= 1) return lyrics

            val builder = StringBuilder()
            for ((i, line) in parts.withIndex()) {
                builder.append(line)
                // Append original single newline after each line except last
                if (i < parts.size - 1) builder.append('\n')

                // After the first N lines append an extra newline to create spacing
                if (i < firstLines && i < parts.size - 1) {
                    builder.append('\n')
                }
            }
            return builder.toString()
        }

        fun dim(clicked: Boolean): Color {
            return if (clicked) Color.Companion.White else Color.Companion.White.copy(alpha = 0.4f)
        }


        fun getThumbnail(context: Context, uri: Uri?): Bitmap {
            val mmr = MediaMetadataRetriever()
            val bfo = BitmapFactory.Options()
            if (uri == null) {
                return BitmapFactory.decodeResource(context.resources, R.drawable.ic_vinyl_record)
            }
            return try {
                mmr.setDataSource(context, uri)
                val rawArt = mmr.embeddedPicture
                if (rawArt != null && rawArt.isNotEmpty()) {
                    BitmapFactory.decodeByteArray(rawArt, 0, rawArt.size, bfo)
                } else {
                    BitmapFactory.decodeResource(context.resources, R.drawable.ic_vinyl_record)
                }
            } catch (e: Exception) {
                Log.w(TAG, "getThumbnail: setDataSource failed for uri=$uri", e)
                BitmapFactory.decodeResource(context.resources, R.drawable.ic_vinyl_record)
            } finally {
                try { mmr.release() } catch (_: Exception) {}
            }
        }

        fun shortToast(context: Context, text:String){
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        }

        //fun showSnack(view: View, text:String){
        //    Snackbar.make(view,text,Snackbar.LENGTH_LONG).show()
        //}

        // Searches radio stations by name via the Radio Browser API, with optional country/state filters; returns an empty list on error.
        suspend fun fetchRadioStations(query: String = "", limit: Int = 50, country: String? = null, state: String? = null): List<RadioStation> {
            return try {
                // Log the exact query parameters used to call the Radio Browser API
                try {
                    Log.i(TAG, "fetchRadioStations: name='${query}' limit=${limit} country=${country ?: "<any>"} state=${state ?: "<any>"}")
                } catch (_: Throwable) {}
                val api = RadioApiService.create()
                api.searchStations(query, limit, country, state)
            } catch (e: Exception) {
                //Log.w(TAG, "fetchRadioStations failed for query='$query' limit=$limit country=$country state=$state", e)
                emptyList()
            }
        }

        // New helper: search stations by geographic coordinates using the RadioApiService
        suspend fun fetchRadioStationsNearby(lat: Double, lng: Double, limit: Int = 50, distanceKm: Int? = null): List<RadioStation> {
            return try {
                try { Log.d(TAG, "fetchRadioStationsNearby: lat=$lat lng=$lng limit=$limit distanceKm=${distanceKm ?: "<any>"}") } catch (_: Throwable) {}
                val api = RadioApiService.create()
                api.searchStationsNearby(lat, lng, limit, distanceKm)
            } catch (_: Exception) {
                //Log.w(TAG, "fetchRadioStationsNearby failed for lat=$lat lng=$lng limit=$limit distanceKm=$distanceKm", e)
                emptyList()
            }
        }

        // Identifies real FM/AM broadcast stations (excludes internet-only streams) by call letters (CHUM, KISS, CIDC), FM/AM frequency in name (e.g. "104.5"), or known broadcast networks.
        private fun isRealBroadcastStation(station: RadioStation): Boolean {
            val name = station.name?.uppercase() ?: return false
            val tags = station.tags?.uppercase() ?: ""

            // Exclude known internet-only services (STRICT exclusion)
            val excludedPatterns = listOf(
                "TUNEIN", "IHEARTRADIO", "SPOTIFY", "APPLE MUSIC",
                "YOUTUBE", "SOUNDCLOUD", "TIDAL", "AMAZON MUSIC",
                "INTERNET ONLY", "WEB ONLY", "PANDORA", "SLACKER",
                "JANGO", "LAST.FM", "SHOUTCAST", "WEBRADIO", "INTERNET RADIO"
            )

            // If it's explicitly an excluded service, reject it
            if (excludedPatterns.any { pattern -> name.contains(pattern) || tags.contains(pattern) }) {
                Log.d(TAG, "Excluded station: $name (streaming service)")
                return false
            }

            // Real broadcast stations have FM/AM designation OR frequencies
            val hasFMorAM = name.contains("FM") || name.contains("AM") ||
                           tags.contains("FM") || tags.contains("AM")

            val hasFrequency = name.contains(Regex("\\d+\\.\\d+")) // 104.5 format

            // Has call letters (2-4 letter combination)
            val hasCallLetters = name.contains(Regex("[A-Z]{2,4}")) &&
                                !name.contains(Regex("[A-Z]{5,}")) // avoid all-caps phrases

            val isReal = hasFMorAM || hasFrequency || hasCallLetters

            if (isReal) {
                Log.d(TAG, "Accepted station: $name (FM=$hasFMorAM, Freq=$hasFrequency, Letters=$hasCallLetters)")
            }

            return isReal
        }

        // Fetches stations near the GTA, preferring well-known music stations (CHUM 104.5, KISS 92.5, VIRGIN 99.9, Z103.5), filtered to real FM/AM broadcast stations only.
        suspend fun fetchStationsNearGTA(limit: Int = 50): List<RadioStation> {
            // Simplified: prefer the geo-nearby endpoint, fall back to a general search -- avoids the brittle heavy heuristic filtering/deduping this used to do.
            val GTA_LAT = 43.6532
            val GTA_LON = -79.3832
            return try {
                Log.d(TAG, "fetchStationsNearGTA: Fetching stations near GTA (lat=$GTA_LAT, lng=$GTA_LON)")
                val nearby = fetchRadioStationsNearby(GTA_LAT, GTA_LON, limit * 3) // fetch more to account for filtering
                    .also { Log.d(TAG, "fetchStationsNearGTA: Got ${it.size} stations from nearby, filtering...") }
                    .filter { isRealBroadcastStation(it) }
                    .also { Log.d(TAG, "fetchStationsNearGTA: After filtering, ${it.size} real broadcast stations") }
                    .take(limit)
                if (nearby.isNotEmpty()) {
                    Log.d(TAG, "fetchStationsNearGTA: Returning ${nearby.size} nearby stations")
                    return nearby
                }

                // fallback to a province-wide / simple search
                Log.d(TAG, "fetchStationsNearGTA: No nearby stations, trying province-wide search")
                val province = fetchRadioStations(query = "", limit = limit * 3, country = "Canada", state = "Ontario")
                    .also { Log.d(TAG, "fetchStationsNearGTA: Got ${it.size} stations from province search") }
                    .filter { isRealBroadcastStation(it) }
                    .also { Log.d(TAG, "fetchStationsNearGTA: After filtering, ${it.size} real broadcast stations") }
                    .take(limit)
                Log.d(TAG, "fetchStationsNearGTA: Returning ${province.size} province stations")
                province
            } catch (e: Exception) {
                try { Log.w(TAG, "fetchStationsNearGTA simplified failed", e) } catch (_: Throwable) {}
                emptyList()
            }
        }

        // Best image URL for a RadioStation, in order: station.favicon (https preferred), https://<host>/favicon.ico from station.url, then Google's favicon helper; empty string if no candidate (caller should use a local placeholder).
        fun getStationImageUrl(st: RadioStation?): String {
            if (st == null) return ""

            fun toHttps(u: String?): String? {
                val s = u?.trim() ?: return null
                if (s.startsWith("//")) return "https:$s"
                if (s.startsWith("http://")) return s.replaceFirst("http://", "https://")
                if (s.startsWith("https://")) return s
                return null
            }

            // Prefer explicit https favicon if available
            val fav = toHttps(st.favicon)
            if (!fav.isNullOrBlank()) return fav

            // Fallback: use host's favicon via Google's s2 helper when we can derive a host
            val src = st.url ?: st.favicon
            val host = try {
                if (src.isNullOrBlank()) null else URL(if (src.contains("://")) src else "https://$src").host?.lowercase()?.removePrefix("www.")
            } catch (_: Exception) { null }
            if (!host.isNullOrBlank()) return "https://www.google.com/s2/favicons?sz=256&domain_url=$host"

            return ""
        }

        /** Return the saved user stations (empty list when none or on error). */
        fun getUserStations(context: Context): List<RadioStation> {
            // Per user request: always use hard-coded defaults (Z103.5, Virgin 99.9, etc.) rather than any saved/dynamic list.
            return getDefaultUserStations(context)
         }

        /** Try to read a stations JSON array from the app assets folder. */
        fun getUserStationsFromAssets(context: Context, assetFileName: String): List<RadioStation> {
            return try {
                val am = context.assets
                val stream = am.open(assetFileName)
                val json = stream.bufferedReader().use { it.readText() }
                val type = object : TypeToken<List<RadioStation>>() {}.type
                val parsed: List<RadioStation>? = try { gson.fromJson(json, type) as? List<RadioStation> } catch (_: Exception) { null }
                parsed ?: emptyList()
            } catch (e: Exception) {
                // asset missing or parse error -> return empty so callers can fallback
                Log.d(TAG, "getUserStationsFromAssets: failed to read asset $assetFileName: ${e.message}")
                emptyList()
            }
        }

        // Loads radio stations from res/raw/radio_stations.json.
        fun getDefaultUserStations(context: Context): List<RadioStation> {
            return try {
                val inputStream = context.resources.openRawResource(R.raw.radio_stations)
                val jsonString = inputStream.bufferedReader().use { it.readText() }

                val jsonObject = JSONObject(jsonString)
                val stationsArray = jsonObject.getJSONArray("stations")

                val stations = mutableListOf<RadioStation>()
                for (i in 0 until stationsArray.length()) {
                    val stationJson = stationsArray.getJSONObject(i)
                    stations.add(
                        RadioStation(
                            stationuuid = stationJson.optString("stationuuid"),
                            name = stationJson.optString("name"),
                            url = stationJson.optString("url"),
                            favicon = stationJson.optString("favicon"),
                            country = stationJson.optString("country"),
                            tags = stationJson.optString("tags"),
                            bitrate = stationJson.optInt("bitrate", 0)
                        )
                    )
                }

                Log.d("Util", "Loaded ${stations.size} radio stations from JSON")
                stations
            } catch (e: Exception) {
                Log.e("Util", "Failed to load radio stations from JSON: ${e.message}", e)
                // Return fallback stations if JSON loading fails
                getFallbackStations()
            }
        }

        // Fallback stations in case JSON loading fails.
        private fun getFallbackStations(): List<RadioStation> {
            return listOf(
                RadioStation(
                    stationuuid = "cidc-z103",
                    name = "Z103.5",
                    url = "https://21363.live.streamtheworld.com/CIDC_FM.mp3",
                    favicon = "https://cdn-profiles.tunein.com/s12366/images/logod.png?t=637554031500000000",
                    country = "Canada",
                    tags = "Top40, Euro, Pop, Hip-Hop, Reggae",
                    bitrate = 128
                ),
                RadioStation(
                    stationuuid = "kiss-925",
                    name = "KISS 92.5",
                    url = "https://21323.live.streamtheworld.com/CKIS_FM.mp3",
                    favicon = "https://cdn-radiotime-logos.tunein.com/s31199d.png",
                    country = "Canada",
                    tags = "Top 40, Pop, Hip-Hop, R&B, Dance",
                    bitrate = 0
                )
            )
            }
        }

        // Simple artist image lookup; in a real app, you’d query an API.
        private val artistImageMap: Map<String, String> = mapOf(
            "Artist One" to "https://upload.wikimedia.org/wikipedia/commons/4/4f/Blank_profile.png",
            "Artist Two" to "https://upload.wikimedia.org/wikipedia/commons/4/4f/Blank_profile.png",
            "Artist Three" to "https://upload.wikimedia.org/wikipedia/commons/4/4f/Blank_profile.png"
        )

        fun getArtistImageUrl(name: String): String? {
            val key = name.trim()
            return artistImageMap[key]
        }

}


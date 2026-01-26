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
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLEncoder
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
                MediaStore.Audio.AudioColumns.DATA,
                MediaStore.Audio.AudioColumns.TITLE,
                MediaStore.Audio.ArtistColumns.ARTIST,
                MediaStore.Audio.AlbumColumns.ALBUM,
                MediaStore.Audio.AudioColumns.DURATION
            )
            // check if it is a song
            val where = MediaStore.Audio.Media.IS_MUSIC + "=1"
            val c = context.contentResolver.query(uri, projection, where, null, "title")
            var count = 0
            if (c != null) {
                while (c.moveToNext()) {
                    val tempPath = c.getString(0)
                    val path = tempPath.toUri()
                    // Skip entries without a valid path to avoid passing empty strings to MediaMetadataRetriever
                    if (path.toString().isBlank()) continue
                    val title = c.getString(1) ?: "Unknown"
                    val artist = c.getString(2) ?: "Unknown"
                    val album = c.getString(3) ?: "Unknown"
                    val duration = c.getDouble(4)
                    val song = Song(count, title, artist, duration, path.toString(), album)
                    tempAudioList.add(song)
                    count++

                    val msg =
                        "Album id: ${song.id} | Title: ${song.title} | Artist: ${song.artist} | Path: ${song.path} | Duration: ${
                            Util.converter(song.duration)
                        }"
                    //Log.i("data", formatSongRow(song))
                }
                c.close()
            }
            return tempAudioList
        }

        private fun padOrTruncate(s: String?, width: Int): String {
            val str = s ?: "Unknown"
            return if (str.length <= width) str.padEnd(width) else str.take(width - 3) + "..."
        }

        // Return the first quoted substring (double or single quotes) if present, otherwise return trimmed original.
        // Examples:
        //  - "Z103.5 \"CIDC-FM\" Live" -> CIDC-FM
        //  - "Some Station 'Nickname' Extra" -> Nickname
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

        /**
         * Format a station name for storage/display.
         * Rules: prefer quoted substring (via extractQuotedOrOriginal), fallback to the raw name trimmed.
         */
        fun formatStation(st: RadioStation?): String {
            if (st == null) return ""
            val raw = st.name ?: ""
            val extracted = extractQuotedOrOriginal(raw).ifBlank { raw }
            return extracted.trim()
        }

        fun formatSongTableHeader(): String {
            // %-4s = left-aligned width 4, %-30s = left-aligned width 30, etc.
            // Columns: ID, Title, Artist, Album, Path, Duration
            return String.Companion.format(
                Locale.US, "%-4s %-30s %-20s %-20s %-40s %8s",
                "ID", "Title", "Artist", "Album", "Path", "Duration")
        }

        fun formatSongRow(song: Song): String {
            val id = song.id.toString()
            val title = padOrTruncate(song.title.trim(), 30)
            val artist = padOrTruncate(song.artist.trim(), 20)
            // Normalize album and path presentation
            val album = padOrTruncate(song.album?.trim(), 40)
            val duration = padOrTruncate(song.duration.toString(), 10)
            val path = padOrTruncate(song.path, 70)
            return String.Companion.format(Locale.US, "%-4s %-25s %-15s %-40s %-40s %8s", id, title, artist, album, path, duration)
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

        /**
         * Test a radio station URL to see if it's reachable and what format it returns.
         * Returns a diagnostic string with details about the URL.
         * This is a suspend function for background testing.
         */
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

        /**
         * Return a list of related songs that share the same album as the song at [currentIndex].
         * The result is a list of Pair(index, Song). Fast-path uses Song.album when available
         * and falls back to extracting album metadata from the file/uri when needed.
         * This is a suspend function and should be called from a coroutine (it runs IO work).
         */
        suspend fun getRelatedSongs(songs: List<Song>, currentIndex: Int): List<Pair<Int, Song>> {
            return withContext(Dispatchers.IO) {
                if (currentIndex < 0 || currentIndex >= songs.size) return@withContext emptyList()
                val current = songs[currentIndex]
                val currentAlbum = if (!current.album.isNullOrBlank()) current.album else null
                if (currentAlbum.isNullOrBlank()) return@withContext emptyList()

                // Treat plain "Single"/"Singles" albums as not eligible for related songs.
                val currentNorm = currentAlbum.trim().lowercase(Locale.getDefault())
                if (currentNorm == "single" || currentNorm == "singles") return@withContext emptyList()

                val related = ArrayList<Pair<Int, Song>>()
                for ((idx, s) in songs.withIndex()) {
                    if (idx == currentIndex) continue
                    val album = if (!s.album.isNullOrBlank()) s.album else null
                    if (!album.isNullOrBlank() && album.trim() == currentAlbum.trim()) {
                        related.add(Pair(idx, s))
                    }
                }
                related
            }
        }


        /**
         * Parse tags from Radio Browser's tags field (space or comma separated).
         * Keep quoted segments intact if provided (e.g. "classic rock").
         */
        fun parseTags(raw: String?): List<String> {
            if (raw.isNullOrBlank()) return emptyList()
            // radio-browser tags often are space-separated or comma-separated
            // Normalize commas to spaces, then split on whitespace, but keep quoted groups
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

        /**
         * Insert an extra blank line after each of the first [firstLines] lines in the lyrics text.
         * Preserves existing newline style and is robust to shorter inputs.
         * Returns null if the input is null.
         */
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

        // New radio API helpers
        /**
         * Suspend function that searches radio stations by name using the Radio Browser API.
         * Returns an empty list on error.
         * New: optional country and state filters are supported.
         */
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

        /**
         * Convenience helper: fetch stations near the Greater Toronto Area (GTA).
         * Focus on music stations and preferentially return well-known music stations
         * such as CHUM 104.5, KISS 92.5, VIRGIN 99.9 and Z103.5.
         */
        suspend fun fetchStationsNearGTA(limit: Int = 50): List<RadioStation> {
            // Simplified version: prefer geo-nearby endpoint and fall back to a general search.
            // This avoids heavy heuristic filtering and complex deduping logic which was brittle.
            val GTA_LAT = 43.6532
            val GTA_LON = -79.3832
            return try {
                val nearby = fetchRadioStationsNearby(GTA_LAT, GTA_LON, limit)
                if (nearby.isNotEmpty()) return nearby.take(limit)
                // fallback to a province-wide / simple search
                val province = fetchRadioStations(query = "", limit = limit, country = "Canada", state = "Ontario")
                province.take(limit)
            } catch (e: Exception) {
                try { Log.w(TAG, "fetchStationsNearGTA simplified failed", e) } catch (_: Throwable) {}
                emptyList()
            }
        }

        /**
         * Choose the best image URL for a RadioStation.
         * Order of preference:
         * 1) station.favicon (https preferred)
         * 2) construct https://<host>/favicon.ico from station.url
         * 3) Google's favicon helper: https://www.google.com/s2/favicons?sz=64&domain_url=<host>
         * Returns empty string when no candidate is available (caller should use local placeholder).
         */
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
            // Per user request: always use hard-coded defaults.
            // This makes the app reliably return the built-in station list (Z103.5, Virgin 99.9, etc.).
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

        /**
         * Load radio stations from JSON file in res/raw/radio_stations.json
         */
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

        /**
         * Fallback stations in case JSON loading fails
         */
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
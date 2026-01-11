package com.example.musicplayer

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
import com.example.musicplayer.model.Song
import com.example.musicplayer.model.RadioStation
import java.io.File
import java.util.ArrayList
import java.util.Locale
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.net.URL
import java.net.URLEncoder
import java.net.HttpURLConnection
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
            return String.format(Locale.US, "%-4s %-30s %-20s %-20s %-40s %8s",
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
            return String.format(Locale.US, "%-4s %-25s %-15s %-40s %-40s %8s", id, title, artist, album, path, duration)
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
                // Parse the URI and decide whether to call setDataSource with a Uri (content://) or a file path
                val parsedUri = try { Uri.parse(uri) } catch (_: Exception) { null }
                val hasScheme = parsedUri?.scheme?.isNotBlank() == true

                if (hasScheme) {
                    // Common case: content:// uri from MediaStore
                    try {
                        retriever.setDataSource(context, parsedUri)
                    } catch (e: Exception) {
                        Log.w(TAG, "getAlbumArt: setDataSource(context, uri) failed for parsedUri=$parsedUri", e)
                        return null
                    }
                } else {
                    // Treat as a file path. Ensure the file exists before calling setDataSource.
                    val file = File(uri)
                    if (file.exists()) {
                        try {
                            retriever.setDataSource(file.absolutePath)
                        } catch (e: Exception) {
                            Log.w(TAG, "getAlbumArt: setDataSource(file) failed for file=${file.absolutePath}", e)
                            return null
                        }
                    } else {
                        // Path doesn't exist on disk; avoid calling setDataSource with a non-existent path
                        Log.w(TAG, "getAlbumArt: file does not exist: $uri")
                        return null
                    }
                }

                val art = retriever.embeddedPicture
                if (art == null || art.isEmpty()) {
                    null
                } else {
                    val bmp = BitmapFactory.decodeByteArray(art, 0, art.size)
                    bmp?.asImageBitmap()
                }
            } catch (e: Exception) {
                Log.w(TAG, "getAlbumArt failed for uri=$uri", e)
                null
            } finally {
                try { retriever.release() } catch (_: Exception) {}
            }
        }

        suspend fun fetchLyricsOnline(song: Song?): String? {
            if (song == null) return null
            return withContext(Dispatchers.IO) {
                try {
                    val artist = song.artist.ifBlank { "" }
                    val title = song.title.ifBlank { "" }
                    if (artist.isBlank() && title.isBlank()) {
                        try { Log.d(TAG, "fetchLyricsOnline: skip, empty artist/title for id=${song.id}") } catch (_: Throwable) {}
                        return@withContext null
                    }

                    val encArtist = URLEncoder.encode(artist, "UTF-8")
                    val encTitle = URLEncoder.encode(title, "UTF-8")
                    val urlStr = "https://api.lyrics.ovh/v1/$encArtist/$encTitle"
                    try { Log.d(TAG, "fetchLyricsOnline: request url=$urlStr title='${song.title}' artist='${song.artist}'") } catch (_: Throwable) {}
                    val url = URL(urlStr)
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 6000
                        readTimeout = 6000
                        doInput = true
                    }

                    try {
                        val code = conn.responseCode
                        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                        val text = stream.bufferedReader().use { it.readText() }
                        try { Log.d(TAG, "fetchLyricsOnline: http=$code responseLen=${text.length}") } catch (_: Throwable) {}
                        if (code !in 200..299) {
                            // Log a short snippet to help diagnose failures
                            val snippet = text.take(200).replace('\n', ' ')
                            try { Log.w(TAG, "fetchLyricsOnline: non-2xx code=$code body='${snippet}'") } catch (_: Throwable) {}
                        }
                        val json = JSONObject(text)
                        val lyrics = json.optString("lyrics", "").trim()
                        if (lyrics.isBlank()) {
                            try { Log.d(TAG, "fetchLyricsOnline: no lyrics found for '${song.title}'") } catch (_: Throwable) {}
                            null
                        } else {
                            try { Log.d(TAG, "fetchLyricsOnline: lyrics length=${lyrics.length} for '${song.title}'") } catch (_: Throwable) {}
                            lyrics
                        }
                    } finally {
                        conn.disconnect()
                    }
                } catch (e: Throwable) {
                    try { Log.w(TAG, "fetchLyricsOnline: exception for '${song.title}': ${e.message}", e) } catch (_: Throwable) {}
                    null
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

        fun dim(clicked: Boolean): Color{
            return if (clicked) Color.White else Color.White.copy(alpha = 0.4f)
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
                val api = com.example.musicplayer.radio.RadioApiService.create()
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
                val api = com.example.musicplayer.radio.RadioApiService.create()
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
                if (src.isNullOrBlank()) null else java.net.URL(if (src.contains("://")) src else "https://$src").host?.lowercase()?.removePrefix("www.")
            } catch (_: Exception) { null }
            if (!host.isNullOrBlank()) return "https://www.google.com/s2/favicons?sz=256&domain_url=$host"

            return ""
        }

        /** Return the saved user stations (empty list when none or on error). */
        fun getUserStations(context: Context): List<RadioStation> {
            // Per user request: always use hard-coded defaults.
            // This makes the app reliably return the built-in station list (Z103.5, Virgin 99.9, etc.).
            return getDefaultUserStations()
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

        /** Return a small, safe default list of stations embedded in the app.
         *  These should be editable by providing an assets/user_stations.json instead.
         */
        fun getDefaultUserStations(): List<RadioStation> {
             return listOf(
                 RadioStation(
                     stationuuid = "cidc-z103",
                     name = "Z103.5",
                     url = "https://buf-streamb1-ais-relay1.streamb.live/SB00222/playlist.m3u8?listeningSessionID=6922047d0079a643_3999634_eTaWePVI_YnVmLXN0cmVhbWIxLWFpcy1yZWxheTEuc3RyZWFtYi5saXZlOjgwMDA!_0000001rk4j&downloadSessionID=0&args=app_04&clientType=web&host=webapp.CA&modTime=1767836142893&profileid=11815995833&terminalid=163&territory=CA&us_privacy=1-N-&callLetters=CIDC-FM&devicename=web-desktop&stationid=7757&dist=iheart&subscription_type=free",
                     favicon = "https://cdn-profiles.tunein.com/s12366/images/logod.png?t=637554031500000000",
                     country = "Canada",
                     tags = "Top40, Euro, Pop, Hip-Hop, Reggae",
                     bitrate = 128
                 ),
                 RadioStation(
                     stationuuid = "virgin-999",
                     name = "Virgin 99.9",
                     url = "https://18153.live.streamtheworld.com/CKFMFMAAC_SC",
                     favicon = "https://archive.org/services/img/ckfm_20230202",
                     country = "Canada",
                     tags = "Pop, Top40",
                     bitrate = 128
                 ),
                 RadioStation(
                     stationuuid = "kiss-925",
                     name = "KISS 92.5",
                     url = "https://radio-dai.rogersdigitalmedia.com/hls/chi/rogers/tor925.stream/48k/6shJwgOxffa-176783403-10031.aac",
                     favicon = "https://cdn-radiotime-logos.tunein.com/s31199d.png",
                     country = "Canada",
                     tags = "Top 40, Pop, Hip-Hop, R&B, Dance",
                     bitrate = 0
                 ),
                 RadioStation(
                     stationuuid = "chum-1045",
                     name = "CHUM 104.5",
                     url = "https://26293.live.streamtheworld.com/CHUMFMAAC_SC",
                     favicon = "https://cdn-profiles.tunein.com/s31180/images/logod.png?t=637400097550000000",
                     country = "Canada",
                     tags = "Classic, Rock, Pop",
                     bitrate = 0
                 ),
                 RadioStation(
                     stationuuid = "chfi-981",
                     name = "CHFI 98.1",
                     url = "https://19313.live.streamtheworld.com/CHUMFM_ADP/HLS/playlist.m3u8?clientType=web&host=webapp.CA&modTime=1767836142893&profileid=11815995833&terminalid=163&territory=CA&us_privacy=1-N-&callLetters=CHUM-FM&devicename=web-desktop&stationid=6270&dist=iheart&subscription_type=free&partnertok=eyJraWQiOiJpaGVhcnQiLCJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJ0ZCIsImNvcHBhIjowLCJwcm92aWRlcklkIjo0OCwicHJvZmlsZWlkIjoiMTE4MTU5OTU4MzMiLCJpc3MiOiJpaGVhcnQiLCJ1c19wcml2YWN5IjoiMVlZTiIsImRpc3QiOiJpaGVhcnQiLCJleHAiOjE3Njc5MTk4ODcsImlhdCI6MTc2NzgzMzQ4Nywib21pZCI6MH0.XNols_EQzgf7w0jxwCTavH6zmV-BtjM-_aGmMwK5mhs&country=CA&locale=en-CA&site-url=https%3A%2F%2Fwww.iheart.com%2Flive%2Fchum-1045-6270%2F",
                     favicon = "https://www.seekyoursounds.com/wp-content/uploads/2024/06/Seekr-RadioCover-CHFI-981-1-300x300.png",
                     country = "Canada",
                     tags = "classic, rock",
                     bitrate = 0
                 ),
                 RadioStation(
                     stationuuid = "Boom-973",
                     name = "Boom 97.3",
                     url = "https://stingray.leanstream.co/stingray/CHBMFM.stream/playlist.m3u8?dist=iheart&args=other_04&clientType=web&host=webapp.CA&modTime=1767837779840&profileid=11815995833&terminalid=163&territory=CA&us_privacy=1-N-&callLetters=CHBM-FM&devicename=web-desktop&stationid=9824&dist=iheart&subscription_type=free",
                     favicon = "https://cdn-radiotime-logos.tunein.com/s31212d.png",
                     country = "Canada",
                     tags = "70's, 80's, 90's, Pop, Rock, Soul, R&B",
                     bitrate = 0
                 ),
                 RadioStation(
                     stationuuid = "Flow-987",
                     name = "Flow 98.7",
                     url = "https://ice64.securenetsystems.net/CKFG",
                     favicon = "https://cdn-profiles.tunein.com/s142066/images/logod.jpg?t=637808074610000000",
                     country = "Canada",
                     tags = "Hip-Hop, Pop, Afrobeat, Reggae, Soul, Soca, R&B",
                     bitrate = 0
                 ),
                 RadioStation(
                     stationuuid = "Fresh-931",
                     name = "Fresh 93.1",
                     url = "https://live.leanstream.co/CHAYFM-MP3?args=tunein",
                     favicon = "https://cdn-profiles.tunein.com/s31156/images/logod.png?t=155144",
                     country = "Canada",
                     tags = "classic, rock",
                     bitrate = 0
                 ),
             )
         }


     }
 }

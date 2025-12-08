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

class Util {

    companion object {
        private const val TAG = "Util"

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
                    val duration = c.getDouble(3)
                    //val song = Song(count, title, artist, path, duration, getAlbumArt(c.getString(0)))
                    val song = Song(count, title, artist, duration, path.toString() )
                    tempAudioList.add(song)
                    count++

                    val msg =
                        "Album id: ${song.id} | Title: ${song.title} | Artist: ${song.artist} | Path: ${song.path} | Duration: ${
                            Util.converter(song.duration)
                        }"
                    Log.i("data", formatSongRow(song))
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

        fun formatSongTableHeader(): String {
            // %-4s = left-aligned width 4, %-30s = left-aligned width 30, etc.
            return String.format(Locale.US, "%-4s %-30s %-20s %-40s %8s",
                "ID", "Title", "Artist", "Path", "Duration")
        }

        fun formatSongRow(song: Song): String {
            val id = song.id.toString()
            val title = padOrTruncate(song.title, 40)
            val artist = padOrTruncate(song.artist, 40)
            val duration = padOrTruncate(song.duration.toString(), 10)
            val path = padOrTruncate(song.path, 70)
            return String.format(Locale.US, "%-4s %-30s %-20s %8s %-40s",id, title, artist, duration, path)
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

        /**
         * Return an Android Bitmap of the embedded album art for the provided uri/path, or null.
         * This mirrors the logic in getAlbumArt but returns Bitmap to be used for Palette and caching.
         */
        fun getAlbumArtBitmap(context: Context, uri: String?): Bitmap? {
            if (uri.isNullOrBlank()) return null

            val retriever = MediaMetadataRetriever()
            return try {
                val parsedUri = try { Uri.parse(uri) } catch (_: Exception) { null }
                val hasScheme = parsedUri?.scheme?.isNotBlank() == true

                if (hasScheme) {
                    try {
                        retriever.setDataSource(context, parsedUri)
                    } catch (e: Exception) {
                        Log.w(TAG, "getAlbumArtBitmap: setDataSource(context, uri) failed for parsedUri=$parsedUri", e)
                        return null
                    }
                } else {
                    val file = File(uri)
                    if (file.exists()) {
                        try {
                            retriever.setDataSource(file.absolutePath)
                        } catch (e: Exception) {
                            Log.w(TAG, "getAlbumArtBitmap: setDataSource(file) failed for file=${file.absolutePath}", e)
                            return null
                        }
                    } else {
                        Log.w(TAG, "getAlbumArtBitmap: file does not exist: $uri")
                        return null
                    }
                }

                val art = retriever.embeddedPicture
                if (art == null || art.isEmpty()) {
                    null
                } else {
                    BitmapFactory.decodeByteArray(art, 0, art.size)
                }
            } catch (e: Exception) {
                Log.w(TAG, "getAlbumArtBitmap failed for uri=$uri", e)
                null
            } finally {
                try { retriever.release() } catch (_: Exception) {}
            }
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
                    Log.d(TAG, "fetchRadioStations: name='${query}' limit=${limit} country=${country ?: "<any>"} state=${state ?: "<any>"}")
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
            } catch (e: Exception) {
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
            // preferred search keywords (ordered by preference)
            val preferredQueries = listOf(
                "boom 97.3", "energy 95.3", "kiss 92.5", "z103.5", "105.3","CKFM 99.9"
            )


            fun isMusicStation(st: RadioStation): Boolean {
                val tags = st.tags?.lowercase() ?: ""
                val name = st.name?.lowercase() ?: ""
                // simple genre/keyword check in tags or name
                val musicKeywords = listOf("music", "pop", "rock", "dance", "hip", "hip-hop", "hiphop", "hits", "top 40", "top40", "chart", "r&b", "indie", "adult contemporary")
                if (musicKeywords.any { tags.contains(it) || name.contains(it) }) return true

                // prefer stations that appear to be FM music stations (name contains fm or frequency)
                if (name.contains("fm") || Regex("\\b\\d{2,3}(\\.\\d)?\\b").containsMatchIn(name)) return true

                // fallback: reasonable bitrate suggests a music stream
                val bitrate = st.bitrate ?: 0
                if (bitrate >= 32) return true

                return false
            }

            val results = mutableListOf<RadioStation>()
            val seen = mutableSetOf<String>()

            // Helper to format station info for logging
            fun formatStation(st: RadioStation?): String {
                if (st == null) return "<null>"
                val nameRaw = st.name ?: "<no-name>"
                val displayName = extractQuotedOrOriginal(nameRaw).ifBlank { nameRaw }
                val uuid = st.stationuuid ?: "<no-uuid>"
                val url = st.url ?: "<no-url>"
                val tags = st.tags ?: ""
                val br = st.bitrate ?: 0
                // Include both displayName (quoted-extracted) and raw name for clearer logs
                return "name=${displayName} raw=${nameRaw} | uuid=$uuid | url=$url | tags=$tags | br=$br"
            }

            // Haversine formula to compute distance between two lat/lon points in kilometers
            fun haversineDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
                val r = 6371.0 // Earth radius km
                val dLat = Math.toRadians(lat2 - lat1)
                val dLon = Math.toRadians(lon2 - lon1)
                val a = Math.sin(dLat/2) * Math.sin(dLat/2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon/2) * Math.sin(dLon/2)
                val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a))
                return r * c
            }

            // GTA center (Toronto) and radius filter in km
            val GTA_LAT = 43.6532
            val GTA_LON = -79.3832
            val RADIUS_KM = 200.0

            // Whitelist of known GTA station name tokens (lowercase substrings) to always include
            val whitelist = listOf("z103.5", "z1035", "z103", "chum 104.5", "chum fm", "kiss 92.5", "kiss fm", "virgin 99.9", "boom 97.3", "fresh radio 93.1", "105.3 virgin radio", "energy 95.3")

            fun tryAdd(st: RadioStation?) {
                if (st == null) return
                val nameLower = st.name?.lowercase() ?: ""
                // normalize name (remove non-alphanumeric) to match variants like "Z 103.5", "CIDC-FM", etc.
                val nameNormalized = nameLower.replace(Regex("[^a-z0-9]"), "")
                val whitelistNormalized = whitelist.map { it.replace(Regex("[^a-z0-9]"), "") }
                val urlLower = st.url?.lowercase() ?: ""
                val faviconLower = st.favicon?.lowercase() ?: ""
                val urlTokens = listOf("cidcfm", "evanov", "leanstream", "z103")
                val isWhitelisted = whitelist.any { nameLower.contains(it) } || whitelistNormalized.any { nameNormalized.contains(it) } || urlTokens.any { urlLower.contains(it) || faviconLower.contains(it) }

                 // Only add FM stations: require evidence the station is an FM broadcaster
                 // (name/tags contains 'fm' or the name contains a numeric frequency like "104.5").
                 fun isFMStation(s: RadioStation): Boolean {
                     val n = s.name?.lowercase() ?: ""
                     val t = s.tags?.lowercase() ?: ""
                     val u = s.url?.lowercase() ?: ""
                     // common heuristics for FM stations
                     if (t.contains(" fm") || t.contains("fm ") || t.split(Regex("[,; ]")).any { it == "fm" }) return true
                     if (n.contains(" fm") || n.startsWith("fm ") || n.endsWith(" fm") || n.contains("fm ") || n.contains("fm.") ) return true
                     // url that includes .fm domains often indicates FM broadcaster branding
                     if (u.contains(".fm")) return true
                     // numeric frequency in the name (e.g. 104.5, 92.5), treat as FM
                     if (Regex("\\b\\d{2,3}(\\.\\d)?\\b").containsMatchIn(n)) return true
                     return false
                 }

                // Duplicate detection: normalize multiple identity keys and skip if any already seen
                fun normalizedUrl(u: String?): String? {
                    if (u == null) return null
                    val noQuery = u.split('?')[0]
                    return noQuery.trim().lowercase().trimEnd('/')
                }

                val keys = mutableListOf<String>()
                st.stationuuid?.let { if (it.isNotBlank()) keys.add(it.trim().lowercase()) }
                normalizedUrl(st.url)?.let { if (it.isNotBlank()) keys.add(it) }
                // also consider favicon host as a weak key
                normalizedUrl(st.favicon)?.let { if (it.isNotBlank()) keys.add(it) }
                if (nameNormalized.isNotBlank()) keys.add(nameNormalized)

                // If any key already seen, skip duplicate
                if (keys.any { seen.contains(it) }) {
                    try { Log.d(TAG, "fetchStationsNearGTA: skipping duplicate station: ${formatStation(st)}") } catch (_: Throwable) {}
                    return
                }

                 // Heuristic to identify aggregator/network entries (TuneIn, Radio.net, etc.) and skip them
                 fun isNetwork(s: RadioStation): Boolean {
                     val n = s.name?.lowercase() ?: ""
                     val t = s.tags?.lowercase() ?: ""
                     val u = s.url?.lowercase() ?: ""

                     val networkKeywords = listOf("network", "networks", "affiliate", "affiliates", "group", "syndicat", "syndicated")
                     if (networkKeywords.any { n.contains(it) || t.contains(it) }) return true

                     val aggregatorTokens = listOf(
                         "tunein",
                         "radio.net",
                         "radioplayer",
                         "streema",
                         "shoutcast",
                         "icecast",
                         "dirble",
                         "audacy",
                         "mixcloud",
                         "soundcloud",
                         "player.fm",
                         "radioparadise",
                         "tsn",
                         "rdmix",
                         "multicultural"
                     )
                     if (aggregatorTokens.any { u.contains(it) || n.contains(it) || t.contains(it) }) return true

                     return false
                  }

                // Heuristic: detect news/talk/traffic/weather/sports stations to skip them (unless whitelisted)
                fun isNewsStation(s: RadioStation): Boolean {
                    val n = s.name?.lowercase() ?: ""
                    val t = s.tags?.lowercase() ?: ""
                    val u = s.url?.lowercase() ?: ""
                    val newsKeywords = listOf(
                        "news",
                        "talk",
                        "traffic",
                        "weather",
                        "headline",
                        "headlines",
                        "newstalk",
                        "talkradio",
                        "talk radio",
                        "newsradio",
                        "cbc",
                        "globalnews",
                        "sports"
                    )
                    return newsKeywords.any { kw -> n.contains(kw) || t.contains(kw) || u.contains(kw) }
                }

                // If not whitelisted, apply network/FM/geo filters. Whitelisted stations bypass these checks so
                // common station-name variants (e.g. "Z 103.5" or "CIDC-FM") are preserved.
                if (!isWhitelisted) {
                    if (isNetwork(st)) {
                        //Log.d(TAG, "fetchStationsNearGTA: filtered network/aggregator station: ${formatStation(st)}")
                        return
                    }

                    // Filter out news/talk/weather/traffic stations unless explicitly whitelisted
                    if (isNewsStation(st)) {
                        //Log.d(TAG, "fetchStationsNearGTA: filtered news/talk station: ${formatStation(st)}")
                        return
                    }

                     // Apply FM heuristics
                     if (!isFMStation(st)) return

                    // Enforce radius: require geo coords and check distance
                    val lat = st.geo_lat
                    val lon = st.geo_long
                    if (lat == null || lon == null) {
                        if (!isWhitelisted) {
                            //Log.d(TAG, "fetchStationsNearGTA: skipping station (no geo coords): ${formatStation(st)}")
                            return
                        } else {
                            //Log.d(TAG, "fetchStationsNearGTA: whitelisted station with no geo coords, including: ${formatStation(st)}")
                        }
                    } else {
                        val dist = haversineDistanceKm(lat, lon, GTA_LAT, GTA_LON)
                        if (dist > RADIUS_KM && !isWhitelisted) {
                            //Log.d(TAG, "fetchStationsNearGTA: skipping station (outside ${RADIUS_KM}km): ${formatStation(st)} dist=${"%.1f".format(Locale.US, dist)}km")
                            return
                        }
                    }
                }
                 // Passed filters; mark all normalized keys as seen and add
                 // Allow whitelist to bypass the loose 'isMusicStation' heuristics
                 if (!isMusicStation(st) && !isWhitelisted) return
                 keys.forEach { seen.add(it) }
                 results.add(st)
             }

            try {
                // First, attempt targeted searches for the preferred station names
                for (q in preferredQueries) {
                    if (results.size >= limit) break
                    try {
                        val list = fetchRadioStations(query = q, limit = 10, country = "Canada", state = "Ontario")
                        // try to find an exact/strong match first
                        val strong = list.firstOrNull { st ->
                            val n = st.name?.lowercase() ?: ""
                            // match against any preferred keyword
                            preferredQueries.any { pref -> n.contains(pref) }
                        }
                        if (strong != null) {
                            Log.d(TAG, "fetchStationsNearGTA: preferred query='$q' found strong match: ${formatStation(strong)}")
                            tryAdd(strong)
                            if (results.size >= limit) break
                        } else {
                            // otherwise add first music-like station from the results
                            Log.d(TAG, "fetchStationsNearGTA: preferred query='$q' returned ${list.size} stations")
                            // log a subset of returned stations for inspection
                            try {
                                Log.d(TAG, "fetchStationsNearGTA: sample returned: ${list.take(6).joinToString(" | ") { formatStation(it) }}")
                            } catch (_: Throwable) {}
                            val music = list.firstOrNull { isMusicStation(it) }
                            tryAdd(music)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "preferred query failed: $q", e)
                    }
                }

                // Before performing searches, ensure the custom Virgin 99.9 stream is included.
                try {
                    val customVirgin = RadioStation(
                        stationuuid = "custom-virgin-999",
                        name = "Virgin 99.9",
                        url = "https://18153.live.streamtheworld.com/CKFMFMAAC_SC",
                        favicon = "https://static.mytuner.mobi/media/tvos_radios/LypQKVJVaB.png",
                        tags = "pop, top 40",
                        bitrate = 128,
                        geo_lat = null,
                        geo_long = null
                    )
                    tryAdd(customVirgin)
                } catch (_: Throwable) {}

                // If we still need more, fetch Ontario-wide stations and prioritize music ones
                if (results.size < limit) {
                    // First try the geo-based nearby endpoint using GTA center and radius
                    val byNearby = fetchRadioStationsNearby(GTA_LAT, GTA_LON, limit * 2, distanceKm = RADIUS_KM.toInt())
                    if (byNearby.isNotEmpty()) {
                        for (st in byNearby) {
                            if (results.size >= limit) break
                            try { Log.d(TAG, "fetchStationsNearGTA: nearby candidate: ${formatStation(st)}") } catch (_: Throwable) {}
                            tryAdd(st)
                        }
                    } else {
                        // Fallback to province-wide search if nearby endpoint didn't return data
                        val byProvince = fetchRadioStations(query = "", limit = limit * 2, country = "Canada", state = "Ontario")
                        for (st in byProvince) {
                            if (results.size >= limit) break
                            Log.d(TAG, "fetchStationsNearGTA: province candidate: ${formatStation(st)}")
                            tryAdd(st)
                        }
                    }
                }

                // Final ordering: ensure preferred stations appear first in the defined order
                val order = preferredQueries.map { it.lowercase() }
                results.sortWith(compareBy { st ->
                    val name = st.name?.lowercase() ?: ""
                    val idx = order.indexOfFirst { name.contains(it) }
                    if (idx == -1) Int.MAX_VALUE else idx
                })

                // Helper: build a simple ASCII table for logs (ensure this is available in scope)
                fun makeTable(headers: List<String>, rows: List<List<String>>): String {
                    if (rows.isEmpty()) return headers.joinToString(" | ")
                    val colCount = headers.size
                    val widths = IntArray(colCount) { i ->
                        val headLen = headers.getOrNull(i)?.length ?: 0
                        val maxCell = rows.mapNotNull { it.getOrNull(i) }.map { it.length }.maxOrNull() ?: 0
                        maxOf(headLen, maxCell)
                    }
                    val sb = StringBuilder()
                    sb.append(headers.mapIndexed { i, h -> h.padEnd(widths[i]) }.joinToString(" | "))
                    sb.append('\n')
                    sb.append(widths.map { "-".repeat(it) }.joinToString("-+-"))
                    sb.append('\n')
                    for (r in rows) {
                        val rowLine = (0 until colCount).map { i -> (r.getOrNull(i) ?: "").padEnd(widths[i]) }.joinToString(" | ")
                        sb.append(rowLine).append('\n')
                    }
                    return sb.toString()
                }

                fun stationToRow(st: RadioStation?): List<String> {
                    if (st == null) return listOf("<null>")
                    fun short(s: String?, max: Int): String {
                        val str = (s ?: "").replace('\n',' ').trim()
                        return if (str.length <= max) str else str.take(max - 3) + "..."
                    }
                    val name = short(extractQuotedOrOriginal(st.name), 30)
                     val uuid = short(st.stationuuid, 8)
                     val url = short(st.url, 40)
                     val tags = short(st.tags, 20)
                     val br = (st.bitrate ?: 0).toString()
                     val lat = st.geo_lat
                     val lon = st.geo_long
                     val distStr = if (lat != null && lon != null) String.format(Locale.US, "%.1fkm", haversineDistanceKm(lat, lon, 43.6532, -79.3832)) else "n/a"
                     return listOf(name, uuid, url, tags, br, distStr)
                 }


                 val rows = results.map { stationToRow(it) }
                 val table = makeTable(listOf("Name", "UUID", "URL", "Tags", "BR", "Dist"), rows)
                 Log.d(TAG, "fetchStationsNearGTA: final filtered results (${results.size}):\n$table")
            } catch (_: Throwable) {}
            return results.take(limit)
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
                if (u.isNullOrBlank()) return null
                var s = u.trim()
                if ((s.startsWith('"') && s.endsWith('"')) || (s.startsWith('\'') && s.endsWith('\''))) {
                    s = s.substring(1, s.length - 1)
                }
                if (s.startsWith("//")) s = "https:$s"
                if (s.startsWith("http://")) s = s.replaceFirst("http://", "https://")
                if (s.startsWith("https://")) return s
                return null
            }

            val favCandidate = toHttps(st.favicon)
            if (!favCandidate.isNullOrBlank()) return favCandidate

            var hostOnly: String? = null
            val srcUrl = st.url ?: st.favicon
            if (!srcUrl.isNullOrBlank()) {
                try {
                    val uri = java.net.URI(srcUrl)
                    val host = uri.host ?: uri.schemeSpecificPart
                    if (!host.isNullOrBlank()) hostOnly = host.trim().lowercase().removePrefix("www.")
                } catch (_: Exception) {}
            }

            fun abs(host: String, path: String) = "https://$host${if (path.startsWith("/")) path else "/$path"}"

            if (!hostOnly.isNullOrBlank()) {
                val host = hostOnly
                val candidates = listOf(
                    abs(host, "apple-touch-icon.png"),
                    abs(host, "favicon-196x196.png"),
                    abs(host, "favicon-32x32.png"),
                    abs(host, "favicon.ico"),
                    abs(host, "logo.png"),
                    abs(host, "images/logo.png"),
                    // provisioning.streamtheworld.com heuristic candidates (HTTPS)
                    // common patterns: logos by station uuid or by customer/host; these are heuristics — verify per-station.
                    "https://provisioning.streamtheworld.com/logos/${st.stationuuid ?: ""}.png",
                    "https://provisioning.streamtheworld.com/logo/${st.stationuuid ?: ""}.png",
                    "https://provisioning.streamtheworld.com/images/${st.stationuuid ?: ""}.png",
                    "https://provisioning.streamtheworld.com/${host}/logo.png",
                    "https://provisioning.streamtheworld.com/${host}/favicon.png",
                    // favicon services
                    "https://www.google.com/s2/favicons?sz=256&domain_url=$host",
                    "https://icons.duckduckgo.com/ip3/$host.ico",
                    "https://www.google.com/s2/favicons?sz=64&domain_url=$host"
                )
                for (c in candidates) {
                    if (!c.isNullOrBlank()) return c
                }
            }

            val rawFav = st.favicon
            if (!rawFav.isNullOrBlank() && hostOnly != null) {
                val s = rawFav.trim()
                if (s.startsWith("/")) return "https://$hostOnly$s"
                if (s.startsWith("//")) return "https:$s"
                if (!s.startsWith("http://") && !s.startsWith("https://")) return "https://$hostOnly/$s"
            }

            if (!srcUrl.isNullOrBlank()) {
                try {
                    val uri2 = java.net.URI(srcUrl)
                    val host = (uri2.host ?: uri2.schemeSpecificPart)?.toString()?.lowercase()?.removePrefix("www.")
                    if (!host.isNullOrBlank()) return "https://$host/favicon.ico"
                } catch (_: Exception) {}
            }

            return ""
        }
     }
 }

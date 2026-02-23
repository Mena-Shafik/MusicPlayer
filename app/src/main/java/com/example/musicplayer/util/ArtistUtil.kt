package com.example.musicplayer.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Service to fetch artist images from MusicBrainz and Fanart.tv APIs (free, no auth required).
 * Uses music-specific databases for better accuracy.
 */
object ArtistUtil {
    private const val TAG = "ArtistImageService"
    private const val TIMEOUT_MS = 15000

    private val imageCache = mutableMapOf<String, String?>()
    private var isAppActive = true

    // Artist name aliases for better matching
    private val artistAliases = mapOf(
        "2pac" to "Tupac Shakur",
        "tupac" to "Tupac Shakur",
        "the beatles" to "The Beatles",
        "beatles" to "The Beatles",
        "jay z" to "Jay-Z",
        "jay-z" to "Jay-Z",
        "ng and queen" to "King & Queen"
    )

    private val artistMbidOverrides = mapOf(
        "king and queen" to "f6b56532-bff1-4ebc-ae8d-a5286958841d",
        "eminem" to "b95ce3ff-3d05-4e87-9e01-c97b471e7d05",
        "ll cool j" to "4975872c-e29c-43f0-b5cc-3c4c13921406"
    )

    private fun normalizeArtistName(name: String): String {
        val normalized = java.text.Normalizer.normalize(name, java.text.Normalizer.Form.NFD)
        val result = normalized
            .replace("\\p{Mn}+".toRegex(), "") // Remove diacritics
            .lowercase()
            .replace("&", "and")
            .replace(Regex("\\b(feat|featuring|ft)(?:\\s|\\.|\\s\\.)?\\b"), "")
            .replace("\\bwith\\b".toRegex(), "")
            .replace("\\bx\\b".toRegex(), "")
            .replace("[^a-z0-9]+".toRegex(), " ")
            .trim()

        if (name != result) {
            Log.d(TAG, "normalize: '$name' → '$result'")
        }
        return result
    }

    private fun matchScore(candidate: String, target: String): Int {
        if (candidate == target) return 3
        if (candidate.removePrefix("the ") == target.removePrefix("the ")) return 3

        val stopwords = setOf("and", "the")
        val candidateTokens = candidate.split(" ").filter { it.isNotBlank() && it !in stopwords }
        val targetTokens = target.split(" ").filter { it.isNotBlank() && it !in stopwords }
        if (candidateTokens.isEmpty() || targetTokens.isEmpty()) return 0

        if (candidateTokens.toSet() == targetTokens.toSet()) return 2

        val overlap = candidateTokens.count { it in targetTokens }
        val overlapRatio = overlap.toFloat() / maxOf(candidateTokens.size, targetTokens.size)
        return if (overlapRatio >= 0.8f) 1 else 0
    }

    /**
     * Fetch artist image URL using MusicBrainz API + Fanart.tv fallback.
     * MusicBrainz is a free, open music encyclopedia with accurate artist data.
     */
    private suspend fun fetchArtistImage(artistName: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val normalizedName = normalizeArtistName(artistName)
                Log.d(TAG, "fetchArtistImage: artistName='$artistName' normalized='$normalizedName'")

                val overrideMbid = artistMbidOverrides[normalizedName]
                Log.d(TAG, "fetchArtistImage: Looking for override with key='$normalizedName' -> found=$overrideMbid")

                if (overrideMbid != null) {
                    Log.d(TAG, "fetchArtistImage: Using MBID override for $artistName: $overrideMbid")
                    val overrideImage = fetchFromTheAudioDB(overrideMbid, artistName)
                    if (overrideImage != null) {
                        Log.d(TAG, "Using MBID override for $artistName: $overrideMbid")
                        return@withContext overrideImage
                    } else {
                        Log.d(TAG, "fetchArtistImage: MBID override returned no image, continuing with search")
                    }
                }

                val encoded = URLEncoder.encode("\"${artistName.trim()}\"", "UTF-8")
                Log.d(TAG, "Fetching artist image for: $artistName from MusicBrainz")
                // ...existing code...
                // Step 1: Search MusicBrainz for artist MBID
                val searchUrl = "https://musicbrainz.org/ws/2/artist/?query=artist:$encoded&fmt=json&limit=5"
                val searchConn = URL(searchUrl).openConnection() as HttpURLConnection
                searchConn.requestMethod = "GET"
                searchConn.connectTimeout = TIMEOUT_MS
                searchConn.readTimeout = TIMEOUT_MS
                searchConn.setRequestProperty("User-Agent", "MusicPlayer/1.0 ( contact@example.com )")
                searchConn.setRequestProperty("Accept", "application/json")

                val searchResponse = if (searchConn.responseCode == 200) {
                    searchConn.inputStream.bufferedReader().use { it.readText() }
                } else {
                    Log.w(TAG, "MusicBrainz search returned code ${searchConn.responseCode}")
                    searchConn.disconnect()
                    return@withContext null
                }
                searchConn.disconnect()

                // Parse search results to get MBID
                val searchObj = JSONObject(searchResponse)
                val artists = searchObj.optJSONArray("artists")
                if (artists == null || artists.length() == 0) {
                    Log.d(TAG, "No MusicBrainz artist found for: $artistName")
                    return@withContext null
                }

                val targetName = normalizeArtistName(artistName)
                var bestIndex = -1
                var bestScore = -1
                var bestMbScore = -1
                for (i in 0 until artists.length()) {
                    val candidateObj = artists.getJSONObject(i)
                    val candidateName = normalizeArtistName(candidateObj.optString("name", ""))
                    val score = matchScore(candidateName, targetName)
                    val mbScore = candidateObj.optInt("score", 0)
                    if (score > bestScore || (score == bestScore && mbScore > bestMbScore)) {
                        bestScore = score
                        bestMbScore = mbScore
                        bestIndex = i
                    }
                }

                if (bestIndex < 0 || bestScore <= 0) {
                    Log.d(TAG, "No close MusicBrainz match for: $artistName")
                    return@withContext null
                }

                val artist = artists.getJSONObject(bestIndex)
                val mbid = artist.optString("id", "")
                if (mbid.isBlank()) {
                    Log.d(TAG, "No MBID found for: $artistName")
                    return@withContext null
                }

                Log.d(TAG, "Found MBID for $artistName: $mbid")

                // Step 2: Try to get image from TheAudioDB (free, no key required for basic use)
                val imageUrl = fetchFromTheAudioDB(mbid, artistName)
                if (imageUrl != null) {
                    Log.d(TAG, "Found image from TheAudioDB for $artistName")
                    return@withContext imageUrl
                }

                // Step 3: Fallback - try to get a simple image URL pattern
                // Some services use predictable patterns with MBID
                Log.d(TAG, "No image found for $artistName via APIs")
                null

            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch artist image for $artistName: ${e.message}")
                null
            }
        }
    }

    /**
     * Fetch artist image from TheAudioDB API (free service).
     * Searches by name first, then falls back to MBID if provided.
     */
    private suspend fun fetchFromTheAudioDB(mbid: String, artistName: String): String? {
        return try {
            // Try searching by name first
            val nameEncoded = URLEncoder.encode(artistName.trim(), "UTF-8")
            val nameApiUrl = "https://www.theaudiodb.com/api/v1/json/2/search.php?s=$nameEncoded"

            Log.d(TAG, "Searching TheAudioDB by name: $artistName")
            var conn = URL(nameApiUrl).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            conn.setRequestProperty("User-Agent", "MusicPlayer/1.0")

            var response = if (conn.responseCode == 200) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                Log.d(TAG, "TheAudioDB name search failed with code ${conn.responseCode}")
                conn.disconnect()
                ""
            }
            conn.disconnect()

            // Try to extract image from name search
            if (response.isNotBlank()) {
                val obj = JSONObject(response)
                val artists = obj.optJSONArray("artists")
                if (artists != null && artists.length() > 0) {
                    Log.d(TAG, "  TheAudioDB returned ${artists.length()} results for '$artistName'")
                    val targetName = normalizeArtistName(artistName)
                    var bestMatch: JSONObject? = null
                    var bestScore = 0

                    for (i in 0 until artists.length()) {
                        val artistObj = artists.getJSONObject(i)
                        val apiArtistName = artistObj.optString("strArtist", "")
                        val apiName = normalizeArtistName(apiArtistName)
                        val score = matchScore(apiName, targetName)
                        Log.d(TAG, "    [$i] '$apiArtistName' (normalized: '$apiName') → score=$score")

                        if (score > bestScore) {
                            bestScore = score
                            bestMatch = artistObj
                        }
                    }

                    if (bestMatch != null) {
                        val bestArtistName = bestMatch.optString("strArtist", "")
                        val imageUrl = bestMatch.optString("strArtistThumb", "")
                            .ifBlank { bestMatch.optString("strArtistLogo", "") }
                            .ifBlank { bestMatch.optString("strArtistBanner", "") }

                        if (imageUrl.isNotBlank()) {
                            Log.d(TAG, "  ✓ BEST MATCH: '$bestArtistName' (score=$bestScore)")
                            Log.d(TAG, "  ✓ IMAGE: ${imageUrl.take(80)}...")
                            return imageUrl
                        } else {
                            Log.d(TAG, "  ✗ BEST MATCH: '$bestArtistName' (score=$bestScore) but NO IMAGE")
                        }
                    } else {
                        Log.d(TAG, "  ✗ No matching artists found (bestScore=$bestScore)")
                    }
                } else {
                    Log.d(TAG, "  ✗ TheAudioDB returned null or empty artists array")
                }
            }

            // Fallback: search by MBID if name search didn't work
            if (mbid.isNotBlank()) {
                Log.d(TAG, "Falling back to MBID search for $artistName: $mbid")
                val mbidApiUrl = "https://www.theaudiodb.com/api/v1/json/2/artist-mb.php?i=$mbid"
                conn = URL(mbidApiUrl).openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = TIMEOUT_MS
                conn.readTimeout = TIMEOUT_MS
                conn.setRequestProperty("User-Agent", "MusicPlayer/1.0")

                response = if (conn.responseCode == 200) {
                    conn.inputStream.bufferedReader().use { it.readText() }
                } else {
                    conn.disconnect()
                    return null
                }
                conn.disconnect()

                val obj = JSONObject(response)
                val artists = obj.optJSONArray("artists")
                if (artists != null && artists.length() > 0) {
                    val artistObj = artists.getJSONObject(0)
                    val imageUrl = artistObj.optString("strArtistThumb", "")
                        .ifBlank { artistObj.optString("strArtistLogo", "") }
                        .ifBlank { artistObj.optString("strArtistBanner", "") }

                    if (imageUrl.isNotBlank()) {
                        Log.d(TAG, "Found TheAudioDB image by MBID for $artistName: ${imageUrl.take(80)}")
                        return imageUrl
                    }
                }
            }

            Log.d(TAG, "No image found in TheAudioDB for $artistName")
            null
        } catch (e: Exception) {
            Log.e(TAG, "TheAudioDB lookup failed for $artistName: ${e.message}")
            null
        }
    }


    /**
     * Get artist image URL with caching.
     * Checks cache first, then fetches from MusicBrainz + TheAudioDB.
     * Falls back to aliases if original name doesn't return images.
     * Note: Only successful fetches are cached when app is active; failures are retried on next call.
     */
    suspend fun getArtistImageUrl(artistName: String): String? {
        val normalizedName = normalizeArtistName(artistName)
        val cacheKey = normalizedName  // Use normalized name as cache key for consistency

        // Check cache first (always check, regardless of app state)
        if (imageCache.containsKey(cacheKey)) {
            val cached = imageCache[cacheKey]
            if (cached != null) {
                Log.d(TAG, "✓ CACHE HIT: '$artistName' (normalized: '$cacheKey') (app active: $isAppActive)")
                return cached
            }
        }

        Log.d(TAG, "━━━ FETCHING: '$artistName' (normalized: '$normalizedName') (app active: $isAppActive) ━━━")

        // Try the original name first
        var url = fetchArtistImage(artistName)
        if (url != null) {
            Log.d(TAG, "✓ SUCCESS: Found image for '$artistName'")
            // Only cache when app is active
            if (isAppActive) {
                imageCache[cacheKey] = url
                Log.d(TAG, "💾 CACHED: '$artistName' (key: '$cacheKey') (app is active)")
            } else {
                Log.d(TAG, "⊘ NOT CACHED: '$artistName' (app is in background)")
            }
            return url
        }

        // If no image found, try aliases
        val alias = artistAliases[normalizedName]
        if (alias != null && alias != artistName) {
            Log.d(TAG, "→ TRYING ALIAS: '$normalizedName' -> '$alias'")
            url = fetchArtistImage(alias)
            if (url != null) {
                Log.d(TAG, "✓ ALIAS SUCCESS: Found image for '$alias'")
                // Only cache when app is active
                if (isAppActive) {
                    imageCache[cacheKey] = url
                    Log.d(TAG, "💾 CACHED: '$artistName' (via alias, key: '$cacheKey', app is active)")
                } else {
                    Log.d(TAG, "⊘ NOT CACHED: '$artistName' (via alias, app is in background)")
                }
                return url
            }
        }

        Log.e(TAG, "✗ FAILED TO FETCH IMAGE: '$artistName' (normalized: '$normalizedName', alias: ${alias ?: "none"})")
        // Don't cache null - allow retry on next call
        return null
    }

    /**
     * Clear the image cache and mark app as inactive.
     * Called when app goes to background.
     */
    fun onAppBackground() {
        isAppActive = false
        imageCache.clear()
        Log.d(TAG, "⊗ APP BACKGROUNDED: Cache cleared and caching disabled")
    }

    /**
     * Mark app as active - caching will resume.
     * Called when app comes to foreground.
     */
    fun onAppForeground() {
        isAppActive = true
        imageCache.clear()
        Log.d(TAG, "⊕ APP FOREGROUNDED: Cache enabled, previous cache cleared")
    }

    /**
     * Clear the image cache (useful for testing or memory management).
     */
    fun clearCache() {
        imageCache.clear()
        Log.d(TAG, "Image cache cleared")
    }
}
package com.example.musicplayer.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Service to fetch artist images from Wikipedia API (free, no auth required).
 * Uses Wikipedia page images which are more relevant than generic Commons search.
 */
object ArtistUtil {
    private const val TAG = "ArtistImageService"
    private const val TIMEOUT_MS = 10000

    private val imageCache = mutableMapOf<String, String?>()

    // Artist name aliases: local name -> Wikipedia canonical name
    private val artistAliases = mapOf(
        "2pac" to "Tupac Shakur",
        "tupac" to "Tupac Shakur",
        "eminem" to "Marshall Mathers",
        "the beatles" to "The Beatles",
        "beatles" to "The Beatles",
        "jay z" to "Jay-Z",
        "jay-z" to "Jay-Z"
    )

    /**
     * Fetch artist image URL from Wikipedia (free, no authentication).
     * Uses Wikipedia page images which are specific to the article.
     * Returns null if artist not found or API call fails.
     */
    private suspend fun fetchArtistImage(artistName: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                // Try Wikipedia page images first (better quality and relevance)
                val encoded = URLEncoder.encode(artistName.trim(), "UTF-8")

                Log.d(TAG, "Fetching artist image for: $artistName from Wikipedia")

                // Use Wikipedia API to get the page image filename
                val apiUrl =
                    "https://en.wikipedia.org/w/api.php?action=query&titles=$encoded&prop=pageimages&format=json&piprop=original"
                val conn = URL(apiUrl).openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = TIMEOUT_MS
                conn.readTimeout = TIMEOUT_MS
                conn.setRequestProperty("User-Agent", "MusicPlayer/1.0")

                val response = if (conn.responseCode == 200) {
                    conn.inputStream.bufferedReader().use { it.readText() }
                } else {
                    Log.w(
                        TAG,
                        "Wikipedia API returned code ${conn.responseCode} for artist: $artistName"
                    )
                    null
                }

                conn.disconnect()

                // Parse Wikipedia JSON response to get the Commons filename
                if (response != null) {
                    try {
                        val obj = JSONObject(response)
                        val query = obj.optJSONObject("query")
                        if (query != null) {
                            val pages = query.optJSONObject("pages")
                            if (pages != null) {
                                val keys = pages.keys()
                                while (keys.hasNext()) {
                                    val pageId = keys.next()
                                    if (pageId == "-1") continue

                                    val page = pages.getJSONObject(pageId)

                                    // Get the original image URL (full resolution Commons URL)
                                    val original = page.optJSONObject("original")
                                    if (original != null) {
                                        val imgUrl = original.optString("source", "")
                                        if (imgUrl.isNotBlank()) {
                                            // Convert thumbnail URL to full Commons URL if needed
                                            val commonsUrl = if (imgUrl.contains("/thumb/")) {
                                                // Remove /thumb/ and everything after the filename
                                                imgUrl.replace("/thumb/", "/")
                                                    .replaceAfter(
                                                        Regex(
                                                            "\\.(jpg|png|jpeg|svg|gif)",
                                                            RegexOption.IGNORE_CASE
                                                        ).find(imgUrl)?.value ?: "", ""
                                                    )
                                                    .removeSuffix("/")
                                            } else {
                                                imgUrl
                                            }
                                            Log.d(
                                                TAG,
                                                "Found Wikipedia Commons image for $artistName: $commonsUrl"
                                            )
                                            return@withContext commonsUrl
                                        }
                                    }
                                }
                            }
                        }
                        Log.d(TAG, "No Wikipedia page image found for $artistName")
                        null
                    } catch (e: Exception) {
                        Log.e(
                            TAG,
                            "Failed to parse Wikipedia response for $artistName: ${e.message}"
                        )
                        null
                    }
                } else {
                    Log.d(TAG, "Empty response from Wikipedia for artist: $artistName")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch from Wikipedia for $artistName: ${e.message}")
                null
            }
        }
    }

    /**
     * Get artist image URL with caching.
     * Checks cache first, then fetches from Wikipedia.
     * Falls back to aliases if original name doesn't return images.
     */
    suspend fun getArtistImageUrl(artistName: String): String? {
        val normalizedName = artistName.lowercase().trim()

        // Check cache first
        if (imageCache.containsKey(artistName)) {
            val cached = imageCache[artistName]
            Log.d(TAG, "Cache hit for $artistName")
            return cached
        }

        Log.d(TAG, "Cache miss for $artistName, fetching from Wikipedia...")

        // Try the original name first
        var url = fetchArtistImage(artistName)

        // If no image found, try aliases
        if (url == null) {
            val alias = artistAliases[normalizedName]
            if (alias != null && alias != artistName) {
                Log.d(TAG, "No image for '$artistName', trying alias: '$alias'")
                url = fetchArtistImage(alias)
            }
        }

        imageCache[artistName] = url
        return url
    }

    /**
     * Clear the image cache (useful for testing or memory management).
     */
    fun clearCache() {
        imageCache.clear()
        Log.d(TAG, "Image cache cleared")
    }
}
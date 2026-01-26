package com.example.musicplayer.model

/**
 * Represents an artist with associated metadata and image URL.
 */
data class Artist(
    val name: String,
    val normalizedKey: String,  // Normalized name for grouping (lowercase, no "the", etc.)
    val imageUrl: String? = null,  // URL to artist profile image from Last.fm
    val songCount: Int = 0  // Number of songs by this artist
) {
    companion object {
        /**
         * Create an Artist from a song artist name.
         * Normalizes the name and fetches the image if needed.
         */
        fun fromSongArtist(artistName: String): Artist {
            val normalized = normalizeArtistKey(artistName)
            return Artist(
                name = artistName.trim(),
                normalizedKey = normalized,
                imageUrl = null,
                songCount = 0
            )
        }

        private fun normalizeArtistKey(name: String): String {
            val trimmed = name.trim().lowercase()
            val noThe = if (trimmed.startsWith("the ")) trimmed.removePrefix("the ") else trimmed
            return noThe.replace(Regex("^[^a-z0-9]+|[^a-z0-9]+$"), "")
        }
    }

    /**
     * Create a copy with updated image URL (useful after fetching from API).
     */
    fun withImageUrl(url: String?): Artist {
        return this.copy(imageUrl = url)
    }

    /**
     * Create a copy with updated song count.
     */
    fun withSongCount(count: Int): Artist {
        return this.copy(songCount = count)
    }
}

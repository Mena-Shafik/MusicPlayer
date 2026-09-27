package com.example.musicplayer.model

data class Artist(
    val name: String,
    val normalizedKey: String,  // Normalized name for grouping (lowercase, no "the", etc.)
    val imageUrl: String? = null,  // URL to artist profile image from Last.fm
    val songCount: Int = 0  // Number of songs by this artist
) {
    companion object {
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

    fun withImageUrl(url: String?): Artist {
        return this.copy(imageUrl = url)
    }

    fun withSongCount(count: Int): Artist {
        return this.copy(songCount = count)
    }
}

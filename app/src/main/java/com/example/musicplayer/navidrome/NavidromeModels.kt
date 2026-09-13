package com.example.musicplayer.navidrome

import com.google.gson.annotations.SerializedName

/** Every Subsonic/Navidrome REST response is wrapped in this top-level envelope. */
data class SubsonicResponseWrapper(
    @SerializedName("subsonic-response") val response: SubsonicResponse?
)

data class SubsonicResponse(
    val status: String?,
    val version: String?,
    val type: String?,
    val serverVersion: String?,
    val error: SubsonicError?,
    val albumList2: AlbumList2? = null,
    val searchResult3: SearchResult3? = null
) {
    val isOk: Boolean get() = status == "ok"
}

data class SubsonicError(
    val code: Int?,
    val message: String?
)

data class AlbumList2(
    val album: List<SubsonicAlbum>?
)

data class SubsonicAlbum(
    val id: String?,
    val name: String?,
    val artist: String?,
    val songCount: Int?
)

data class SearchResult3(
    val song: List<SubsonicSong>?
)

data class SubsonicSong(
    val id: String?,
    val title: String?,
    val artist: String?,
    val album: String?,
    val track: Int?,
    val year: Int?,
    val duration: Int?,
    val coverArt: String?
)

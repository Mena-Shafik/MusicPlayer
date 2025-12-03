package com.example.musicplayer.radio

data class RadioStation(
    val stationuuid: String?,
    val name: String?,
    val url: String?,
    val favicon: String? = null,
    val country: String? = null,
    val tags: String? = null,
    val bitrate: Int? = null,
    val codec: String? = null,
    val votes: Int? = null
)

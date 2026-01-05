package com.example.musicplayer.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class RadioStation(
    val stationuuid: String?,
    val name: String?,
    val url: String?,
    val favicon: String? = null,
    val country: String? = null,
    val tags: String? = null,
    val bitrate: Int? = null,
    val codec: String? = null,
    val votes: Int? = null,
    @SerializedName("geo_lat") val geo_lat: Double? = null,
    @SerializedName("geo_long") val geo_long: Double? = null
) : Parcelable

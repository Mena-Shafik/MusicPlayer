package com.example.musicplayer.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Playlist(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val songIds: List<Int> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val description: String = ""
) : Parcelable

package com.example.musicplayer.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Radio(
    val title: String?,
    val path: String?,
) : Parcelable
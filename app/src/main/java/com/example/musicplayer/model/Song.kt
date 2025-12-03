package com.example.musicplayer.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

class Song {
    var id: Int = 0
    var title: String = "-"
    var artist: String = "-"
    var path: String = "-"
    var duration: Double = 00.00
    var cover: ByteArray? = null

    @Parcelize
    data class Song(
        val id: Int,
        val title: String?,
        val artist: String?,
        val duration: Double = 00.00,
        val path: String?,
    ) : Parcelable

   constructor(id: Int, title: String, artist: String, duration: Double, path: String?,) {
    this.id = id
    this.title = title
    this.artist = artist
    this.duration = duration
    this.path = path?: "-"

   }

   constructor(id: Int, title: String, artist: String, duration: Double, path: String, cover:ByteArray?) {
    this.id = id
    this.title = title
    this.artist = artist
    this.duration = duration
    this.path = path
    this.cover = cover
   }
  }





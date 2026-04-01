package com.example.musicplayer.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

class Song {
    var id: Int = 0
    var title: String = "-"
    var artist: String = "-"
    var path: String = "-"
    var album: String? = "-"
    var duration: Double = 00.00
    var track: Int? = null
    var year: Int? = null
    var cover: ByteArray? = null
    var lyrics: String? = null
    var lyricsFetched: Boolean = false

    @Parcelize
    data class Song(
        val id: Int,
        val title: String?,
        val artist: String?,
        val duration: Double = 00.00,
        val track: Int? = null,
        val path: String?,
        val album: String? = null,
        val year: Int? = null,
    ) : Parcelable

    constructor(id: Int, track: Int?, title: String, artist: String, duration: Double, path: String?, album: String? = null, year: Int) {
        this.id = id
        this.track = track
        this.title = title
        this.artist = artist
        this.duration = duration
        this.path = path?: "-"
        this.album = album
        this.year = year

    }

    constructor(id: Int, title: String, artist: String, duration: Double, path: String, cover:ByteArray?, album: String? = null) {
        this.id = id
        this.title = title
        this.artist = artist
        this.duration = duration
        this.path = path
        this.cover = cover
        this.album = album
    }

    // Backward-compatible convenience constructor used by unit tests and callers
    // that only provide id, title, artist, duration and path (album optional).
    constructor(id: Int, title: String, artist: String, duration: Double, path: String, album: String? = null) {
        this.id = id
        this.title = title
        this.artist = artist
        this.duration = duration
        this.path = path
        this.album = album
    }
}

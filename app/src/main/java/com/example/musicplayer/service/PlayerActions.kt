package com.example.musicplayer.service

object PlayerActions {
    const val ACTION_PLAY = "com.example.musicplayer.music.ACTION_PLAY"
    const val ACTION_PAUSE = "com.example.musicplayer.music.ACTION_PAUSE"
    const val ACTION_STOP = "com.example.musicplayer.music.ACTION_STOP"
    const val ACTION_NEXT = "com.example.musicplayer.music.ACTION_NEXT"
    const val ACTION_PREV = "com.example.musicplayer.music.ACTION_PREV"
    const val ACTION_SEEK = "com.example.musicplayer.music.ACTION_SEEK"
    const val ACTION_UPDATE = "com.example.musicplayer.music.ACTION_UPDATE"
    const val ACTION_PREPARE = "com.example.musicplayer.music.ACTION_PREPARE"

    // Seek actions used by notification buttons
    const val ACTION_SEEK_FORWARD = "com.example.musicplayer.music.ACTION_SEEK_FORWARD"
    const val ACTION_SEEK_BACK = "com.example.musicplayer.music.ACTION_SEEK_BACK"
    const val SEEK_STEP_MS = 15_000L

    const val EXTRA_IS_PLAYING = "com.example.musicplayer.music.EXTRA_IS_PLAYING"
    const val EXTRA_CURRENT_INDEX = "com.example.musicplayer.music.EXTRA_CURRENT_INDEX"
    const val EXTRA_SONG_TITLE = "com.example.musicplayer.music.EXTRA_SONG_TITLE"
    const val EXTRA_SONG_ARTIST = "com.example.musicplayer.music.EXTRA_SONG_ARTIST"
    const val EXTRA_SEEK_POSITION = "com.example.musicplayer.music.EXTRA_SEEK_POSITION"
    const val EXTRA_SUPPRESS_NOTIFICATION = "com.example.musicplayer.music.EXTRA_SUPPRESS_NOTIFICATION"
}

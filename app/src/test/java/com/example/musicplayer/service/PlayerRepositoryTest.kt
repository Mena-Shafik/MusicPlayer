package com.example.musicplayer.service

import com.example.musicplayer.model.Song
import org.junit.Assert.*
import org.junit.Test

class PlayerRepositoryTest {

    private fun sampleSongs(): List<Song> = listOf(
        Song(1, "One", "Artist A", 120.0, "/path/one.mp3"),
        Song(2, "Two", "Artist B", 200.0, "/path/two.mp3"),
        Song(3, "Three", "Artist C", 180.0, "/path/three.mp3")
    )

    @Test
    fun setPlaylist_returnsTrue_whenPlaylistDiffers() {
        val songs = sampleSongs()
        // ensure repository starts empty
        PlayerRepository.setPlaylist(listOf(), 0)

        val changed = PlayerRepository.setPlaylist(songs, 1)
        assertTrue("setPlaylist should return true when changing playlist", changed)
        assertEquals(3, PlayerRepository.playlist.value.size)
        assertEquals(1, PlayerRepository.currentIndex.value)
    }

    @Test
    fun setPlaylist_returnsFalse_whenSamePlaylistAndIndex() {
        val songs = sampleSongs()
        // set playlist first time
        val first = PlayerRepository.setPlaylist(songs, 0)
        assertTrue(first)

        // calling again with identical playlist and index should return false
        val second = PlayerRepository.setPlaylist(songs, 0)
        assertFalse("setPlaylist should return false when playlist and index are identical", second)
    }
}


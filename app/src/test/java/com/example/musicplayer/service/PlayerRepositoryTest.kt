package com.example.musicplayer.service

import com.example.musicplayer.model.Song
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PlayerRepositoryTest {

    @Before
    fun reset() {
        // reset shared singleton state before each test
        PlayerRepository.clearPrepared()
        PlayerRepository.setPlaylist(emptyList(), 0)
        PlayerRepository.setIsPlaying(false)
        PlayerRepository.setPositionMs(0)
        PlayerRepository.setDurationMs(0)
        PlayerRepository.toggleShuffle(false)
        // disable replay if it was enabled
        if (PlayerRepository.replayEnabled.value) PlayerRepository.toggleReplay()
    }

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

    @Test
    fun markPrepared_setsFlagsAndDuration() {
        PlayerRepository.markPrepared(1234L)
        assertTrue(PlayerRepository.isPrepared.value)
        assertEquals(1234L, PlayerRepository.durationMs.value)
        PlayerRepository.clearPrepared()
        assertFalse(PlayerRepository.isPrepared.value)
        assertEquals(0L, PlayerRepository.durationMs.value)
    }

    @Test
    fun setPlaylist_clearsPreparedFlag() {
        PlayerRepository.markPrepared(999L)
        val changed = PlayerRepository.setPlaylist(sampleSongs(), 0)
        assertTrue(changed)
        assertFalse("setPlaylist should clear prepared flag", PlayerRepository.isPrepared.value)
    }

    @Test
    fun nextIndex_respectsShuffleAndReplay() {
        val songs = sampleSongs()
        PlayerRepository.setPlaylist(songs, 0)

        PlayerRepository.toggleShuffle(true)
        val shuffledNext = PlayerRepository.nextIndex()
        assertTrue(shuffledNext in 1 until songs.size)

        // enabling replay should hold current index
        PlayerRepository.toggleShuffle(false)
        PlayerRepository.toggleReplay()
        val replayNext = PlayerRepository.nextIndex()
        assertEquals(PlayerRepository.currentIndex.value, replayNext)
    }

    @Test
    fun prevIndex_usesHistoryWhenShuffled() {
        val songs = sampleSongs()
        PlayerRepository.setPlaylist(songs, 0)
        PlayerRepository.toggleShuffle(true)
        val firstNext = PlayerRepository.nextIndex()
        PlayerRepository.setCurrentIndex(firstNext)
        val prev = PlayerRepository.prevIndex()
        assertEquals(0, prev)
    }
}

package com.example.musicplayer.service

import com.example.musicplayer.model.Song
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PlayerRepositoryTest {

    @Before
    fun reset() {
        // reset shared singleton state before each test
        PlayerStateManager.clearPrepared()
        PlayerStateManager.setPlaylist(emptyList(), 0)
        PlayerStateManager.setIsPlaying(false)
        PlayerStateManager.setPositionMs(0)
        PlayerStateManager.setDurationMs(0)
        PlayerStateManager.toggleShuffle(false)
        // disable replay if it was enabled
        if (PlayerStateManager.replayEnabled.value) PlayerStateManager.toggleReplay()
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
        PlayerStateManager.setPlaylist(listOf(), 0)

        val changed = PlayerStateManager.setPlaylist(songs, 1)
        assertTrue("setPlaylist should return true when changing playlist", changed)
        assertEquals(3, PlayerStateManager.playlist.value.size)
        assertEquals(1, PlayerStateManager.currentIndex.value)
    }

    @Test
    fun setPlaylist_returnsFalse_whenSamePlaylistAndIndex() {
        val songs = sampleSongs()
        // set playlist first time
        val first = PlayerStateManager.setPlaylist(songs, 0)
        assertTrue(first)

        // calling again with identical playlist and index should return false
        val second = PlayerStateManager.setPlaylist(songs, 0)
        assertFalse("setPlaylist should return false when playlist and index are identical", second)
    }

    @Test
    fun markPrepared_setsFlagsAndDuration() {
        PlayerStateManager.markPrepared(1234L)
        assertTrue(PlayerStateManager.isPrepared.value)
        assertEquals(1234L, PlayerStateManager.durationMs.value)
        PlayerStateManager.clearPrepared()
        assertFalse(PlayerStateManager.isPrepared.value)
        assertEquals(0L, PlayerStateManager.durationMs.value)
    }

    @Test
    fun setPlaylist_clearsPreparedFlag() {
        PlayerStateManager.markPrepared(999L)
        val changed = PlayerStateManager.setPlaylist(sampleSongs(), 0)
        assertTrue(changed)
        assertFalse("setPlaylist should clear prepared flag", PlayerStateManager.isPrepared.value)
    }

    @Test
    fun nextIndex_respectsShuffleAndReplay() {
        val songs = sampleSongs()
        PlayerStateManager.setPlaylist(songs, 0)

        PlayerStateManager.toggleShuffle(true)
        val shuffledNext = PlayerStateManager.nextIndex()
        assertTrue(shuffledNext in 1 until songs.size)

        // enabling replay should hold current index
        PlayerStateManager.toggleShuffle(false)
        PlayerStateManager.toggleReplay()
        val replayNext = PlayerStateManager.nextIndex()
        assertEquals(PlayerStateManager.currentIndex.value, replayNext)
    }

    @Test
    fun prevIndex_usesHistoryWhenShuffled() {
        val songs = sampleSongs()
        PlayerStateManager.setPlaylist(songs, 0)
        PlayerStateManager.toggleShuffle(true)
        val firstNext = PlayerStateManager.nextIndex()
        PlayerStateManager.setCurrentIndex(firstNext)
        val prev = PlayerStateManager.prevIndex()
        assertEquals(0, prev)
    }
}

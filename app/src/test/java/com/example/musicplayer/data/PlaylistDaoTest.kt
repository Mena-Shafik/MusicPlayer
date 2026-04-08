package com.example.musicplayer.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class PlaylistDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var songDao: SongDao
    private lateinit var playlistDao: PlaylistDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        songDao = db.songDao()
        playlistDao = db.playlistDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun playlistOrderingIsRespected() = runBlocking {
        val s1 = Song(id = 1, title = "A", artist = "X", album = "Album", albumId = 10, trackNumber = 2, rawTrack = "2", duration = 1000, data = "p1", genre = null)
        val s2 = Song(id = 2, title = "B", artist = "X", album = "Album", albumId = 10, trackNumber = 1, rawTrack = "1", duration = 1000, data = "p2", genre = null)

        songDao.insertSongs(s1, s2)

        val playlistId = playlistDao.insertPlaylist(Playlist(name = "Test", isTemporary = false))
        // Add in custom order (position)
        playlistDao.insertEntry(PlaylistEntry(playlistId = playlistId, songId = s1.id, position = 0))
        playlistDao.insertEntry(PlaylistEntry(playlistId = playlistId, songId = s2.id, position = 1))

        val items = playlistDao.getSongsForPlaylistOrdered(playlistId).first()
        // Should return entries in position order: s1 then s2
        assertEquals(2, items.size)
        assertEquals(s1.id, items[0].songId)
        assertEquals(s2.id, items[1].songId)
    }
}


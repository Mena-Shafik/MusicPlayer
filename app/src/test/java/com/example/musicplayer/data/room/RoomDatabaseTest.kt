package com.example.musicplayer.data.room

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class RoomDatabaseTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: PlaylistDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = db.playlistDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndRetrievePlaylist() = runBlocking {
        val p = PlaylistEntity(name = "Temp", songIds = "1,2,3")
        val id = dao.insert(p)
        val loaded = dao.findById(id)
        assertEquals("Temp", loaded?.name)
        assertEquals("1,2,3", loaded?.songIds)
    }
}


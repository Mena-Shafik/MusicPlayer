package com.example.musicplayer.navidrome

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Fake Subsonic search3 surface for listCatalogueSongsViaApi so pagination/Song-mapping can be tested without a real Retrofit/OkHttp client; pages keyed by songOffset/songCount, mirroring FakeNavidromeApiService in NavidromeRepositoryPaginationTest.
private class FakeSearch3ApiService(private val pages: List<List<SubsonicSong>>) : NavidromeApiService {
    val requestedOffsets = mutableListOf<Int>()

    override suspend fun ping(): SubsonicResponseWrapper =
        SubsonicResponseWrapper(SubsonicResponse("ok", "1.16.1", "navidrome", "0.53.3", null, null, null))

    override suspend fun getAlbumList2(type: String, size: Int, offset: Int): SubsonicResponseWrapper =
        SubsonicResponseWrapper(SubsonicResponse("ok", "1.16.1", "navidrome", "0.53.3", null, null, null))

    override suspend fun search3(query: String, songCount: Int, songOffset: Int, albumCount: Int, artistCount: Int): SubsonicResponseWrapper {
        requestedOffsets.add(songOffset)
        val pageIndex = songOffset / songCount
        val songs = pages.getOrNull(pageIndex) ?: emptyList()
        return SubsonicResponseWrapper(
            SubsonicResponse("ok", "1.16.1", "navidrome", "0.53.3", null, null, SearchResult3(songs))
        )
    }
}

class NavidromeCatalogueSongsTest {

    private fun song(id: String, title: String, durationSeconds: Int = 200, coverArt: String? = "cover-1") =
        SubsonicSong(id = id, title = title, artist = "Artist", album = "Album", track = 1, year = 2020, duration = durationSeconds, coverArt = coverArt)

    @Test
    fun listCatalogueSongsViaApi_mapsFieldsAndMarksRemote() = runBlocking {
        val api = FakeSearch3ApiService(listOf(listOf(song(id = "abc-123", title = "Afterglow", durationSeconds = 238))))

        val songs = listCatalogueSongsViaApi(api, "https://music.example.com", "marla", "hunter2")

        assertEquals(1, songs.size)
        val mapped = songs[0]
        assertEquals("Afterglow", mapped.title)
        assertEquals("Artist", mapped.artist)
        assertTrue(mapped.isRemote)
        assertEquals("abc-123", mapped.remoteId)
        assertEquals(238000.0, mapped.duration, 0.001) // seconds -> ms, matching Song.duration's convention
        assertTrue("path should be a stream.view URL", mapped.path.contains("stream.view"))
        assertNotNull(mapped.remoteCoverArtUrl)
        assertTrue(mapped.remoteCoverArtUrl!!.contains("getCoverArt.view"))
    }

    @Test
    fun listCatalogueSongsViaApi_leavesCoverArtNull_whenServerOmitsIt() = runBlocking {
        val api = FakeSearch3ApiService(listOf(listOf(song(id = "abc-123", title = "Afterglow", coverArt = null))))

        val songs = listCatalogueSongsViaApi(api, "https://music.example.com", "marla", "hunter2")

        assertNull(songs[0].remoteCoverArtUrl)
    }

    @Test
    fun listCatalogueSongsViaApi_skipsSongsMissingAnId() = runBlocking {
        val api = FakeSearch3ApiService(listOf(listOf(
            song(id = "has-id", title = "Kept"),
            SubsonicSong(id = null, title = "Dropped", artist = "Artist", album = null, track = null, year = null, duration = 100, coverArt = null)
        )))

        val songs = listCatalogueSongsViaApi(api, "https://music.example.com", "marla", "hunter2")

        assertEquals(1, songs.size)
        assertEquals("Kept", songs[0].title)
    }

    @Test
    fun listCatalogueSongsViaApi_paginatesAcrossFullPages() = runBlocking {
        // A first page of exactly 500 songs (the page size) must trigger a second fetch.
        val fullPage = (1..500).map { song(id = "song-$it", title = "Track $it") }
        val secondPage = listOf(song(id = "song-501", title = "Track 501"))
        val api = FakeSearch3ApiService(listOf(fullPage, secondPage))

        val songs = listCatalogueSongsViaApi(api, "https://music.example.com", "marla", "hunter2")

        assertEquals(501, songs.size)
        assertEquals(listOf(0, 500), api.requestedOffsets)
    }

    @Test
    fun listCatalogueSongsViaApi_returnsEmpty_whenCatalogueEmpty() = runBlocking {
        val api = FakeSearch3ApiService(listOf(emptyList()))

        val songs = listCatalogueSongsViaApi(api, "https://music.example.com", "marla", "hunter2")

        assertTrue(songs.isEmpty())
    }

    @Test
    fun syntheticRemoteSongId_isAlwaysNegative_soItCantCollideWithLocalMediaStoreIds() {
        // Local ids from Util.getAllAudioFromDevice are always >= 0 (MediaStore _ID), so every remote id staying negative guarantees no collision regardless of hash quality.
        val ids = listOf("abc-123", "", "z", "a-very-long-uuid-like-remote-song-identifier-0001")
            .map { syntheticRemoteSongId(it) }
        ids.forEach { assertTrue(it < 0) }
    }

    @Test
    fun syntheticRemoteSongId_isDeterministicAndDistinctPerId() {
        assertEquals(syntheticRemoteSongId("abc-123"), syntheticRemoteSongId("abc-123"))
        assertNotEquals(syntheticRemoteSongId("abc-123"), syntheticRemoteSongId("xyz-789"))
    }
}

package com.example.musicplayer.navidrome

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

// Fake Subsonic API surface for scanLibraryViaApi so pagination math can be tested without a real Retrofit/OkHttp client; pages keyed by offset/pageSize.
private class FakeNavidromeApiService(private val pages: List<List<SubsonicAlbum>>) : NavidromeApiService {
    val requestedOffsets = mutableListOf<Int>()

    override suspend fun ping(): SubsonicResponseWrapper =
        SubsonicResponseWrapper(SubsonicResponse("ok", "1.16.1", "navidrome", "0.53.3", null, null))

    override suspend fun getAlbumList2(type: String, size: Int, offset: Int): SubsonicResponseWrapper {
        requestedOffsets.add(offset)
        val pageIndex = offset / size
        val albums = pages.getOrNull(pageIndex) ?: emptyList()
        return SubsonicResponseWrapper(
            SubsonicResponse("ok", "1.16.1", "navidrome", "0.53.3", null, AlbumList2(albums))
        )
    }

    override suspend fun search3(query: String, songCount: Int, songOffset: Int, albumCount: Int, artistCount: Int): SubsonicResponseWrapper =
        SubsonicResponseWrapper(SubsonicResponse("ok", "1.16.1", "navidrome", "0.53.3", null, null, null))
}

class NavidromeRepositoryPaginationTest {

    private fun album(songCount: Int) = SubsonicAlbum(id = "id", name = "name", artist = "artist", songCount = songCount)

    @Test
    fun scanLibraryViaApi_sumsSinglePage() = runBlocking {
        val api = FakeNavidromeApiService(listOf(listOf(album(3), album(5), album(2))))

        val result = scanLibraryViaApi(api)

        assertEquals(10, result.songCount)
        assertEquals(3, result.albumCount)
    }

    @Test
    fun scanLibraryViaApi_paginatesAcrossFullPages() = runBlocking {
        // A first page of exactly 500 albums (the page size) must trigger a second fetch -- that's the "did we get a full page, so there might be more" signal the loop relies on.
        val fullPage = (1..500).map { album(1) }
        val secondPage = listOf(album(2), album(3))
        val api = FakeNavidromeApiService(listOf(fullPage, secondPage))

        val result = scanLibraryViaApi(api)

        assertEquals(505, result.songCount)
        assertEquals(502, result.albumCount)
        assertEquals(listOf(0, 500), api.requestedOffsets)
    }

    @Test
    fun scanLibraryViaApi_returnsZero_whenLibraryEmpty() = runBlocking {
        val api = FakeNavidromeApiService(listOf(emptyList()))

        val result = scanLibraryViaApi(api)

        assertEquals(0, result.songCount)
        assertEquals(0, result.albumCount)
    }

    @Test
    fun scanLibraryViaApi_reportsCumulativeProgressPerPage() = runBlocking {
        val api = FakeNavidromeApiService(listOf(listOf(album(4))))
        val progress = mutableListOf<LibraryScanProgress>()

        scanLibraryViaApi(api) { progress.add(it) }

        assertEquals(1, progress.size)
        assertEquals(4, progress[0].songCount)
        assertEquals(1, progress[0].albumCount)
    }

    @Test
    fun scanLibraryViaApi_treatsMissingSongCountAsZero() = runBlocking {
        val api = FakeNavidromeApiService(listOf(listOf(SubsonicAlbum("id", "name", "artist", null))))

        val result = scanLibraryViaApi(api)

        assertEquals(0, result.songCount)
        assertEquals(1, result.albumCount)
    }
}

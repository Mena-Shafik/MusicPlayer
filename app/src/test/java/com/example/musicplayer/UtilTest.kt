package com.example.musicplayer

import com.example.musicplayer.model.RadioStation
import com.example.musicplayer.model.Song
import com.example.musicplayer.util.Util
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UtilTest {

    @Test
    fun extractQuotedOrOriginal_prefersQuoted() {
        assertEquals("CIDC-FM", Util.extractQuotedOrOriginal("Z103.5 \"CIDC-FM\" Live"))
        assertEquals("Nickname", Util.extractQuotedOrOriginal("Some Station 'Nickname' Extra"))
        assertEquals("Plain", Util.extractQuotedOrOriginal("Plain"))
        assertEquals("", Util.extractQuotedOrOriginal(null))
    }

    @Test
    fun formatStation_fallsBackToRaw() {
        val st = RadioStation(stationuuid = "id", name = " Z103.5 \"CIDC\" ", url = null)
        assertEquals("CIDC", Util.formatStation(st))
    }

    @Test
    fun parseTags_handlesQuotesAndDelimiters() {
        val tags = Util.parseTags("rock, \"classic rock\" pop 'alt pop'")
        assertEquals(listOf("rock", "classic rock", "pop", "alt pop"), tags)
        assertTrue(Util.parseTags(null).isEmpty())
    }

    @Test
    fun getStationImageUrl_prefersFaviconThenHost() {
        val withFav = RadioStation("id", "n", "http://example.com/stream", favicon = "http://example.com/logo.png")
        assertTrue(Util.getStationImageUrl(withFav).startsWith("https://example.com"))

        val noFav = RadioStation("id", "n", "http://radio.example.com/stream", favicon = null)
        val url = Util.getStationImageUrl(noFav)
        assertTrue(url.contains("google.com/s2/favicons"))
    }

    @Test
    fun addSpacingToFirstLines_insertsNewlines() {
        val input = "l1\nl2\nl3"
        val spaced = Util.addSpacingToFirstLines(input, firstLines = 2)
        assertEquals("l1\n\nl2\n\nl3", spaced)
    }

    @Test
    fun getRelatedSongs_excludesSinglesAndMatchesAlbum() = runTest {
        val songs = listOf(
            Song(1, "A", "AA", 100.0, "p1", album = "Album X"),
            Song(2, "B", "BB", 100.0, "p2", album = "Album X"),
            Song(3, "C", "CC", 100.0, "p3", album = "Single")
        )
        val related = Util.getRelatedSongs(songs, 0)
        assertEquals(1, related.size)
        assertEquals(1, related.first().first)

        val none = Util.getRelatedSongs(songs, 2)
        assertTrue(none.isEmpty())
    }
}

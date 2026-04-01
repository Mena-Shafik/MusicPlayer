package com.example.musicplayer.util

import com.example.musicplayer.model.RadioStation
import com.example.musicplayer.model.Song
import org.junit.Assert.*
import org.junit.Test

class UtilTest {

    @Test
    fun testExtractQuotedOrOriginal_doubleQuotes() {
        val input = "Z103.5 \"CIDC-FM\" Live"
        val out = Util.extractQuotedOrOriginal(input)
        assertEquals("CIDC-FM", out)
    }

    @Test
    fun testExtractQuotedOrOriginal_singleQuotes() {
        val input = "Some Station 'Nickname' Extra"
        val out = Util.extractQuotedOrOriginal(input)
        assertEquals("Nickname", out)
    }

    @Test
    fun testFormatStation_prefersQuoted() {
        val st = RadioStation(stationuuid = "1", name = "Station \"Cool\" Name", url = null)
        val formatted = Util.formatStation(st)
        assertEquals("Cool", formatted)
    }

    @Test
    fun testConverter_double_minutesSeconds() {
        // 125000 ms -> 2:05
        val out = Util.converter(125000.0)
        assertEquals("2:05", out)
    }

    @Test
    fun testAddSpacingToFirstLines_insertsBlankLines() {
        val lyrics = listOf("line1","line2","line3","line4","line5","line6").joinToString("\n")
        val spaced = Util.addSpacingToFirstLines(lyrics, firstLines = 3)
        // After the first N lines there should be at least one double-newline inserted
        assertNotNull(spaced)
        assertTrue(spaced!!.contains("\n\n"))
        // Ensure original content still present
        assertTrue(spaced.contains("line1"))
        assertTrue(spaced.contains("line6"))
    }

    @Test
    fun testParseTags_keepsQuotedGroups() {
        val raw = "rock pop \"classic rock\",electronic"
        val parsed = Util.parseTags(raw)
        assertTrue(parsed.contains("classic rock"))
        assertTrue(parsed.contains("rock"))
        assertTrue(parsed.contains("electronic"))
    }

    @Test
    fun testFormatSongRow_prefersRawTrack() {
        val song = Song(42, 5, "My Song", "The Artist", 180000.0, "/music/mysong.mp3", "The Album", 2020)

        val rowWithRaw = Util.formatSongRow(song, rawTrack = 3)
        assertTrue(rowWithRaw.contains("3"))

        val rowWithoutRaw = Util.formatSongRow(song, rawTrack = null)
        assertTrue(rowWithoutRaw.contains("5"))
    }

}



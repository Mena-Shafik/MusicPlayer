package com.example.musicplayer.navidrome

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class NavidromeAuthTest {

    @Test
    fun md5Hex_matchesKnownVector() {
        // Standard MD5 test vector: md5("") = d41d8cd98f00b204e9800998ecf8427e
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", NavidromeApiService.md5Hex(""))
        // md5("abc") = 900150983cd24fb0d6963f7d28e17f72
        assertEquals("900150983cd24fb0d6963f7d28e17f72", NavidromeApiService.md5Hex("abc"))
    }

    @Test
    fun md5Hex_isLowercaseHexOfCorrectLength() {
        val hash = NavidromeApiService.md5Hex("password+somesalt")
        assertEquals(32, hash.length)
        assertEquals(hash.lowercase(), hash)
    }

    @Test
    fun subsonicToken_differsPerSalt() {
        // Same password, different salts must produce different tokens -- otherwise the salt would be pointless and the same token could be replayed.
        val tokenA = NavidromeApiService.md5Hex("hunter2" + "salt1")
        val tokenB = NavidromeApiService.md5Hex("hunter2" + "salt2")
        assertNotEquals(tokenA, tokenB)
    }
}

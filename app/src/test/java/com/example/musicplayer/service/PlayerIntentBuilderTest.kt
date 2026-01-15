package com.example.musicplayer.service

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PlayerIntentBuilderTest {

    @Before
    fun setup() {
        // Mock Log to prevent "not mocked" errors
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
    }

    @Test
    fun placeholder_runs() {
        assertTrue(true)
    }

    @Test
    fun playerActionsConstantsDefined() {
        // Verify that PlayerActions constants exist
        assertNotNull(PlayerActions.ACTION_PREPARE)
        assertNotNull(PlayerActions.ACTION_SEEK)
        assertNotNull(PlayerActions.ACTION_PLAY)
        assertNotNull(PlayerActions.EXTRA_CURRENT_INDEX)
        assertNotNull(PlayerActions.EXTRA_SEEK_POSITION)
        assertNotNull(PlayerActions.EXTRA_IS_PLAYING)
    }

    @Test
    fun playerActionsConstantsHaveValues() {
        // Verify that PlayerActions constants have actual string values
        assertTrue(PlayerActions.ACTION_PREPARE.isNotBlank())
        assertTrue(PlayerActions.ACTION_SEEK.isNotBlank())
        assertTrue(PlayerActions.ACTION_PLAY.isNotBlank())
        assertTrue(PlayerActions.EXTRA_CURRENT_INDEX.isNotBlank())
        assertTrue(PlayerActions.EXTRA_SEEK_POSITION.isNotBlank())
        assertTrue(PlayerActions.EXTRA_IS_PLAYING.isNotBlank())
    }
}

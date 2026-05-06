package com.example.musicplayer

import org.junit.Assert.assertFalse
import org.junit.Test

class VersionTest {
    @Test
    fun buildConfig_hasVersionName() {
        // BuildConfig is generated at compile time; ensure version is exposed
        val v = BuildConfig.APP_VERSION_NAME
        // BuildConfig.APP_VERSION_NAME is a non-null String generated at compile time
        assertFalse("APP_VERSION_NAME should not be empty", v.isBlank())
    }
}



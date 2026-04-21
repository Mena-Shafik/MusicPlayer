package com.example.musicplayer

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.screenshot.Screenshot
import com.example.musicplayer.preferences.PreferencesManager
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import androidx.activity.compose.setContent
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import com.example.musicplayer.model.Song
import com.example.musicplayer.service.PlayerStateManager

@RunWith(AndroidJUnit4::class)
class ShotExampleTest {

	@get:Rule
	val activityRule = ActivityScenarioRule(MainActivity::class.java)

	private fun saveBitmapToDevice(bitmap: android.graphics.Bitmap, name: String) {
		val outDir = File("/sdcard/shot-screenshots")
		if (!outDir.exists()) outDir.mkdirs()
		val outFile = File(outDir, name)
		FileOutputStream(outFile).use { fos ->
			bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, fos)
		}
	}

	@Test
	fun captureHomeScreen() {
		// Give UI a moment to settle
		Thread.sleep(800)
		val capture = Screenshot.capture()
		saveBitmapToDevice(capture.bitmap, "home.png")
	}

	@Test
	fun captureAlbumView() {
		// Set preference directly so the UI will present album grouping on next recreate
		val ctx = InstrumentationRegistry.getInstrumentation().targetContext
		runBlocking { PreferencesManager.setAlbumView(ctx, true) }

		// Recreate activity to pick up the preference change
		activityRule.scenario.recreate()
		Thread.sleep(800)

		val capture = Screenshot.capture()
		saveBitmapToDevice(capture.bitmap, "album.png")
	}

	@Test
	fun captureArtistView() {
		val ctx = InstrumentationRegistry.getInstrumentation().targetContext
		runBlocking { PreferencesManager.setArtistView(ctx, true) }

		activityRule.scenario.recreate()
		Thread.sleep(800)

		val capture = Screenshot.capture()
		saveBitmapToDevice(capture.bitmap, "artist.png")
	}

	@Test
	fun captureEraView() {
		val ctx = InstrumentationRegistry.getInstrumentation().targetContext
		runBlocking { PreferencesManager.setEraView(ctx, true) }

		activityRule.scenario.recreate()
		Thread.sleep(800)

		val capture = Screenshot.capture()
		saveBitmapToDevice(capture.bitmap, "era.png")
	}

	@Test
	fun capturePlayerScreen() {
		// Create sample songs and set playlist directly on the PlayerStateManager
		activityRule.scenario.onActivity { activity ->
			val sampleSongs = listOf(
				Song(0, null, "First Song", "Artist A", 180000.0, "", "Album X", 2020),
				Song(1, null, "Second Song", "Artist B", 200000.0, "", "Album Y", 2019),
				Song(2, null, "Third Song", "Artist C", 240000.0, "", "Album Z", 2018)
			)

			// Set the playlist in the global PlayerStateManager so the MusicPlayerScreen reflects it
			PlayerStateManager.setPlaylist(sampleSongs, 0)
			PlayerStateManager.setIsPlaying(true)

			// Replace activity content with the MusicPlayerScreen composable showing songId=0
			activity.setContent {
				androidx.compose.material.MaterialTheme {
					val navController = androidx.navigation.NavController(activity)
					com.example.musicplayer.music.MusicPlayerScreen(songId = 0, songs = sampleSongs, navController = navController)
				}
			}
		}

		// Give Compose some time to render
		Thread.sleep(800)

		val capture = Screenshot.capture()
		saveBitmapToDevice(capture.bitmap, "player.png")
	}
}






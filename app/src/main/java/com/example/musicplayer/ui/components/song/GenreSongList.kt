package com.example.musicplayer.ui.components.song

// ...existing code...
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.Shader
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.Surface
import android.content.res.Configuration
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import com.example.musicplayer.model.Song
import androidx.compose.foundation.layout.Box
import com.example.musicplayer.ui.components.common.MainBackground

/**
 * Group songs by genre and display sections. Genre names are normalized with a fallback.
 */
@Composable
fun GenreSongList(
	songs: List<Song>,
	modifier: Modifier = Modifier,
	onSongClick: (Song) -> Unit = {}
) {
	val grouped = remember(songs) {
		songs.groupBy { (it.genre?.trim()?.ifBlank { null } ?: "Unknown Genre") }
			.toSortedMap(String.CASE_INSENSITIVE_ORDER)
	}

	Column(modifier = modifier.fillMaxSize()) {
		LazyColumn(modifier = Modifier.fillMaxWidth()) {
			grouped.forEach { (genre, list) ->
				item {
					// Header shows genre name and count on a subtle surface so it stands out
					// Frosted header: blur the background behind the header and overlay a translucent tint
					// ensure header only occupies its content height to avoid overlapping list items
					Box(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
						// Backdrop blur + translucent overlay to create a frosted-glass effect
						Box(
							modifier = Modifier
								.fillMaxSize()
								.graphicsLayer {
									// use Android RenderEffect for a reliable blur on supported devices
									renderEffect = AndroidRenderEffect
										.createBlurEffect(20f, 20f, Shader.TileMode.CLAMP)
										.asComposeRenderEffect()
									// ensure blur is clipped to this header's bounds to avoid overlapping list items
									clip = true
								}
								// white translucent overlay to achieve a frosted-glass look consistent with PlaylistCard
								.background(Color.White.copy(alpha = 0.10f))
						)

						Row(modifier = Modifier
							.fillMaxWidth()
							.padding(horizontal = 12.dp, vertical = 8.dp)) {
							Text(
								text = genre,
								style = MaterialTheme.typography.titleMedium,
								color = Color.White
							)
							Spacer(modifier = Modifier.width(8.dp))
							Text(
								text = "(${list.size})",
								style = MaterialTheme.typography.bodySmall,
								color = Color.Gray
							)
						}
					}
					HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
				}
				itemsIndexed(list, key = { _, s -> s.id }) { _, song ->
					SongCardRow(
						song = song,
						onClick = { onSongClick(song) }
					)
				}
			}
		}
	}
}


/** Preview for the grouped genre list. Uses a small set of sample songs to show headers and rows. */
@Preview(showBackground = true)
@Composable
fun GenreSongListPreview() {
	// Use the simple convenience constructor and set genre after construction so the preview
	// works regardless of which Song constructor overload is selected by the compiler.
	val s1 = Song(1, "Aurora", "Artist A", 180.0, "", "Album X")
	s1.genre = "Ambient"
	val s2 = Song(2, "Borealis", "Artist B", 200.0, "", "Album Y")
	s2.genre = "Ambient"
	val s3 = Song(3, "Drift", "Artist C", 240.0, "", "Album Z")
	s3.genre = "Electronic"
	val s4 = Song(4, "Pulse", "Artist D", 210.0, "", "Album Z")
	s4.genre = null

	val sampleSongs = listOf(s1, s2, s3, s4)

	MaterialTheme {
		// Use the project's shared background and render the list on top of it
		Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
			Box(modifier = Modifier.fillMaxSize()) {
				MainBackground()
				GenreSongList(songs = sampleSongs, modifier = Modifier.fillMaxSize())
			}
		}
	}
}


@Preview(name = "Dark Preview", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun GenreSongListDarkPreview() {
	// reuse the same preview content but show dark mode
	GenreSongListPreview()
}



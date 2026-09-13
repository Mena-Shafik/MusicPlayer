package com.example.musicplayer.ui.components.player

import android.util.Log
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import com.example.musicplayer.R
import com.example.musicplayer.model.Song
import com.example.musicplayer.util.Util
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AlbumImage(
    song: Song,
    modifier: Modifier = Modifier,
    onDominantColor: (Color) -> Unit = {},
    onAccentColor: (Color) -> Unit = {},
    onBitmap: (android.graphics.Bitmap?) -> Unit = {}
) {
    val context = LocalContext.current
    var displayBitmap by remember(song.path) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(song.path) {
        displayBitmap = null
        if (song.path.isNotBlank()) {
            displayBitmap = withContext(Dispatchers.IO) {
                try {
                    // First try embedded album art
                    var bitmap = Util.getAlbumArt(context, song.path)

                    // If no embedded art, fetch from web
                    if (bitmap == null) {
                        Log.d("AlbumImage", "No embedded artwork for '${song.title}', fetching from web...")
                        val webUrl = Util.getAlbumArtWebUrl(song)
                        if (webUrl != null) {
                            bitmap = Util.loadBitmapFromUrl(webUrl)
                            if (bitmap != null) {
                                Log.d("AlbumImage", "✓ Loaded web album art for '${song.title}'")
                            }
                        }
                    }
                    bitmap
                } catch (_: Throwable) {
                    null
                }
            }
        }
    }

    // Size comes entirely from the caller's `modifier` now -- PersistentPlayerHost lerps it between the mini bar's 44dp thumb and the full player's 340dp cover -- rather than being fixed here.
    val imageModifier = modifier
        .shadow(
            elevation = 24.dp,
            shape = RoundedCornerShape(8.dp),
            ambientColor = Color.Black.copy(alpha = 0.55f),
            spotColor = Color.Black.copy(alpha = 0.55f)
        )
        .clip(RoundedCornerShape(8.dp))

    Crossfade(targetState = displayBitmap, animationSpec = tween(500), label = "Album art crossfade") { bitmap ->
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Album Art",
                // Crop rather than the default Fit -- most covers are square but the half-bleed art rect (360x448) isn't, so Fit would letterbox it instead of filling edge to edge.
                contentScale = ContentScale.Crop,
                modifier = imageModifier
            )

            LaunchedEffect(bitmap) {
                val (dominantInt, accentInt) = withContext(Dispatchers.Default) {
                    try {
                        val palette = Palette.from(bitmap.asAndroidBitmap()).generate()
                        val dominant = palette.getDominantColor(android.graphics.Color.BLACK)
                        // prefer vibrant swatch, fallback to dominant
                        val accent = palette.vibrantSwatch?.rgb ?: palette.mutedSwatch?.rgb ?: dominant
                        Pair(dominant, accent)
                    } catch (_: Throwable) {
                        Pair(android.graphics.Color.BLACK, android.graphics.Color.WHITE)
                    }
                }
                onDominantColor(Color(dominantInt))
                onAccentColor(Color(accentInt))
                // forward the loaded android Bitmap to caller for background sampling
                try { onBitmap(bitmap.asAndroidBitmap()) } catch (_: Throwable) { onBitmap(null) }
            }
        } else {
            Box(
                modifier = imageModifier.background(Color(0xFF2A2A2A)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Album,
                    contentDescription = "Album Art",
                    modifier = Modifier.size(100.dp),
                    tint = Color(0xFF5A5A5A)
                )
            }
            // no bitmap available — inform caller
            LaunchedEffect(Unit) { onBitmap(null) }
        }
    }
}

@Suppress("unused")
@Composable
fun SmallAlbumImage(path: String?, size: Dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val imageBitmap = try {
        Util.getAlbumArt(context, path)
    } catch (_: Throwable) { null }

    Crossfade(targetState = imageBitmap, animationSpec = tween(500), label = "Small album art crossfade") { bitmap ->
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = modifier.size(size),
                contentScale = ContentScale.Crop
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.img),
                contentDescription = null,
                modifier = modifier.size(size),
                contentScale = ContentScale.Crop
            )
        }
    }
}

package com.example.musicplayer.ui.components.player

import android.graphics.Bitmap
import android.content.Context
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

// Loads a song's cover (embedded, then web), keeping the previous cover until the new one is ready so track changes crossfade instead of flashing the placeholder.
@Composable
fun rememberAlbumBitmap(song: Song?): ImageBitmap? {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(song?.path) {
        bitmap = if (song == null || song.path.isBlank()) null else withContext(Dispatchers.IO) { loadAlbumBitmap(context, song) }
    }
    return bitmap
}

private suspend fun loadAlbumBitmap(context: Context, song: Song): ImageBitmap? = try {
    var bitmap = Util.getAlbumArt(context, song.path)
    if (bitmap == null) {
        Log.d("AlbumImage", "No embedded artwork for '${song.title}', fetching from web...")
        val webUrl = Util.getAlbumArtWebUrl(song)
        if (webUrl != null) {
            bitmap = Util.loadBitmapFromUrl(webUrl)
            if (bitmap != null) Log.d("AlbumImage", "✓ Loaded web album art for '${song.title}'")
        }
    }
    bitmap
} catch (_: Throwable) {
    null
}

// Dominant and accent (vibrant, else muted, else dominant) colors, computed off the main thread.
suspend fun albumPaletteColors(bitmap: ImageBitmap): Pair<Color, Color> = withContext(Dispatchers.Default) {
    try {
        val palette = Palette.from(bitmap.asAndroidBitmap()).generate()
        val dominant = palette.getDominantColor(android.graphics.Color.BLACK)
        val accent = palette.vibrantSwatch?.rgb ?: palette.mutedSwatch?.rgb ?: dominant
        Pair(Color(dominant), Color(accent))
    } catch (_: Throwable) {
        Pair(Color.Black, Color.White)
    }
}

// Draws an already-loaded cover (or the placeholder); size comes entirely from `modifier`, and it only crossfades when the bitmap changes, never on first composition.
@Composable
fun AlbumArt(bitmap: ImageBitmap?, modifier: Modifier = Modifier) {
    val imageModifier = modifier
        .shadow(
            elevation = 24.dp,
            shape = RoundedCornerShape(8.dp),
            ambientColor = Color.Black.copy(alpha = 0.55f),
            spotColor = Color.Black.copy(alpha = 0.55f)
        )
        .clip(RoundedCornerShape(8.dp))

    Crossfade(targetState = bitmap, animationSpec = tween(500), label = "Album art crossfade") { b ->
        if (b != null) {
            Image(
                bitmap = b,
                contentDescription = "Album Art",
                // Crop, since the half-bleed art rect isn't square and Fit would letterbox it.
                contentScale = ContentScale.Crop,
                modifier = imageModifier
            )
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
        }
    }
}

@Composable
fun AlbumImage(
    song: Song,
    modifier: Modifier = Modifier,
    onDominantColor: (Color) -> Unit = {},
    onAccentColor: (Color) -> Unit = {},
    onBitmap: (Bitmap?) -> Unit = {}
) {
    val bitmap = rememberAlbumBitmap(song)
    AlbumArt(bitmap = bitmap, modifier = modifier)
    LaunchedEffect(bitmap) {
        if (bitmap == null) {
            onBitmap(null)
            return@LaunchedEffect
        }
        val (dominant, accent) = albumPaletteColors(bitmap)
        onDominantColor(dominant)
        onAccentColor(accent)
        try { onBitmap(bitmap.asAndroidBitmap()) } catch (_: Throwable) { onBitmap(null) }
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

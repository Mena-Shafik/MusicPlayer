package com.example.musicplayer.composable

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.musicplayer.R
import com.example.musicplayer.Util
import com.example.musicplayer.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SongCardRow(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var imageBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

    LaunchedEffect(song.path) {
        imageBitmap = null  // reset when song changes
        if (song.path.isNotBlank()) {
            imageBitmap = withContext(Dispatchers.IO) {
                try {
                    Util.getAlbumArt(context, song.path)
                } catch (_: Throwable) {
                    null
                }
            }
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            //.background(Color.Black) // removed so list items are semi-transparent over the background
            .clickable(onClick = onClick)
            .padding(10.dp,0.dp,0.dp,0.dp)
    ) {
        val imgModifier = Modifier
            .width(60.dp)
            .height(60.dp)
            .clip(RoundedCornerShape(2.dp))

        Crossfade(
            targetState = imageBitmap,
            animationSpec = tween(500),
            label = "Album art crossfade"
        ) { bitmap ->
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "Album art",
                    contentScale = ContentScale.Crop,
                    modifier = imgModifier
                )
            } else {
                // Show placeholder while loading or if no album art found
                Image(
                    painter = painterResource(id = R.drawable.ic_album),
                    contentDescription = "Placeholder image",
                    contentScale = ContentScale.Crop,
                    modifier = imgModifier
                )
            }
        }

        Column(Modifier
            .padding(start = 12.dp)
            .weight(1f)) {
            Text(
                text = song.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = song.artist,
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 1
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = Util.converter(song.duration),
                Modifier
                    .width(80.dp)
                    .padding(10.dp),
                color = Color.White,
                textAlign = TextAlign.End,

                )
        }
    }
}

@Preview( name = "SongCardRow Preview", backgroundColor = 0xFF000000)
@Composable
fun CardPreview() {
    // Use the real SongCardRow so preview shows the same UI and placeholder logic
    MaterialTheme {
        Surface(color = Color.Black) {
            SongCardRow(
                song = Song(id = 1, title = "Title", artist = "Artist", duration = 260000.0, path = ""),
                onClick = {}
            )
        }
    }
}
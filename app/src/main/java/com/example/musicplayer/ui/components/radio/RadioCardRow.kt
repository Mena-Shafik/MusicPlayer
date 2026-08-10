package com.example.musicplayer.ui.components.radio

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.musicplayer.R
import com.example.musicplayer.util.Util
import com.example.musicplayer.model.RadioStation

@Composable
fun RadioCardRow(
    station: RadioStation,
    displayName: String = Util.extractQuotedOrOriginal(station.name).ifBlank { station.name ?: "Unknown" },
    modifier: Modifier = Modifier,
    onPlay: () -> Unit
) {
    val context = LocalContext.current
    val imageUrl = Util.getStationImageUrl(station).ifBlank { null }
    // One comma-set tagline instead of a row of pill chips — unreadable at chip size and
    // clipped on long names.
    val tagline = Util.parseTags(station.tags).joinToString(", ")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .clickable(onClick = onPlay)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageUrl)
                .crossfade(500)
                .build(),
            contentDescription = displayName,
            modifier = Modifier
                .width(52.dp)
                .height(52.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(id = R.drawable.ic_radio),
            error = painterResource(id = R.drawable.ic_radio)
        )

        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f)
        ) {
            Text(
                text = displayName,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (tagline.isNotBlank()) {
                Text(
                    text = tagline,
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, name = "RadioCardRow Preview")
@Composable
fun RadioCardRowPreview() {
    MaterialTheme {
        RadioCardRow(
            station = RadioStation(
                stationuuid = "sample-uuid",
                name = "Preview Radio",
                url = "https://example.com/stream",
                favicon = "https://example.com/logo.png",
                country = "USA",
                tags = "pop,rock",
                bitrate = 128,
                codec = "mp3"
            ),
            onPlay = {}
        )
    }
}

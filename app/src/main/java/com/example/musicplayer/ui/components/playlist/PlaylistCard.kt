package com.example.musicplayer.ui.components.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.example.musicplayer.model.Playlist

@Composable
fun PlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .width(64.dp)
                .height(64.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF2A2A2A)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = null,
                tint = Color(0xFF5A5A5A),
                modifier = Modifier.size(30.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                lineHeight = 22.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (playlist.description.isNotBlank()) {
                Text(
                    text = playlist.description,
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Text(
                text = "${playlist.songIds.size} songs",
                color = Color(0xFFFFA500),
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Outlined.DeleteOutline,
                contentDescription = "Delete Playlist",
                tint = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}

// backgroundColor/showBackground are required here — without them this white-text-on-dark
// card previews invisibly on Android Studio's default white preview canvas.
@Preview(name = "PlaylistCard sample", showBackground = true, backgroundColor = 0xFF03050C)
@Composable
private fun PlaylistCardPreview() {
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PlaylistCard(
            playlist = Playlist(
                id = 1,
                name = "Favorites",
                description = "Hand-picked tracks",
                songIds = listOf(1, 2, 3, 4)
            ),
            onClick = {},
            onDelete = {}
        )
        PlaylistCard(
            playlist = Playlist(
                id = 2,
                name = "Late drives",
                description = "",
                songIds = emptyList()
            ),
            onClick = {},
            onDelete = {},
        )
    }
}

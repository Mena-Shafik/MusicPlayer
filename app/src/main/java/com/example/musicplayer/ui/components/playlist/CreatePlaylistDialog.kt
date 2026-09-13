package com.example.musicplayer.ui.components.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Filled, bordered text field with an external uppercase-eyebrow label -- matches the redesign's field language (no Material floating label/indicator line).
@Composable
private fun SheetTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (value.isEmpty()) {
            Text(text = placeholder, color = Color.White.copy(alpha = 0.4f), fontSize = 15.sp)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium),
            cursorBrush = SolidColor(Color(0xFFFFA500)),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePlaylistDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onCreatePlaylist: (name: String, description: String) -> Unit
) {
    var playlistName by remember { mutableStateOf("") }
    var playlistDescription by remember { mutableStateOf("") }

    if (showDialog) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        fun reset() {
            playlistName = ""
            playlistDescription = ""
        }

        ModalBottomSheet(
            onDismissRequest = {
                onDismiss()
                reset()
            },
            sheetState = sheetState,
            containerColor = Color(0xFF12141C),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            scrimColor = Color(0xFF03050C).copy(alpha = 0.72f),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 18.dp)
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.22f))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
            ) {
                Text(text = "New playlist", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Text(
                    text = "Give it a name now — you can add songs straight after.",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )

                Column(modifier = Modifier.padding(top = 20.dp)) {
                    Text(
                        text = "NAME",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.2.sp
                    )
                    SheetTextField(
                        value = playlistName,
                        onValueChange = { playlistName = it },
                        placeholder = "",
                        borderColor = Color(0xFFFFA500),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Column(modifier = Modifier.padding(top = 14.dp)) {
                    Row {
                        Text(
                            text = "DESCRIPTION",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = " optional",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                    SheetTextField(
                        value = playlistDescription,
                        onValueChange = { playlistDescription = it },
                        placeholder = "What is it for?",
                        borderColor = Color.White.copy(alpha = 0.12f),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 22.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(104.dp)
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .border(1.5.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(24.dp))
                            .clickable {
                                onDismiss()
                                reset()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "Cancel", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(if (playlistName.isNotBlank()) Color.White else Color.White.copy(alpha = 0.3f))
                            .clickable(enabled = playlistName.isNotBlank()) {
                                onCreatePlaylist(playlistName, playlistDescription)
                                reset()
                            }
                    ) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = null, tint = Color(0xFF111111))
                        Text(
                            text = "Create",
                            color = Color(0xFF111111),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF03050C, name = "CreatePlaylistDialog Preview")
@Composable
fun CreatePlaylistDialogPreview() {
    MaterialTheme {
        CreatePlaylistDialog(
            showDialog = true,
            onDismiss = { },
            onCreatePlaylist = { _, _ -> }
        )
    }
}

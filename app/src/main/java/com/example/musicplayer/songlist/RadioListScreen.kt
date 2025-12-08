package com.example.musicplayer.songlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.style.TextAlign
import com.example.musicplayer.R
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.ui.unit.sp
import com.example.musicplayer.model.RadioStation
import com.example.musicplayer.radio.RadioPlayerViewModel
import coil.compose.AsyncImage
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplayRadioList(radioViewModel: RadioPlayerViewModel) {
    // observe stations from the RadioPlayerViewModel (comes from repository / API)
    val stations by radioViewModel.stations.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Radio Stations") }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            ImageGrid2x2(
                stations = stations,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                onStationClick = { idx ->
                    // placeholder: handle station click (play/select) here
                    // currently a no-op to keep behavior safe
                }
            )
        }
    }
}

@Composable
fun ImageGrid2x2(
    stations: List<RadioStation>,
    modifier: Modifier = Modifier,
    spacing: Dp = 4.dp,
    cellCorner: Dp = 8.dp,
    onStationClick: (index: Int) -> Unit = {}
) {
    // normalize to 4 slots
    val normalized: List<RadioStation?> = (stations.take(4) + List(maxOf(0, 4 - stations.size)) { null }).take(4)

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier,
        contentPadding = PaddingValues(spacing),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        itemsIndexed(normalized) { idx: Int, station: RadioStation? ->
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(cellCorner))
                    .background(Color.LightGray.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                StationImageButton(
                    station = station,
                    placeholderRes = R.drawable.radio_z103_5,
                    contentDescription = station?.name ?: "grid image $idx",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onClick = { onStationClick(idx) }
                )

                if (station == null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.25f))
                            .padding(vertical = 6.dp)
                    ) {
                        Text(
                            text = "Empty",
                            fontSize = 12.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StationImageButton(
    station: RadioStation?,
    @androidx.annotation.DrawableRes placeholderRes: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    onClick: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (station?.favicon != null && station.favicon.isNotBlank()) {
            // load remote favicon with Coil; fall back to placeholder if load fails
            AsyncImage(
                model = station.favicon,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                placeholder = painterResource(id = placeholderRes),
                error = painterResource(id = placeholderRes)
            )
        } else {
            Image(
                painter = painterResource(id = placeholderRes),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 320, heightDp = 320, backgroundColor = 0xFF000000 )
@Composable
private fun ImageGrid2x2Preview_Placeholders() {
    ImageGrid2x2(stations = emptyList(), modifier = Modifier.padding(12.dp))
}

@Preview(showBackground = true, widthDp = 320, heightDp = 320, backgroundColor = 0xFF000000)
@Composable
private fun ImageGrid2x2Preview_Mixed() {
    ImageGrid2x2(stations = listOf(null, null, null, null).mapNotNull { it }, modifier = Modifier.padding(12.dp))
}

@Preview(showBackground = true, widthDp = 360, heightDp = 480, backgroundColor = 0xFF000000)
@Composable
private fun ImageGrid2x2InteractivePreview() {
    var selected by remember { mutableStateOf(-1) }
    Column(modifier = Modifier.padding(12.dp)) {
        // sample fake stations for preview
        val sample = listOf(
            RadioStation("1", "One", "", favicon = ""),
            RadioStation("2", "Two", "", favicon = ""),
            RadioStation("3", "Three", "", favicon = ""),
            RadioStation("4", "Four", "", favicon = "")
        )
        ImageGrid2x2(stations = sample, modifier = Modifier.fillMaxWidth(), onStationClick = { idx -> selected = idx })
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = if (selected >= 0) "Clicked: $selected" else "Click a cell", color = Color.White)
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun RadioListScreenPreview() {
    // keep preview simple: show a static grid rather than invoking network ViewModel
    ImageGrid2x2InteractivePreview()
}

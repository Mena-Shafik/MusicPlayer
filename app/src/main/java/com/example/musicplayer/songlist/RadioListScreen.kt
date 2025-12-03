package com.example.musicplayer.songlist

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
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
import com.example.musicplayer.songlist.RadioListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadioListScreen(viewModel: RadioListViewModel) {
    val images by viewModel.images.collectAsState(initial = emptyList<ImageBitmap?>())

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
                images = images,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                onImageClick = { idx ->
                    // use idx to avoid unused-parameter warning; update ViewModel slot (no-op)
                    viewModel.updateAt(idx, images.getOrNull(idx))
                }
            )
        }
    }
}

@Composable
fun ImageGrid2x2(
    images: List<ImageBitmap?>,
    modifier: Modifier = Modifier,
    spacing: Dp = 4.dp,
    cellCorner: Dp = 8.dp,
    onImageClick: (index: Int) -> Unit = {}
) {
    val normalized: List<ImageBitmap?> =
        (images.take(4) + List(maxOf(0, 4 - images.size)) { null }).take(4)

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier,
        contentPadding = PaddingValues(spacing),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        itemsIndexed(normalized) { idx: Int, bitmap: ImageBitmap? ->
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(cellCorner))
                    .background(Color.LightGray.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                GridImageButton(
                    bitmap = bitmap,
                    placeholderRes = R.drawable.radio_z103_5,
                    contentDescription = "grid image $idx",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onClick = { onImageClick(idx) }
                )

                if (bitmap == null) {
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
fun GridImageButton(
    bitmap: ImageBitmap?,
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
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
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
    ImageGrid2x2(images = emptyList(), modifier = Modifier.padding(12.dp))
}

@Preview(showBackground = true, widthDp = 320, heightDp = 320, backgroundColor = 0xFF000000)
@Composable
private fun ImageGrid2x2Preview_Mixed() {
    ImageGrid2x2(images = listOf(null, null, null, null), modifier = Modifier.padding(12.dp))
}

@Preview(showBackground = true, widthDp = 360, heightDp = 480, backgroundColor = 0xFF000000)
@Composable
private fun ImageGrid2x2InteractivePreview() {
    var selected by remember { mutableStateOf(-1) }
    Column(modifier = Modifier.padding(12.dp)) {
        ImageGrid2x2(images = listOf(null, null, null, null), modifier = Modifier.fillMaxWidth(), onImageClick = { idx -> selected = idx })
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = if (selected >= 0) "Clicked: $selected" else "Click a cell", color = Color.White)
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun RadioListScreenPreview() {
    val vm = remember { RadioListViewModel() }
    // keep preview simple: ViewModel starts with placeholder (null) slots and ImageGrid2x2 will show drawable placeholders
    RadioListScreen(viewModel = vm)
}

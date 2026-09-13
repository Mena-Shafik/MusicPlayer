package com.example.musicplayer.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import com.example.musicplayer.BuildConfig
import com.example.musicplayer.navigation.NavRoutes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.musicplayer.songlist.SongListViewModel
import com.example.musicplayer.ui.components.background.AuroraRibbonBackground
import com.example.musicplayer.ui.components.background.AuroraRibbonPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavHostController) {
    val context = LocalContext.current
    val viewModel: SongListViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SongListViewModel(context = context) as T
            }
        }
    )

    val isAlbumView by viewModel.isAlbumView.collectAsState(initial = false)
    val isArtistView by viewModel.isArtistView.collectAsState(initial = false)
    val isEraView by viewModel.isEraView.collectAsState(initial = false)
    val useDefaultRadioList by viewModel.useDefaultRadioList.collectAsState(initial = true)
    val useAuroraBackground by viewModel.useAuroraBackground.collectAsState(initial = false)
    val auroraPalette by viewModel.auroraPalette.collectAsState(initial = "Northern")
    val useAlbumPalette by viewModel.useAlbumPalette.collectAsState(initial = false)
    var showPaletteDialog by remember { mutableStateOf(false) }

    val navidromeConnected by com.example.musicplayer.preferences.PreferencesManager
        .getNavidromeConnectedFlow(context)
        .collectAsState(initial = false)
    val navidromeStatus = remember(navidromeConnected) {
        if (navidromeConnected) {
            com.example.musicplayer.navidrome.NavidromeCredentialsStore.get(context)?.serverUrl ?: "Connected"
        } else {
            "Not connected"
        }
    }

    // Background drawn full-screen behind the whole Scaffold (including the top bar), not just the content area below it -- otherwise a "transparent" bar just shows the Scaffold's own flat containerColor instead of this blur.
    Box(modifier = Modifier.fillMaxSize()) {
    AuroraRibbonBackground()
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Settings", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SettingsGroup(title = "SONGS") {
                    SettingsSwitchRow(
                        title = "Sort by album",
                        checked = isAlbumView,
                        onCheckedChange = { enabled -> viewModel.setAlbumView(enabled) }
                    )
                    SettingsDivider()
                    SettingsSwitchRow(
                        title = "Sort by artist",
                        checked = isArtistView,
                        onCheckedChange = { enabled -> viewModel.setArtistView(enabled) }
                    )
                    SettingsDivider()
                    SettingsSwitchRow(
                        title = "Show by eras",
                        checked = isEraView,
                        onCheckedChange = { enabled -> viewModel.setEraView(enabled) }
                    )
                }

                SettingsGroup(title = "UI") {
                    SettingsSwitchRow(
                        title = "Aurora background",
                        checked = useAuroraBackground,
                        onCheckedChange = { enabled -> viewModel.setUseAuroraBackground(enabled) }
                    )
                    SettingsDivider()
                    SettingsValueRow(
                        title = "Aurora palette",
                        value = auroraPalette.replace('_', ' '),
                        swatch = AuroraRibbonPalette.forName(auroraPalette),
                        onClick = { showPaletteDialog = true }
                    )
                    SettingsDivider()
                    SettingsSwitchRow(
                        title = "Use album art colors in song list",
                        checked = useAlbumPalette,
                        onCheckedChange = { enabled -> viewModel.setUseAlbumPalette(enabled) }
                    )
                }

                SettingsGroup(title = "STREAMING") {
                    SettingsValueRow(
                        title = "Navidrome server",
                        value = navidromeStatus,
                        onClick = { navController.navigate(NavRoutes.Navidrome.route) }
                    )
                }

                SettingsGroup(title = "RADIO") {
                    SettingsSwitchRow(
                        title = "Default radio list",
                        checked = useDefaultRadioList,
                        onCheckedChange = { viewModel.setUseDefaultRadioList(it) }
                    )
                }
            }

            // App / OS version shown at the bottom of the settings screen
            val osVersion = "Android ${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT})"
            val appVersion = BuildConfig.APP_VERSION_NAME
            Text(
                text = "MusicPlayer $appVersion · $osVersion",
                color = Color.White.copy(alpha = 0.35f),
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)
            )
        }
    }
    }

    if (showPaletteDialog) {
        AuroraPaletteDialog(
            selected = auroraPalette,
            onSelect = { name ->
                viewModel.setAuroraPalette(name)
                showPaletteDialog = false
            },
            onDismiss = { showPaletteDialog = false }
        )
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = title,
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.4.sp,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.08f))
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .background(Color.White.copy(alpha = 0.08f))
    )
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        PillSwitch(checked = checked)
    }
}

// Value-and-chevron settings row (e.g. "Stream quality"); a small color swatch stands in for a value icon when the row picks a color palette.
@Composable
private fun SettingsValueRow(
    title: String,
    value: String,
    swatch: List<Color> = emptyList(),
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (swatch.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy((-4).dp)) {
                    swatch.take(3).forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(1.dp, Color.Black.copy(alpha = 0.4f), CircleShape)
                        )
                    }
                }
            }
            Text(text = value, color = Color.White.copy(alpha = 0.72f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.45f))
        }
    }
}

@Composable
private fun AuroraPaletteDialog(
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1A),
        title = { Text(text = "Aurora palette", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(modifier = Modifier.height(360.dp)) {
                items(AuroraRibbonPalette.names) { name ->
                    val isSelected = name == selected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(name) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy((-5).dp)) {
                                AuroraRibbonPalette.forName(name).take(4).forEach { color ->
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .border(1.dp, Color.Black.copy(alpha = 0.4f), CircleShape)
                                    )
                                }
                            }
                            Text(
                                text = name.replace('_', ' '),
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                        if (isSelected) {
                            Icon(imageVector = Icons.Filled.Check, contentDescription = "Selected", tint = Color(0xFFFFA500))
                        }
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(text = "Close", color = Color(0xFFFFA500))
            }
        }
    )
}

// Custom 44x26 pill switch matching the redesign -- Material3's Switch is a different size/shape entirely, so this is drawn directly rather than restyled from it.
@Composable
private fun PillSwitch(checked: Boolean) {
    val trackColor = if (checked) Color(0xFFFFA500).copy(alpha = 0.35f) else Color.White.copy(alpha = 0.10f)
    val borderColor = if (checked) Color(0xFFFFA500) else Color.White.copy(alpha = 0.45f)
    val thumbColor = if (checked) Color(0xFFFFA500) else Color.White.copy(alpha = 0.55f)

    Box(
        modifier = Modifier
            .width(44.dp)
            .height(26.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(trackColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(13.dp))
            .padding(3.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .width(16.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(thumbColor)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun SettingsScreenPreview() {
    val navController = rememberNavController()
    // Use the real composable so the preview stays in sync with implementation
    SettingsScreen(navController = navController)
}

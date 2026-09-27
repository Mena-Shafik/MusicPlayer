package com.example.musicplayer.ui.components.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.musicplayer.service.PlayerDockController
import com.example.musicplayer.service.PlayerStateManager

// Extra bottom padding a scrollable list should reserve so its last row scrolls clear of PersistentPlayerHost's mini bar (a screen-agnostic overlay, not a Scaffold bottomBar slot); tracks the bar's real measured height (PlayerDockController.reportMiniBarHeightPx) instead of a hand-picked constant that could drift.
@Composable
fun miniPlayerBottomPadding(): Dp {
    val playlist by PlayerStateManager.playlist.collectAsState()
    if (playlist.isEmpty()) return 0.dp
    val heightPx by PlayerDockController.miniBarHeightPx.collectAsState()
    val density = LocalDensity.current
    return with(density) { heightPx.toDp() }
}

package com.example.musicplayer.ui.components.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.musicplayer.model.Song
import com.example.musicplayer.ui.components.common.LibraryViewTabs
import com.example.musicplayer.ui.components.song.SongCardRow
import com.example.musicplayer.util.Util

// SongsSheetContent: sheet UI (Up Next + Lyrics + Related) without its own scaffold so it can be used inside a persistent BottomSheetScaffold.
@Composable
fun SongsSheetContent(
    songs: List<Song>,
    currentIndex: Int,
    backgroundColor: Color,
    onSelect: (Int) -> Unit,
    initialSelectedTab: Int = 0, // allow preview to set the starting tab
    onTabSelected: (Int) -> Unit = {}, // called when a tab is tapped, so the parent can jump collapsed->half
    // Drag (settles to the nearest of MusicPlayerChrome's 3 detents) + tap (steps collapsed->half->full->collapsed) on the grab handle -- built and owned by the caller (needs the shared swipeableState/sheetProgress other elements react to too), just attached here since the handle lives in this Column.
    grabHandleModifier: Modifier = Modifier,
    showIndicator: Boolean = true, // when false the tab indicator is hidden (useful when sheet is collapsed)
    openFraction: Float = 1f, // live collapsed(0)->half(0.5)->full(1) progress, from MusicPlayerChrome
    // Lower alpha cap so the animated Aurora mesh reads through the panel instead of being smothered by the album-sampled tint at the usual 0.80 cap.
    useAuroraBackground: Boolean = false
) {
    val sheetBg = backgroundColor
    // The tinted panel reveals in lockstep with openFraction as the sheet is dragged/animated open, rather than snapping in only once fully expanded.
    val liveSheetAlpha = (if (useAuroraBackground) 0.40f else 0.80f) * openFraction
    val liveSheetBg = sheetBg.copy(alpha = liveSheetAlpha)
    // Composites the panel's own color with what's behind it (the screen's darker gradient stop, dominant while collapsed) so the contrast decision below tracks what's really on screen at the current drag position, not just the closed/open extremes.
    val behindSheet = Util.darkerColor(sheetBg, 0.25f)
    val effectiveSheetBg = Color(
        red = sheetBg.red * liveSheetAlpha + behindSheet.red * (1f - liveSheetAlpha),
        green = sheetBg.green * liveSheetAlpha + behindSheet.green * (1f - liveSheetAlpha),
        blue = sheetBg.blue * liveSheetAlpha + behindSheet.blue * (1f - liveSheetAlpha)
    )
    val contentOnBg by animateColorAsState(
        targetValue = if (effectiveSheetBg.luminance() > 0.5f) Color.Black else Color.White,
        animationSpec = tween(220),
        label = "sheetContentOnBg"
    )
    val subtle = contentOnBg.copy(alpha = 0.06f)

    val startIndex = currentIndex.coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)

    // Auto-scroll to current song when it changes
    LaunchedEffect(currentIndex) {
        listState.animateScrollToItem(startIndex)
    }

    // shared tab data/state (must be declared before we reference it in modifiers)
    val tabs = listOf("Up Next", "Lyrics", "Related")
    var selectedTab by remember { mutableStateOf(initialSelectedTab) }
    var relatedSongs by remember { mutableStateOf<List<Pair<Int, Song>>>(emptyList()) }

    // Populate relatedSongs whenever the Related tab is selected or when the current index/songs change.
    LaunchedEffect(selectedTab, currentIndex, songs) {
        if (selectedTab == 2) {
            try {
                relatedSongs = Util.getRelatedSongs(songs, currentIndex)
            } catch (e: Throwable) {
                // On error just clear related list (avoid crashing the UI)
                relatedSongs = emptyList()
            }
        } else {
            relatedSongs = emptyList()
        }
    }

    // accumulate drag distance between press and release so we can decide a swipe
    var dragAccum by remember { mutableStateOf(0f) }
    // threshold in pixels to be considered a swipe
    val swipeThreshold = 100f

    // Horizontal-only so vertical drags reach the sheet/list instead of being swallowed as a tab swipe.
    val swipeModifier = Modifier.pointerInput(selectedTab) {
        detectHorizontalDragGestures(
            onDragStart = { dragAccum = 0f },
            onHorizontalDrag = { change, dragAmount ->
                dragAccum += dragAmount
                change.consume()
            },
            onDragEnd = {
                if (dragAccum > swipeThreshold) {
                    // dragged right -> previous tab
                    selectedTab = (selectedTab - 1).coerceAtLeast(0)
                } else if (dragAccum < -swipeThreshold) {
                    // dragged left -> next tab
                    selectedTab = (selectedTab + 1).coerceAtMost(tabs.lastIndex)
                }
                dragAccum = 0f
            },
            onDragCancel = { dragAccum = 0f }
        )
    }

    // Swipe left/right across the content area switches tabs; when collapsed the sheet is invisible except for the handle/tab pills floating over the screen, and the tinted panel fades in as the sheet opens (liveSheetBg above) rather than appearing only once fully expanded.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(liveSheetBg)
            .padding(bottom = 8.dp)
            // The parent Box already sets an exact height driven by MusicPlayerChrome's own swipeableState offset -- fill it rather than sizing independently.
            .fillMaxHeight()
    ) {
        // Grab handle: drag (settles to nearest of collapsed/half/full) and tap (steps through them) are wired via grabHandleModifier (built by MusicPlayerChrome, which owns swipeableState), attached to this wider invisible touch area rather than the 36x4dp visual bar, which would be too small a target.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .then(grabHandleModifier),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.28f))
            )
        }
        // Pill segmented control replaces the underline TabRow; the tab stays selected underneath while collapsed, but the highlight tracks openFraction directly -- invisible when closed, fading/sliding in as the sheet opens rather than snapping in once fully expanded.
        LibraryViewTabs(
            labels = tabs,
            selectedIndex = selectedTab,
            openFraction = openFraction,
            unselectedColor = contentOnBg.copy(alpha = 0.75f),
            onSelected = { index ->
                selectedTab = index
                // Ask parent to jump collapsed->half when a tab is tapped
                try { onTabSelected(index) } catch (_: Throwable) {}
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Content area: capture horizontal swipes to switch tabs and show tab content.
        // Takes all remaining sheet height so lists run to the sheet's bottom instead of stopping at a fixed cap.
        Column(modifier = swipeModifier.fillMaxWidth().weight(1f)) {
            if (selectedTab == 0) {
                HorizontalDivider(color = subtle)
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(vertical = 4.dp)) {
                    itemsIndexed(songs) { idx, s ->
                        val isCurrent = idx == currentIndex
                        // Reuses the app's standard song row instead of a bespoke one; trailingIcon is the design's drag_handle glyph, non-interactive (visual affordance only, no functional reordering).
                        SongCardRow(
                            song = s,
                            onClick = { if (idx != currentIndex) onSelect(idx) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isCurrent) Color(0xFFFFDAB9).copy(alpha = 0.12f) else Color.Transparent),
                            trailingIcon = Icons.Filled.DragHandle,
                            trailingIconClickable = false
                        )
                    }
                }
            } else if (selectedTab == 1) {
                val currentSong = songs.getOrNull(currentIndex)
                LyricsTab(currentSong = currentSong, contentColor = contentOnBg)
            } else {
                // Related tab UI
                HorizontalDivider(color = subtle)
                if (relatedSongs.isEmpty()) {
                    // show helpful message when no related songs found
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "No related songs found", color = contentOnBg.copy(alpha = 0.85f))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        items(items = relatedSongs) { pair ->
                            val idx = pair.first
                            val s = pair.second

                            // Reuse the same song row used in the main song list so related items look identical.
                            SongCardRow(
                                song = s,
                                onClick = { onSelect(idx) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

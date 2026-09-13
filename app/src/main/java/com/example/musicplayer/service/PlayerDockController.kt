package com.example.musicplayer.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// One-shot event channel letting any screen ask PersistentPlayerHost to expand, without holding a reference to it or a NavController -- mirrors PlayerStateManager's plain-singleton style; expandRequests is a monotonic counter (not a boolean) so repeated requests each still trigger the collecting LaunchedEffect; deliberately a StateFlow, not a zero-replay SharedFlow, so the first request of a session (which usually fires synchronously before PersistentPlayerHost starts collecting) isn't dropped -- the corresponding stale-replay-on-later-resubscribe risk is avoided by keeping PersistentPlayerHost mounted at all times and gating only its rendering (hideContent) rather than conditionally composing it per route.
object PlayerDockController {
    private val _expandRequests = MutableStateFlow(0)
    val expandRequests: StateFlow<Int> = _expandRequests

    fun requestExpand() {
        _expandRequests.value++
    }

    // The mini bar's actual measured height in px, reported by PersistentPlayerHost (onGloballyPositioned) rather than guessed by every screen; kept as a raw Float (not Dp) so this plain service object needs no Compose UI dependency -- callers convert with LocalDensity.
    private val _miniBarHeightPx = MutableStateFlow(0f)
    val miniBarHeightPx: StateFlow<Float> = _miniBarHeightPx

    fun reportMiniBarHeightPx(px: Float) {
        _miniBarHeightPx.value = px
    }
}

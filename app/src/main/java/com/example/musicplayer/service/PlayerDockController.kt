package com.example.musicplayer.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// Lets any screen ask PersistentPlayerHost to expand; a pending flag the host consumes, so a request survives until handled but is never replayed to a recreated host.
object PlayerDockController {
    private val _pendingExpand = MutableStateFlow(false)
    val pendingExpand: StateFlow<Boolean> = _pendingExpand

    fun requestExpand() {
        _pendingExpand.value = true
    }

    // True only for the one caller that actually takes the pending request.
    fun consumeExpand(): Boolean = _pendingExpand.compareAndSet(expect = true, update = false)

    // The mini bar's actual measured height in px, reported by PersistentPlayerHost (onGloballyPositioned) rather than guessed by every screen; kept as a raw Float (not Dp) so this plain service object needs no Compose UI dependency -- callers convert with LocalDensity.
    private val _miniBarHeightPx = MutableStateFlow(0f)
    val miniBarHeightPx: StateFlow<Float> = _miniBarHeightPx

    fun reportMiniBarHeightPx(px: Float) {
        _miniBarHeightPx.value = px
    }

    // Live 0 (docked) -> 1 (full) player progress, so each screen's BottomNav can slide out of the way as the player expands.
    private val _dockProgress = MutableStateFlow(0f)
    val dockProgress: StateFlow<Float> = _dockProgress

    fun reportDockProgress(progress: Float) {
        _dockProgress.value = progress
    }
}

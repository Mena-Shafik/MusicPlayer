package com.example.musicplayer.ui.theme

import android.view.Window
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

// The app's baseline (set up by enableEdgeToEdge on a dark UI); screens that tint the status bar restore this when they close.
fun restoreDefaultSystemBars(window: Window) {
    try {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        @Suppress("DEPRECATION")
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false
    } catch (_: Throwable) {}
}

package com.example.musicplayer.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationDeleteReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        // For now, simply stop the foreground service if implemented elsewhere
        Intent(context, PlayerForegroundService::class.java).apply {
            action = PlayerActions.ACTION_PAUSE
            context.startService(this)
        }
    }
}


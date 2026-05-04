package com.musicplayer.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receives BOOT_COMPLETED so the app can restore the last playback session
 * after the device reboots. Actual restoration is handled by MainActivity on launch.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // TODO: optionally auto-start service and restore last queue
        }
    }
}

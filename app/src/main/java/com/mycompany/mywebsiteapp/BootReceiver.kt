package com.mycompany.mywebsiteapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Use ContextCompat to avoid crashes on Android 8+
        ContextCompat.startForegroundService(context, Intent(context, MyService::class.java))
    }
}

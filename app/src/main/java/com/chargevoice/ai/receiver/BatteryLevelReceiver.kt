package com.chargevoice.ai.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BatteryLevelReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
            // Battery monitoring logic goes here
            // Note: This receiver should be registered dynamically by the service.
        }
    }
}

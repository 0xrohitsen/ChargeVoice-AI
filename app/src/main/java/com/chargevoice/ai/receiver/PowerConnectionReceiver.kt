package com.chargevoice.ai.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.chargevoice.ai.service.ChargingMonitorService

class PowerConnectionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_POWER_CONNECTED) {
            // Start foreground service
            val serviceIntent = Intent(context, ChargingMonitorService::class.java)
            context.startForegroundService(serviceIntent)
        } else if (intent.action == Intent.ACTION_POWER_DISCONNECTED) {
            // Stop foreground service
            val serviceIntent = Intent(context, ChargingMonitorService::class.java)
            context.stopService(serviceIntent)
        }
    }
}

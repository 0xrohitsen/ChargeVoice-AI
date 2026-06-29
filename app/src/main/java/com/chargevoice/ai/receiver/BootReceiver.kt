package com.chargevoice.ai.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import com.chargevoice.ai.service.ChargingMonitorService
import com.chargevoice.ai.service.ChargingTriggerWorker

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Log.d("ChargeVoice", "Boot completed — re-enqueuing WorkManager for next charging session.")

        // Always re-enqueue WorkManager on boot — jobs are cleared by the OS on reboot.
        // This ensures the app detects the next plug-in even if the user never opens the app.
        ChargingTriggerWorker.enqueueNextChargingWork(context)

        // Additionally, if the device is ALREADY charging at boot, start the service immediately.
        val batteryStatus: Intent? = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL

        if (isCharging) {
            Log.d("ChargeVoice", "Device is charging at boot — starting ChargingMonitorService.")
            context.startForegroundService(Intent(context, ChargingMonitorService::class.java))
        }
    }
}

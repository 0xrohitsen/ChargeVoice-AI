package com.chargevoice.ai.service

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class ChargingTriggerWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("ChargingTriggerWorker", "Device connected to power! Starting Monitoring Service.")

        val serviceIntent = Intent(applicationContext, ChargingMonitorService::class.java)
        try {
            ContextCompat.startForegroundService(applicationContext, serviceIntent)
        } catch (e: Exception) {
            Log.e("ChargingTriggerWorker", "Failed to start foreground service: ${e.message}")
            return Result.failure()
        }

        // NOTE: Do NOT re-enqueue here — device is still plugged in, so constraint is
        // immediately met and would cause an infinite loop.
        // Re-enqueue happens in ChargingMonitorService.onDestroy() when charger disconnects.

        return Result.success()
    }

    companion object {
        const val WORK_NAME = "ChargingTriggerWork"

        fun enqueueNextChargingWork(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresCharging(true)
                .build()
            val request = OneTimeWorkRequestBuilder<ChargingTriggerWorker>()
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
            Log.d("ChargingTriggerWorker", "Next charging work enqueued.")
        }
    }
}

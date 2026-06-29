package com.chargevoice.ai.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.chargevoice.ai.MainActivity
import com.chargevoice.ai.datastore.SettingsDataStore
import com.chargevoice.ai.repository.ChargeVoiceRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@AndroidEntryPoint
class ChargingMonitorService : Service() {

    @Inject lateinit var settingsDataStore: SettingsDataStore
    @Inject lateinit var repository: ChargeVoiceRepository

    companion object {
        const val MONITORING_CHANNEL_ID = "monitoring_channel"
        const val REMINDER_CHANNEL_ID   = "reminder_channel"
        const val MONITORING_NOTIF_ID   = 1
        const val REMINDER_NOTIF_ID     = 2

        const val ACTION_STOP_REMINDER  = "com.chargevoice.ai.STOP_REMINDER"
        const val ACTION_SNOOZE_REMINDER = "com.chargevoice.ai.SNOOZE_REMINDER"
    }

    private lateinit var voiceManager: VoiceAnnouncementManager

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var reminderJob: Job? = null

    // Serialise all battery updates – prevents race conditions on announcedMilestones
    private val batteryUpdateDispatcher = Dispatchers.IO.limitedParallelism(1)
    private val milestoneMutex = Mutex()
    private val announcedMilestones = mutableSetOf<Int>()

    private var currentSessionId: Long? = null
    private var sessionStartTime: Long = 0
    private var startBatteryLevel: Int = 0
    private var maxBatteryLevel: Int = 0

    // ─── Battery level receiver (dynamically registered) ───────────────────────
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != Intent.ACTION_BATTERY_CHANGED) return
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level == -1 || scale == -1) return
            val batteryPct = (level * 100) / scale
            Log.d("ChargingMonitor", "Battery level updated: $batteryPct%")
            if (batteryPct > maxBatteryLevel) maxBatteryLevel = batteryPct
            handleBatteryUpdate(batteryPct)
        }
    }

    // ─── Power disconnect receiver (dynamically registered) ────────────────────
    // This is the KEY fix: dynamically registered receivers ARE allowed for
    // ACTION_POWER_DISCONNECTED even in background on Android 8+
    private val powerDisconnectReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_POWER_DISCONNECTED) {
                Log.d("ChargeVoice", "Charger disconnected — stopping monitoring service.")
                stopSelf()
            }
        }
    }

    // ─── Notification action receiver (Stop / Snooze) ──────────────────────────
    private val notificationActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_STOP_REMINDER -> {
                    Log.d("ChargeVoice", "Stop Reminder tapped.")
                    reminderJob?.cancel()
                    reminderJob = null
                    dismissReminderNotification()
                }
                ACTION_SNOOZE_REMINDER -> {
                    val snoozeMin = intent.getIntExtra("snooze_minutes", 2)
                    Log.d("ChargeVoice", "Snooze tapped — $snoozeMin minutes.")
                    reminderJob?.cancel()
                    reminderJob = serviceScope.launch {
                        delay(snoozeMin * 60 * 1000L)
                        startReminderLoop()
                    }
                    dismissReminderNotification()
                }
            }
        }
    }

    // ───────────────────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        Log.d("ChargeVoice", "ChargingMonitorService created and starting...")
        createNotificationChannels()
        voiceManager = VoiceAnnouncementManager(this)

        // Keep voice settings in sync with DataStore changes
        serviceScope.launch {
            settingsDataStore.languageFlow.collect { lang ->
                val speed = settingsDataStore.speechSpeedFlow.first()
                val pitch = settingsDataStore.speechPitchFlow.first()
                voiceManager.updateSettings(lang, speed, pitch)
            }
        }

        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        registerReceiver(powerDisconnectReceiver, IntentFilter(Intent.ACTION_POWER_DISCONNECTED))

        val actionFilter = IntentFilter().apply {
            addAction(ACTION_STOP_REMINDER)
            addAction(ACTION_SNOOZE_REMINDER)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(notificationActionReceiver, actionFilter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(notificationActionReceiver, actionFilter)
        }

        // Record session start
        serviceScope.launch {
            delay(500)
            val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            startBatteryLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            maxBatteryLevel = startBatteryLevel
            sessionStartTime = System.currentTimeMillis()
            val newSession = com.chargevoice.ai.model.ChargingHistory(
                startDate = sessionStartTime,
                startTime = sessionStartTime,
                startBatteryPercentage = startBatteryLevel,
                chargingType = "AC"
            )
            currentSessionId = repository.insertChargingSession(newSession)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(MONITORING_NOTIF_ID, buildMonitoringNotification())
        return START_STICKY
    }

    // ─── Battery Update Logic ──────────────────────────────────────────────────

    private fun handleBatteryUpdate(batteryPct: Int) {
        val milestone = (batteryPct / 10) * 10
        if (milestone <= 0) return

        serviceScope.launch(batteryUpdateDispatcher) {
            val isVoiceEnabled = settingsDataStore.voiceEnabledFlow.first()

            // --- Milestone announcements (10%–90%) ---
            if (milestone < 100) {
                val isAlertEnabled = settingsDataStore.getAlertEnabledFlow(milestone).first()
                milestoneMutex.withLock {
                    if (isAlertEnabled && !announcedMilestones.contains(milestone)) {
                        announcedMilestones.add(milestone)
                        Log.d("ChargeVoice", "Announcing milestone: $milestone%")
                        if (isVoiceEnabled) {
                            val text = voiceManager.getLocalizedMilestoneText(milestone)
                            withContext(Dispatchers.Main) { voiceManager.announce(text) }
                        }
                    }
                }
            }

            // --- Full charge reminder ---
            val fullChargeReminderEnabled = settingsDataStore.fullChargeReminderFlow.first()
            milestoneMutex.withLock {
                if (batteryPct >= 100 && fullChargeReminderEnabled && reminderJob == null) {
                    announcedMilestones.add(100)
                    Log.d("ChargeVoice", "Battery full — starting reminder loop.")
                    withContext(Dispatchers.Main) { startReminderLoop() }
                }
            }
        }
    }

    // ─── Reminder Loop ────────────────────────────────────────────────────────

    private fun startReminderLoop() {
        reminderJob = serviceScope.launch {
            val intervalMin = settingsDataStore.reminderIntervalFlow.first()
            val useVoice    = settingsDataStore.reminderVoiceFlow.first()
            while (isActive) {
                // 1. Show high-priority notification with actions
                showReminderNotification()

                // 2. Speak voice reminder
                if (useVoice) {
                    withContext(Dispatchers.Main) {
                        voiceManager.announce(voiceManager.getLocalizedFullChargeText())
                    }
                }

                // 3. Wait for the interval before repeating
                delay(intervalMin * 60 * 1000L)
            }
        }
    }

    // ─── Notifications ────────────────────────────────────────────────────────

    private fun buildMonitoringNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, MONITORING_CHANNEL_ID)
            .setContentTitle("ChargeVoice AI")
            .setContentText("Monitoring charging session...")
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(openIntent)
            .build()
    }

    private fun showReminderNotification() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val openIntent = PendingIntent.getActivity(
            this, 10,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getBroadcast(
            this, 11,
            Intent(ACTION_STOP_REMINDER).setPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val snooze2Intent = PendingIntent.getBroadcast(
            this, 12,
            Intent(ACTION_SNOOZE_REMINDER).setPackage(packageName).putExtra("snooze_minutes", 2),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, REMINDER_CHANNEL_ID)
            .setContentTitle("🔋 Battery Fully Charged")
            .setContentText("Your battery has reached 100%. Please unplug the charger.")
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setOngoing(false)
            .setContentIntent(openIntent)
            .addAction(0, "Stop Reminder", stopIntent)
            .addAction(0, "Snooze 2 Min", snooze2Intent)
            .build()

        nm.notify(REMINDER_NOTIF_ID, notification)
    }

    private fun dismissReminderNotification() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(REMINDER_NOTIF_ID)
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    override fun onDestroy() {
        super.onDestroy()
        Log.d("ChargeVoice", "ChargingMonitorService destroyed — saving session.")

        try { unregisterReceiver(batteryReceiver) } catch (_: Exception) {}
        try { unregisterReceiver(powerDisconnectReceiver) } catch (_: Exception) {}
        try { unregisterReceiver(notificationActionReceiver) } catch (_: Exception) {}

        dismissReminderNotification()

        // Re-enqueue WorkManager for the NEXT charging session
        // (safe here because charger is now disconnected — constraint won't be met immediately)
        ChargingTriggerWorker.enqueueNextChargingWork(applicationContext)

        // Save session to DB
        currentSessionId?.let { sessionId ->
            val endTime = System.currentTimeMillis()
            val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val endBattery = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            val finalSession = com.chargevoice.ai.model.ChargingHistory(
                sessionId = sessionId,
                startDate = sessionStartTime,
                startTime = sessionStartTime,
                endDate = endTime,
                endTime = endTime,
                duration = endTime - sessionStartTime,
                startBatteryPercentage = startBatteryLevel,
                endBatteryPercentage = endBattery,
                highestBatteryPercentage = maxBatteryLevel,
                chargingType = "AC"
            )
            CoroutineScope(Dispatchers.IO).launch {
                repository.updateChargingSession(finalSession)
            }
        }

        reminderJob?.cancel()
        reminderJob = null
        announcedMilestones.clear()
        currentSessionId = null
        serviceScope.cancel()
        voiceManager.release()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ─── Notification Channels ────────────────────────────────────────────────

    private fun createNotificationChannels() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Low importance — silent background monitoring
        nm.createNotificationChannel(
            NotificationChannel(MONITORING_CHANNEL_ID, "Charging Monitor", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Silent background monitoring notification"
                setSound(null, null)
                enableVibration(false)
            }
        )

        // High importance — full charge reminder
        nm.createNotificationChannel(
            NotificationChannel(REMINDER_CHANNEL_ID, "Full Charge Reminder", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "High-priority notification when battery is fully charged"
            }
        )
    }
}

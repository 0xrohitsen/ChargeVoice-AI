package com.chargevoice.ai.service

import android.content.Context

class NotificationManager(private val context: Context) {
    fun showMonitoringNotification() {
        // Logic to show low-priority monitoring notification
    }

    fun showReminderNotification() {
        // Logic to show high-priority reminder notification
    }

    fun removeNotifications() {
        // Logic to clear notifications when charging stops
    }
}

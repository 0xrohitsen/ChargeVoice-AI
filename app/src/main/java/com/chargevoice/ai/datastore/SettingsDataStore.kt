package com.chargevoice.ai.datastore

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        val THEME_KEY = stringPreferencesKey("theme")
        val DYNAMIC_COLOR_KEY = booleanPreferencesKey("dynamic_color")
        val LANGUAGE_KEY = stringPreferencesKey("language")
        val VOICE_ENABLED_KEY = booleanPreferencesKey("voice_enabled")
        val SPEECH_SPEED_KEY = floatPreferencesKey("speech_speed")
        val SPEECH_PITCH_KEY = floatPreferencesKey("speech_pitch")
        
        val FULL_CHARGE_REMINDER_KEY = booleanPreferencesKey("full_charge_reminder")
        val REMINDER_INTERVAL_KEY = intPreferencesKey("reminder_interval")
        val REMINDER_VOICE_KEY = booleanPreferencesKey("reminder_voice")
        
        val NOTIFICATIONS_ENABLED_KEY = booleanPreferencesKey("notifications_enabled")
        val SILENT_MONITORING_NOTIF_KEY = booleanPreferencesKey("silent_monitoring_notif")
        val REMINDER_NOTIF_KEY = booleanPreferencesKey("reminder_notif")
        
        fun getAlertKeyForLevel(level: Int) = booleanPreferencesKey("alert_$level")
    }

    val themeFlow: Flow<String> = context.dataStore.data.map { it[THEME_KEY] ?: "System" }
    val dynamicColorFlow: Flow<Boolean> = context.dataStore.data.map { it[DYNAMIC_COLOR_KEY] ?: true }
    
    val languageFlow: Flow<String> = context.dataStore.data.map { it[LANGUAGE_KEY] ?: "English" }
    val voiceEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[VOICE_ENABLED_KEY] ?: true }
    val speechSpeedFlow: Flow<Float> = context.dataStore.data.map { it[SPEECH_SPEED_KEY] ?: 1.0f }
    val speechPitchFlow: Flow<Float> = context.dataStore.data.map { it[SPEECH_PITCH_KEY] ?: 1.0f }
    
    val fullChargeReminderFlow: Flow<Boolean> = context.dataStore.data.map { it[FULL_CHARGE_REMINDER_KEY] ?: true }
    val reminderIntervalFlow: Flow<Int> = context.dataStore.data.map { it[REMINDER_INTERVAL_KEY] ?: 2 }
    val reminderVoiceFlow: Flow<Boolean> = context.dataStore.data.map { it[REMINDER_VOICE_KEY] ?: true }
    
    val notificationsEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[NOTIFICATIONS_ENABLED_KEY] ?: true }
    val silentMonitoringNotifFlow: Flow<Boolean> = context.dataStore.data.map { it[SILENT_MONITORING_NOTIF_KEY] ?: true }
    val reminderNotifFlow: Flow<Boolean> = context.dataStore.data.map { it[REMINDER_NOTIF_KEY] ?: true }

    fun getAlertEnabledFlow(level: Int): Flow<Boolean> = context.dataStore.data.map {
        it[getAlertKeyForLevel(level)] ?: (level == 20 || level == 50 || level == 80 || level == 100)
    }

    suspend fun updateTheme(theme: String) = context.dataStore.edit { it[THEME_KEY] = theme }
    suspend fun updateDynamicColor(enabled: Boolean) = context.dataStore.edit { it[DYNAMIC_COLOR_KEY] = enabled }
    
    suspend fun updateLanguage(lang: String) = context.dataStore.edit { it[LANGUAGE_KEY] = lang }
    suspend fun updateVoiceEnabled(enabled: Boolean) = context.dataStore.edit { it[VOICE_ENABLED_KEY] = enabled }
    suspend fun updateSpeechSpeed(speed: Float) = context.dataStore.edit { it[SPEECH_SPEED_KEY] = speed }
    suspend fun updateSpeechPitch(pitch: Float) = context.dataStore.edit { it[SPEECH_PITCH_KEY] = pitch }
    
    suspend fun updateFullChargeReminder(enabled: Boolean) = context.dataStore.edit { it[FULL_CHARGE_REMINDER_KEY] = enabled }
    suspend fun updateReminderInterval(interval: Int) = context.dataStore.edit { it[REMINDER_INTERVAL_KEY] = interval }
    suspend fun updateReminderVoice(enabled: Boolean) = context.dataStore.edit { it[REMINDER_VOICE_KEY] = enabled }
    
    suspend fun updateNotificationsEnabled(enabled: Boolean) = context.dataStore.edit { it[NOTIFICATIONS_ENABLED_KEY] = enabled }
    suspend fun updateSilentMonitoringNotif(enabled: Boolean) = context.dataStore.edit { it[SILENT_MONITORING_NOTIF_KEY] = enabled }
    suspend fun updateReminderNotif(enabled: Boolean) = context.dataStore.edit { it[REMINDER_NOTIF_KEY] = enabled }

    suspend fun saveAlertEnabled(level: Int, enabled: Boolean) {
        context.dataStore.edit { it[getAlertKeyForLevel(level)] = enabled }
    }
}

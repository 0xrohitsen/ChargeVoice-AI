package com.chargevoice.ai.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chargevoice.ai.datastore.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    val theme = settingsDataStore.themeFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "System")
    val dynamicColor = settingsDataStore.dynamicColorFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    
    val language = settingsDataStore.languageFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "English")
    val voiceEnabled = settingsDataStore.voiceEnabledFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val speechSpeed = settingsDataStore.speechSpeedFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0f)
    val speechPitch = settingsDataStore.speechPitchFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0f)

    val fullChargeReminder = settingsDataStore.fullChargeReminderFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val reminderInterval = settingsDataStore.reminderIntervalFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 5)
    val reminderVoice = settingsDataStore.reminderVoiceFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val notificationsEnabled = settingsDataStore.notificationsEnabledFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val silentMonitoringNotif = settingsDataStore.silentMonitoringNotifFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val reminderNotif = settingsDataStore.reminderNotifFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // Cache one StateFlow per level so recomposition always gets the same instance
    private val alertFlowCache = HashMap<Int, StateFlow<Boolean>>()

    fun getAlertEnabled(level: Int): StateFlow<Boolean> {
        return alertFlowCache.getOrPut(level) {
            settingsDataStore.getAlertEnabledFlow(level)
                .stateIn(viewModelScope, SharingStarted.Eagerly, level == 100 || level == 20)
        }
    }

    fun updateTheme(valTheme: String) = viewModelScope.launch { settingsDataStore.updateTheme(valTheme) }
    fun updateDynamicColor(enabled: Boolean) = viewModelScope.launch { settingsDataStore.updateDynamicColor(enabled) }
    
    fun updateLanguage(lang: String) = viewModelScope.launch { settingsDataStore.updateLanguage(lang) }
    fun toggleVoiceEnabled(enabled: Boolean) = viewModelScope.launch { settingsDataStore.updateVoiceEnabled(enabled) }
    fun updateSpeechSpeed(speed: Float) = viewModelScope.launch { settingsDataStore.updateSpeechSpeed(speed) }
    fun updateSpeechPitch(pitch: Float) = viewModelScope.launch { settingsDataStore.updateSpeechPitch(pitch) }

    fun updateFullChargeReminder(enabled: Boolean) = viewModelScope.launch { settingsDataStore.updateFullChargeReminder(enabled) }
    fun updateReminderInterval(interval: Int) = viewModelScope.launch { settingsDataStore.updateReminderInterval(interval) }
    fun updateReminderVoice(enabled: Boolean) = viewModelScope.launch { settingsDataStore.updateReminderVoice(enabled) }

    fun updateNotificationsEnabled(enabled: Boolean) = viewModelScope.launch { settingsDataStore.updateNotificationsEnabled(enabled) }
    fun updateSilentMonitoringNotif(enabled: Boolean) = viewModelScope.launch { settingsDataStore.updateSilentMonitoringNotif(enabled) }
    fun updateReminderNotif(enabled: Boolean) = viewModelScope.launch { settingsDataStore.updateReminderNotif(enabled) }

    fun toggleAlertEnabled(level: Int, enabled: Boolean) = viewModelScope.launch { settingsDataStore.saveAlertEnabled(level, enabled) }
}

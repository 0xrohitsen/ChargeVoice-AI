package com.chargevoice.ai.ui.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chargevoice.ai.datastore.SettingsDataStore
import com.chargevoice.ai.repository.ChargeVoiceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BatteryInfo(
    val percentage: Int = 0,
    val isCharging: Boolean = false,
    val statusText: String = "Unknown",
    val temperatureCelsius: Float = 0f,
    val voltageMv: Int = 0,
    val healthText: String = "Unknown",
    val chargingType: String = "None"
)

data class HomeUiState(
    val batteryInfo: BatteryInfo = BatteryInfo(),
    val voiceAssistantEnabled: Boolean = false,
    val selectedLanguage: String = "English",
    val nextReminderTarget: Int = 100,
    val sessionStartTime: Long = 0,
    val sessionDurationMillis: Long = 0
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsDataStore: SettingsDataStore,
    private val repository: ChargeVoiceRepository
) : ViewModel() {

    private val _sessionDurationMillis = MutableStateFlow(0L)
    

    private val batteryFlow: Flow<BatteryInfo> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    val percentage = if (level != -1 && scale != -1) (level * 100 / scale) else 0

                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
                    val isCharging = plugged > 0 || status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
                    
                    val statusText = when {
                        plugged > 0 -> "Charging"
                        status == BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
                        status == BatteryManager.BATTERY_STATUS_FULL -> "Fully Charged"
                        status == BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
                        else -> "Unknown"
                    }
                    
                    val chargingType = when (plugged) {
                        BatteryManager.BATTERY_PLUGGED_AC -> "AC Charger"
                        BatteryManager.BATTERY_PLUGGED_USB -> "USB Charger"
                        BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless Charger"
                        else -> "None"
                    }

                    val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
                    val temperatureCelsius = tempTenths / 10f

                    val voltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)

                    val health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)
                    val healthText = when (health) {
                        BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
                        BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
                        BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
                        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
                        BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
                        BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
                        else -> "Unknown"
                    }

                    trySend(BatteryInfo(percentage, isCharging, statusText, temperatureCelsius, voltageMv, healthText, chargingType))
                }
            }
        }

        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        // Initial push if possible
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        intent?.let { receiver.onReceive(context, it) }

        awaitClose {
            context.unregisterReceiver(receiver)
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        batteryFlow,
        settingsDataStore.voiceEnabledFlow,
        settingsDataStore.languageFlow,
        repository.allChargingSessions,
        _sessionDurationMillis
    ) { battery, voiceEnabled, language, sessions, duration ->
        
        // Find the active session if charging
        var startTime = 0L
        if (battery.isCharging && sessions.isNotEmpty()) {
            val lastSession = sessions.last() // Assuming they are appended/ordered
            if (lastSession.endTime == null || lastSession.endTime == 0L) {
                // Active session
                startTime = lastSession.startTime
            }
        }
        
        HomeUiState(
            batteryInfo = battery,
            voiceAssistantEnabled = voiceEnabled,
            selectedLanguage = language,
            nextReminderTarget = 100, // Still placeholder until full logic, but UI will look right
            sessionStartTime = startTime,
            sessionDurationMillis = if (startTime > 0) duration else 0L
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    init {
        // Simple ticker to update session duration while on screen
        viewModelScope.launch {
            while(true) {
                val state = uiState.value
                if (state.batteryInfo.isCharging && state.sessionStartTime > 0) {
                    _sessionDurationMillis.value = System.currentTimeMillis() - state.sessionStartTime
                }
                delay(1000)
            }
        }
    }
}

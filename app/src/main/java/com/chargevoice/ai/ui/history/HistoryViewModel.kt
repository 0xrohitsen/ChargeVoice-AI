package com.chargevoice.ai.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chargevoice.ai.model.ChargingHistory
import com.chargevoice.ai.repository.ChargeVoiceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: ChargeVoiceRepository
) : ViewModel() {

    val historyState: StateFlow<List<ChargingHistory>> = repository.allChargingSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        
    fun deleteSession(session: ChargingHistory) {
        viewModelScope.launch {
            repository.deleteSession(session) // assuming repository has this
        }
    }
}

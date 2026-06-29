package com.chargevoice.ai.repository

import com.chargevoice.ai.database.dao.ChargeVoiceDao
import com.chargevoice.ai.model.ChargingHistory
import com.chargevoice.ai.model.ReminderHistory
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ChargeVoiceRepository @Inject constructor(
    private val dao: ChargeVoiceDao
) {
    // Charging History
    suspend fun insertChargingSession(session: ChargingHistory): Long {
        return dao.insertChargingSession(session)
    }

    suspend fun updateChargingSession(session: ChargingHistory) {
        dao.updateChargingSession(session)
    }
    
    suspend fun deleteSession(session: ChargingHistory) {
        dao.deleteChargingSession(session)
    }

    val allChargingSessions: Flow<List<ChargingHistory>> = dao.getAllChargingSessions()

    suspend fun getLastChargingSession(): ChargingHistory? {
        return dao.getLastChargingSession()
    }

    // Reminder History
    suspend fun insertReminderEvent(reminder: ReminderHistory): Long {
        return dao.insertReminderEvent(reminder)
    }

    fun getRemindersForSession(sessionId: Long): Flow<List<ReminderHistory>> {
        return dao.getRemindersForSession(sessionId)
    }
}

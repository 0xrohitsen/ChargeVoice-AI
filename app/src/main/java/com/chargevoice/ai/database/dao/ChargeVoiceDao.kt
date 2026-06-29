package com.chargevoice.ai.database.dao

import androidx.room.*
import com.chargevoice.ai.model.ChargingHistory
import com.chargevoice.ai.model.ReminderHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface ChargeVoiceDao {

    // Charging History
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChargingSession(session: ChargingHistory): Long

    @Update
    suspend fun updateChargingSession(session: ChargingHistory)
    
    @Delete
    suspend fun deleteChargingSession(session: ChargingHistory)

    @Query("SELECT * FROM charging_history ORDER BY createdTimestamp DESC")
    fun getAllChargingSessions(): Flow<List<ChargingHistory>>

    @Query("SELECT * FROM charging_history ORDER BY createdTimestamp DESC LIMIT 1")
    suspend fun getLastChargingSession(): ChargingHistory?

    // Reminder History
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminderEvent(reminder: ReminderHistory): Long

    @Query("SELECT * FROM reminder_history WHERE sessionId = :sessionId ORDER BY createdTimestamp DESC")
    fun getRemindersForSession(sessionId: Long): Flow<List<ReminderHistory>>
}

package com.chargevoice.ai.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "charging_history")
data class ChargingHistory(
    @PrimaryKey(autoGenerate = true)
    val sessionId: Long = 0,
    val startDate: Long,
    val startTime: Long,
    val endDate: Long? = null,
    val endTime: Long? = null,
    val duration: Long = 0,
    val startBatteryPercentage: Int,
    val endBatteryPercentage: Int = 0,
    val highestBatteryPercentage: Int = 0,
    val lowestBatteryPercentage: Int = 0,
    val averageTemperature: Float = 0f,
    val highestTemperature: Float = 0f,
    val chargingType: String,
    val reminderTarget: Int = 100,
    val reminderTriggered: Boolean = false,
    val createdTimestamp: Long = System.currentTimeMillis()
)

package com.chargevoice.ai.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminder_history")
data class ReminderHistory(
    @PrimaryKey(autoGenerate = true)
    val reminderId: Long = 0,
    val sessionId: Long,
    val reminderPercentage: Int,
    val reminderTime: Long,
    val voiceUsed: Boolean = true,
    val createdTimestamp: Long = System.currentTimeMillis()
)

package com.chargevoice.ai.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.chargevoice.ai.database.dao.ChargeVoiceDao
import com.chargevoice.ai.model.ChargingHistory
import com.chargevoice.ai.model.ReminderHistory

@Database(
    entities = [ChargingHistory::class, ReminderHistory::class],
    version = 1,
    exportSchema = false
)
abstract class ChargeVoiceDatabase : RoomDatabase() {

    abstract fun chargeVoiceDao(): ChargeVoiceDao

    companion object {
        @Volatile
        private var INSTANCE: ChargeVoiceDatabase? = null

        fun getDatabase(context: Context): ChargeVoiceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ChargeVoiceDatabase::class.java,
                    "chargevoice.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

package com.chargevoice.ai.di

import android.content.Context
import androidx.room.Room
import com.chargevoice.ai.database.ChargeVoiceDatabase
import com.chargevoice.ai.database.dao.ChargeVoiceDao
import com.chargevoice.ai.datastore.SettingsDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): SettingsDataStore {
        return SettingsDataStore(context)
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ChargeVoiceDatabase {
        return Room.databaseBuilder(
            context,
            ChargeVoiceDatabase::class.java,
            "chargevoice_db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideChargeVoiceDao(database: ChargeVoiceDatabase): ChargeVoiceDao {
        return database.chargeVoiceDao()
    }
}

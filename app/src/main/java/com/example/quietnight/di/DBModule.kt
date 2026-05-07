package com.example.quietnight.di

import android.content.Context
import androidx.room.Room
import com.example.quietnight.data.QuietNightDatabase
import com.example.quietnight.data.SnoreDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object DBModule {
    @Singleton
    @Provides
    fun provideQuietNightDatabase(@ApplicationContext context: Context): QuietNightDatabase =
        Room.databaseBuilder(
            context,
            QuietNightDatabase::class.java,
            "quiet_night_database"
        ).fallbackToDestructiveMigration(true).build()

    @Singleton
    @Provides
    fun provideSnoreDao(database: QuietNightDatabase): SnoreDao = database.snoreDao()
}
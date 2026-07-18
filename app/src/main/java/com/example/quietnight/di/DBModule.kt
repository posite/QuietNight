package com.example.quietnight.di

import android.content.Context
import androidx.room.Room
import com.example.quietnight.data.MIGRATION_4_5
import com.example.quietnight.data.MIGRATION_5_6
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
        )
            // 실제 마이그레이션으로 사용자 기록 보존.
            .addMigrations(MIGRATION_4_5, MIGRATION_5_6)
            // 마이그레이션이 없는 초기 개발 버전(1~3)에서 올라오는 경우에만 파괴적 폴백 허용.
            .fallbackToDestructiveMigrationFrom(true, 1, 2, 3)
            .build()

    @Singleton
    @Provides
    fun provideSnoreDao(database: QuietNightDatabase): SnoreDao = database.snoreDao()
}
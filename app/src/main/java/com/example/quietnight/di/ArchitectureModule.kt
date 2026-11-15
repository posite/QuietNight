package com.example.quietnight.di

import com.example.quietnight.data.AndroidMonitoringController
import com.example.quietnight.data.RoomSleepRepository
import com.example.quietnight.data.SharedPreferencesSettingsRepository
import com.example.quietnight.domain.monitoring.MonitoringController
import com.example.quietnight.domain.repository.SettingsRepository
import com.example.quietnight.domain.repository.SleepRepository
import com.example.quietnight.domain.usecase.ObserveSleepHistoryUseCase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

// 실제 구현 선택은 앱의 조립 지점에서만 수행한다.
@Module
@InstallIn(SingletonComponent::class)
abstract class ArchitectureModule {
    @Binds @Singleton
    abstract fun sleepRepository(implementation: RoomSleepRepository): SleepRepository

    @Binds @Singleton
    abstract fun settingsRepository(implementation: SharedPreferencesSettingsRepository): SettingsRepository

    @Binds @Singleton
    abstract fun monitoringController(implementation: AndroidMonitoringController): MonitoringController

    companion object {
        @Provides
        fun clock(): Clock = Clock.systemDefaultZone()

        @Provides
        fun observeSleepHistory(repository: SleepRepository, clock: Clock) =
            ObserveSleepHistoryUseCase(repository, clock)
    }
}

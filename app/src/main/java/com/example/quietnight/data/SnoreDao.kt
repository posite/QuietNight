package com.example.quietnight.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SnoreDao {
    @Insert
    suspend fun insertSession(session: SleepSession)

    @Query("SELECT * FROM sleep_sessions ORDER BY date DESC")
    fun getHistory(): Flow<List<SleepSession>>
}
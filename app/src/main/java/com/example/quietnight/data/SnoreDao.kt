package com.example.quietnight.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SnoreDao {

    @Update
    suspend fun updateSession(session: SleepSession)

    @Insert
    suspend fun insertSession(session: SleepSession)


    @Query("SELECT * FROM sleep_sessions WHERE date = :date")
    suspend fun getSessionByDate(date: Long): SleepSession?


    @Query("SELECT * FROM sleep_sessions ORDER BY date DESC")
    fun getHistory(): Flow<List<SleepSession>>
}
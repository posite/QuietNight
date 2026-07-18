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

    @Query("DELETE FROM sleep_sessions")
    suspend fun clearAll()

    // ── 코골이 오디오 클립 ──
    @Insert
    suspend fun insertClip(clip: SnoreClip)

    @Query("SELECT * FROM snore_clips WHERE date = :date ORDER BY timestamp ASC")
    suspend fun getClipsByDate(date: Long): List<SnoreClip>

    @Query("SELECT COUNT(*) FROM snore_clips WHERE date = :date")
    suspend fun countClipsByDate(date: Long): Int

    // 보관 기간(N일) 초과 클립 정리용
    @Query("SELECT * FROM snore_clips WHERE date < :cutoff")
    suspend fun getClipsBefore(cutoff: Long): List<SnoreClip>

    @Query("DELETE FROM snore_clips WHERE date < :cutoff")
    suspend fun deleteClipsBefore(cutoff: Long)

    @Query("DELETE FROM snore_clips")
    suspend fun clearAllClips()
}
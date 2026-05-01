package com.example.quietnight.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sleep_sessions")
data class SleepSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val score: Int,
    val snoreMinutes: Int,
    val positionStatsJson: String // "등:82,옆:18" 형태의 JSON 저장
)
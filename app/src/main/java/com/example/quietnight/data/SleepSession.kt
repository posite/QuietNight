package com.example.quietnight.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "sleep_sessions", indices = [Index(value = ["date"], unique = true)])
data class SleepSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val score: Int,
    val snoreTime: Long,           // 밀리초
    val sleepTime: Long,           // 밀리초
    val timelineCsv: String = ""   // 1분 단위 dB 샘플의 CSV ("12,15,40,...")
)

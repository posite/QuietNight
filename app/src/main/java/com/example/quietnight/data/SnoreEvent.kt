package com.example.quietnight.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "snore_events")
data class SnoreEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val timestamp: Long,
    val dbLevel: Int,
    val position: String, // "등", "옆", "배" -> 원인 분석의 핵심
    val isCorrected: Boolean
)

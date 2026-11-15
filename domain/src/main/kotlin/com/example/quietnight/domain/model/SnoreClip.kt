package com.example.quietnight.domain.model

data class SnoreClip(
    val id: Long = 0,
    val date: Long,
    val timestamp: Long,
    val dbLevel: Int,
    val filePath: String,
    val durationMs: Long
)

package com.example.quietnight.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// 코골이 이벤트 오디오 클립. date 로 해당 날짜의 SleepSession 과 연결된다.
// filePath 는 앱 내부 저장소의 WAV 절대 경로.
@Entity(tableName = "snore_clips", indices = [Index(value = ["date"])])
data class SnoreClip(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,          // 소속 날짜(자정 epoch millis)
    val timestamp: Long,     // 클립 발생 시각(epoch millis)
    val dbLevel: Int,        // 감지 당시 dB
    val filePath: String,    // WAV 파일 절대 경로
    val durationMs: Long     // 클립 길이(밀리초)
)

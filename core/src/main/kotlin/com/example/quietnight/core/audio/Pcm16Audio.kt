package com.example.quietnight.core.audio

import kotlin.math.sqrt

// 플랫폼 입출력이나 감지 정책 없이 signed PCM 16-bit 진폭만 계산한다.
object Pcm16Audio {
    // 재사용 버퍼의 앞 sampleCount개만 읽고 원본 샘플을 변경하지 않는다.
    fun rms(samples: ShortArray, sampleCount: Int): Double {
        require(sampleCount in 0..samples.size) { "샘플 수는 버퍼 크기 범위 안이어야 합니다." }
        if (sampleCount == 0) return 0.0

        var sumSq = 0.0
        for (i in 0 until sampleCount) {
            val sample = samples[i].toDouble()
            sumSq += sample * sample
        }
        return sqrt(sumSq / sampleCount)
    }
}

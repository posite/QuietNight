package com.example.quietnight.service

import com.example.quietnight.core.audio.Pcm16Audio
import kotlin.math.log10

// 기존 PCM 진폭 기준의 dB 변환을 유지한다. 물리적인 음압(dB SPL) 보정값은 아니다.
internal fun calculateDecibels(samples: ShortArray, sampleCount: Int): Int {
    val rms = Pcm16Audio.rms(samples, sampleCount)
    return (20 * log10(rms.coerceAtLeast(1.0))).toInt().coerceIn(0, 100)
}

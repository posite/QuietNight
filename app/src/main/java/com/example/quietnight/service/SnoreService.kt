package com.example.quietnight.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.LifecycleService
import com.example.quietnight.R
import com.example.quietnight.data.SettingsRepository
import com.example.quietnight.data.SleepSession
import com.example.quietnight.data.SnoreDao
import com.example.quietnight.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import kotlin.math.log10
import kotlin.math.sqrt

@AndroidEntryPoint
class SnoreService : LifecycleService() {

    @Inject
    lateinit var dao: SnoreDao

    @Inject
    lateinit var settings: SettingsRepository

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var cachedBitmap: Bitmap
    private lateinit var openAppPendingIntent: PendingIntent

    // 세션 누적 상태 (측정 시작 시 DB 값 기준으로 rebaseline)
    private var startTime = 0L
    private var baseSleepMs = 0L
    private var baseSnoreMs = 0L
    private var runSnoreMs = 0L
    private var todayDate = 0L
    private val timeline = mutableListOf<Int>()
    private var minuteMaxDb = 0
    private var lastSampleAt = 0L
    private var lastFlushAt = 0L
    private var lastVibrateAt = 0L

    companion object {
        val dbFlow = MutableStateFlow(0)
        val logFlow = MutableSharedFlow<String>()
        val monitoringFlow = MutableStateFlow(false)
        val startTimeFlow = MutableStateFlow(0L)

        private const val NOTIFICATION_ID = 1004
        private const val CHANNEL_ID = "QuietNightChannel"
        private const val VIBRATE_COOLDOWN_MS = 3000L   // 진동/로그 최소 간격
        private const val FLUSH_INTERVAL_MS = 10_000L    // DB 자동 저장 주기
        private const val SAMPLE_INTERVAL_MS = 60_000L   // 타임라인 샘플 주기
        private const val LOOP_INTERVAL_MS = 100L
    }

    override fun onCreate() {
        super.onCreate()
        cachedBitmap = ContextCompat.getDrawable(this, R.drawable.ic_app_foreground)!!.toBitmap()
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startForeground(NOTIFICATION_ID, createNotification())
        startSession()
        // 시스템에 의해 종료되면 자동 재시작(측정 이어가기). 명시적 stopService 시에는 재시작하지 않음.
        return START_STICKY
    }

    // 오늘 세션 값으로 rebaseline 후 측정 시작. 프로세스가 죽었다 살아나도 DB 값에서 이어진다.
    private fun startSession() {
        if (recordingJob?.isActive == true) return
        serviceScope.launch {
            todayDate = startOfTodayMillis()
            val existing = dao.getSessionByDate(todayDate)
            baseSleepMs = existing?.sleepTime ?: 0L
            baseSnoreMs = existing?.snoreTime ?: 0L
            timeline.clear()
            existing?.timelineCsv
                ?.split(",")
                ?.mapNotNull { it.toIntOrNull() }
                ?.let { timeline.addAll(it) }

            val now = System.currentTimeMillis()
            startTime = now
            runSnoreMs = 0L
            minuteMaxDb = 0
            lastSampleAt = now
            lastFlushAt = now
            lastVibrateAt = 0L
            startTimeFlow.value = now
            monitoringFlow.value = true
            startAnalysis()
        }
    }

    private fun startAnalysis() {
        val bufferSize = AudioRecord.getMinBufferSize(
            44100,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (bufferSize <= 0) {
            monitoringFlow.value = false
            stopSelf()
            return
        }
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // 마이크 권한이 없으면 측정 불가 → 서비스 종료 (UI에서 권한 재요청 안내)
            monitoringFlow.value = false
            stopSelf()
            return
        }

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            44100,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )
        audioRecord?.startRecording()

        recordingJob = serviceScope.launch {
            val audioData = ShortArray(bufferSize)
            while (isActive) {
                val readResult = audioRecord?.read(audioData, 0, bufferSize) ?: 0
                if (readResult > 0) {
                    val rms = sqrt(
                        (0 until readResult)
                            .map { audioData[it].toDouble() * audioData[it] }
                            .average()
                    )
                    val db = (20 * log10(rms.coerceAtLeast(1.0))).toInt().coerceIn(0, 100)
                    dbFlow.emit(db)

                    val now = System.currentTimeMillis()
                    val current = settings.settings.value
                    if (db > current.threshold) {
                        // 임계값 초과 구간은 모두 코골이 시간으로 누적
                        runSnoreMs += LOOP_INTERVAL_MS
                        // 진동/로그는 쿨다운 간격으로만 (연속 진동 방지)
                        if (now - lastVibrateAt >= VIBRATE_COOLDOWN_MS) {
                            if (current.vibrationEnabled) vibrateOnce(current.vibrationStrength)
                            logFlow.emit("교정 실행: ${db}dB")
                            lastVibrateAt = now
                        }
                    }

                    if (db > minuteMaxDb) minuteMaxDb = db
                    if (now - lastSampleAt >= SAMPLE_INTERVAL_MS) {
                        timeline.add(minuteMaxDb)
                        minuteMaxDb = 0
                        lastSampleAt = now
                    }
                    if (now - lastFlushAt >= FLUSH_INTERVAL_MS) {
                        flush(now)
                        lastFlushAt = now
                    }
                }
                delay(LOOP_INTERVAL_MS)
            }
        }
    }

    // 현재까지의 세션을 오늘 행에 upsert. 반복 호출해도 덮어쓰므로 idempotent.
    private suspend fun flush(now: Long) {
        if (startTime == 0L) return
        val sleepMs = baseSleepMs + (now - startTime)
        val snoreMs = baseSnoreMs + runSnoreMs
        val score = if (sleepMs > 0) {
            ((sleepMs - snoreMs).toDouble() / sleepMs * 100).toInt().coerceIn(0, 100)
        } else 0
        val existing = dao.getSessionByDate(todayDate)
        val session = SleepSession(
            id = existing?.id ?: 0,
            date = todayDate,
            score = score,
            snoreTime = snoreMs,
            sleepTime = sleepMs,
            timelineCsv = timeline.joinToString(",")
        )
        if (existing != null) dao.updateSession(session) else dao.insertSession(session)
    }

    private fun vibrateOnce(strength: Int) {
        val vibrator = getSystemService(Vibrator::class.java) ?: return
        // 진동 세기(amplitude)는 1~255 범위여야 하며, 기기가 amplitude 제어를 지원하지 않으면 무시됨
        vibrator.vibrate(VibrationEffect.createOneShot(300, strength.coerceIn(1, 255)))
    }

    private fun createNotification(): Notification {
        val channel = NotificationChannel(CHANNEL_ID, "수면 관리", NotificationManager.IMPORTANCE_HIGH)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("QuietNight 작동 중")
            .setSmallIcon(R.drawable.ic_stat_snore)
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent)
            .setLargeIcon(cachedBitmap)
            .setOnlyAlertOnce(true)
            .build()
    }

    override fun onDestroy() {
        recordingJob?.cancel()
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        // 종료 버튼을 누르지 않아도(자동 저장) 이미 DB에 있으나, 마지막 구간까지 최종 저장
        if (startTime != 0L) {
            runBlocking { flush(System.currentTimeMillis()) }
        }
        monitoringFlow.value = false
        startTimeFlow.value = 0L
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startOfTodayMillis(): Long =
        Instant.ofEpochMilli(System.currentTimeMillis())
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
}

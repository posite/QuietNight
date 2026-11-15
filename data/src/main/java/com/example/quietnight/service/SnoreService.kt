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
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.LifecycleService
import com.example.quietnight.data.R
import com.example.quietnight.domain.repository.SettingsRepository
import com.example.quietnight.domain.monitoring.SnoreDetector
import com.example.quietnight.domain.monitoring.NightRecorder
import com.example.quietnight.domain.monitoring.AudioReadWindow
import com.example.quietnight.domain.model.NightInsights
import com.example.quietnight.domain.model.VibrationPolicy
import com.example.quietnight.data.toInsights
import com.example.quietnight.data.VibrationObservationCodec
import com.example.quietnight.data.SleepSession
import com.example.quietnight.data.SnoreClip
import com.example.quietnight.data.SnoreDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class SnoreService : LifecycleService() {

    @Inject
    lateinit var dao: SnoreDao

    @Inject
    lateinit var settings: SettingsRepository

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var startupJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var cachedBitmap: Bitmap
    private lateinit var openAppPendingIntent: PendingIntent
    private var wakeLock: PowerManager.WakeLock? = null
    private val supportsClips by lazy { resources.getBoolean(R.bool.supports_snore_clips) }

    // '오늘 날짜' 행에 대한 세션 누적 상태. 자정을 넘기면 rebaseline() 으로 다음 날 행으로 이월된다.
    private var todayDate = 0L
    private var startTime = 0L        // 현재 날짜 행의 수면 누적 기준 시각(자정에 리베이스)
    private var baseSleepMs = 0L
    private var baseSnoreMs = 0L
    private var runSnoreMs = 0L
    private val timeline = mutableListOf<Int>()
    private var minuteMaxDb = 0
    private var lastSampleAt = 0L
    private var monitoringCadence: MonitoringCadence? = null
    private val detector = SnoreDetector()
    private var nightRecorder = NightRecorder()

    // 코골이 소리 클립 녹음 상태 (recordClips 설정이 켜진 경우에만 동작)
    private val preRoll = ArrayDeque<ShortArray>()   // 코골이 시작 직전 오디오(프리롤)
    private var preRollSamples = 0
    private var clipRecording = false
    private var clipStartMs = 0L
    private var clipDbLevel = 0
    private var clipSamples = ArrayList<ShortArray>()
    private var clipCountToday = 0    // 오늘 저장된 클립 수(야간 상한 제한)

    companion object {
        val dbFlow = MutableStateFlow(0)
        val logFlow = MutableSharedFlow<String>()
        val monitoringFlow = MutableStateFlow(false)
        val startTimeFlow = MutableStateFlow(0L)   // 세션 표시용 시작 시각(자정 이월 시에도 유지)

        const val ACTION_STOP = "com.example.quietnight.action.STOP"

        private const val NOTIFICATION_ID = 1004
        private const val CHANNEL_ID = "QuietNightChannel"
        private const val SAMPLE_INTERVAL_MS = 60_000L    // 타임라인 샘플 주기
        private const val SAMPLE_RATE = 44100
        private const val READ_CHUNK = SAMPLE_RATE / 10   // 약 100ms 분량(샘플 수)

        // 클립 녹음
        private const val CLIP_MAX_MS = 6000L             // 녹음 개시 이후 최대 길이(프리롤 ~1.5초 별도)
        private const val MAX_CLIPS_PER_NIGHT = 30        // 야간당 저장 상한(저장공간 보호)
        private const val PRE_ROLL_SAMPLES = SAMPLE_RATE * 3 / 2  // 약 1.5초 프리롤
        private const val CLIP_RETENTION_DAYS = 14        // 이 기간 지난 클립은 자동 삭제(용량 제한)
        private const val DAY_MS = 86_400_000L
    }

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
        cachedBitmap = ContextCompat.getDrawable(this, R.drawable.ic_app_foreground)!!.toBitmap()
        // 각 APK의 런처로 돌아간다(휴대폰/워치 화면에 대한 의존성 제거).
        val openAppIntent = requireNotNull(packageManager.getLaunchIntentForPackage(packageName)).apply {
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
        // 알림의 '종료' 액션 → 앱을 열지 않고 측정 정지
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, buildNotification("측정을 시작합니다"))
        startSession()
        // 시스템에 의해 종료되면 자동 재시작(측정 이어가기). 명시적 stopService 시에는 재시작하지 않음.
        return START_STICKY
    }

    // 측정 시작. DB의 오늘 값 기준으로 rebaseline 하므로 프로세스가 죽었다 살아나도 이어진다.
    private fun startSession() {
        if (startupJob?.isActive == true || recordingJob?.isActive == true) return
        startupJob = launchMonitoringTask(serviceScope, onFailure = { stopWithError(it) }) {
            val now = System.currentTimeMillis()
            rebaseline(startOfDayMillis(now), now)
            monitoringCadence = MonitoringCadence(SystemClock.elapsedRealtime())
            startTimeFlow.value = now
            monitoringFlow.value = true
            acquireWakeLock()
            purgeOldClips(now)
            startAnalysis()
        }
    }

    // 보관 기간이 지난 클립의 파일과 DB 행을 삭제(장기 누적 방지). 세션 시작 시 1회 실행.
    private suspend fun purgeOldClips(now: Long) {
        val cutoff = startOfDayMillis(now) - CLIP_RETENTION_DAYS * DAY_MS
        val old = dao.getClipsBefore(cutoff)
        if (old.isEmpty()) return
        old.forEach { runCatching { File(it.filePath).delete() } }
        dao.deleteClipsBefore(cutoff)
    }

    // 주어진 날짜(dayStart) 행의 DB 값으로 누적 상태를 재설정. startSession 과 자정 이월에서 공용.
    private suspend fun rebaseline(dayStart: Long, startAt: Long) {
        todayDate = dayStart
        val existing = dao.getSessionByDate(dayStart)
        nightRecorder = NightRecorder(existing?.toInsights() ?: NightInsights())
        baseSleepMs = existing?.sleepTime ?: 0L
        baseSnoreMs = existing?.snoreTime ?: 0L
        timeline.clear()
        existing?.timelineCsv
            ?.split(",")
            ?.mapNotNull { it.toIntOrNull() }
            ?.let { timeline.addAll(it) }
        startTime = startAt
        runSnoreMs = 0L
        minuteMaxDb = 0
        lastSampleAt = startAt
        // 클립 상태 초기화(자정 이월 시 새 날 기준으로 개수/버퍼 재설정)
        clipCountToday = dao.countClipsByDate(dayStart)
        clipRecording = false
        clipSamples = ArrayList()
        preRoll.clear()
        preRollSamples = 0
        detector.reset()
    }

    private fun startAnalysis() {
        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuffer <= 0) {
            stopWithError()
            return
        }
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // 마이크 권한이 없으면 측정 불가 → 서비스 종료 (UI에서 권한 재요청 안내)
            stopWithError()
            return
        }

        // 내부 버퍼는 바이트 단위. 100ms 청크를 끊김 없이 읽을 수 있도록 여유를 둔다.
        val bufferBytes = maxOf(minBuffer, READ_CHUNK * Short.SIZE_BYTES * 2)
        val record = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferBytes
            )
        } catch (_: Exception) {
            null
        }
        // 다른 앱이 마이크를 점유했거나 초기화에 실패하면 startRecording 이 예외를 던지므로 사전 검증한다.
        if (record == null || record.state != AudioRecord.STATE_INITIALIZED) {
            record?.release()
            stopWithError()
            return
        }
        audioRecord = record
        try {
            record.startRecording()
        } catch (_: IllegalStateException) {
            record.release()
            audioRecord = null
            stopWithError()
            return
        }

        recordingJob = launchMonitoringTask(serviceScope, onFailure = { stopWithError(it) }) {
            val audioData = ShortArray(READ_CHUNK)
            var lastTickAt = SystemClock.elapsedRealtime()
            var failedReadMs = 0L
            var calibrationAudioMs = 0L
            logFlow.emit("처음 20초는 침실 소리를 확인합니다. 이때 진동하지 않습니다.")
            while (isActive) {
                val readResult = audioRecord?.read(audioData, 0, READ_CHUNK) ?: 0
                val now = System.currentTimeMillis()
                val elapsed = SystemClock.elapsedRealtime()
                val coverage = AudioReadWindow.from(readResult, SAMPLE_RATE, elapsed - lastTickAt)
                lastTickAt = elapsed

                // 자정 통과 → 이전 날을 자정 기준으로 마감하고 새 날 행으로 이월
                val currentDay = startOfDayMillis(now)
                if (currentDay != todayDate) {
                    if (clipRecording) finalizeClip()
                    nightRecorder.interrupt()
                    flush(currentDay)
                    rebaseline(currentDay, currentDay)
                    calibrationAudioMs = 0
                    monitoringCadence = MonitoringCadence(elapsed)
                }

                val current = settings.settings.value
                val db = if (readResult > 0) calculateDecibels(audioData, readResult) else 0
                dbFlow.value = db
                nightRecorder.sample(db, current.threshold, coverage.observedMs, coverage.missingMs, elapsed)
                if (coverage.missingMs > 200 || readResult <= 0) detector.reset()
                if (readResult <= 0) {
                    failedReadMs += coverage.missingMs
                    if (failedReadMs >= 5000) {
                        logFlow.emit("마이크 읽기가 끊겨 측정을 종료합니다. 권한과 다른 녹음 앱을 확인해 주세요.")
                        stopWithError()
                        break
                    }
                    delay(20)
                    continue
                }
                failedReadMs = 0
                val previousCalibration = calibrationAudioMs
                calibrationAudioMs += coverage.observedMs
                if (previousCalibration < 20_000 && calibrationAudioMs >= 20_000) {
                    val ambient = nightRecorder.snapshot()
                    logFlow.emit(if (ambient.ambientLoudMs * 2 >= ambient.ambientMs)
                        "침실 소리가 감지 기준을 자주 넘습니다. 주변 소음과 기기 위치를 확인해 주세요."
                    else "20초 환경 확인 완료 · 소리의 출처나 실제 수면 여부는 구분하지 못합니다.")
                }
                val recordClips = supportsClips && current.recordClips

                // 클립 녹음: 프리롤 유지 + 진행 중인 클립에 현재 청크 누적
                if (recordClips) {
                    val chunk = audioData.copyOf(readResult)
                    preRoll.addLast(chunk)
                    preRollSamples += chunk.size
                    while (preRollSamples > PRE_ROLL_SAMPLES && preRoll.size > 1) {
                        preRollSamples -= preRoll.removeFirst().size
                    }
                    if (clipRecording) {
                        clipSamples.add(chunk)
                        if (now - clipStartMs >= CLIP_MAX_MS) finalizeClip()
                    }
                }

                val detection = detector.update(db, current.threshold, coverage.observedMs)
                runSnoreMs += detection.snoreMs
                // 코골이 이벤트 시작 시점에 클립 녹음 개시(1 이벤트 1 클립)
                if (detection.justConfirmed && recordClips) maybeStartClip(now, db)
                if (detection.justConfirmed) logFlow.emit("지속 소리 감지: ${db}dB (진폭 기준)")
                val policy = if (current.gentleVibration) VibrationPolicy.GENTLE else VibrationPolicy.STANDARD
                if (detection.confirmed && current.vibrationEnabled &&
                    nightRecorder.canVibrate(elapsed, now, policy) && vibrateOnce(current.vibrationStrength)) {
                    nightRecorder.vibrationSent(elapsed, now)
                    // 요청 직후 저장해 재시작 시 날짜별 횟수와 휴식 간격을 이어간다.
                    flush(now)
                    monitoringCadence?.markFlushed(elapsed)
                    logFlow.emit("진동 요청 · 1초 뒤부터 5초간 소리 변화를 관찰합니다.")
                }

                if (db > minuteMaxDb) minuteMaxDb = db
                if (now - lastSampleAt >= SAMPLE_INTERVAL_MS) {
                    timeline.add(minuteMaxDb)
                    minuteMaxDb = 0
                    lastSampleAt = now
                }
                val cadence = monitoringCadence ?: continue
                if (cadence.shouldFlush(elapsed)) {
                    flush(now)
                    cadence.markFlushed(elapsed)
                }
                if (cadence.shouldNotify(elapsed)) {
                    updateNotification(now)
                    cadence.markNotified(elapsed)
                }
            }
        }
    }

    // 현재까지의 세션을 오늘 행에 upsert. 반복 호출해도 덮어쓰므로 idempotent.
    private suspend fun flush(now: Long) {
        if (startTime == 0L) return
        val sleepMs = baseSleepMs + (now - startTime).coerceAtLeast(0)
        val snoreMs = baseSnoreMs + runSnoreMs
        val score = if (sleepMs > 0) {
            ((sleepMs - snoreMs).toDouble() / sleepMs * 100).toInt().coerceIn(0, 100)
        } else 0
        val insights = nightRecorder.snapshot()
        val session = SleepSession(
            date = todayDate,
            score = score,
            snoreTime = snoreMs,
            sleepTime = sleepMs,
            timelineCsv = timeline.joinToString(","),
            observedMs = insights.observedMs,
            missingMs = insights.missingMs,
            loudMs = insights.loudMs,
            ambientMs = insights.ambientMs,
            ambientLoudMs = insights.ambientLoudMs,
            vibrationEventsCsv = VibrationObservationCodec.encode(insights.vibrations)
        )
        dao.saveMonitoringSession(session)
    }

    private fun vibrateOnce(strength: Int): Boolean {
        val vibrator = getSystemService(Vibrator::class.java) ?: return false
        if (!vibrator.hasVibrator()) return false
        // 진동 세기(amplitude)는 1~255 범위여야 하며, 기기가 amplitude 제어를 지원하지 않으면 무시됨
        return try {
            vibrator.vibrate(VibrationEffect.createOneShot(300, strength.coerceIn(1, 255)))
            true
        } catch (_: SecurityException) { false }
    }

    // ── 코골이 클립 녹음 ──

    private data class PendingClip(
        val chunks: List<ShortArray>,
        val startedAt: Long,
        val dbLevel: Int,
        val date: Long
    )

    private fun maybeStartClip(now: Long, db: Int) {
        if (clipRecording || clipCountToday >= MAX_CLIPS_PER_NIGHT) return
        clipRecording = true
        clipStartMs = now
        clipDbLevel = db
        clipSamples = ArrayList(preRoll)   // 프리롤(코골이 시작 앞부분)으로 시작
    }

    // 진행 중 클립을 떼어내고 상태를 리셋. 저장 대상이 없으면 null.
    private fun detachClip(): PendingClip? {
        if (!clipRecording) return null
        val chunks = clipSamples
        clipRecording = false
        clipSamples = ArrayList()
        if (chunks.sumOf { it.size } == 0) return null
        clipCountToday++
        return PendingClip(chunks, clipStartMs, clipDbLevel, todayDate)
    }

    private suspend fun persistClip(clip: PendingClip) {
        try {
            val file = writeWav(clip.chunks, clip.startedAt)
            val totalSamples = clip.chunks.sumOf { it.size }
            dao.insertClip(
                SnoreClip(
                    date = clip.date,
                    timestamp = clip.startedAt,
                    dbLevel = clip.dbLevel,
                    filePath = file.absolutePath,
                    durationMs = totalSamples.toLong() * 1000 / SAMPLE_RATE
                )
            )
        } catch (_: Exception) {
            // 파일/DB 오류 시 해당 클립만 건너뛴다(측정은 계속).
        }
    }

    // 루프에서 호출: 파일 쓰기/DB 삽입을 별도 코루틴으로 넘겨 오디오 흐름을 막지 않는다.
    private fun finalizeClip() {
        val clip = detachClip() ?: return
        serviceScope.launch { persistClip(clip) }
    }

    // 16-bit PCM mono WAV 로 저장.
    private fun writeWav(chunks: List<ShortArray>, startedAt: Long): File {
        val dir = File(filesDir, "snore_clips").apply { mkdirs() }
        val file = File(dir, "clip_$startedAt.wav")
        val totalSamples = chunks.sumOf { it.size }
        val dataSize = totalSamples * 2
        val byteRate = SAMPLE_RATE * 2
        FileOutputStream(file).use { fos ->
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            header.put("RIFF".toByteArray(Charsets.US_ASCII))
            header.putInt(36 + dataSize)
            header.put("WAVE".toByteArray(Charsets.US_ASCII))
            header.put("fmt ".toByteArray(Charsets.US_ASCII))
            header.putInt(16)               // fmt 청크 크기
            header.putShort(1.toShort())    // PCM
            header.putShort(1.toShort())    // 모노
            header.putInt(SAMPLE_RATE)
            header.putInt(byteRate)
            header.putShort(2.toShort())    // block align
            header.putShort(16.toShort())   // bits per sample
            header.put("data".toByteArray(Charsets.US_ASCII))
            header.putInt(dataSize)
            fos.write(header.array())

            val data = ByteBuffer.allocate(dataSize).order(ByteOrder.LITTLE_ENDIAN)
            chunks.forEach { chunk -> for (s in chunk) data.putShort(s) }
            fos.write(data.array())
        }
        return file
    }

    private fun stopWithError(error: Throwable? = null) {
        if (error != null) Log.e("SnoreService", "측정 작업이 중단되었습니다.", error)
        monitoringFlow.value = false
        stopSelf()
    }

    private fun ensureChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "수면 관리", NotificationManager.IMPORTANCE_HIGH)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(content: String? = null): Notification {
        val stopIntent = Intent(this, SnoreService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("QuietNight 작동 중")
            .apply { if (content != null) setContentText(content) }
            .setSmallIcon(R.drawable.ic_stat_snore)
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent)
            .setLargeIcon(cachedBitmap)
            .setOnlyAlertOnce(true)
            .addAction(R.drawable.ic_stat_snore, "종료", stopPendingIntent)
            .build()
    }

    // 알림에 경과 시간과 누적 코골이 시간을 표시(1분마다 갱신)
    private fun updateNotification(now: Long) {
        val elapsed = formatElapsed(now - startTimeFlow.value)
        val snoreMin = (baseSnoreMs + runSnoreMs) / 60_000
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification("측정 중 · $elapsed · 코골이 ${snoreMin}분"))
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        // 화면이 꺼진 밤 동안 Doze로 인해 측정 루프 타이밍이 어긋나지 않도록 CPU를 깨워 둔다.
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "QuietNight::Monitoring")
            .apply {
                setReferenceCounted(false)
                acquire()
            }
    }

    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) wakeLock?.release()
        wakeLock = null
    }

    override fun onDestroy() {
        // 루프를 먼저 '완전히' 정지시켜 클립 상태(clipSamples 등) 동시 접근 경합을 없앤다.
        // read() 블로킹이 풀릴 때까지 잠깐 대기하되, 1초로 제한해 ANR 을 피한다.
        runBlocking { withTimeoutOrNull(1000) {
            startupJob?.cancelAndJoin()
            recordingJob?.cancelAndJoin()
        } }
        recordingJob = null
        try {
            audioRecord?.stop()
        } catch (_: Exception) {
            // 이미 정지/해제된 경우 무시
        }
        audioRecord?.release()
        audioRecord = null
        // 진행 중이던 클립을 (scope 취소 전에) 동기로 마무리 저장.
        val pendingClip = detachClip()
        nightRecorder.interrupt()
        // 종료 버튼을 누르지 않아도(자동 저장) 이미 DB에 있으나, 마지막 구간까지 최종 저장.
        // 메인 스레드 블로킹을 2초로 제한해 DB 락 상황에서도 ANR 을 피한다.
        if (startTime != 0L) {
            runBlocking {
                withTimeoutOrNull(2000) {
                    runCatching { flush(System.currentTimeMillis()) }
                        .onFailure { Log.e("SnoreService", "종료 시 측정 기록 저장에 실패했습니다.", it) }
                    pendingClip?.let { persistClip(it) }
                }
            }
        }
        releaseWakeLock()
        dbFlow.value = 0
        monitoringFlow.value = false
        startTimeFlow.value = 0L
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startOfDayMillis(millis: Long): Long =
        Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    private fun formatElapsed(ms: Long): String {
        val totalSec = (ms / 1000).coerceAtLeast(0)
        return String.format(
            Locale.getDefault(),
            "%02d:%02d:%02d",
            totalSec / 3600,
            (totalSec % 3600) / 60,
            totalSec % 60
        )
    }
}

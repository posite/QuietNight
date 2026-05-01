package com.example.quietnight.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.example.quietnight.R
import com.example.quietnight.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.log10
import kotlin.math.sqrt

@AndroidEntryPoint
class SnoreService : LifecycleService() {
    private val CHANNEL_ID = "QuietNightChannel"
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null

    companion object {
        val dbFlow = MutableStateFlow(0)
        val logFlow = MutableSharedFlow<String>()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startForeground(1, createNotification())
        startAnalysis()
        return START_STICKY
    }

    private fun startAnalysis() {
        val bufferSize = AudioRecord.getMinBufferSize(
            44100,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            44100,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )
        audioRecord?.startRecording()

        recordingJob = CoroutineScope(Dispatchers.IO).launch {
            val audioData = ShortArray(bufferSize)
            while (isActive) {
                val readResult = audioRecord?.read(audioData, 0, bufferSize) ?: 0
                if (readResult > 0) {
                    val rms = sqrt(audioData.map { it.toDouble() * it }.average())
                    val db = (20 * log10(rms.coerceAtLeast(1.0))).toInt().coerceIn(0, 100)
                    dbFlow.emit(db)
                    if (db > 75) {
                        (getSystemService(Vibrator::class.java)).vibrate(
                            VibrationEffect.createOneShot(
                                300,
                                150
                            )
                        )
                        logFlow.emit("교정 실행: ${db}dB")
                    }
                }
                delay(100)
            }
        }
    }

    private fun createNotification(): Notification {
        val channel = NotificationChannel(CHANNEL_ID, "수면 관리", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            // 이미 실행 중인 Activity를 재사용
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            // 또는
            // flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_app_foreground)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("QuietNight 작동 중")
            .setSmallIcon(R.drawable.ic_stat_snore)
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent)
            .setLargeIcon(bitmap)
            .build()
    }

    override fun onDestroy() {
        recordingJob?.cancel()
        audioRecord?.stop()
        audioRecord?.release()
        super.onDestroy()
    }
}
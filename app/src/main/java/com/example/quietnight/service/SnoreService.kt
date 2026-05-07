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
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private lateinit var cachedBitmap: Bitmap
    private lateinit var openAppPendingIntent: PendingIntent

    companion object {
        val dbFlow = MutableStateFlow(0)
        val logFlow = MutableSharedFlow<String>()
        private const val NOTIFICATION_ID = 1004
        private const val CHANNEL_ID = "QuietNightChannel"
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
        val initialNotification = createNotification()
        startForeground(NOTIFICATION_ID, initialNotification)
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
//            launch {
//                dbFlow.collect { db ->
//                    updateNotification(db)
//                    delay(1000)
//                }
//            }

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
//        val notificationSmallLayout = RemoteViews(packageName, R.layout.notification_decibel)
//        val notificationLayout = RemoteViews(packageName, R.layout.notification_decibel)


        val channel = NotificationChannel(CHANNEL_ID, "수면 관리", NotificationManager.IMPORTANCE_HIGH)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("QuietNight 작동 중")
            .setSmallIcon(R.drawable.ic_stat_snore)
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent)
            .setLargeIcon(cachedBitmap)
            .setOnlyAlertOnce(true)
//            .setCustomContentView(notificationSmallLayout)
//            .setCustomBigContentView(notificationLayout)
            .build()
    }

//    private fun updateNotification(decibel: Int) {
//        val notificationSmallLayout =
//            RemoteViews(packageName, R.layout.notification_decibel).apply {
//                setTextViewText(R.id.tv_decibel, decibel.toString())
//            }
//        val notificationLayout = RemoteViews(packageName, R.layout.notification_decibel).apply {
//            setTextViewText(R.id.tv_decibel, decibel.toString())
//        }
//
//        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
//            .setContentTitle("QuietNight 작동 중")
//            .setSmallIcon(R.drawable.ic_stat_snore)
//            .setOngoing(true)
//            .setContentIntent(openAppPendingIntent)
//            .setLargeIcon(cachedBitmap)
//            .setOnlyAlertOnce(true)
//            .setCustomContentView(notificationSmallLayout)
//            .setCustomBigContentView(notificationLayout)
//            .build()
//
//        val notificationManager = getSystemService(NotificationManager::class.java)
//        notificationManager.notify(NOTIFICATION_ID, notification)
//    }

    override fun onDestroy() {
        recordingJob?.cancel()
        audioRecord?.stop()
        audioRecord?.release()
        super.onDestroy()
    }
}
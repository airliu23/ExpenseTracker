package com.example.expensetracker.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.expensetracker.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class FunAsrVoiceService : Service() {

    @Inject lateinit var voiceBus: VoiceResultBus
    @Inject lateinit var asrEngine: FunAsrEngine

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var mainLoop: Job? = null
    private var lastPartialText = ""

    companion object {
        const val TAG = "ASR"
        const val CHANNEL_ID = "funasr_voice"
        const val NOTIFY_ID = 3003
        private const val SAMPLE_RATE = 16000
        private const val PARTIAL_INTERVAL_MS = 900L
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFY_ID, buildNotify("🎤 正在聆听"))
        lastPartialText = ""
        voiceBus.clearStopRequest()
        voiceBus.setListening()

        mainLoop = serviceScope.launch(Dispatchers.IO) {
            try {
                val pcm = recordAudioUntilStop()
                if (pcm.isEmpty()) {
                    voiceBus.sendError("未录制到音频")
                    stopAndExit()
                    return@launch
                }
                if (pcm.size < SAMPLE_RATE / 3) {
                    voiceBus.sendError("录音过短，请按住说话")
                    stopAndExit()
                    return@launch
                }

                // 最终识别
                withContext(Dispatchers.Main) {
                    voiceBus.setProcessing()
                    updateNotify("🤔 识别中…")
                }

                val result = asrEngine.recognize(pcm)
                result.onSuccess { text ->
                    Log.i(TAG, "识别成功: \"$text\"")
                    voiceBus.sendResult(text)
                }.onFailure { err ->
                    Log.e(TAG, "识别失败: ${err.message}")
                    voiceBus.sendError(err.message ?: "识别失败")
                }
            } catch (e: Exception) {
                Log.e(TAG, "异常", e)
                voiceBus.sendError("异常: ${e.message}")
            } finally {
                stopAndExit()
            }
        }

        return START_NOT_STICKY
    }

    /** 录音循环 — 直接轮询 @Volatile stopRequested 标志, 松手 → 立即停止 */
    private suspend fun recordAudioUntilStop(): ShortArray {
        val buffer = mutableListOf<Short>()

        val bufSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(SAMPLE_RATE * 2)

        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufSize
        )
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            voiceBus.sendError("麦克风初始化失败")
            return ShortArray(0)
        }
        recorder.startRecording()

        val readBuf = ShortArray(SAMPLE_RATE / 10)
        var lastPartialMs = System.currentTimeMillis()

        while (!voiceBus.stopRequested) {
            val n = recorder.read(readBuf, 0, readBuf.size)
            if (n > 0) {
                for (i in 0 until n) buffer.add(readBuf[i])

                // 增量推理 (实时回显)
                val now = System.currentTimeMillis()
                if (now - lastPartialMs >= PARTIAL_INTERVAL_MS && buffer.size >= SAMPLE_RATE / 2) {
                    lastPartialMs = now
                    try {
                        val text = asrEngine.recognize(buffer.toShortArray()).getOrNull()
                        if (!text.isNullOrBlank() && text != lastPartialText) {
                            lastPartialText = text
                            withContext(Dispatchers.Main) { voiceBus.updatePartial(text) }
                            Log.d(TAG, "部分: \"$text\"")
                        }
                    } catch (_: Exception) {}
                }
            }
        }

        // 停止
        Log.d(TAG, "松手: 已录${buffer.size}样本")
        try { recorder.stop() } catch (_: Exception) {}
        try { recorder.release() } catch (_: Exception) {}
        return buffer.toShortArray()
    }

    private suspend fun stopAndExit() {
        voiceBus.setIdle()
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        mainLoop?.cancel()
        voiceBus.setIdle()
        super.onDestroy()
    }

    private fun updateNotify(text: String) {
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIFY_ID, buildNotify(text))
    }

    private fun buildNotify(text: String) = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("语音记账")
        .setContentText(text)
        .setSmallIcon(android.R.drawable.ic_btn_speak_now)
        .setContentIntent(PendingIntent.getActivity(this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        .setOngoing(true)
        .build()

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "语音记账", NotificationManager.IMPORTANCE_LOW)
                    .apply { description = "语音识别" }
            )
        }
    }
}

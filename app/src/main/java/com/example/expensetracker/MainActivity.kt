package com.example.expensetracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.example.expensetracker.service.MonthSummaryService
import com.example.expensetracker.service.ServiceWatchdog
import com.example.expensetracker.service.VoiceResultBus
import com.example.expensetracker.service.FunAsrVoiceService
import com.example.expensetracker.ui.navigation.NavGraph
import com.example.expensetracker.ui.theme.ExpenseTrackerTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var voiceBus: VoiceResultBus

    private val recordAudioLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) Toast.makeText(this, "需要录音权限", Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkPermissions()
        startMonthSummaryService()

        setContent {
            ExpenseTrackerTheme {
                val navController = rememberNavController()
                var voiceInputText by mutableStateOf("")

                // 收集语音结果和错误
                LaunchedEffect("voiceResults") {
                    voiceBus.results.collect { text -> voiceInputText = text }
                }
                LaunchedEffect("voiceErrors") {
                    voiceBus.errors.collect { err ->
                        Toast.makeText(this@MainActivity, err, Toast.LENGTH_LONG).show()
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavGraph(
                        navController = navController,
                        onVoiceInput = { startVoiceRecognition() },
                        voiceInputText = voiceInputText,
                        onVoiceConsumed = { voiceInputText = "" }
                    )
                }
            }
        }
    }

    /** 启动月度概览通知 */
    private fun startMonthSummaryService() {
        try {
            val intent = Intent(this, MonthSummaryService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (_: Exception) { }
        // 即使这次启动失败也要把看门狗排上，让它在服务被杀后能拉回来
        ServiceWatchdog.schedule(this)
    }

    /** 启动 FunASR Paraformer-Nano 离线语音引擎 */
    private fun startVoiceRecognition() {
        try {
            val intent = Intent(this, FunAsrVoiceService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "语音服务启动失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
            }
        }
    }
}

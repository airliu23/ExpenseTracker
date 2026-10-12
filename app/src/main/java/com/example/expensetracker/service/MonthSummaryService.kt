package com.example.expensetracker.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.expensetracker.MainActivity
import com.example.expensetracker.R
import com.example.expensetracker.domain.usecase.GetStatisticsUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 通知栏月度账单概览服务
 * 启动后每 30 秒更新一次通知内容
 */
@AndroidEntryPoint
class MonthSummaryService : Service() {

    @Inject lateinit var getStatisticsUseCase: GetStatisticsUseCase

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var refreshJob: Job? = null

    companion object {
        const val CHANNEL_ID = "month_summary"
        const val NOTIFY_ID = 4001
        const val TAG = "MonthSummary"

        /**
         * 服务是否存活。看门狗靠它判断要不要重新拉起服务；
         * 进程已被系统杀掉时它自然为 false，正好触发拉起。
         */
        @Volatile
        var isRunning: Boolean = false
            private set
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFY_ID, buildNotify("本月", 0.0, 0.0))
        // MainActivity.onCreate 每次启动都会调 startForegroundService，
        // 这里必须先取消上一条循环：否则每打开一次 App 就多出一条永久轮询，
        // 而旧 Job 的引用被覆盖后再也没有人能取消它。
        refreshJob?.cancel()
        refreshJob = serviceScope.launch {
            while (isActive) {
                updateNotification()
                delay(30_000)
            }
        }
        ServiceWatchdog.schedule(this)
        return START_STICKY
    }

    override fun onBind(i: Intent?): IBinder? = null

    override fun onDestroy() {
        isRunning = false
        serviceScope.cancel()
        super.onDestroy()
    }

    private suspend fun updateNotification() {
        try {
            val monthKey = java.text.SimpleDateFormat("yyyyMM", java.util.Locale.CHINA)
                .format(java.util.Date())
            val expense = getStatisticsUseCase.getTotalExpense(monthKey)
            val income = getStatisticsUseCase.getTotalIncome(monthKey)
            val label = monthKey.substring(0, 4) + "年" + monthKey.substring(4) + "月"

            val summary = if (income > 0) {
                "支出 ¥%.2f  收入 ¥%.2f  余额 ¥%.2f".format(expense, income, income - expense)
            } else {
                "总支出 ¥%.2f".format(expense)
            }

            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(
                NOTIFY_ID, buildNotify(label, expense, income)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Update failed: ${e.message}")
        }
    }

    private fun buildNotify(month: String, expense: Double, income: Double) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("智能记账 · $month 账单")
            .setContentText(
                if (income > 0) {
                    "支出 ¥%.2f  |  收入 ¥%.2f  |  余额 ¥%.2f".format(expense, income, income - expense)
                } else {
                    "总支出 ¥%.2f".format(expense)
                }
            )
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "账单概览", NotificationManager.IMPORTANCE_LOW)
                    .apply { description = "显示本月消费概览" }
            )
        }
    }
}

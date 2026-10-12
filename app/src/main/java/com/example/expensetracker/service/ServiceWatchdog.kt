package com.example.expensetracker.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * 常驻通知看门狗。
 *
 * 系统在空闲期会回收后台服务（华为/荣耀等国产 ROM 尤其激进），通知栏里的月度概览随之消失。
 * 而 Android 12+ 起应用不能在后台启动前台服务，所以服务被杀之后不会自己回来 ——
 * 这就是「放一段时间没用 App，通知就没了」的成因。
 *
 * 这里用 AlarmManager 定时自查：发现服务不在了就重新拉起，然后把下一次检查排上。
 *
 * 前提：Android 12+ 允许后台启动前台服务的豁免条件之一，是「应用已关闭电池优化」。
 * 没有这个豁免，拉起会被系统拒绝 —— 所以设置页里那个开关是这套机制能生效的基础。
 */
object ServiceWatchdog {

    const val TAG = "Watchdog"

    private const val REQUEST_CODE = 5001
    private const val INTERVAL_MS = 15 * 60 * 1000L

    /** 排下一次自查（覆盖式，重复调用不会累积多个闹钟） */
    fun schedule(context: Context) {
        try {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + INTERVAL_MS,
                pendingIntent(context)
            )
        } catch (e: Exception) {
            Log.e(TAG, "排程失败: ${e.message}")
        }
    }

    fun cancel(context: Context) {
        try {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.cancel(pendingIntent(context))
        } catch (_: Exception) {
        }
    }

    private fun pendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, WatchdogReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}

class WatchdogReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (!MonthSummaryService.isRunning) {
            Log.i(ServiceWatchdog.TAG, "检测到概览服务不在，尝试拉起")
            try {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, MonthSummaryService::class.java)
                )
            } catch (e: Exception) {
                // 未关闭电池优化时，Android 12+ 会拒绝后台启动前台服务
                Log.e(ServiceWatchdog.TAG, "拉起失败（可能需要关闭电池优化）: ${e.message}")
            }
        }
        // 无论成功与否都排下一次，否则闹钟只响一次就断了
        ServiceWatchdog.schedule(context)
    }
}

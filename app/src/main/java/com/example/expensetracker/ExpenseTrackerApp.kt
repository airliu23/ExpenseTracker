package com.example.expensetracker

import android.app.Application
import android.util.Log
import com.example.expensetracker.service.FunAsrModelManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class ExpenseTrackerApp : Application() {

    @Inject lateinit var modelManager: FunAsrModelManager

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                Log.i("ASR", "预热模型…")
                modelManager.ensureModel()
                modelManager.loadTokenList()
                modelManager.loadCMVN()
                modelManager.loadSession()
                Log.i("ASR", "模型就绪")
            } catch (e: Exception) {
                Log.e("ASR", "预热失败: ${e.message}", e)
            }
        }
    }
}

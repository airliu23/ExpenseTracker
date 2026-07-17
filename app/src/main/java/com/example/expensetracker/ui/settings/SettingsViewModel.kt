package com.example.expensetracker.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.domain.usecase.ExportImportUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val exportImportUseCase: ExportImportUseCase
) : ViewModel() {

    private val _exporting = MutableStateFlow(false)
    val exporting: StateFlow<Boolean> = _exporting

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast

    /** 导出数据并写入到用户选择的 URI */
    fun exportToUri(uri: Uri) {
        viewModelScope.launch {
            _exporting.value = true
            try {
                val json = withContext(Dispatchers.IO) {
                    exportImportUseCase.exportToJson().getOrThrow()
                }
                withContext(Dispatchers.IO) {
                    appContext.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray())
                    } ?: throw Exception("无法写入文件")
                }
                _toast.value = "导出成功"
            } catch (e: Exception) {
                _toast.value = "导出失败: ${e.message}"
            }
            _exporting.value = false
        }
    }

    /** 从用户选择的 URI 读取并导入 */
    fun importFromUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val json = withContext(Dispatchers.IO) {
                    appContext.contentResolver.openInputStream(uri)?.use { input ->
                        BufferedReader(InputStreamReader(input)).readText()
                    } ?: throw Exception("无法读取文件")
                }

                val result = exportImportUseCase.importFromJson(json).getOrThrow()
                _toast.value = "已导入 ${result.expenseCount} 条记录"
            } catch (e: Exception) {
                _toast.value = "导入失败: ${e.message}"
            }
        }
    }

    fun clearToast() { _toast.value = null }
}

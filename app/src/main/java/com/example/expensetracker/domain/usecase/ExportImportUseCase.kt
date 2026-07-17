package com.example.expensetracker.domain.usecase

import com.example.expensetracker.data.local.entity.CategoryEntity
import com.example.expensetracker.data.local.entity.ExpenseEntity
import com.example.expensetracker.data.repository.ExpenseRepository
import com.example.expensetracker.data.repository.ExportData
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import javax.inject.Inject

class ExportImportUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val moshi: Moshi
) {
    /** 导出全量数据为 JSON 字符串 */
    suspend fun exportToJson(): Result<String> {
        return try {
            val data = expenseRepository.exportAllData()
            val adapter = moshi.adapter(ExportData::class.java)
            val json = adapter.toJson(data)
            Result.success(json)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 从 JSON 字符串导入数据 (重置 id 避免冲突) */
    suspend fun importFromJson(json: String): Result<ImportResult> {
        return try {
            val adapter = moshi.adapter(ExportData::class.java)
            val data = adapter.fromJson(json)
                ?: return Result.failure(IllegalArgumentException("JSON 解析失败: 数据为空"))

            // 重置 ID 让 Room 自动分配，避免与已有数据冲突
            val newExpenses = data.expenses.map { it.copy(id = 0) }
            expenseRepository.importExpenses(newExpenses)

            Result.success(ImportResult(expenseCount = newExpenses.size))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class ImportResult(
    val expenseCount: Int
)

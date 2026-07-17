package com.example.expensetracker.domain.usecase

import com.example.expensetracker.data.repository.DeepSeekRepository
import com.example.expensetracker.domain.model.ExpenseParseResult
import javax.inject.Inject

/**
 * 语音文本 → AI 语义解析 → ExpenseParseResult
 */
class ParseVoiceExpenseUseCase @Inject constructor(
    private val deepSeekRepository: DeepSeekRepository
) {
    suspend operator fun invoke(voiceText: String): Result<ExpenseParseResult> {
        if (voiceText.isBlank()) {
            return Result.failure(IllegalArgumentException("语音文本为空"))
        }
        val result = deepSeekRepository.parseExpense(voiceText)
        return if (result != null) {
            Result.success(result)
        } else {
            Result.failure(Exception("AI 解析失败，请重试"))
        }
    }
}

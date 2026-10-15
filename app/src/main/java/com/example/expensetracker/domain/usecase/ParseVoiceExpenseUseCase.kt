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
        // 直接透传仓储层的结果：失败时它的 message 已经是可以展示给用户的中文原因
        return deepSeekRepository.parseExpense(voiceText)
    }
}

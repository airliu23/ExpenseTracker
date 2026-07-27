package com.example.expensetracker.data.repository

import android.content.SharedPreferences
import com.example.expensetracker.data.remote.DeepSeekApiService
import com.example.expensetracker.data.remote.dto.ChatRequest
import com.example.expensetracker.data.remote.dto.Message
import com.example.expensetracker.domain.model.ExpenseParseResult
import com.squareup.moshi.Moshi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeepSeekRepository @Inject constructor(
    private val apiService: DeepSeekApiService,
    private val securePrefs: SharedPreferences,
    moshi: Moshi
) {
    private val expenseResultAdapter = moshi.adapter(ExpenseParseResult::class.java)

    companion object {
        private val SYSTEM_PROMPT = """
你是一个专业的记账助手，负责将离线语音识别的结果解析为结构化消费记录。

注意：输入来自离线语音引擎，可能包含识别错误（同音字、漏字、错字等）。请先根据上下文理解用户想表达的真实含义，再做解析。

规则:
1. amount: 提取金额数字，只返回数值如 35.5、128
2. category: 必须从以下分类中选择: 餐饮、交通、购物、娱乐、居住、医疗、教育、通讯、其他支出、收入
3. date: 日期，ISO格式 yyyy-MM-dd。若用户没有指明日期，使用今天
4. note: 简短备注，中文，不超过20个字

常见识别错误纠正示例:
- "五星" → 可能是"午"餐 → 金额是星期的可能性低，优先判断为餐饮
- "五块钱" → 可能说"多少钱"，但要结合上下文判断金额
- "买乐" / "埋了" → 可能是"买了"，根据消费语境理解
- "三思" / "伞式" → 可能是"三十"，结合语境判断

请严格只返回以下JSON格式，不要包含markdown代码块标记或其他任何内容:
{"amount": 35.5, "category": "餐饮", "date": "2026-06-23", "note": "午餐外卖"}
        """.trimIndent()
    }

    /**
     * 将语音文本发送给 DeepSeek 解析成消费记录
     * @param voiceText 语音识别结果文本
     * @return ExpenseParseResult 或 null (解析失败时)
     */
    suspend fun parseExpense(voiceText: String): ExpenseParseResult? {
        val model = securePrefs.getString("deepseek_model", "deepseek-chat") ?: "deepseek-chat"
        val request = ChatRequest(
            model = model,
            messages = listOf(
                Message(role = "system", content = SYSTEM_PROMPT),
                Message(role = "user", content = voiceText)
            )
        )

        return try {
            val response = apiService.chatCompletions(request)
            val content = response.choices.firstOrNull()?.message?.content ?: return null
            parseJsonResponse(content)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseJsonResponse(jsonContent: String): ExpenseParseResult? {
        val cleanJson = jsonContent
            .replace("```json", "")
            .replace("```", "")
            .trim()
        return try {
            expenseResultAdapter.fromJson(cleanJson)
        } catch (e: Exception) {
            null
        }
    }
}

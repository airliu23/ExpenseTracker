package com.example.expensetracker.data.repository

import android.content.SharedPreferences
import com.example.expensetracker.data.remote.DeepSeekApiService
import com.example.expensetracker.data.remote.dto.ChatRequest
import com.example.expensetracker.data.remote.dto.Message
import com.example.expensetracker.domain.model.ExpenseParseResult
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
        /** 失败后最多重试次数（含首次共 MAX_RETRIES + 1 次尝试） */
        private const val MAX_RETRIES = 10

        /** 每次重试前的等待间隔，避免立即重试撞上同样的瞬时故障 */
        private const val RETRY_DELAY_MS = 500L

        /** 上一次输出无法解析时，追加到重试请求中的纠正提示 */
        private fun retryHint(today: String) = """

(注意: 你上一次的输出无法被程序解析。请严格只返回一个 JSON 对象，例如 {"amount": 35.5, "category": "餐饮", "date": "$today", "note": "午餐外卖"}。amount 必须是纯数字，不带引号、货币符号或单位；不要包含 markdown 代码块或其他任何文本。)
        """.trimIndent()

        /** 示例中的日期与规则中的"今天"都用动态日期，防止模型抄示例里的静态日期 */
        private fun systemPrompt(today: String) = """
你是一个专业的记账助手，负责将离线语音识别的结果解析为结构化消费记录。

注意：输入来自离线语音引擎，可能包含识别错误（同音字、漏字、错字等）。请先根据上下文理解用户想表达的真实含义，再做解析。

规则:
1. amount: 提取金额数字，只返回数值如 35.5、128
2. category: 必须从以下分类中选择: 餐饮、交通、购物、娱乐、居住、医疗、教育、通讯、其他支出、收入
3. date: 日期，ISO格式 yyyy-MM-dd。今天的日期是 $today。若用户没有指明日期，使用今天
4. note: 简短备注，中文，不超过20个字

常见识别错误纠正示例:
- "五星" → 可能是"午"餐 → 金额是星期的可能性低，优先判断为餐饮
- "五块钱" → 可能说"多少钱"，但要结合上下文判断金额
- "买乐" / "埋了" → 可能是"买了"，根据消费语境理解
- "三思" / "伞式" → 可能是"三十"，结合语境判断

请严格只返回以下JSON格式，不要包含markdown代码块标记或其他任何内容:
{"amount": 35.5, "category": "餐饮", "date": "$today", "note": "午餐外卖"}
        """.trimIndent()
    }

    /**
     * 将语音文本发送给 DeepSeek 解析成消费记录
     *
     * AI 解析会概率性失败（网络抖动、限流、模型返回格式不规范等），
     * 失败后自动重试，最多重试 [MAX_RETRIES] 次；
     * 解析失败时，重试请求会附带纠正提示，让模型修正输出格式。
     *
     * @param voiceText 语音识别结果文本
     * @return ExpenseParseResult 或 null (重试耗尽后仍失败时)
     */
    suspend fun parseExpense(voiceText: String): ExpenseParseResult? {
        val model = securePrefs.getString("deepseek_model", "deepseek-v4-flash")
            ?: "deepseek-v4-flash"
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date())
        var previousAttemptFailed = false

        // 首次尝试 + 最多 MAX_RETRIES 次重试
        repeat(MAX_RETRIES + 1) { attempt ->
            if (attempt > 0) {
                delay(RETRY_DELAY_MS)
            }

            // 上次尝试失败时，追加提示让模型纠正输出格式
            val userContent = if (previousAttemptFailed) voiceText + retryHint(today) else voiceText
            val request = ChatRequest(
                model = model,
                messages = listOf(
                    Message(role = "system", content = systemPrompt(today)),
                    Message(role = "user", content = userContent)
                )
            )

            try {
                val httpResponse = apiService.chatCompletions(request)
                if (!httpResponse.isSuccessful) {
                    previousAttemptFailed = true // HTTP 错误(限流/网关等)
                    return@repeat
                }
                val choice = httpResponse.body()?.choices?.firstOrNull()
                val content = choice?.message?.content
                val result = if (content != null) parseJsonResponse(content) else null
                if (result != null) {
                    return result
                }
                previousAttemptFailed = true // 空响应或 JSON 解析失败
            } catch (e: CancellationException) {
                throw e // 协程取消必须向上传播，不能吞掉
            } catch (e: Exception) {
                previousAttemptFailed = true // 网络错误等
            }
        }
        return null
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

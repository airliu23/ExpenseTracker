package com.example.expensetracker.data.repository

import android.content.SharedPreferences
import com.example.expensetracker.data.remote.DeepSeekApiService
import com.example.expensetracker.data.remote.dto.ChatRequest
import com.example.expensetracker.data.remote.dto.Message
import com.example.expensetracker.domain.model.ExpenseParseResult
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
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
        /**
         * 最多尝试次数（首次 + 重试）。
         *
         * 解析失败绝大多数是永久性问题（Key 无效、余额不足、模型名错误），
         * 重试 10 次既不会变好，还让界面白等几分钟。3 次足够覆盖网络抖动。
         */
        private const val MAX_ATTEMPTS = 3

        /** 每次重试前的等待间隔，避免立即重试撞上同样的瞬时故障 */
        private const val RETRY_DELAY_MS = 800L

        /**
         * 单次解析的硬性总上限。
         *
         * 超过就放弃并把失败原因交给界面 —— 绝不允许出现「AI 解析中…」永久卡住的情况。
         */
        private const val TOTAL_TIMEOUT_MS = 45_000L

        /**
         * 把 HTTP 状态码翻译成用户能看懂、并能据此行动的原因。
         *
         * 以前无论什么错误都只显示「AI 解析失败，请重试」，
         * 用户根本无从判断是 Key 过期、欠费还是模型名写错。
         */
        private fun describeHttpError(code: Int): String = when (code) {
            400 -> "请求被拒绝（HTTP 400），通常是「模型名称」填错了，请到设置里检查"
            401 -> "API Key 无效或已过期（HTTP 401），请到设置里重新填写"
            402 -> "DeepSeek 账户余额不足（HTTP 402），请先充值"
            403 -> "该 API Key 无权访问这个模型（HTTP 403）"
            404 -> "接口地址或模型不存在（HTTP 404），请检查 Endpoint 与模型名称"
            429 -> "请求过于频繁被限流（HTTP 429），请稍后重试"
            in 500..599 -> "DeepSeek 服务端错误（HTTP $code），请稍后重试"
            else -> "请求失败（HTTP $code）"
        }

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
     * 将语音文本发送给 DeepSeek 解析成消费记录。
     *
     * 行为约定：
     * - 整个解析过程受 [TOTAL_TIMEOUT_MS] 约束，绝不无限等待；
     * - 4xx（429 限流除外）属于永久性错误，立即失败并说明具体原因，不做无谓重试；
     * - 只有网络异常、5xx、429 和模型输出格式错误才重试，最多 [MAX_ATTEMPTS] 次；
     * - 失败时返回 [DeepSeekException]，其 message 已经是可直接展示给用户的中文说明。
     *
     * @param voiceText 语音识别结果文本
     * @return 成功时 Result.success(ExpenseParseResult)，失败时 Result.failure(DeepSeekException)
     */
    suspend fun parseExpense(voiceText: String): Result<ExpenseParseResult> {
        return withTimeoutOrNull(TOTAL_TIMEOUT_MS) { parseExpenseAttempts(voiceText) }
            ?: Result.failure(
                DeepSeekException("AI 解析超时（超过 ${TOTAL_TIMEOUT_MS / 1000} 秒），请检查网络后重试")
            )
    }

    private suspend fun parseExpenseAttempts(voiceText: String): Result<ExpenseParseResult> {
        val model = securePrefs.getString("deepseek_model", "deepseek-v4-flash")
            ?: "deepseek-v4-flash"
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date())
        var lastError = "未知原因"
        var lastFailureWasFormat = false

        repeat(MAX_ATTEMPTS) { attempt ->
            if (attempt > 0) {
                delay(RETRY_DELAY_MS)
            }

            // 只有「模型输出格式不对」才值得追加纠正提示；
            // 网络错误或鉴权失败时追加提示毫无帮助，只会让请求更长更慢
            val userContent = if (lastFailureWasFormat) voiceText + retryHint(today) else voiceText
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
                    val code = httpResponse.code()
                    // 4xx（429 限流除外）是请求本身就有问题：鉴权失败、余额不足、模型名错误……
                    // 再重试多少次都是同一个结果，直接失败并把真实原因告诉用户
                    if (code in 400..499 && code != 429) {
                        return Result.failure(DeepSeekException(describeHttpError(code)))
                    }
                    lastError = describeHttpError(code)
                    lastFailureWasFormat = false
                    return@repeat
                }
                val choice = httpResponse.body()?.choices?.firstOrNull()
                val content = choice?.message?.content
                val result = if (content != null) parseJsonResponse(content) else null
                if (result != null) {
                    return Result.success(result)
                }
                lastError = "模型返回的内容无法解析为 JSON"
                lastFailureWasFormat = true
            } catch (e: CancellationException) {
                throw e // 协程取消必须向上传播，不能吞掉
            } catch (e: Exception) {
                lastError = e.message ?: e.javaClass.simpleName
                lastFailureWasFormat = false
            }
        }
        return Result.failure(DeepSeekException("AI 解析失败：$lastError"))
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

/**
 * AI 解析失败。
 *
 * [message] 已经是能直接展示给用户的中文原因，例如
 * 「API Key 无效或已过期（HTTP 401），请到设置里重新填写」。
 */
class DeepSeekException(message: String) : Exception(message)

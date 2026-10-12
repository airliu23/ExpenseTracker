package com.example.expensetracker.domain.model

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 账单领域模型 (纯 Kotlin，与数据库无关)
 */
data class Expense(
    val id: Long = 0,
    val amount: Double,
    val category: ExpenseCategory,
    val categoryName: String,
    val note: String = "",
    val date: Long,             // 消费日期时间戳
    val createdAt: Long = System.currentTimeMillis(),
    val source: ExpenseSource = ExpenseSource.MANUAL
) {
    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("MM月dd日 HH:mm", Locale.CHINA)
            return sdf.format(Date(date))
        }

    val isExpense: Boolean
        get() = category != ExpenseCategory.INCOME

    val displayAmount: String
        get() = if (category == ExpenseCategory.INCOME) {
            "+¥%.2f".format(amount)
        } else {
            "-¥%.2f".format(amount)
        }
}

enum class ExpenseSource(val value: String) {
    VOICE("voice"),
    MANUAL("manual")
}

/** AI 解析语音的结果 */
data class ExpenseParseResult(
    val amount: Double,
    val category: String,   // AI 返回的分类显示名
    val date: String,       // ISO 日期格式 yyyy-MM-dd
    val note: String
) {
    fun toExpense(): Expense {
        val cat = ExpenseCategory.fromDisplayName(category)
        return Expense(
            amount = amount,
            category = cat,
            categoryName = cat.displayName,
            note = note,
            date = resolveDate(date),
            source = ExpenseSource.VOICE
        )
    }

    companion object {
        private const val DATE_PATTERN = "yyyy-MM-dd"

        /**
         * 决定这笔账使用哪个时间戳。
         *
         * - AI 解析出的日期**就是今天** → 用**当前时刻**。
         *   用户没指明日期时（"刚买了杯咖啡"），AI 按提示词规则返回今天；
         *   若这时退化成"今天 00:00"，界面上时间就成了 00:00，
         *   与"此刻记的账"不符。
         * - AI 解析出的是**别的日期** → 用**那一天**（补记历史消费）。
         *   AI 只给到天、没有时刻，因此取当天 00:00。
         * - 解析失败（模型偶尔返回非日期内容）→ 回退当前时刻，不让保存失败。
         */
        fun resolveDate(iso: String): Long {
            val parsed = parseIsoDate(iso) ?: return System.currentTimeMillis()
            val now = System.currentTimeMillis()
            return if (isSameDay(parsed, now)) now else parsed
        }

        /**
         * 把 AI 返回的 yyyy-MM-dd 解析为「设备本地当天 00:00」的时间戳。
         *
         * 用非宽松模式：宽松模式会把 "2026-13-45"、"2026-02-30" 这类脏数据
         * 悄悄规范化成别的日期，宁可解析失败走回退分支。
         */
        fun parseIsoDate(iso: String): Long? {
            if (iso.isBlank()) return null
            return try {
                SimpleDateFormat(DATE_PATTERN, Locale.CHINA)
                    .apply { isLenient = false }
                    .parse(iso.trim())
                    ?.time
            } catch (e: ParseException) {
                null
            }
        }

        private fun isSameDay(a: Long, b: Long): Boolean {
            val ca = Calendar.getInstance().apply { timeInMillis = a }
            val cb = Calendar.getInstance().apply { timeInMillis = b }
            return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) &&
                    ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
        }
    }
}

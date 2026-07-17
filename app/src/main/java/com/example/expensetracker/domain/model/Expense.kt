package com.example.expensetracker.domain.model

import java.text.SimpleDateFormat
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
            date = System.currentTimeMillis(),
            source = ExpenseSource.VOICE
        )
    }
}

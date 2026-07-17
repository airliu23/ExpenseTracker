package com.example.expensetracker.domain.usecase

import com.example.expensetracker.data.repository.ExpenseRepository
import com.example.expensetracker.domain.model.Expense
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class GetExpensesUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository
) {
    /** 获取所有账单 */
    operator fun invoke(): Flow<List<Expense>> = expenseRepository.getAllExpenses()

    /** 按月份获取账单 (yyyyMM) */
    fun byMonth(monthKey: String): Flow<List<Expense>> =
        expenseRepository.getExpensesByMonth(monthKey)

    /** 获取当前月份的 key (yyyyMM) */
    fun currentMonthKey(): String {
        val sdf = SimpleDateFormat("yyyyMM", Locale.CHINA)
        return sdf.format(Date())
    }
}

package com.example.expensetracker.domain.usecase

import com.example.expensetracker.domain.model.Expense
import com.example.expensetracker.data.repository.ExpenseRepository
import javax.inject.Inject

class GetCategoryDetailUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository
) {
    /** 获取某分类在某月 (yyyyMM) 的所有账单 */
    suspend fun byCategoryAndMonth(categoryName: String, monthKey: String): List<Expense> =
        expenseRepository.getExpensesByCategoryAndMonth(categoryName, monthKey)

    /** 获取某分类在某年 (yyyy) 的所有账单 */
    suspend fun byCategoryAndYear(categoryName: String, year: String): List<Expense> =
        expenseRepository.getExpensesByCategoryAndYear(categoryName, year)
}

package com.example.expensetracker.domain.usecase

import com.example.expensetracker.data.local.dao.CategorySummary
import com.example.expensetracker.data.local.dao.MonthlyTotal
import com.example.expensetracker.data.repository.ExpenseRepository
import javax.inject.Inject

class GetStatisticsUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository
) {
    // ── 月度统计 ──

    suspend fun getTotalExpense(monthKey: String): Double =
        expenseRepository.getTotalExpenseByMonth(monthKey)

    suspend fun getTotalIncome(monthKey: String): Double =
        expenseRepository.getTotalIncomeByMonth(monthKey)

    suspend fun getCategorySummary(monthKey: String): List<CategorySummary> =
        expenseRepository.getExpenseSummaryByCategory(monthKey)

    suspend fun getCategorySummaryAll(monthKey: String): List<CategorySummary> =
        expenseRepository.getCategorySummaryByMonthAll(monthKey)

    suspend fun getMonthlyTrend(months: Int = 6): List<MonthlyTotal> =
        expenseRepository.getMonthlyTrend(months)

    // ── 年度统计 ──

    suspend fun getTotalExpenseByYear(year: String): Double =
        expenseRepository.getTotalExpenseByYear(year)

    suspend fun getTotalIncomeByYear(year: String): Double =
        expenseRepository.getTotalIncomeByYear(year)

    suspend fun getCategorySummaryByYear(year: String): List<CategorySummary> =
        expenseRepository.getExpenseSummaryByCategoryForYear(year)

    suspend fun getCategorySummaryByYearAll(year: String): List<CategorySummary> =
        expenseRepository.getCategorySummaryByYearAll(year)

    suspend fun getYearlyTrend(years: Int = 5): List<MonthlyTotal> =
        expenseRepository.getYearlyTrend(years)
}

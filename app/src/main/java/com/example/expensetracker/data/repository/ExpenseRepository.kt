package com.example.expensetracker.data.repository

import com.example.expensetracker.data.local.dao.CategoryDao
import com.example.expensetracker.data.local.dao.CategorySummary
import com.example.expensetracker.data.local.dao.MonthlyTotal
import com.example.expensetracker.data.local.dao.ExpenseDao
import com.example.expensetracker.data.local.entity.CategoryEntity
import com.example.expensetracker.data.local.entity.ExpenseEntity
import com.example.expensetracker.domain.model.Expense
import com.example.expensetracker.domain.model.ExpenseCategory
import com.example.expensetracker.domain.model.ExpenseSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseRepository @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val categoryDao: CategoryDao
) {

    // ── 账单操作 ──

    fun getAllExpenses(): Flow<List<Expense>> =
        expenseDao.getAllExpenses().map { entities -> entities.map { it.toDomain() } }

    fun getExpensesByMonth(monthKey: String): Flow<List<Expense>> =
        expenseDao.getExpensesByMonth(monthKey).map { entities -> entities.map { it.toDomain() } }

    suspend fun addExpense(expense: Expense): Long {
        return expenseDao.insert(expense.toEntity())
    }

    suspend fun updateExpense(expense: Expense) {
        expenseDao.update(expense.toEntity())
    }

    suspend fun deleteExpense(id: Long) {
        expenseDao.deleteById(id)
    }

    suspend fun getExpenseById(id: Long): Expense? {
        return expenseDao.getById(id)?.toDomain()
    }

    // ── 统计查询 ──

    suspend fun getTotalExpenseByMonth(monthKey: String): Double =
        expenseDao.getTotalExpenseByMonth(monthKey)

    suspend fun getTotalIncomeByMonth(monthKey: String): Double =
        expenseDao.getTotalIncomeByMonth(monthKey)

    suspend fun getExpenseSummaryByCategory(monthKey: String): List<CategorySummary> =
        expenseDao.getExpenseSummaryByCategory(monthKey)

    suspend fun getCategorySummaryByMonthAll(monthKey: String): List<CategorySummary> =
        expenseDao.getCategorySummaryByMonthAll(monthKey)

    suspend fun getMonthlyTrend(months: Int = 12): List<MonthlyTotal> =
        expenseDao.getMonthlyTrend(months)

    // ── 年度统计 ──

    suspend fun getTotalExpenseByYear(year: String): Double =
        expenseDao.getTotalExpenseByYear(year)

    suspend fun getTotalIncomeByYear(year: String): Double =
        expenseDao.getTotalIncomeByYear(year)

    suspend fun getExpenseSummaryByCategoryForYear(year: String): List<CategorySummary> =
        expenseDao.getExpenseSummaryByCategoryForYear(year)

    suspend fun getCategorySummaryByYearAll(year: String): List<CategorySummary> =
        expenseDao.getCategorySummaryByYearAll(year)

    suspend fun getYearlyTrend(years: Int = 5): List<MonthlyTotal> =
        expenseDao.getYearlyTrend(years)

    // ── 分类明细查询 ──

    suspend fun getExpensesByCategoryAndMonth(categoryName: String, monthKey: String): List<Expense> =
        expenseDao.getExpensesByCategoryAndMonth(categoryName, monthKey).map { it.toDomain() }

    suspend fun getExpensesByCategoryAndYear(categoryName: String, year: String): List<Expense> =
        expenseDao.getExpensesByCategoryAndYear(categoryName, year).map { it.toDomain() }

    // ── 数据导出 / 导入 ──

    suspend fun exportAllData(): ExportData {
        val expenses = expenseDao.getAllExpensesOnce()
        val categories = categoryDao.getAllCategoriesOnce()
        return ExportData(expenses = expenses, categories = categories)
    }

    suspend fun importExpenses(expenses: List<ExpenseEntity>) {
        expenseDao.insertAll(expenses)
    }

    // ── 分类操作 ──

    fun getAllCategories(): Flow<List<CategoryEntity>> =
        categoryDao.getAllCategories()

    suspend fun initDefaultCategories() {
        if (categoryDao.count() == 0) {
            categoryDao.insertAll(
                ExpenseCategory.entries.map { cat ->
                    CategoryEntity(
                        name = cat.displayName,
                        icon = cat.iconName,
                        color = cat.colorHex,
                        isDefault = true
                    )
                }
            )
        }
    }

    suspend fun addCustomCategory(category: CategoryEntity): Long {
        return categoryDao.insert(category.copy(isDefault = false))
    }

    suspend fun deleteCategory(id: Long) {
        categoryDao.deleteById(id)
    }

    // ── 映射 ──

    private fun ExpenseEntity.toDomain(): Expense = Expense(
        id = id,
        amount = amount,
        category = ExpenseCategory.fromDisplayName(categoryName),
        categoryName = categoryName,
        note = note,
        date = date,
        createdAt = createdAt,
        source = if (source == "voice") ExpenseSource.VOICE else ExpenseSource.MANUAL
    )

    private fun Expense.toEntity(): ExpenseEntity = ExpenseEntity(
        id = id,
        amount = amount,
        categoryId = 0, // 不严格要求外键，用 categoryName 即可
        categoryName = categoryName,
        note = note,
        date = date,
        createdAt = createdAt,
        source = source.value
    )
}

/** 数据库导出结构 (用于 JSON 序列化备份) */
data class ExportData(
    val expenses: List<ExpenseEntity>,
    val categories: List<CategoryEntity>
)

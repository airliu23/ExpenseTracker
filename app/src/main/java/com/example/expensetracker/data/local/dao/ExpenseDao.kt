package com.example.expensetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.expensetracker.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    /** 按日期降序返回所有账单 */
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    /** 按月份筛选账单 (yyyyMM格式参数) */
    @Query("""
        SELECT * FROM expenses
        WHERE strftime('%Y%m', date / 1000, 'unixepoch', 'localtime') = :monthKey
        ORDER BY date DESC
    """)
    fun getExpensesByMonth(monthKey: String): Flow<List<ExpenseEntity>>

    /** 按日期范围筛选 */
    @Query("""
        SELECT * FROM expenses
        WHERE date BETWEEN :startMs AND :endMs
        ORDER BY date DESC
    """)
    fun getExpensesByDateRange(startMs: Long, endMs: Long): Flow<List<ExpenseEntity>>

    /** 按分类查询 */
    @Query("SELECT * FROM expenses WHERE category_id = :categoryId ORDER BY date DESC")
    fun getExpensesByCategory(categoryId: Long): Flow<List<ExpenseEntity>>

    /** 单月汇总: 总支出 (排除收入) */
    @Query("""
        SELECT COALESCE(SUM(amount), 0) FROM expenses
        WHERE strftime('%Y%m', date / 1000, 'unixepoch', 'localtime') = :monthKey
        AND category_name != '收入'
    """)
    suspend fun getTotalExpenseByMonth(monthKey: String): Double

    /** 单月汇总: 总收入 */
    @Query("""
        SELECT COALESCE(SUM(amount), 0) FROM expenses
        WHERE strftime('%Y%m', date / 1000, 'unixepoch', 'localtime') = :monthKey
        AND category_name = '收入'
    """)
    suspend fun getTotalIncomeByMonth(monthKey: String): Double

    /** 按月+分类汇总支出 (排除收入) */
    @Query("""
        SELECT category_name, COALESCE(SUM(amount), 0) as total
        FROM expenses
        WHERE strftime('%Y%m', date / 1000, 'unixepoch', 'localtime') = :monthKey
        AND category_name != '收入'
        GROUP BY category_name
        ORDER BY total DESC
    """)
    suspend fun getExpenseSummaryByCategory(monthKey: String): List<CategorySummary>

    /** 按月+分类汇总 (包含收入，收入排最前面) */
    @Query("""
        SELECT category_name, COALESCE(SUM(amount), 0) as total
        FROM expenses
        WHERE strftime('%Y%m', date / 1000, 'unixepoch', 'localtime') = :monthKey
        GROUP BY category_name
        ORDER BY CASE WHEN category_name = '收入' THEN -1 ELSE 1 END, total DESC
    """)
    suspend fun getCategorySummaryByMonthAll(monthKey: String): List<CategorySummary>

    /** 最近 N 个月的月总支出趋势 */
    @Query("""
        SELECT strftime('%Y%m', date / 1000, 'unixepoch', 'localtime') as month, COALESCE(SUM(amount), 0) as total
        FROM expenses
        WHERE category_name != '收入'
        GROUP BY month
        ORDER BY month DESC
        LIMIT :n
    """)
    suspend fun getMonthlyTrend(n: Int): List<MonthlyTotal>

    // ── 年度统计 ──

    /** 按年汇总支出 (排除收入) */
    @Query("""
        SELECT COALESCE(SUM(amount), 0) FROM expenses
        WHERE strftime('%Y', date / 1000, 'unixepoch', 'localtime') = :year
        AND category_name != '收入'
    """)
    suspend fun getTotalExpenseByYear(year: String): Double

    /** 按年汇总收入 */
    @Query("""
        SELECT COALESCE(SUM(amount), 0) FROM expenses
        WHERE strftime('%Y', date / 1000, 'unixepoch', 'localtime') = :year
        AND category_name = '收入'
    """)
    suspend fun getTotalIncomeByYear(year: String): Double

    /** 按年+分类汇总支出 (排除收入) */
    @Query("""
        SELECT category_name, COALESCE(SUM(amount), 0) as total
        FROM expenses
        WHERE strftime('%Y', date / 1000, 'unixepoch', 'localtime') = :year
        AND category_name != '收入'
        GROUP BY category_name
        ORDER BY total DESC
    """)
    suspend fun getExpenseSummaryByCategoryForYear(year: String): List<CategorySummary>

    /** 按年+分类汇总 (包含收入，收入排最前面) */
    @Query("""
        SELECT category_name, COALESCE(SUM(amount), 0) as total
        FROM expenses
        WHERE strftime('%Y', date / 1000, 'unixepoch', 'localtime') = :year
        GROUP BY category_name
        ORDER BY CASE WHEN category_name = '收入' THEN -1 ELSE 1 END, total DESC
    """)
    suspend fun getCategorySummaryByYearAll(year: String): List<CategorySummary>

    /** 最近 N 年的年总支出趋势 */
    @Query("""
        SELECT strftime('%Y', date / 1000, 'unixepoch', 'localtime') as month, COALESCE(SUM(amount), 0) as total
        FROM expenses
        WHERE category_name != '收入'
        GROUP BY month
        ORDER BY month DESC
        LIMIT :n
    """)
    suspend fun getYearlyTrend(n: Int): List<MonthlyTotal>

    // ── 分类明细查询 ──

    /** 某分类在某月的所有记录 */
    @Query("""
        SELECT * FROM expenses
        WHERE category_name = :categoryName
        AND strftime('%Y%m', date / 1000, 'unixepoch', 'localtime') = :monthKey
        ORDER BY date DESC
    """)
    suspend fun getExpensesByCategoryAndMonth(categoryName: String, monthKey: String): List<ExpenseEntity>

    /** 某分类在某年的所有记录 */
    @Query("""
        SELECT * FROM expenses
        WHERE category_name = :categoryName
        AND strftime('%Y', date / 1000, 'unixepoch', 'localtime') = :year
        ORDER BY date DESC
    """)
    suspend fun getExpensesByCategoryAndYear(categoryName: String, year: String): List<ExpenseEntity>

    // ── 数据导出 / 导入 ──

    /** 一次性获取全部账单 (用于导出) */
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    suspend fun getAllExpensesOnce(): List<ExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: ExpenseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(expenses: List<ExpenseEntity>)

    @Update
    suspend fun update(expense: ExpenseEntity)

    @Delete
    suspend fun delete(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getById(id: Long): ExpenseEntity?
}

/** 分类汇总结果 */
data class CategorySummary(
    val category_name: String,
    val total: Double
)

/** 月度总计结果 */
data class MonthlyTotal(
    val month: String,
    val total: Double
)

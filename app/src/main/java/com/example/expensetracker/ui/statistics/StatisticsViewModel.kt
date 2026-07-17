package com.example.expensetracker.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.local.dao.CategorySummary
import com.example.expensetracker.data.local.dao.MonthlyTotal
import com.example.expensetracker.domain.usecase.GetStatisticsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

enum class PeriodType { MONTH, YEAR }

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val getStatisticsUseCase: GetStatisticsUseCase
) : ViewModel() {

    private val now = Calendar.getInstance()
    private val monthSdf = SimpleDateFormat("yyyyMM", Locale.CHINA)
    private val yearSdf = SimpleDateFormat("yyyy", Locale.CHINA)

    // ── 时间状态 ──

    private val _periodType = MutableStateFlow(PeriodType.MONTH)
    val periodType: StateFlow<PeriodType> = _periodType

    private val _selectedYear = MutableStateFlow(yearSdf.format(Date()))
    val selectedYear: StateFlow<String> = _selectedYear

    private val _selectedMonth = MutableStateFlow(monthSdf.format(Date()))
    val selectedMonth: StateFlow<String> = _selectedMonth

    /** 用于显示的月份标签，如 "2025年6月" */
    val monthDisplayLabel: String
        get() {
            val m = _selectedMonth.value
            return "${m.substring(0, 4)}年${m.substring(4).toInt()}月"
        }

    /** 用于显示的年份标签，如 "2025年" */
    val yearDisplayLabel: String
        get() = "${_selectedYear.value}年"

    // ── 统计数据 ──

    private val _totalExpense = MutableStateFlow(0.0)
    val totalExpense: StateFlow<Double> = _totalExpense

    private val _totalIncome = MutableStateFlow(0.0)
    val totalIncome: StateFlow<Double> = _totalIncome

    /** 全部分类汇总（含收入，收入排最前） */
    private val _categorySummary = MutableStateFlow<List<CategorySummary>>(emptyList())
    val categorySummary: StateFlow<List<CategorySummary>> = _categorySummary

    private val _trend = MutableStateFlow<List<MonthlyTotal>>(emptyList())
    val trend: StateFlow<List<MonthlyTotal>> = _trend

    init {
        loadStatistics()
    }

    // ── 切换 月度/年度 ──

    fun selectPeriodType(type: PeriodType) {
        if (_periodType.value != type) {
            _periodType.value = type
            loadStatistics()
        }
    }

    // ── 月份导航 ──

    fun goToPreviousMonth() {
        val current = _selectedMonth.value
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, current.substring(0, 4).toInt())
            set(Calendar.MONTH, current.substring(4).toInt() - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        cal.add(Calendar.MONTH, -1)
        _selectedMonth.value = monthSdf.format(cal.time)
        loadStatistics()
    }

    fun goToNextMonth() {
        val current = _selectedMonth.value
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, current.substring(0, 4).toInt())
            set(Calendar.MONTH, current.substring(4).toInt() - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        cal.add(Calendar.MONTH, 1)
        val nextMonth = monthSdf.format(cal.time)
        // 不允许超过当前月份
        if (nextMonth <= monthSdf.format(Date())) {
            _selectedMonth.value = nextMonth
            loadStatistics()
        }
    }

    // ── 年份导航 ──

    fun goToPreviousYear() {
        val y = _selectedYear.value.toInt() - 1
        _selectedYear.value = y.toString()
        loadStatistics()
    }

    fun goToNextYear() {
        val y = _selectedYear.value.toInt() + 1
        val currentYear = yearSdf.format(Date()).toInt()
        if (y <= currentYear) {
            _selectedYear.value = y.toString()
            loadStatistics()
        }
    }

    // ── 加载 ──

    fun loadStatistics() {
        viewModelScope.launch {
            if (_periodType.value == PeriodType.MONTH) {
                val monthKey = _selectedMonth.value
                _totalExpense.value = getStatisticsUseCase.getTotalExpense(monthKey)
                _totalIncome.value = getStatisticsUseCase.getTotalIncome(monthKey)
                _categorySummary.value = getStatisticsUseCase.getCategorySummaryAll(monthKey)
                _trend.value = getStatisticsUseCase.getMonthlyTrend(12)
            } else {
                val yearKey = _selectedYear.value
                _totalExpense.value = getStatisticsUseCase.getTotalExpenseByYear(yearKey)
                _totalIncome.value = getStatisticsUseCase.getTotalIncomeByYear(yearKey)
                _categorySummary.value = getStatisticsUseCase.getCategorySummaryByYearAll(yearKey)
                _trend.value = getStatisticsUseCase.getYearlyTrend(5)
            }
        }
    }
}

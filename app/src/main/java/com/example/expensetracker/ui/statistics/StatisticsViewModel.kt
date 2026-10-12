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

    private val monthSdf = SimpleDateFormat("yyyyMM", Locale.CHINA)
    private val yearSdf = SimpleDateFormat("yyyy", Locale.CHINA)

    // ── 时间状态 ──

    private val _periodType = MutableStateFlow(PeriodType.MONTH)
    val periodType: StateFlow<PeriodType> = _periodType

    private val _selectedYear = MutableStateFlow(yearSdf.format(Date()))
    val selectedYear: StateFlow<String> = _selectedYear

    private val _selectedMonth = MutableStateFlow(monthSdf.format(Date()))
    val selectedMonth: StateFlow<String> = _selectedMonth

    // 注意：这里不要再放 "monthDisplayLabel" 之类的普通 getter 供 UI 直接读取。
    // StateFlow.value 不属于 Compose 快照系统，UI 读它不会建立订阅，
    // 月份变化时不会触发重组。显示标签请在 Composable 里由 selectedMonth/selectedYear 派生。

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

    /**
     * 以 yyyyMM 为基准平移月份。
     *
     * 必须一次性 set(year, month, 1, 0, 0, 0)：Calendar 默认是宽松模式，
     * 若先单独改 MONTH、而 DAY_OF_MONTH 还停在 29~31，遇到短月份会溢出
     * （例：当前 31 号、把月份设成 2 月 → 2 月 31 日被规范化成 3 月 3 日）。
     */
    private fun shiftMonth(monthKey: String, delta: Int): String {
        val cal = Calendar.getInstance().apply {
            set(monthKey.substring(0, 4).toInt(), monthKey.substring(4).toInt() - 1, 1, 0, 0, 0)
        }
        cal.add(Calendar.MONTH, delta)
        return monthSdf.format(cal.time)
    }

    fun goToPreviousMonth() {
        _selectedMonth.value = shiftMonth(_selectedMonth.value, -1)
        loadStatistics()
    }

    fun goToNextMonth() {
        val nextMonth = shiftMonth(_selectedMonth.value, 1)
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

package com.example.expensetracker.ui.statistics

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.domain.model.Expense
import com.example.expensetracker.domain.usecase.GetCategoryDetailUseCase
import com.example.expensetracker.domain.usecase.GetStatisticsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getCategoryDetailUseCase: GetCategoryDetailUseCase,
    private val getStatisticsUseCase: GetStatisticsUseCase
) : ViewModel() {

    private val categoryName: String = savedStateHandle["categoryName"] ?: ""
    private val periodType: String = savedStateHandle["periodType"] ?: PeriodType.MONTH.name
    private val periodKey: String = savedStateHandle["periodKey"] ?: ""

    val displayTitle: String = categoryName
    val isIncome: Boolean = categoryName == "收入"

    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    val expenses: StateFlow<List<Expense>> = _expenses

    private val _categoryTotal = MutableStateFlow(0.0)
    val categoryTotal: StateFlow<Double> = _categoryTotal

    // 基数：收入则用总收入，支出则用总支出（用于计算占比）
    private val _baseAmount = MutableStateFlow(0.0)
    val baseAmount: StateFlow<Double> = _baseAmount

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            if (periodType == PeriodType.MONTH.name) {
                _expenses.value = getCategoryDetailUseCase.byCategoryAndMonth(categoryName, periodKey)
                // 用包含收入的全体分类汇总来查
                _categoryTotal.value = getStatisticsUseCase.getCategorySummaryAll(periodKey)
                    .firstOrNull { it.category_name == categoryName }?.total ?: 0.0
                _baseAmount.value = if (isIncome)
                    getStatisticsUseCase.getTotalIncome(periodKey)
                else
                    getStatisticsUseCase.getTotalExpense(periodKey)
            } else {
                _expenses.value = getCategoryDetailUseCase.byCategoryAndYear(categoryName, periodKey)
                _categoryTotal.value = getStatisticsUseCase.getCategorySummaryByYearAll(periodKey)
                    .firstOrNull { it.category_name == categoryName }?.total ?: 0.0
                _baseAmount.value = if (isIncome)
                    getStatisticsUseCase.getTotalIncomeByYear(periodKey)
                else
                    getStatisticsUseCase.getTotalExpenseByYear(periodKey)
            }
        }
    }

    fun getPercentage(): Double {
        val base = _baseAmount.value
        return if (base > 0) (_categoryTotal.value / base * 100) else 0.0
    }
}

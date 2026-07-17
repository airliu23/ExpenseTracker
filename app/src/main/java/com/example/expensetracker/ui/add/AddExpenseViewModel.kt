package com.example.expensetracker.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.domain.model.Expense
import com.example.expensetracker.domain.model.ExpenseCategory
import com.example.expensetracker.domain.model.ExpenseParseResult
import com.example.expensetracker.domain.model.ExpenseSource
import com.example.expensetracker.domain.usecase.AddExpenseUseCase
import com.example.expensetracker.domain.usecase.ParseVoiceExpenseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class AddExpenseViewModel @Inject constructor(
    private val addExpenseUseCase: AddExpenseUseCase,
    private val parseVoiceExpenseUseCase: ParseVoiceExpenseUseCase,
) : ViewModel() {

    // 用户输入的文字描述
    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText

    // AI 解析状态
    private val _parsing = MutableStateFlow(false)
    val parsing: StateFlow<Boolean> = _parsing

    // AI 解析结果
    private val _parsedResult = MutableStateFlow<ExpenseParseResult?>(null)
    val parsedResult: StateFlow<ExpenseParseResult?> = _parsedResult

    // 一次性的成功/错误事件（用 Channel 保证不丢）
    private val _saveSuccess = Channel<Unit>(Channel.BUFFERED)
    val saveSuccess = _saveSuccess.receiveAsFlow()

    private val _errorEvent = Channel<String>(Channel.BUFFERED)
    val errorEvent = _errorEvent.receiveAsFlow()

    fun setInputText(value: String) { _inputText.value = value }

    /** AI 解析用户输入的文字描述 */
    fun parseInput() {
        val text = _inputText.value.trim()
        if (text.isBlank()) {
            _errorEvent.trySend("请输入消费描述")
            return
        }
        viewModelScope.launch {
            _parsing.value = true
            _parsedResult.value = null
            parseVoiceExpenseUseCase(text)
                .onSuccess { result ->
                    _parsedResult.value = result
                }
                .onFailure { error ->
                    _errorEvent.trySend(error.message ?: "AI 解析失败，请重试")
                }
            _parsing.value = false
        }
    }

    /** 确认并保存 AI 解析结果 */
    fun saveParsed() {
        val result = _parsedResult.value ?: return
        val expense = result.toExpense().copy(source = ExpenseSource.MANUAL)
        viewModelScope.launch {
            addExpenseUseCase(expense)
                .onSuccess { _saveSuccess.trySend(Unit) }
                .onFailure { e -> _errorEvent.trySend(e.message ?: "保存失败") }
        }
    }
}

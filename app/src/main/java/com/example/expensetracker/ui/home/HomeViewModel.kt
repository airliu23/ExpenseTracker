package com.example.expensetracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.repository.ExpenseRepository
import com.example.expensetracker.domain.model.Expense
import com.example.expensetracker.domain.model.ExpenseParseResult
import com.example.expensetracker.domain.model.ExpenseSource
import com.example.expensetracker.domain.usecase.AddExpenseUseCase
import com.example.expensetracker.domain.usecase.GetExpensesUseCase
import com.example.expensetracker.domain.usecase.GetStatisticsUseCase
import com.example.expensetracker.domain.usecase.ParseVoiceExpenseUseCase
import com.example.expensetracker.service.VoiceResultBus
import com.example.expensetracker.service.VoiceState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getExpensesUseCase: GetExpensesUseCase,
    private val getStatisticsUseCase: GetStatisticsUseCase,
    private val addExpenseUseCase: AddExpenseUseCase,
    private val parseVoiceExpenseUseCase: ParseVoiceExpenseUseCase,
    private val expenseRepository: ExpenseRepository,
    private val voiceBus: VoiceResultBus
) : ViewModel() {

    /** 语音识别是否活跃（用于显示动画） */
    val voiceState: StateFlow<VoiceState> = voiceBus.voiceState

    /** 实时部分识别文字 */
    val voicePartialText: StateFlow<String> = voiceBus.partialText

    private val monthKey = getExpensesUseCase.currentMonthKey()
    private val monthLabel = monthKey.substring(0, 4) + "年" + monthKey.substring(4) + "月"

    val expenses: StateFlow<List<Expense>> = getExpensesUseCase.byMonth(monthKey)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _totalExpense = MutableStateFlow(0.0)
    val totalExpense: StateFlow<Double> = _totalExpense

    private val _totalIncome = MutableStateFlow(0.0)
    val totalIncome: StateFlow<Double> = _totalIncome

    val monthLabelDisplay: String = monthLabel

    // ── 输入与 AI 解析 ──

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText

    private val _parsing = MutableStateFlow(false)
    val parsing: StateFlow<Boolean> = _parsing

    private val _parsedResult = MutableStateFlow<ExpenseParseResult?>(null)
    val parsedResult: StateFlow<ExpenseParseResult?> = _parsedResult

    // 事件
    private val _toast = Channel<String>(Channel.BUFFERED)
    val toast = _toast.receiveAsFlow()

    /** 当前正在进行的解析任务，用于「解析中」时取消 */
    private var parseJob: Job? = null

    /**
     * 输入框里的内容是否来自语音识别。
     *
     * 首页同时支持「按住说话」和「手动打字」两条路径，保存时要据此标记来源，
     * 否则语音记的账会被错误地记成手动录入（首页不会显示 🎤 标记）。
     */
    private var inputFromVoice = false

    fun setInput(value: String) {
        _inputText.value = value
        inputFromVoice = false // 手动输入
    }

    fun parseInput() {
        val text = _inputText.value.trim()
        if (text.isBlank()) {
            _toast.trySend("请输入消费描述")
            return
        }
        // 已有一个解析在跑时忽略重复点击，避免多个请求叠加把界面拖住
        if (parseJob?.isActive == true) return
        parseJob = viewModelScope.launch {
            _parsing.value = true
            _parsedResult.value = null
            try {
                parseVoiceExpenseUseCase(text)
                    .onSuccess { _parsedResult.value = it }
                    .onFailure { e -> _toast.trySend(e.message ?: "AI 解析失败") }
            } finally {
                // 必须放在 finally：协程被取消时（用户点取消、页面销毁等）也要复位，
                // 否则按钮会永久停在「AI 解析中…」，界面再也回不来
                _parsing.value = false
            }
        }
    }

    /** 用户在解析过程中点击按钮时取消当前解析 */
    fun cancelParse() {
        parseJob?.cancel()
        parseJob = null
        _parsing.value = false
    }

    fun saveParsed() {
        val result = _parsedResult.value ?: return
        // 按真实来源标记：语音记的账要显示 🎤，手动打字的才是 MANUAL。
        // 这里原先无条件 copy(source = MANUAL)，把 toExpense() 标好的 VOICE 覆盖掉了，
        // 导致首页语音记账的来源永远是「手动」。
        val expense = result.toExpense().copy(
            source = if (inputFromVoice) ExpenseSource.VOICE else ExpenseSource.MANUAL
        )
        viewModelScope.launch {
            addExpenseUseCase(expense)
                .onSuccess {
                    _inputText.value = ""
                    _parsedResult.value = null
                    inputFromVoice = false
                    refreshSummary()
                    _toast.trySend("已保存")
                }
                .onFailure { e -> _toast.trySend(e.message ?: "保存失败") }
        }
    }

    /** 取消 AI 解析结果，清空输入 */
    fun cancelParsed() {
        _inputText.value = ""
        _parsedResult.value = null
        inputFromVoice = false
    }

    /** 收到语音结果时调用 */
    fun onVoiceInput(text: String) {
        if (text.isBlank()) return
        _inputText.value = text
        inputFromVoice = true // 这条内容来自语音
        parseInput()
    }

    /** 停止正在进行的语音识别（用户松开按钮时调用） */
    fun stopVoice() {
        voiceBus.requestStop()
    }

    fun refreshSummary() {
        viewModelScope.launch {
            _totalExpense.value = getStatisticsUseCase.getTotalExpense(monthKey)
            _totalIncome.value = getStatisticsUseCase.getTotalIncome(monthKey)
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            expenseRepository.deleteExpense(expense.id)
            refreshSummary()
        }
    }

    init {
        refreshSummary()
        viewModelScope.launch { expenseRepository.initDefaultCategories() }
    }
}

package com.example.expensetracker.service

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

/** 语音结果事件总线 — Service ↔ UI */
@Singleton
class VoiceResultBus @Inject constructor() {

    private val _results = Channel<String>(Channel.BUFFERED)
    val results = _results.receiveAsFlow()

    private val _errors = Channel<String>(Channel.BUFFERED)
    val errors = _errors.receiveAsFlow()

    private val _voiceState = MutableStateFlow(VoiceState.Idle)
    val voiceState: StateFlow<VoiceState> = _voiceState

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText

    // ── 结果 ──

    fun sendResult(text: String) { _results.trySend(text) }
    fun sendError(msg: String) { _errors.trySend(msg) }

    // ── 状态 ──

    fun setListening() { _voiceState.value = VoiceState.Listening; _partialText.value = "" }
    fun setProcessing() { _voiceState.value = VoiceState.Processing }
    fun setIdle() { _voiceState.value = VoiceState.Idle; _partialText.value = "" }
    fun updatePartial(text: String) { _partialText.value = text }

    // ── 停止信号 (录音循环轮询) ──

    @Volatile var stopRequested = false
    fun requestStop() { stopRequested = true }
    fun clearStopRequest() { stopRequested = false }
}

enum class VoiceState { Idle, Listening, Processing }

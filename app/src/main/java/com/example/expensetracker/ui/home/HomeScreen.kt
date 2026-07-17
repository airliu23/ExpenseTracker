package com.example.expensetracker.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import com.example.expensetracker.ui.components.FloatingMicButton
import com.example.expensetracker.ui.components.VoiceOverlay
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.expensetracker.domain.model.ExpenseCategory
import com.example.expensetracker.ui.components.CategoryIcon
import com.example.expensetracker.ui.components.ExpenseCard
import com.example.expensetracker.ui.components.MonthSummaryCard
import com.example.expensetracker.ui.components.categoryColor
import com.example.expensetracker.ui.theme.IncomeGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    voiceInputText: String = "",
    onVoiceInput: () -> Unit,
    onVoiceConsumed: () -> Unit = {},
    onStatistics: () -> Unit,
    onSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val expenses by viewModel.expenses.collectAsState()
    val totalExpense by viewModel.totalExpense.collectAsState()
    val totalIncome by viewModel.totalIncome.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val parsing by viewModel.parsing.collectAsState()
    val parsedResult by viewModel.parsedResult.collectAsState()
    val voiceState by viewModel.voiceState.collectAsState()
    val voicePartialText by viewModel.voicePartialText.collectAsState()

    val snackbar = remember { SnackbarHostState() }
    val focusMgr = LocalFocusManager.current

    val isVoiceActive = voiceState != com.example.expensetracker.service.VoiceState.Idle
    val isProcessing = voiceState == com.example.expensetracker.service.VoiceState.Processing

    LaunchedEffect(Unit) {
        viewModel.toast.collect { snackbar.showSnackbar(it) }
    }

    // 收到语音输入 → 自动填入并解析 (一次性消费)
    LaunchedEffect(voiceInputText) {
        if (voiceInputText.isNotBlank()) {
            viewModel.onVoiceInput(voiceInputText)
            onVoiceConsumed()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("智能记账", fontWeight = FontWeight.Bold) },
                    actions = {
                        IconButton(onClick = onStatistics) {
                            Icon(Icons.Filled.BarChart, contentDescription = "统计")
                        }
                        IconButton(onClick = onSettings) {
                            Icon(Icons.Filled.Settings, contentDescription = "设置")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            },
            snackbarHost = { SnackbarHost(snackbar) }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 40.dp)
            ) {
                // ── 月度汇总 ──
                item { MonthSummaryCard(monthLabel = viewModel.monthLabelDisplay, totalExpense = totalExpense, totalIncome = totalIncome) }

                // ── 输入框 ──
                item {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { viewModel.setInput(it) },
                        label = { Text("说一句话记账") },
                        placeholder = { Text("例：午餐外卖35块") },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        singleLine = true
                    )
                }

                // ── AI 解析按钮 ──
                item {
                    Button(
                        onClick = {
                            focusMgr.clearFocus()
                            viewModel.parseInput()
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 16.dp),
                        enabled = !parsing && inputText.isNotBlank()
                    ) {
                        if (parsing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(Modifier.width(8.dp))
                            Text("AI 解析中…")
                        } else {
                            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("AI 解析")
                        }
                    }
                }

                // ── 解析结果 ──
                parsedResult?.let { result ->
                    val cat = ExpenseCategory.fromDisplayName(result.category)
                    val catColor = categoryColor(cat.displayName)

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = catColor.copy(alpha = 0.08f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CategoryIcon(category = cat.displayName, color = catColor, size = 36)
                                    Spacer(Modifier.width(10.dp))
                                    Text(cat.displayName, fontWeight = FontWeight.Bold, color = catColor)
                                }

                                Spacer(Modifier.height(8.dp))

                                Text(
                                    text = "¥ ${"%.2f".format(result.amount)}",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (cat == ExpenseCategory.INCOME) IncomeGreen else MaterialTheme.colorScheme.error
                                )

                                if (result.note.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text("备注: ${result.note}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                }

                                Spacer(Modifier.height(12.dp))

                                Row(modifier = Modifier.fillMaxWidth()) {
                                    // 取消
                                    OutlinedButton(
                                        onClick = { viewModel.cancelParsed() },
                                        modifier = Modifier.weight(1f).height(44.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    ) {
                                        Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("取消")
                                    }

                                    Spacer(Modifier.width(12.dp))

                                    // 确认保存
                                    Button(
                                        onClick = { viewModel.saveParsed() },
                                        modifier = Modifier.weight(1f).height(44.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = catColor)
                                    ) {
                                        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("确认保存")
                                    }
                                }
                            }
                        }
                    }
                }

                // ── 账单列表 ──
                val grouped = expenses.groupBy { e ->
                    SimpleDateFormat("MM月dd日 EEEE", Locale.CHINA).format(Date(e.date))
                }

                if (expenses.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(
                                "暂无记录\n在上方输入一句话自动记账",
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }
                    }
                } else {
                    grouped.forEach { (label, list) ->
                        item {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                        items(list, key = { it.id }) { e ->
                            ExpenseCard(expense = e, onDelete = { viewModel.deleteExpense(e) })
                        }
                    }
                }
            }
        } // Scaffold

        // ── 语音录入动画遮罩 (按住说话时显示) ──
        VoiceOverlay(
            isVisible = isVoiceActive,
            isProcessing = isProcessing,
            partialText = voicePartialText,
            onDismiss = { viewModel.stopVoice() }
        )

        // 悬浮麦克风按钮 (始终可见, VoiceOverlay 叠加在上做视觉反馈)
        FloatingMicButton(
            isVisible = true,
            onPressStart = { if (voiceState == com.example.expensetracker.service.VoiceState.Idle) onVoiceInput() },
            onPressEnd = { viewModel.stopVoice() }
        )
    } // Box
}

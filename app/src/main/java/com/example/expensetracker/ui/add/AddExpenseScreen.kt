package com.example.expensetracker.ui.add

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.expensetracker.domain.model.ExpenseCategory
import com.example.expensetracker.ui.components.CategoryIcon
import com.example.expensetracker.ui.components.categoryColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    voiceInputText: String = "",
    onNavigateBack: () -> Unit,
    viewModel: AddExpenseViewModel = hiltViewModel()
) {
    val inputText by viewModel.inputText.collectAsState()
    val parsing by viewModel.parsing.collectAsState()
    val parsedResult by viewModel.parsedResult.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // 接收语音输入并自动解析
    LaunchedEffect(voiceInputText) {
        if (voiceInputText.isNotBlank()) {
            viewModel.setInputText(voiceInputText)
            viewModel.parseInput()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.saveSuccess.collect { onNavigateBack() }
        viewModel.errorEvent.collect { msg -> snackbarHostState.showSnackbar(msg) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("添加账单") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── 输入框 ──
            OutlinedTextField(
                value = inputText,
                onValueChange = { viewModel.setInputText(it) },
                label = { Text("说一句话记账") },
                placeholder = { Text("例：午餐外卖35块") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 3
            )

            // ── AI 解析按钮 ──
            Button(
                onClick = { if (parsing) viewModel.cancelParse() else viewModel.parseInput() },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                // 解析中保持可点，让用户能随时取消，而不是干等到超时
                enabled = parsing || inputText.isNotBlank()
            ) {
                if (parsing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI 正在解析… 点击取消")
                } else {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("AI 解析")
                }
            }

            // ── 解析结果 ──
            parsedResult?.let { result ->
                val cat = ExpenseCategory.fromDisplayName(result.category)
                val catColor = categoryColor(cat.displayName)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = catColor.copy(alpha = 0.08f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CategoryIcon(category = cat.displayName, color = catColor, size = 40)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = cat.displayName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = catColor
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 金额
                        Text(
                            text = "¥ ${"%.2f".format(result.amount)}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (cat == ExpenseCategory.INCOME)
                                com.example.expensetracker.ui.theme.IncomeGreen
                            else
                                MaterialTheme.colorScheme.error
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // 日期 & 备注
                        if (result.date.isNotBlank()) {
                            Text(
                                text = "日期: $result.date",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        if (result.note.isNotBlank()) {
                            Text(
                                text = "备注: ${result.note}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 确认按钮
                        Button(
                            onClick = { viewModel.saveParsed() },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = catColor)
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("确认保存")
                        }
                    }
                }
            }

            // ── 空状态提示 ──
            if (parsedResult == null && !parsing) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "输入一句话消费描述\nAI 自动帮你分类、提取金额和备注",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

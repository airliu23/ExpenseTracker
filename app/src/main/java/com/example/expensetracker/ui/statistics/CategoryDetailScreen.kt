package com.example.expensetracker.ui.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.expensetracker.ui.components.CategoryIcon
import com.example.expensetracker.ui.components.ExpenseCard
import com.example.expensetracker.ui.components.categoryColor
import com.example.expensetracker.ui.theme.IncomeGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: CategoryDetailViewModel = hiltViewModel()
) {
    val expenses by viewModel.expenses.collectAsState()
    val categoryTotal by viewModel.categoryTotal.collectAsState()
    val catColor = if (viewModel.isIncome) IncomeGreen else categoryColor(viewModel.displayTitle)
    val percentage = viewModel.getPercentage()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(viewModel.displayTitle) },
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
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // ── 分类汇总卡片 ──
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = catColor.copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CategoryIcon(category = viewModel.displayTitle, color = catColor, size = 48)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = viewModel.displayTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = catColor
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (viewModel.isIncome) "+¥%.2f".format(categoryTotal)
                                   else "¥%.2f".format(categoryTotal),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (viewModel.isIncome) IncomeGreen else MaterialTheme.colorScheme.error
                        )
                        Row(
                            modifier = Modifier.padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "共${expenses.size}笔",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            if (!viewModel.isIncome) {
                                Text(
                                    text = "  ·  ",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                )
                                Text(
                                    text = "占比 %.1f%%".format(percentage),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = catColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // ── 账单列表 ──
            if (expenses.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "该时段无\"${viewModel.displayTitle}\"类别记录",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            } else {
                val grouped = expenses.groupBy { e ->
                    SimpleDateFormat("MM月dd日 EEEE", Locale.CHINA).format(Date(e.date))
                }
                grouped.forEach { (label, list) ->
                    item {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 4.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    items(list, key = { it.id }) { e ->
                        ExpenseCard(expense = e, onDelete = null)
                    }
                }
            }
        }
    }
}

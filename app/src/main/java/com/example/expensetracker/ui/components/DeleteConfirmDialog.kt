package com.example.expensetracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.expensetracker.domain.model.Expense
import com.example.expensetracker.ui.theme.IncomeGreen

/**
 * 删除前的二次确认框。
 *
 * 删除不可撤销，而历史月份的账目要从统计页翻好几层才能到达，误触代价高。
 * 首页和分类明细页共用这一个，保证两处行为一致。
 *
 * 用法：调用方用 `var pendingDelete by remember { mutableStateOf<Expense?>(null) }`
 * 保存待删项，列表项的 onDelete 只负责赋值，真正的删除放在 onConfirm 里。
 */
@Composable
fun DeleteConfirmDialog(
    expense: Expense,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text("删除这笔记录？") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // 把关键信息列出来，让用户能确认删的是哪一笔
                Text(
                    text = if (expense.note.isBlank()) expense.categoryName
                           else "${expense.categoryName} · ${expense.note}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = expense.displayAmount,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (expense.isExpense) MaterialTheme.colorScheme.error
                                else IncomeGreen
                    )
                    Text(
                        text = expense.formattedDate,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "删除后无法恢复。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = "删除",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

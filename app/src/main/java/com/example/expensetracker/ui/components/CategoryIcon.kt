package com.example.expensetracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.expensetracker.domain.model.ExpenseCategory

@Composable
fun CategoryIcon(
    category: String,
    color: Color,
    modifier: Modifier = Modifier,
    size: Int = 40
) {
    val icon = categoryIcon(category)
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = category,
            tint = color,
            modifier = Modifier.size((size * 0.6).dp)
        )
    }
}

/** 根据分类名返回对应的 Material Icon */
fun categoryIcon(category: String): ImageVector {
    return when (category) {
        "餐饮" -> Icons.Filled.Restaurant
        "交通" -> Icons.Filled.DirectionsCar
        "购物" -> Icons.Filled.ShoppingCart
        "娱乐" -> Icons.Filled.SportsEsports
        "居住" -> Icons.Filled.Home
        "医疗" -> Icons.Filled.LocalHospital
        "教育" -> Icons.Filled.School
        "通讯" -> Icons.Filled.Phone
        "其他支出" -> Icons.Filled.MoreHoriz
        "收入" -> Icons.Filled.TrendingUp
        else -> Icons.Filled.MoreHoriz
    }
}

/** 分类对应的颜色 */
fun categoryColor(category: String): Color {
    return ExpenseCategory.fromDisplayName(category).let { cat ->
        try {
            Color(android.graphics.Color.parseColor(cat.colorHex))
        } catch (_: Exception) {
            Color(0xFF2E7D32) // fallback green
        }
    }
}

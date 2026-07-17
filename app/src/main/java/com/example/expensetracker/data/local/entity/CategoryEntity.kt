package com.example.expensetracker.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "icon")
    val icon: String,

    @ColumnInfo(name = "color")
    val color: String, // #RRGGBB

    @ColumnInfo(name = "is_default")
    val isDefault: Boolean = false
)

/** 预置分类 */
object DefaultCategories {
    val ALL = listOf(
        CategoryEntity(name = "餐饮", icon = "restaurant", color = "#FF6B6B", isDefault = true),
        CategoryEntity(name = "交通", icon = "directions_car", color = "#4ECDC4", isDefault = true),
        CategoryEntity(name = "购物", icon = "shopping_cart", color = "#FFD93D", isDefault = true),
        CategoryEntity(name = "娱乐", icon = "sports_esports", color = "#A855F7", isDefault = true),
        CategoryEntity(name = "居住", icon = "home", color = "#6BCB77", isDefault = true),
        CategoryEntity(name = "医疗", icon = "local_hospital", color = "#FF8A80", isDefault = true),
        CategoryEntity(name = "教育", icon = "school", color = "#64B5F6", isDefault = true),
        CategoryEntity(name = "通讯", icon = "phone", color = "#4DD0E1", isDefault = true),
        CategoryEntity(name = "其他支出", icon = "more_horiz", color = "#90A4AE", isDefault = true),
        CategoryEntity(name = "收入", icon = "trending_up", color = "#66BB6A", isDefault = true),
    )
}

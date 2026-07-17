package com.example.expensetracker.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
    indices = [
        Index(value = ["date"]),
        Index(value = ["category_id"]),
        Index(value = ["source"])
    ]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "amount")
    val amount: Double,

    @ColumnInfo(name = "category_id")
    val categoryId: Long,

    @ColumnInfo(name = "category_name")
    val categoryName: String,

    @ColumnInfo(name = "note")
    val note: String = "",

    /** 消费日期时间戳 (ms) */
    @ColumnInfo(name = "date")
    val date: Long,

    /** 记录创建时间戳 (ms) */
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    /** 来源: voice / manual */
    @ColumnInfo(name = "source")
    val source: String = "manual"
)

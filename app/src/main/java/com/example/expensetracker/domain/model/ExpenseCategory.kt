package com.example.expensetracker.domain.model

/**
 * 消费分类 (领域模型，与 Room Entity 分离)
 */
enum class ExpenseCategory(
    val displayName: String,
    val iconName: String,
    val colorHex: String
) {
    FOOD("餐饮", "restaurant", "#FF6B6B"),
    TRANSPORT("交通", "directions_car", "#4ECDC4"),
    SHOPPING("购物", "shopping_cart", "#FFD93D"),
    ENTERTAINMENT("娱乐", "sports_esports", "#A855F7"),
    HOUSING("居住", "home", "#6BCB77"),
    MEDICAL("医疗", "local_hospital", "#FF8A80"),
    EDUCATION("教育", "school", "#64B5F6"),
    COMMUNICATION("通讯", "phone", "#4DD0E1"),
    OTHER("其他支出", "more_horiz", "#90A4AE"),
    INCOME("收入", "trending_up", "#66BB6A");

    companion object {
        /** 根据显示名匹配分类，找不到返回 OTHER */
        fun fromDisplayName(name: String): ExpenseCategory =
            entries.firstOrNull {
                it.displayName.equals(name, ignoreCase = true)
            } ?: OTHER
    }
}

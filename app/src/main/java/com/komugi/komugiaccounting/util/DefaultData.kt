package com.komugi.komugiaccounting.util

import com.komugi.komugiaccounting.data.model.Category
import com.komugi.komugiaccounting.data.model.Member
import com.komugi.komugiaccounting.data.model.RecordType

object DefaultData {
    val categories = listOf(
        Category("cat-transport", "交通", RecordType.EXPENSE, "DirectionsBus", "#42A5F5", 1, isSystem = true),
        Category("cat-snack", "零食", RecordType.EXPENSE, "Cookie", "#FFA726", 2, isSystem = true),
        Category("cat-breakfast", "早餐", RecordType.EXPENSE, "FreeBreakfast", "#FFCA28", 3, isSystem = true),
        Category("cat-lunch", "午餐", RecordType.EXPENSE, "Restaurant", "#FF7043", 4, isSystem = true),
        Category("cat-dinner", "晚餐", RecordType.EXPENSE, "DinnerDining", "#EC407A", 5, isSystem = true),
        Category("cat-entertainment", "休闲娱乐", RecordType.EXPENSE, "SportsEsports", "#AB47BC", 6, isSystem = true),
        Category("cat-express", "邮寄", RecordType.EXPENSE, "LocalPostOffice", "#26A69A", 7, isSystem = true),
        Category("cat-daily", "日用品", RecordType.EXPENSE, "ShoppingCart", "#66BB6A", 8, isSystem = true),
        Category("cat-rent", "房租", RecordType.EXPENSE, "Home", "#5C6BC0", 9, isSystem = true),
        Category("cat-utility", "水电", RecordType.EXPENSE, "ElectricBolt", "#29B6F6", 10, isSystem = true),
        Category("cat-expense-other", "其他", RecordType.EXPENSE, "MoreHoriz", "#BDBDBD", 99, isSystem = true),
        Category("cat-salary", "工资", RecordType.INCOME, "Payments", "#66BB6A", 1, isSystem = true),
        Category("cat-bonus", "奖金", RecordType.INCOME, "CardGiftcard", "#FFCA28", 2, isSystem = true),
        Category("cat-transfer", "转账", RecordType.INCOME, "SwapHoriz", "#42A5F5", 3, isSystem = true),
        Category("cat-investment", "理财", RecordType.INCOME, "TrendingUp", "#26A69A", 4, isSystem = true),
        Category("cat-parttime", "兼职", RecordType.INCOME, "Work", "#FFA726", 5, isSystem = true),
        Category("cat-refund", "退款", RecordType.INCOME, "AssignmentReturn", "#EC407A", 6, isSystem = true),
        Category("cat-income-other", "其他收入", RecordType.INCOME, "MoreHoriz", "#BDBDBD", 99, isSystem = true)
    )

    val members = listOf(
        Member("mem-self", "自己", "#42A5F5", isSystem = true),
        Member("mem-spouse", "伴侣", "#EC407A", isSystem = true),
        Member("mem-child", "孩子", "#FFCA28", isSystem = true),
        Member("mem-company", "公司", "#5C6BC0", isSystem = true),
        Member("mem-family", "家庭公用", "#66BB6A", isSystem = true)
    )
}

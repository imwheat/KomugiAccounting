package com.komugi.komugiaccounting.util

import com.komugi.komugiaccounting.data.model.Category
import com.komugi.komugiaccounting.data.model.Member
import com.komugi.komugiaccounting.data.model.RecordType

object DefaultData {
    val categories = listOf(
        expense("cat-meals", "早午晚餐", "食品酒水", 1, "Restaurant", "#FF7043"),
        expense("cat-drinks", "饮料", "食品酒水", 2, "LocalCafe", "#42A5F5"),
        expense("cat-fruit", "水果", "食品酒水", 3, "Nutrition", "#66BB6A"),

        expense("cat-public-transport", "公共交通", "行车交通", 11, "DirectionsBus", "#42A5F5"),
        expense("cat-taxi", "打车", "行车交通", 12, "LocalTaxi", "#5C6BC0"),
        expense("cat-car", "私家车费", "行车交通", 13, "DirectionsCar", "#26A69A"),

        expense("cat-daily", "日常用品", "居家物业", 21, "ShoppingCart", "#66BB6A"),
        expense("cat-utility", "水电煤气", "居家物业", 22, "ElectricBolt", "#29B6F6"),
        expense("cat-rent", "房租", "居家物业", 23, "Home", "#5C6BC0"),
        expense("cat-property", "物业管理", "居家物业", 24, "Apartment", "#7E57C2"),
        expense("cat-maintenance", "维修保养", "居家物业", 25, "Build", "#8D6E63"),
        expense("cat-cleaning", "清洁费", "居家物业", 26, "CleaningServices", "#26A69A"),

        expense("cat-phone", "手机费", "交流通讯", 31, "PhoneAndroid", "#42A5F5"),
        expense("cat-internet", "网费", "交流通讯", 32, "Wifi", "#26A69A"),
        expense("cat-express", "邮寄费", "交流通讯", 33, "LocalPostOffice", "#FFA726"),

        expense("cat-steam", "Steam", "休闲娱乐", 41, "SportsEsports", "#5C6BC0"),
        expense("cat-game-pay", "氪金", "休闲娱乐", 42, "VideogameAsset", "#AB47BC"),
        expense("cat-hotel", "酒店", "休闲娱乐", 43, "Hotel", "#FFA726"),
        expense("cat-shopping", "购物", "休闲娱乐", 44, "ShoppingBag", "#66BB6A"),

        expense("cat-clothes", "衣服裤子", "衣服饰品", 51, "Checkroom", "#EC407A"),
        expense("cat-shoes-bags", "鞋帽包包", "衣服饰品", 52, "ShoppingBag", "#AB47BC"),
        expense("cat-skincare", "护肤品", "衣服饰品", 53, "Spa", "#FFCA28"),
        expense("cat-cosmetics", "化妆品", "衣服饰品", 54, "FaceRetouchingNatural", "#F06292"),

        expense("cat-medicine", "药", "医疗保险", 61, "Medication", "#66BB6A"),
        expense("cat-healthcare", "保健费", "医疗保险", 62, "HealthAndSafety", "#26A69A"),
        expense("cat-treatment", "治疗费", "医疗保险", 63, "LocalHospital", "#EC407A"),

        expense("cat-invest-out", "理财投资", "金融保险", 71, "TrendingUp", "#26A69A"),
        expense("cat-repayment", "还款", "金融保险", 72, "CreditCard", "#5C6BC0"),
        expense("cat-compensation", "赔偿罚款", "金融保险", 73, "Gavel", "#EC407A"),

        expense("cat-red-packet", "红包", "人情往来", 81, "Redeem", "#EC407A"),
        expense("cat-gift", "送礼请客", "人情往来", 82, "CardGiftcard", "#FFCA28"),
        expense("cat-elder-support", "孝敬长辈", "人情往来", 83, "FamilyRestroom", "#AB47BC"),
        expense("cat-charity", "慈善捐款", "人情往来", 84, "VolunteerActivism", "#66BB6A"),

        expense("cat-expense-other", "其他支出", "其他杂项", 91, "MoreHoriz", "#BDBDBD"),
        expense("cat-loss", "意外丢失", "其他杂项", 92, "ReportProblem", "#8D6E63"),

        income("cat-salary", "工资收入", "职业收入", 1, "Payments", "#66BB6A"),
        income("cat-overtime", "加班收入", "职业收入", 2, "MoreTime", "#42A5F5"),
        income("cat-bonus", "奖金收入", "职业收入", 3, "CardGiftcard", "#FFCA28"),
        income("cat-parttime", "兼职收入", "职业收入", 4, "Work", "#FFA726"),
        income("cat-business", "经营所得", "职业收入", 5, "Store", "#26A69A"),

        income("cat-investment", "投资收入", "其他收入", 11, "TrendingUp", "#26A69A"),
        income("cat-interest", "利息收入", "其他收入", 12, "Savings", "#42A5F5"),
        income("cat-income-red-packet", "红包", "其他收入", 13, "Redeem", "#EC407A"),
        income("cat-lottery", "中奖", "其他收入", 14, "Celebration", "#FFCA28"),
        income("cat-income-other", "其他", "其他收入", 99, "MoreHoriz", "#BDBDBD")
    )

    val members = listOf(
        Member("mem-self", "自己", "#42A5F5", isSystem = true),
        Member("mem-spouse", "伴侣", "#EC407A", isSystem = true),
        Member("mem-child", "孩子", "#FFCA28", isSystem = true),
        Member("mem-company", "公司", "#5C6BC0", isSystem = true),
        Member("mem-family", "家庭公用", "#66BB6A", isSystem = true)
    )

    private fun expense(id: String, name: String, groupName: String, sortOrder: Int, iconName: String, color: String) =
        category(id, name, RecordType.EXPENSE, groupName, sortOrder, iconName, color)

    private fun income(id: String, name: String, groupName: String, sortOrder: Int, iconName: String, color: String) =
        category(id, name, RecordType.INCOME, groupName, sortOrder, iconName, color)

    private fun category(
        id: String,
        name: String,
        type: RecordType,
        groupName: String,
        sortOrder: Int,
        iconName: String,
        color: String
    ) = Category(
        id = id,
        name = name,
        type = type,
        iconName = iconName,
        color = color,
        sortOrder = sortOrder,
        groupName = groupName,
        isSystem = true
    )
}

package com.komugi.komugiaccounting.data.storage

import android.content.Context
import com.komugi.komugiaccounting.data.model.AppData
import com.komugi.komugiaccounting.data.model.AppSettings
import com.komugi.komugiaccounting.data.model.Category
import com.komugi.komugiaccounting.data.model.Member
import com.komugi.komugiaccounting.data.model.RecordType
import com.komugi.komugiaccounting.data.model.Template
import com.komugi.komugiaccounting.data.model.ThemeMode
import com.komugi.komugiaccounting.data.model.TransactionRecord
import com.komugi.komugiaccounting.util.DefaultData
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class JsonFileStorage(private val context: Context) {
    private val dataFile: File get() = File(context.filesDir, "app_data.json")
    private val tempFile: File get() = File(context.filesDir, "app_data.json.tmp")

    fun loadData(): AppData {
        if (!dataFile.exists()) return defaultData().also(::saveData)
        return runCatching { decode(JSONObject(dataFile.readText())) }
            .getOrElse { defaultData().also(::saveData) }
            .ensureDefaults()
    }

    fun saveData(data: AppData) {
        val json = encode(data).toString(2)
        tempFile.writeText(json)
        if (dataFile.exists()) dataFile.delete()
        check(tempFile.renameTo(dataFile)) { "Failed to replace app_data.json" }
    }

    fun exportJson(data: AppData): String = encode(data).toString(2)

    fun importJson(jsonText: String): AppData =
        decode(JSONObject(jsonText)).ensureDefaults()

    private fun defaultData(): AppData = AppData(
        categories = DefaultData.categories,
        members = DefaultData.members,
        settings = AppSettings(
            lastExpenseCategoryId = "cat-meals",
            lastIncomeCategoryId = "cat-salary",
            lastMemberId = "mem-self"
        )
    )

    private fun AppData.ensureDefaults(): AppData {
        val systemMemberIds = DefaultData.members.map { it.id }.toSet()
        val defaultCategoryIds = DefaultData.categories.map { it.id }.toSet()
        val legacySystemCategoryIds = setOf(
            "cat-transport",
            "cat-snack",
            "cat-breakfast",
            "cat-lunch",
            "cat-dinner",
            "cat-entertainment",
            "cat-express",
            "cat-daily",
            "cat-rent",
            "cat-utility",
            "cat-expense-other",
            "cat-salary",
            "cat-bonus",
            "cat-transfer",
            "cat-investment",
            "cat-parttime",
            "cat-refund",
            "cat-income-other"
        )
        val customCategories = categories
            .filterNot { it.isSystem || it.id in legacySystemCategoryIds || it.id in defaultCategoryIds }
        val migratedCategories = DefaultData.categories + customCategories
        return copy(
            records = records.map { record -> record.copy(categoryId = migrateCategoryId(record.categoryId)) },
            categories = if (categories.isEmpty()) DefaultData.categories else migratedCategories,
            members = if (members.isEmpty()) {
                DefaultData.members
            } else {
                members.map { member -> if (member.id in systemMemberIds) member.copy(isSystem = true) else member }
            },
            templates = templates.map { template -> template.copy(categoryId = migrateCategoryId(template.categoryId)) },
            settings = settings.copy(
                lastExpenseCategoryId = settings.lastExpenseCategoryId?.let(::migrateCategoryId),
                lastIncomeCategoryId = settings.lastIncomeCategoryId?.let(::migrateCategoryId),
                recentCategoryIds = settings.recentCategoryIds.map(::migrateCategoryId).distinct().take(10)
            )
        )
    }

    private fun encode(data: AppData) = JSONObject().apply {
        put("version", data.version)
        put("records", JSONArray().apply { data.records.forEach { put(encodeRecord(it)) } })
        put("categories", JSONArray().apply { data.categories.forEach { put(encodeCategory(it)) } })
        put("members", JSONArray().apply { data.members.forEach { put(encodeMember(it)) } })
        put("templates", JSONArray().apply { data.templates.forEach { put(encodeTemplate(it)) } })
        put("settings", encodeSettings(data.settings))
    }

    private fun encodeRecord(record: TransactionRecord) = JSONObject().apply {
        put("id", record.id)
        put("type", record.type.name)
        put("amount", record.amount)
        put("categoryId", record.categoryId)
        put("memberId", record.memberId)
        put("remark", record.remark)
        put("dateTime", record.dateTime)
        put("createdAt", record.createdAt)
        put("updatedAt", record.updatedAt)
        put("isRefunded", record.isRefunded)
    }

    private fun encodeCategory(category: Category) = JSONObject().apply {
        put("id", category.id)
        put("name", category.name)
        put("type", category.type.name)
        put("iconName", category.iconName)
        put("color", category.color)
        put("sortOrder", category.sortOrder)
        put("groupName", category.groupName)
        put("enabled", category.enabled)
        put("isSystem", category.isSystem)
    }

    private fun encodeMember(member: Member) = JSONObject().apply {
        put("id", member.id)
        put("name", member.name)
        put("avatarColor", member.avatarColor)
        put("iconName", member.iconName)
        put("enabled", member.enabled)
        put("isSystem", member.isSystem)
    }

    private fun encodeTemplate(template: Template) = JSONObject().apply {
        put("id", template.id)
        put("name", template.name)
        put("type", template.type.name)
        template.amount?.let { put("amount", it) }
        put("categoryId", template.categoryId)
        put("memberId", template.memberId)
        put("remark", template.remark)
    }

    private fun encodeSettings(settings: AppSettings) = JSONObject().apply {
        put("themeMode", settings.themeMode.name)
        put("lastExpenseCategoryId", settings.lastExpenseCategoryId)
        put("lastIncomeCategoryId", settings.lastIncomeCategoryId)
        put("lastMemberId", settings.lastMemberId)
        put("recentCategoryIds", JSONArray().apply { settings.recentCategoryIds.forEach { put(it) } })
        put("currencySymbol", settings.currencySymbol)
    }

    private fun decode(json: JSONObject) = AppData(
        version = json.optInt("version", 1),
        records = json.optJSONArray("records").toList(::decodeRecord),
        categories = json.optJSONArray("categories").toList(::decodeCategory),
        members = json.optJSONArray("members").toList(::decodeMember),
        templates = json.optJSONArray("templates").toList(::decodeTemplate),
        settings = json.optJSONObject("settings")?.let(::decodeSettings) ?: AppSettings()
    )

    private fun decodeRecord(json: JSONObject) = TransactionRecord(
        id = json.getString("id"),
        type = RecordType.valueOf(json.getString("type")),
        amount = json.getLong("amount"),
        categoryId = json.getString("categoryId"),
        memberId = json.getString("memberId"),
        remark = json.optString("remark", ""),
        dateTime = json.getLong("dateTime"),
        createdAt = json.optLong("createdAt", json.getLong("dateTime")),
        updatedAt = json.optLong("updatedAt", json.getLong("dateTime")),
        isRefunded = json.optBoolean("isRefunded", false)
    )

    private fun decodeCategory(json: JSONObject) = Category(
        id = json.getString("id"),
        name = json.getString("name"),
        type = RecordType.valueOf(json.getString("type")),
        iconName = json.optString("iconName", ""),
        color = json.optString("color", "#BDBDBD"),
        sortOrder = json.optInt("sortOrder", 0),
        groupName = json.optString("groupName", legacyGroupName(json.optString("name", ""))),
        enabled = json.optBoolean("enabled", true),
        isSystem = json.optBoolean("isSystem", false)
    )

    private fun decodeMember(json: JSONObject) = Member(
        id = json.getString("id"),
        name = json.getString("name"),
        avatarColor = json.optString("avatarColor", "#42A5F5"),
        iconName = json.optString("iconName", ""),
        enabled = json.optBoolean("enabled", true),
        isSystem = json.optBoolean("isSystem", false)
    )

    private fun decodeTemplate(json: JSONObject) = Template(
        id = json.getString("id"),
        name = json.getString("name"),
        type = RecordType.valueOf(json.getString("type")),
        amount = if (json.has("amount") && !json.isNull("amount")) json.getLong("amount") else null,
        categoryId = json.getString("categoryId"),
        memberId = json.getString("memberId"),
        remark = json.optString("remark", "")
    )

    private fun decodeSettings(json: JSONObject) = AppSettings(
        themeMode = runCatching { ThemeMode.valueOf(json.optString("themeMode", ThemeMode.SYSTEM.name)) }.getOrDefault(ThemeMode.SYSTEM),
        lastExpenseCategoryId = json.optNullableString("lastExpenseCategoryId"),
        lastIncomeCategoryId = json.optNullableString("lastIncomeCategoryId"),
        lastMemberId = json.optNullableString("lastMemberId"),
        recentCategoryIds = json.optJSONArray("recentCategoryIds").stringList(),
        currencySymbol = json.optString("currencySymbol", "￥")
    )

    private fun <T> JSONArray?.toList(mapper: (JSONObject) -> T): List<T> {
        if (this == null) return emptyList()
        return (0 until length()).map { mapper(getJSONObject(it)) }
    }

    private fun JSONObject.optNullableString(name: String): String? =
        if (!has(name) || isNull(name)) null else optString(name)

    private fun JSONArray?.stringList(): List<String> {
        if (this == null) return emptyList()
        return (0 until length()).mapNotNull { optString(it).takeIf(String::isNotBlank) }
    }

    private fun migrateCategoryId(categoryId: String): String = when (categoryId) {
        "cat-breakfast", "cat-lunch", "cat-dinner" -> "cat-meals"
        "cat-transport" -> "cat-public-transport"
        "cat-utility" -> "cat-utility"
        "cat-express" -> "cat-express"
        "cat-expense-other" -> "cat-expense-other"
        "cat-salary" -> "cat-salary"
        "cat-bonus" -> "cat-bonus"
        "cat-transfer" -> "cat-income-other"
        "cat-investment" -> "cat-investment"
        "cat-parttime" -> "cat-parttime"
        "cat-refund" -> "cat-income-other"
        "cat-income-other" -> "cat-income-other"
        else -> categoryId
    }

    private fun legacyGroupName(categoryName: String): String = when (categoryName) {
        "早餐", "午餐", "晚餐", "早午晚餐", "零食", "饮料", "水果" -> "食品酒水"
        "交通", "公共交通", "打车", "私家车费" -> "行车交通"
        "日用品", "水电", "水电煤气", "房租", "物业管理", "维修保养", "清洁费" -> "居家物业"
        "邮寄", "邮寄费", "手机费", "网费" -> "交流通讯"
        "休闲娱乐", "Steam", "氪金", "酒店" -> "休闲娱乐"
        "衣服裤子", "鞋帽包包", "护肤品", "化妆品" -> "衣服饰品"
        "其他", "其他支出", "意外丢失" -> "其他杂项"
        "工资", "工资收入", "加班收入", "奖金", "奖金收入", "兼职", "兼职收入", "经营所得" -> "职业收入"
        "投资收入", "利息收入", "红包", "中奖", "其他收入" -> "其他收入"
        else -> ""
    }
}

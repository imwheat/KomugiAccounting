package com.komugi.komugiaccounting.ui.home

import com.komugi.komugiaccounting.data.model.AppData
import com.komugi.komugiaccounting.data.model.Category
import com.komugi.komugiaccounting.data.model.RecordType
import com.komugi.komugiaccounting.data.model.StatResult
import com.komugi.komugiaccounting.data.repository.AppDataRepository
import com.komugi.komugiaccounting.domain.StatisticsCalculator
import com.komugi.komugiaccounting.util.DateTimeUtil
import kotlinx.coroutines.flow.StateFlow
import java.util.Calendar

class HomeViewModel(private val repository: AppDataRepository) {
    val data: StateFlow<AppData> = repository.data

    fun monthStat(data: AppData): StatResult = monthStat(data, 0)

    fun monthStat(data: AppData, monthOffset: Int): StatResult {
        val start = DateTimeUtil.monthOffsetStart(monthOffset)
        val end = DateTimeUtil.endExclusiveFromStart(start, Calendar.MONTH, 1)
        return StatisticsCalculator.calculate(data.records, start, end)
    }

    fun monthTitle(monthOffset: Int): String {
        val start = DateTimeUtil.monthOffsetStart(monthOffset)
        return if (monthOffset == 0) "本月统计" else "${DateTimeUtil.formatDate(start).substring(0, 7)} 统计"
    }

    fun monthExpenseSummary(data: AppData, monthOffset: Int): MonthlyGroupSummary =
        monthGroupSummary(data, monthOffset, RecordType.EXPENSE)

    fun monthIncomeSummary(data: AppData, monthOffset: Int): MonthlyGroupSummary =
        monthGroupSummary(data, monthOffset, RecordType.INCOME)

    fun todayStat(data: AppData): StatResult {
        val start = DateTimeUtil.startOfDay()
        val end = DateTimeUtil.endExclusiveFromStart(start, Calendar.DAY_OF_YEAR, 1)
        return StatisticsCalculator.calculate(data.records, start, end)
    }

    fun weekStat(data: AppData): StatResult {
        val start = DateTimeUtil.startOfWeek()
        val end = DateTimeUtil.endExclusiveFromStart(start, Calendar.DAY_OF_YEAR, 7)
        return StatisticsCalculator.calculate(data.records, start, end)
    }

    fun yearStat(data: AppData): StatResult {
        val start = DateTimeUtil.startOfYear()
        val end = DateTimeUtil.endExclusiveFromStart(start, Calendar.YEAR, 1)
        return StatisticsCalculator.calculate(data.records, start, end)
    }

    private fun monthGroupSummary(data: AppData, monthOffset: Int, type: RecordType): MonthlyGroupSummary {
        val start = DateTimeUtil.monthOffsetStart(monthOffset)
        val end = DateTimeUtil.endExclusiveFromStart(start, Calendar.MONTH, 1)
        val categoryById = data.categories.associateBy { it.id }
        val records = data.records.filter {
            it.type == type && it.dateTime in start until end && it.effectiveAmount > 0L
        }
        val total = records.sumOf { it.effectiveAmount }
        val groupStats = records
            .groupBy { record ->
                val category = categoryById[record.categoryId]
                category?.groupName?.takeIf { it.isNotBlank() } ?: category?.name ?: "未分组"
            }
            .mapNotNull { (groupName, groupRecords) ->
                val amount = groupRecords.sumOf { it.effectiveAmount }
                if (amount <= 0L) return@mapNotNull null
                val categoryIds = groupRecords.map { it.categoryId }.toSet()
                val meta = groupMeta(data.categories, type, groupName, categoryIds)
                GroupStat(
                    groupName = groupName,
                    iconName = meta?.iconName ?: groupName.firstIconText(),
                    color = meta?.color ?: if (type == RecordType.EXPENSE) "#FF7043" else "#66BB6A",
                    iconImageUri = meta?.iconImageUri.orEmpty(),
                    categoryIds = categoryIds,
                    amount = amount,
                    percent = if (total > 0L) amount.toDouble() / total.toDouble() else 0.0
                )
            }
            .sortedByDescending { it.percent }
        return MonthlyGroupSummary(
            recordCount = records.size,
            totalAmount = total,
            groups = groupStats
        )
    }

    private fun groupMeta(categories: List<Category>, type: RecordType, groupName: String, usedCategoryIds: Set<String>): Category? {
        val groupCategories = categories.filter { it.type == type && it.groupName == groupName }
        return groupCategories.firstOrNull { it.name.startsWith("__group__") }
            ?: groupCategories.firstOrNull { it.id in usedCategoryIds }
            ?: groupCategories.minByOrNull { it.sortOrder }
    }
}

data class MonthlyGroupSummary(
    val recordCount: Int,
    val totalAmount: Long,
    val groups: List<GroupStat>
)

data class GroupStat(
    val groupName: String,
    val iconName: String,
    val color: String,
    val iconImageUri: String,
    val categoryIds: Set<String>,
    val amount: Long,
    val percent: Double
)

private fun String.firstIconText(): String =
    trim().firstOrNull()?.toString().orEmpty()

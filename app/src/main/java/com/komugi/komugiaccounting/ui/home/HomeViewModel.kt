package com.komugi.komugiaccounting.ui.home

import com.komugi.komugiaccounting.data.model.AppData
import com.komugi.komugiaccounting.data.model.Category
import com.komugi.komugiaccounting.data.model.RecordType
import com.komugi.komugiaccounting.data.model.StatResult
import com.komugi.komugiaccounting.data.model.TransactionRecord
import com.komugi.komugiaccounting.data.repository.AppDataRepository
import com.komugi.komugiaccounting.domain.StatisticsCalculator
import com.komugi.komugiaccounting.util.DateTimeUtil
import kotlinx.coroutines.flow.StateFlow
import java.util.Calendar

class HomeViewModel(private val repository: AppDataRepository) {
    val data: StateFlow<AppData> = repository.data

    fun monthStat(data: AppData): StatResult {
        return monthStat(data, 0)
    }

    fun monthStat(data: AppData, monthOffset: Int): StatResult {
        val start = DateTimeUtil.monthOffsetStart(monthOffset)
        val end = DateTimeUtil.endExclusiveFromStart(start, Calendar.MONTH, 1)
        return StatisticsCalculator.calculate(data.records, start, end)
    }

    fun monthTitle(monthOffset: Int): String {
        val start = DateTimeUtil.monthOffsetStart(monthOffset)
        return if (monthOffset == 0) "本月统计" else "${DateTimeUtil.formatDate(start).substring(0, 7)} 统计"
    }

    fun highestExpenseCategory(data: AppData, monthOffset: Int): Pair<Category, Long>? {
        val start = DateTimeUtil.monthOffsetStart(monthOffset)
        val end = DateTimeUtil.endExclusiveFromStart(start, Calendar.MONTH, 1)
        val categoryId = data.records
            .filter { it.type == RecordType.EXPENSE && it.dateTime in start until end }
            .groupBy(TransactionRecord::categoryId)
            .mapValues { (_, records) -> records.sumOf { it.effectiveAmount } }
            .filterValues { it > 0L }
            .maxByOrNull { it.value }
            ?: return null
        val category = data.categories.firstOrNull { it.id == categoryId.key } ?: return null
        return category to categoryId.value
    }

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
}

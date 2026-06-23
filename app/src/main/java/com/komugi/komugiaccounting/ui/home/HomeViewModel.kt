package com.komugi.komugiaccounting.ui.home

import com.komugi.komugiaccounting.data.model.AppData
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

    fun monthExpenseSummary(data: AppData, monthOffset: Int): ExpenseSummary {
        val start = DateTimeUtil.monthOffsetStart(monthOffset)
        val end = DateTimeUtil.endExclusiveFromStart(start, Calendar.MONTH, 1)
        val expenseRecords = data.records.filter {
            it.type == RecordType.EXPENSE && it.dateTime in start until end && it.effectiveAmount > 0L
        }
        val categoryById = data.categories.associateBy { it.id }
        val totalExpense = expenseRecords.sumOf { it.effectiveAmount }
        val categoryStats = expenseRecords
            .groupBy(TransactionRecord::categoryId)
            .mapNotNull { (categoryId, records) ->
                val amount = records.sumOf { it.effectiveAmount }
                if (amount <= 0L) return@mapNotNull null
                val categoryName = categoryById[categoryId]?.name ?: "未分类"
                ExpenseCategoryStat(
                    categoryName = categoryName,
                    amount = amount,
                    percent = if (totalExpense > 0L) amount.toDouble() / totalExpense.toDouble() else 0.0
                )
            }
            .sortedByDescending { it.percent }
        return ExpenseSummary(
            recordCount = expenseRecords.size,
            totalExpense = totalExpense,
            categories = categoryStats
        )
    }

    fun monthIncomeSummary(data: AppData, monthOffset: Int): IncomeSummary {
        val start = DateTimeUtil.monthOffsetStart(monthOffset)
        val end = DateTimeUtil.endExclusiveFromStart(start, Calendar.MONTH, 1)
        val incomeRecords = data.records.filter {
            it.type == RecordType.INCOME && it.dateTime in start until end && it.effectiveAmount > 0L
        }
        val categoryById = data.categories.associateBy { it.id }
        val totalIncome = incomeRecords.sumOf { it.effectiveAmount }
        val categoryStats = incomeRecords
            .groupBy(TransactionRecord::categoryId)
            .mapNotNull { (categoryId, records) ->
                val amount = records.sumOf { it.effectiveAmount }
                if (amount <= 0L) return@mapNotNull null
                val categoryName = categoryById[categoryId]?.name ?: "未分类"
                IncomeCategoryStat(
                    categoryName = categoryName,
                    amount = amount,
                    percent = if (totalIncome > 0L) amount.toDouble() / totalIncome.toDouble() else 0.0
                )
            }
            .sortedByDescending { it.percent }
        return IncomeSummary(
            recordCount = incomeRecords.size,
            totalIncome = totalIncome,
            categories = categoryStats
        )
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

data class ExpenseSummary(
    val recordCount: Int,
    val totalExpense: Long,
    val categories: List<ExpenseCategoryStat>
)

data class ExpenseCategoryStat(
    val categoryName: String,
    val amount: Long,
    val percent: Double
)

data class IncomeSummary(
    val recordCount: Int,
    val totalIncome: Long,
    val categories: List<IncomeCategoryStat>
)

data class IncomeCategoryStat(
    val categoryName: String,
    val amount: Long,
    val percent: Double
)

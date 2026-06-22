package com.komugi.komugiaccounting.domain

import com.komugi.komugiaccounting.data.model.RecordType
import com.komugi.komugiaccounting.data.model.StatResult
import com.komugi.komugiaccounting.data.model.TransactionRecord

object StatisticsCalculator {
    fun calculate(records: List<TransactionRecord>, startInclusive: Long, endExclusive: Long): StatResult {
        val scoped = records.filter { it.dateTime in startInclusive until endExclusive }
        return StatResult(
            income = scoped.filter { it.type == RecordType.INCOME }.sumOf { it.effectiveAmount },
            expense = scoped.filter { it.type == RecordType.EXPENSE }.sumOf { it.effectiveAmount }
        )
    }

    fun calculateAll(records: List<TransactionRecord>): StatResult = StatResult(
        income = records.filter { it.type == RecordType.INCOME }.sumOf { it.effectiveAmount },
        expense = records.filter { it.type == RecordType.EXPENSE }.sumOf { it.effectiveAmount }
    )
}

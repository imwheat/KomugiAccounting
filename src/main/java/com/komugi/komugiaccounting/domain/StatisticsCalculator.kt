package com.komugi.komugiaccounting.domain

import com.komugi.komugiaccounting.data.model.RecordType
import com.komugi.komugiaccounting.data.model.StatResult
import com.komugi.komugiaccounting.data.model.TransactionRecord

object StatisticsCalculator {
    fun calculate(records: List<TransactionRecord>, startInclusive: Long, endExclusive: Long): StatResult {
        val scoped = records.filter { it.dateTime in startInclusive until endExclusive }
        return StatResult(
            income = scoped.filter { it.type == RecordType.INCOME }.sumOf { it.amount },
            expense = scoped.filter { it.type == RecordType.EXPENSE }.sumOf { it.amount }
        )
    }

    fun calculateAll(records: List<TransactionRecord>): StatResult = StatResult(
        income = records.filter { it.type == RecordType.INCOME }.sumOf { it.amount },
        expense = records.filter { it.type == RecordType.EXPENSE }.sumOf { it.amount }
    )
}

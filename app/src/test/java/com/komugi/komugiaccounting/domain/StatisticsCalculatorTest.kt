package com.komugi.komugiaccounting.domain

import com.komugi.komugiaccounting.data.model.RecordType
import com.komugi.komugiaccounting.data.model.TransactionRecord
import org.junit.Assert.assertEquals
import org.junit.Test

class StatisticsCalculatorTest {
    @Test
    fun calculate_usesInclusiveStartAndExclusiveEnd() {
        val records = listOf(
            record("before", RecordType.INCOME, 1_000L, 9L),
            record("start", RecordType.INCOME, 2_000L, 10L),
            record("inside", RecordType.EXPENSE, 500L, 15L),
            record("end", RecordType.EXPENSE, 9_999L, 20L)
        )

        val result = StatisticsCalculator.calculate(records, startInclusive = 10L, endExclusive = 20L)

        assertEquals(2_000L, result.income)
        assertEquals(500L, result.expense)
        assertEquals(1_500L, result.balance)
    }

    @Test
    fun calculateAll_sumsIncomeExpenseAndBalance() {
        val result = StatisticsCalculator.calculateAll(
            listOf(
                record("income-a", RecordType.INCOME, 2_000L, 1L),
                record("income-b", RecordType.INCOME, 3_000L, 2L),
                record("expense", RecordType.EXPENSE, 1_200L, 3L)
            )
        )

        assertEquals(5_000L, result.income)
        assertEquals(1_200L, result.expense)
        assertEquals(3_800L, result.balance)
    }

    @Test
    fun calculate_treatsRefundedRecordsAsZero() {
        val result = StatisticsCalculator.calculateAll(
            listOf(
                record("income", RecordType.INCOME, 2_000L, 1L, isRefunded = true),
                record("expense", RecordType.EXPENSE, 1_200L, 2L, isRefunded = true),
                record("normal", RecordType.EXPENSE, 300L, 3L)
            )
        )

        assertEquals(0L, result.income)
        assertEquals(300L, result.expense)
        assertEquals(-300L, result.balance)
    }

    private fun record(
        id: String,
        type: RecordType,
        amount: Long,
        dateTime: Long,
        isRefunded: Boolean = false
    ) = TransactionRecord(
        id = id,
        type = type,
        amount = amount,
        categoryId = "category",
        memberId = "member",
        remark = "",
        dateTime = dateTime,
        createdAt = dateTime,
        updatedAt = dateTime,
        isRefunded = isRefunded
    )
}

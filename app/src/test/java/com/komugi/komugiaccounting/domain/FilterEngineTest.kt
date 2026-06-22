package com.komugi.komugiaccounting.domain

import com.komugi.komugiaccounting.data.model.FilterParams
import com.komugi.komugiaccounting.data.model.RecordType
import com.komugi.komugiaccounting.data.model.SortMode
import com.komugi.komugiaccounting.data.model.TransactionRecord
import org.junit.Assert.assertEquals
import org.junit.Test

class FilterEngineTest {
    @Test
    fun apply_filtersByTypeCategoryMemberAmountTimeAndKeyword() {
        val records = listOf(
            record("expense-match", RecordType.EXPENSE, 2_500L, "food", "me", "Lunch", 20L),
            record("wrong-type", RecordType.INCOME, 2_500L, "food", "me", "Lunch", 20L),
            record("wrong-category", RecordType.EXPENSE, 2_500L, "traffic", "me", "Lunch", 20L),
            record("wrong-member", RecordType.EXPENSE, 2_500L, "food", "family", "Lunch", 20L),
            record("too-small", RecordType.EXPENSE, 900L, "food", "me", "Lunch", 20L),
            record("too-late", RecordType.EXPENSE, 2_500L, "food", "me", "Lunch", 30L),
            record("wrong-keyword", RecordType.EXPENSE, 2_500L, "food", "me", "Dinner", 20L)
        )

        val result = FilterEngine.apply(
            records,
            FilterParams(
                type = RecordType.EXPENSE,
                categoryIds = setOf("food"),
                memberIds = setOf("me"),
                minAmount = 1_000L,
                maxAmount = 3_000L,
                startTime = 10L,
                endTime = 30L,
                keyword = "lunch"
            )
        )

        assertEquals(listOf("expense-match"), result.map { it.id })
    }

    @Test
    fun apply_sortsByAmountAscending() {
        val records = listOf(
            record("middle", RecordType.EXPENSE, 300L, dateTime = 1L),
            record("lowest", RecordType.EXPENSE, 100L, dateTime = 2L),
            record("highest", RecordType.EXPENSE, 500L, dateTime = 3L)
        )

        val result = FilterEngine.apply(records, FilterParams(sortMode = SortMode.AMOUNT_ASC))

        assertEquals(listOf("lowest", "middle", "highest"), result.map { it.id })
    }

    @Test
    fun apply_usesEffectiveAmountForRefundedRecords() {
        val records = listOf(
            record("refunded", RecordType.EXPENSE, 500L, dateTime = 1L, isRefunded = true),
            record("normal", RecordType.EXPENSE, 100L, dateTime = 2L)
        )

        val sorted = FilterEngine.apply(records, FilterParams(sortMode = SortMode.AMOUNT_ASC))
        val filtered = FilterEngine.apply(records, FilterParams(minAmount = 1L))

        assertEquals(listOf("refunded", "normal"), sorted.map { it.id })
        assertEquals(listOf("normal"), filtered.map { it.id })
    }

    private fun record(
        id: String,
        type: RecordType,
        amount: Long,
        categoryId: String = "category",
        memberId: String = "member",
        remark: String = "",
        dateTime: Long,
        isRefunded: Boolean = false
    ) = TransactionRecord(
        id = id,
        type = type,
        amount = amount,
        categoryId = categoryId,
        memberId = memberId,
        remark = remark,
        dateTime = dateTime,
        createdAt = dateTime,
        updatedAt = dateTime,
        isRefunded = isRefunded
    )
}

package com.komugi.komugiaccounting.domain

import com.komugi.komugiaccounting.data.model.FilterParams
import com.komugi.komugiaccounting.data.model.SortMode
import com.komugi.komugiaccounting.data.model.TransactionRecord

object FilterEngine {
    fun apply(records: List<TransactionRecord>, params: FilterParams): List<TransactionRecord> {
        return apply(records, params, emptyMap(), emptyMap())
    }

    fun apply(
        records: List<TransactionRecord>,
        params: FilterParams,
        categoryNames: Map<String, String>,
        memberNames: Map<String, String>
    ): List<TransactionRecord> {
        return records.asSequence()
            .filter { params.type == null || it.type == params.type }
            .filter { params.categoryIds.isEmpty() || it.categoryId in params.categoryIds }
            .filter { params.memberIds.isEmpty() || it.memberId in params.memberIds }
            .filter { params.includeRefunded || !it.isRefunded }
            .filter { params.minAmount == null || it.effectiveAmount >= params.minAmount }
            .filter { params.maxAmount == null || it.effectiveAmount <= params.maxAmount }
            .filter { params.startTime == null || it.dateTime >= params.startTime }
            .filter { params.endTime == null || it.dateTime < params.endTime }
            .filter {
                params.keyword.isBlank() ||
                    it.remark.contains(params.keyword, ignoreCase = true) ||
                    categoryNames[it.categoryId].orEmpty().contains(params.keyword, ignoreCase = true) ||
                    memberNames[it.memberId].orEmpty().contains(params.keyword, ignoreCase = true)
            }
            .let { sequence ->
                when (params.sortMode) {
                    SortMode.TIME_DESC -> sequence.sortedByDescending { it.dateTime }
                    SortMode.TIME_ASC -> sequence.sortedBy { it.dateTime }
                    SortMode.AMOUNT_DESC -> sequence.sortedByDescending { it.effectiveAmount }
                    SortMode.AMOUNT_ASC -> sequence.sortedBy { it.effectiveAmount }
                }
            }
            .toList()
    }
}

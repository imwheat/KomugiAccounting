package com.komugi.komugiaccounting.domain

import com.komugi.komugiaccounting.data.model.FilterParams
import com.komugi.komugiaccounting.data.model.SortMode
import com.komugi.komugiaccounting.data.model.TransactionRecord

object FilterEngine {
    fun apply(records: List<TransactionRecord>, params: FilterParams): List<TransactionRecord> {
        return records.asSequence()
            .filter { params.type == null || it.type == params.type }
            .filter { params.categoryIds.isEmpty() || it.categoryId in params.categoryIds }
            .filter { params.memberIds.isEmpty() || it.memberId in params.memberIds }
            .filter { params.minAmount == null || it.amount >= params.minAmount }
            .filter { params.maxAmount == null || it.amount <= params.maxAmount }
            .filter { params.startTime == null || it.dateTime >= params.startTime }
            .filter { params.endTime == null || it.dateTime < params.endTime }
            .filter { params.keyword.isBlank() || it.remark.contains(params.keyword, ignoreCase = true) }
            .let { sequence ->
                when (params.sortMode) {
                    SortMode.TIME_DESC -> sequence.sortedByDescending { it.dateTime }
                    SortMode.TIME_ASC -> sequence.sortedBy { it.dateTime }
                    SortMode.AMOUNT_DESC -> sequence.sortedByDescending { it.amount }
                    SortMode.AMOUNT_ASC -> sequence.sortedBy { it.amount }
                }
            }
            .toList()
    }
}

package com.komugi.komugiaccounting.data.model

data class FilterParams(
    val type: RecordType? = null,
    val categoryIds: Set<String> = emptySet(),
    val memberIds: Set<String> = emptySet(),
    val minAmount: Long? = null,
    val maxAmount: Long? = null,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val keyword: String = "",
    val sortDescending: Boolean = true
)

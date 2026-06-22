package com.komugi.komugiaccounting.data.model

data class TransactionRecord(
    val id: String,
    val type: RecordType,
    val amount: Long,
    val categoryId: String,
    val memberId: String,
    val remark: String = "",
    val dateTime: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val isRefunded: Boolean = false
) {
    val effectiveAmount: Long
        get() = if (isRefunded) 0L else amount
}

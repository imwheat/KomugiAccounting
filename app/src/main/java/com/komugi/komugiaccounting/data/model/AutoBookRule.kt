package com.komugi.komugiaccounting.data.model

data class AutoBookRule(
    val id: String,
    val name: String,
    val titleKeyword: String,
    val textPattern: String,
    val type: RecordType,
    val enabled: Boolean = true
)

data class AutoBookTodo(
    val id: String,
    val ruleId: String,
    val ruleName: String,
    val type: RecordType,
    val amount: Long,
    val notificationTitle: String,
    val notificationText: String,
    val dateTime: Long
)

data class AddRecordDraft(
    val type: RecordType,
    val amount: Long,
    val dateTime: Long,
    val remark: String = "",
    val categoryId: String? = null,
    val memberId: String? = null
)

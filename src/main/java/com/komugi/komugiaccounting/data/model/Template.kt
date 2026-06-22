package com.komugi.komugiaccounting.data.model

data class Template(
    val id: String,
    val name: String,
    val type: RecordType,
    val amount: Long? = null,
    val categoryId: String,
    val memberId: String,
    val remark: String = ""
)

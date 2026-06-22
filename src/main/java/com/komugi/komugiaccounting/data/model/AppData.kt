package com.komugi.komugiaccounting.data.model

data class AppData(
    val version: Int = 1,
    val records: List<TransactionRecord> = emptyList(),
    val categories: List<Category> = emptyList(),
    val members: List<Member> = emptyList(),
    val templates: List<Template> = emptyList(),
    val settings: AppSettings = AppSettings()
)

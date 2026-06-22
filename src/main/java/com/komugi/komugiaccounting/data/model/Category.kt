package com.komugi.komugiaccounting.data.model

data class Category(
    val id: String,
    val name: String,
    val type: RecordType,
    val iconName: String,
    val color: String,
    val sortOrder: Int,
    val enabled: Boolean = true,
    val isSystem: Boolean = false
)

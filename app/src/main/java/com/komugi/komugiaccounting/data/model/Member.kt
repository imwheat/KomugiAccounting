package com.komugi.komugiaccounting.data.model

data class Member(
    val id: String,
    val name: String,
    val avatarColor: String,
    val iconName: String = "",
    val enabled: Boolean = true,
    val isSystem: Boolean = false
)

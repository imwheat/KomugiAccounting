package com.komugi.komugiaccounting.data.model

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val lastExpenseCategoryId: String? = null,
    val lastIncomeCategoryId: String? = null,
    val lastMemberId: String? = null,
    val currencySymbol: String = "￥"
)

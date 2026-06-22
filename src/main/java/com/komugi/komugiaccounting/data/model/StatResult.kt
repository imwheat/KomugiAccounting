package com.komugi.komugiaccounting.data.model

data class StatResult(
    val income: Long = 0L,
    val expense: Long = 0L
) {
    val balance: Long get() = income - expense
}

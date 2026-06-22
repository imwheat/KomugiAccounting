package com.komugi.komugiaccounting.util

import java.math.BigDecimal
import java.math.RoundingMode

object AmountUtil {
    fun parseToCents(input: String): Long? {
        val clean = input.trim()
        if (clean.isEmpty()) return null
        return runCatching {
            BigDecimal(clean)
                .setScale(2, RoundingMode.HALF_UP)
                .multiply(BigDecimal(100))
                .longValueExact()
        }.getOrNull()?.takeIf { it > 0L }
    }

    fun format(cents: Long, symbol: String = "￥"): String {
        val sign = if (cents < 0) "-" else ""
        val abs = kotlin.math.abs(cents)
        return "$sign$symbol${abs / 100}.${(abs % 100).toString().padStart(2, '0')}"
    }

    fun formatPlain(cents: Long): String {
        val sign = if (cents < 0) "-" else ""
        val abs = kotlin.math.abs(cents)
        return "$sign${abs / 100}.${(abs % 100).toString().padStart(2, '0')}"
    }
}

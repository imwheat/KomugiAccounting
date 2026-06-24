package com.komugi.komugiaccounting.util

import java.math.BigDecimal
import java.math.RoundingMode

object AmountUtil {
    fun parseToCents(input: String): Long? {
        val amountText = normalizeAmountText(input) ?: return null
        return runCatching {
            BigDecimal(amountText)
                .setScale(2, RoundingMode.HALF_UP)
                .multiply(BigDecimal(100))
                .longValueExact()
        }.getOrNull()?.takeIf { it > 0L }
    }

    private fun normalizeAmountText(input: String): String? {
        val clean = input
            .filter { it.isDigit() || it == ',' || it == '.' }
            .replace(",", "")
        if (clean.none { it.isDigit() }) return null
        val builder = StringBuilder()
        var dotSeen = false
        clean.forEach { char ->
            when {
                char.isDigit() -> builder.append(char)
                char == '.' && !dotSeen -> {
                    builder.append(char)
                    dotSeen = true
                }
            }
        }
        return builder.toString().takeIf { it.any(Char::isDigit) }
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

package com.komugi.komugiaccounting.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AmountUtilTest {
    @Test
    fun parseToCents_roundsToTwoDecimals() {
        assertEquals(1235L, AmountUtil.parseToCents("12.345"))
        assertEquals(1200L, AmountUtil.parseToCents("12"))
        assertEquals(1L, AmountUtil.parseToCents("0.01"))
    }

    @Test
    fun parseToCents_rejectsBlankZeroAndNegativeValues() {
        assertNull(AmountUtil.parseToCents(""))
        assertNull(AmountUtil.parseToCents("0"))
        assertNull(AmountUtil.parseToCents("-1"))
        assertNull(AmountUtil.parseToCents("abc"))
    }

    @Test
    fun format_outputsCurrencyAndPlainAmount() {
        assertEquals("￥12.30", AmountUtil.format(1230L))
        assertEquals("-￥12.30", AmountUtil.format(-1230L))
        assertEquals("12.30", AmountUtil.formatPlain(1230L))
    }
}

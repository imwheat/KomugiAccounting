package com.komugi.komugiaccounting.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvTableBuilderTest {
    @Test
    fun build_includesBomByDefault() {
        val csv = CsvTableBuilder.build(listOf(listOf("日期", "金额")))

        assertTrue(csv.startsWith("\uFEFF"))
        assertEquals("\uFEFF日期,金额", csv)
    }

    @Test
    fun build_escapesCommaQuoteAndLineBreaks() {
        val csv = CsvTableBuilder.build(
            rows = listOf(
                listOf("普通", "包含,逗号", "包含\"引号\"", "多\n行", "回\r车")
            ),
            includeBom = false
        )

        assertEquals("普通,\"包含,逗号\",\"包含\"\"引号\"\"\",\"多\n行\",\"回\r车\"", csv)
    }

    @Test
    fun build_joinsRowsWithLf() {
        val csv = CsvTableBuilder.build(
            rows = listOf(
                listOf("A", "B"),
                listOf("1", "2")
            ),
            includeBom = false
        )

        assertEquals("A,B\n1,2", csv)
    }
}

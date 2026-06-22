package com.komugi.komugiaccounting.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

class XlsxWorkbookBuilderTest {
    @Test
    fun build_createsExpectedWorkbookParts() {
        val entries = unzip(
            XlsxWorkbookBuilder.build(
                listOf(
                    "账单明细" to listOf(listOf("日期", "金额"), listOf("2026-06-22", "12.30")),
                    "分类汇总" to listOf(listOf("分类", "占比"))
                )
            )
        )

        assertTrue("[Content_Types].xml" in entries)
        assertTrue("_rels/.rels" in entries)
        assertTrue("xl/workbook.xml" in entries)
        assertTrue("xl/_rels/workbook.xml.rels" in entries)
        assertTrue("xl/worksheets/sheet1.xml" in entries)
        assertTrue("xl/worksheets/sheet2.xml" in entries)
        assertTrue(entries.getValue("xl/workbook.xml").contains("""name="账单明细""""))
        assertTrue(entries.getValue("xl/workbook.xml").contains("""name="分类汇总""""))
    }

    @Test
    fun build_escapesXmlAndSupportsColumnsAfterZ() {
        val wideRow = (0..26).map { "列$it" }
        val entries = unzip(
            XlsxWorkbookBuilder.build(
                listOf(
                    "A&B\"Sheet" to listOf(
                        wideRow,
                        listOf("1 < 2 & \"quoted\"")
                    )
                )
            )
        )

        val workbookXml = entries.getValue("xl/workbook.xml")
        val sheetXml = entries.getValue("xl/worksheets/sheet1.xml")
        assertTrue(workbookXml.contains("""name="A&amp;B&quot;Sheet""""))
        assertTrue(sheetXml.contains("1 &lt; 2 &amp; &quot;quoted&quot;"))
        assertTrue(sheetXml.contains("""<c r="AA1" t="inlineStr">"""))
    }

    private fun unzip(bytes: ByteArray): Map<String, String> {
        val entries = linkedMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entries[entry.name] = zip.readBytes().toString(Charsets.UTF_8)
                zip.closeEntry()
            }
        }
        return entries
    }
}

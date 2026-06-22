package com.komugi.komugiaccounting.util

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object XlsxWorkbookBuilder {
    fun build(sheets: List<Pair<String, List<List<String>>>>): ByteArray {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            zip.textEntry("[Content_Types].xml", contentTypesXml(sheets.size))
            zip.textEntry("_rels/.rels", rootRelsXml())
            zip.textEntry("xl/workbook.xml", workbookXml(sheets.map { it.first }))
            zip.textEntry("xl/_rels/workbook.xml.rels", workbookRelsXml(sheets.size))
            sheets.forEachIndexed { index, (_, rows) ->
                zip.textEntry("xl/worksheets/sheet${index + 1}.xml", worksheetXml(rows))
            }
        }
        return bytes.toByteArray()
    }

    private fun ZipOutputStream.textEntry(path: String, text: String) {
        putNextEntry(ZipEntry(path))
        write(text.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun contentTypesXml(sheetCount: Int): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""")
        append("""<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""")
        append("""<Default Extension="xml" ContentType="application/xml"/>""")
        append("""<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""")
        (1..sheetCount).forEach { index ->
            append("""<Override PartName="/xl/worksheets/sheet$index.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""")
        }
        append("</Types>")
    }

    private fun rootRelsXml(): String =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>"""

    private fun workbookXml(sheetNames: List<String>): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets>""")
        sheetNames.forEachIndexed { index, name ->
            append("""<sheet name="${name.xmlEscape()}" sheetId="${index + 1}" r:id="rId${index + 1}"/>""")
        }
        append("</sheets></workbook>")
    }

    private fun workbookRelsXml(sheetCount: Int): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""")
        (1..sheetCount).forEach { index ->
            append("""<Relationship Id="rId$index" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet$index.xml"/>""")
        }
        append("</Relationships>")
    }

    private fun worksheetXml(rows: List<List<String>>): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""")
        rows.forEachIndexed { rowIndex, row ->
            val excelRow = rowIndex + 1
            append("""<row r="$excelRow">""")
            row.forEachIndexed { columnIndex, value ->
                val ref = "${columnName(columnIndex)}$excelRow"
                append("""<c r="$ref" t="inlineStr"><is><t>${value.xmlEscape()}</t></is></c>""")
            }
            append("</row>")
        }
        append("</sheetData></worksheet>")
    }

    private fun columnName(index: Int): String {
        var value = index
        val builder = StringBuilder()
        do {
            builder.insert(0, ('A'.code + value % 26).toChar())
            value = value / 26 - 1
        } while (value >= 0)
        return builder.toString()
    }

    private fun String.xmlEscape(): String =
        replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
}

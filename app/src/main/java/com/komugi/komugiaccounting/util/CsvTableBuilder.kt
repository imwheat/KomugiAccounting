package com.komugi.komugiaccounting.util

object CsvTableBuilder {
    fun build(rows: List<List<String>>, includeBom: Boolean = true): String {
        val body = rows.joinToString("\n") { row -> row.joinToString(",") { it.cell() } }
        return if (includeBom) "\uFEFF$body" else body
    }

    private fun String.cell(): String {
        val escaped = replace("\"", "\"\"")
        return if (any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"$escaped\"" else escaped
    }
}

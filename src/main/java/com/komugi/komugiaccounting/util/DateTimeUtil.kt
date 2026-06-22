package com.komugi.komugiaccounting.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateTimeUtil {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.CHINA)
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).apply { isLenient = false }
    private val displayDateTimeFormat = SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.CHINA)

    fun now(): Long = System.currentTimeMillis()
    fun formatDate(timestamp: Long): String = dateFormat.format(Date(timestamp))
    fun formatTime(timestamp: Long): String = timeFormat.format(Date(timestamp))
    fun formatDateTime(timestamp: Long): String = dateTimeFormat.format(Date(timestamp))
    fun formatDisplayDateTime(timestamp: Long): String = displayDateTimeFormat.format(Date(timestamp))
    fun parseDateTime(input: String): Long? = runCatching { dateTimeFormat.parse(input)?.time }.getOrNull()

    fun startOfDay(time: Long = now()): Long = Calendar.getInstance().apply {
        this.timeInMillis = time
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun startOfWeek(time: Long = now()): Long = Calendar.getInstance(Locale.CHINA).apply {
        firstDayOfWeek = Calendar.MONDAY
        this.timeInMillis = time
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun startOfMonth(time: Long = now()): Long = Calendar.getInstance().apply {
        this.timeInMillis = time
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun startOfYear(time: Long = now()): Long = Calendar.getInstance().apply {
        this.timeInMillis = time
        set(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun endExclusiveFromStart(start: Long, field: Int, amount: Int): Long = Calendar.getInstance().apply {
        timeInMillis = start
        add(field, amount)
    }.timeInMillis
}

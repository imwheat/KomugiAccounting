package com.komugi.komugiaccounting.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class DateTimeUtilTest {
    @Test
    fun parseDate_rejectsInvalidCalendarDates() {
        assertNotNull(DateTimeUtil.parseDate("2026-02-28"))
        assertNull(DateTimeUtil.parseDate("2026-02-31"))
        assertNull(DateTimeUtil.parseDate("2026-13-01"))
    }

    @Test
    fun parseDateTime_requiresValidDateAndTime() {
        assertNotNull(DateTimeUtil.parseDateTime("2026-06-22 09:30"))
        assertNull(DateTimeUtil.parseDateTime("2026-06-22 25:00"))
        assertNull(DateTimeUtil.parseDateTime("2026-02-31 09:30"))
    }

    @Test
    fun startOfMonth_returnsFirstDayAtMidnight() {
        val timestamp = calendarTime(2026, Calendar.JUNE, 22, 14, 30)

        val start = DateTimeUtil.startOfMonth(timestamp)

        assertEquals("2026-06-01", DateTimeUtil.formatDate(start))
        assertEquals("00:00", DateTimeUtil.formatTime(start))
    }

    @Test
    fun dayOfWeekMondayFirst_returnsSundayAsSeven() {
        val monday = calendarTime(2026, Calendar.JUNE, 22, 9, 0)
        val sunday = calendarTime(2026, Calendar.JUNE, 28, 9, 0)

        assertEquals(1, DateTimeUtil.dayOfWeekMondayFirst(monday))
        assertEquals(7, DateTimeUtil.dayOfWeekMondayFirst(sunday))
    }

    private fun calendarTime(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}

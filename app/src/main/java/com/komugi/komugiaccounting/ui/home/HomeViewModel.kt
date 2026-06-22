package com.komugi.komugiaccounting.ui.home

import com.komugi.komugiaccounting.data.model.AppData
import com.komugi.komugiaccounting.data.model.StatResult
import com.komugi.komugiaccounting.data.repository.AppDataRepository
import com.komugi.komugiaccounting.domain.StatisticsCalculator
import com.komugi.komugiaccounting.util.DateTimeUtil
import kotlinx.coroutines.flow.StateFlow
import java.util.Calendar

class HomeViewModel(private val repository: AppDataRepository) {
    val data: StateFlow<AppData> = repository.data

    fun monthStat(data: AppData): StatResult {
        val start = DateTimeUtil.startOfMonth()
        val end = DateTimeUtil.endExclusiveFromStart(start, Calendar.MONTH, 1)
        return StatisticsCalculator.calculate(data.records, start, end)
    }

    fun todayStat(data: AppData): StatResult {
        val start = DateTimeUtil.startOfDay()
        val end = DateTimeUtil.endExclusiveFromStart(start, Calendar.DAY_OF_YEAR, 1)
        return StatisticsCalculator.calculate(data.records, start, end)
    }

    fun weekStat(data: AppData): StatResult {
        val start = DateTimeUtil.startOfWeek()
        val end = DateTimeUtil.endExclusiveFromStart(start, Calendar.DAY_OF_YEAR, 7)
        return StatisticsCalculator.calculate(data.records, start, end)
    }

    fun yearStat(data: AppData): StatResult {
        val start = DateTimeUtil.startOfYear()
        val end = DateTimeUtil.endExclusiveFromStart(start, Calendar.YEAR, 1)
        return StatisticsCalculator.calculate(data.records, start, end)
    }
}

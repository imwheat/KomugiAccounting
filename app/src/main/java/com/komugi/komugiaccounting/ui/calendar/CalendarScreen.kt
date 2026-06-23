package com.komugi.komugiaccounting.ui.calendar

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.komugi.komugiaccounting.data.model.RecordType
import com.komugi.komugiaccounting.data.model.TransactionRecord
import com.komugi.komugiaccounting.data.repository.AppDataRepository
import com.komugi.komugiaccounting.ui.components.RecordItem
import com.komugi.komugiaccounting.util.AmountUtil
import com.komugi.komugiaccounting.util.DateTimeUtil
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalendarScreen(
    repository: AppDataRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val data by repository.data.collectAsState()
    var year by rememberSaveable { mutableIntStateOf(DateTimeUtil.year()) }
    var month by rememberSaveable { mutableIntStateOf(DateTimeUtil.monthZeroBased()) }
    var selectedDay by rememberSaveable { mutableIntStateOf(DateTimeUtil.dayOfMonth(DateTimeUtil.now())) }
    var showMonthPicker by rememberSaveable { mutableStateOf(false) }
    var pickerYear by rememberSaveable { mutableIntStateOf(year) }

    val monthStart = DateTimeUtil.startOfMonth(year, month)
    val daysInMonth = DateTimeUtil.daysInMonth(year, month)
    val leadingBlankCount = DateTimeUtil.dayOfWeekMondayFirst(monthStart) - 1
    val cells = List(leadingBlankCount) { CalendarCell.Blank } + (1..daysInMonth).map { day ->
        val start = DateTimeUtil.startOfMonth(year, month).let {
            DateTimeUtil.endExclusiveFromStart(it, Calendar.DAY_OF_MONTH, day - 1)
        }
        val end = DateTimeUtil.endExclusiveFromStart(start, Calendar.DAY_OF_MONTH, 1)
        CalendarCell.Day(
            day = day,
            income = data.records.sumByType(start, end, RecordType.INCOME),
            expense = data.records.sumByType(start, end, RecordType.EXPENSE)
        )
    }
    val selectedStart = DateTimeUtil.endExclusiveFromStart(monthStart, Calendar.DAY_OF_MONTH, selectedDay - 1)
    val selectedEnd = DateTimeUtil.endExclusiveFromStart(selectedStart, Calendar.DAY_OF_MONTH, 1)
    val selectedRecords = data.records
        .filter { it.dateTime in selectedStart until selectedEnd }
        .sortedByDescending { it.dateTime }
    val categories = data.categories.associateBy { it.id }
    val members = data.members.associateBy { it.id }

    LazyColumn(
        modifier = modifier.padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Spacer(Modifier.height(10.dp)) }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = onBack) { Text("<") }
                    Text("日历", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        if (month == 0) {
                            year -= 1
                            month = 11
                        } else {
                            month -= 1
                        }
                        pickerYear = year
                        selectedDay = 1
                    }) { Text("上月") }
                    OutlinedButton(onClick = {
                        if (month == 11) {
                            year += 1
                            month = 0
                        } else {
                            month += 1
                        }
                        pickerYear = year
                        selectedDay = 1
                    }) { Text("下月") }
                }
            }
        }
        item {
            Text(
                "${year} 年 ${month + 1} 月",
                modifier = Modifier.clickable {
                    pickerYear = year
                    showMonthPicker = !showMonthPicker
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        if (showMonthPicker) {
            item {
                MonthPickerPopup(
                    pickerYear = pickerYear,
                    selectedYear = year,
                    selectedMonth = month,
                    onYearChange = { pickerYear = it },
                    onSelectMonth = { selectedMonth ->
                        year = pickerYear
                        month = selectedMonth
                        selectedDay = selectedDay.coerceAtMost(DateTimeUtil.daysInMonth(year, month))
                        showMonthPicker = false
                    }
                )
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf("一", "二", "三", "四", "五", "六", "日").forEach {
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                Text(it, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(7),
                        modifier = Modifier.height(520.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(cells) { cell ->
                            when (cell) {
                                CalendarCell.Blank -> Box(Modifier.height(68.dp))
                                is CalendarCell.Day -> DayCell(
                                    cell = cell,
                                    selected = selectedDay == cell.day,
                                    onClick = { selectedDay = cell.day }
                                )
                            }
                        }
                    }
                }
            }
        }
        item {
            Text("${month + 1}月${selectedDay}日明细", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        if (selectedRecords.isEmpty()) {
            item { Text("这一天没有记录。", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(12.dp)) }
        } else {
            items(selectedRecords, key = { it.id }) { record ->
                RecordItem(record, categories[record.categoryId], members[record.memberId])
            }
        }
        item { Spacer(Modifier.height(88.dp)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MonthPickerPopup(
    pickerYear: Int,
    selectedYear: Int,
    selectedMonth: Int,
    onYearChange: (Int) -> Unit,
    onSelectMonth: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = { onYearChange(pickerYear - 1) }) { Text("<") }
                Text(
                    "${pickerYear}年",
                    modifier = Modifier.padding(horizontal = 18.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedButton(onClick = { onYearChange(pickerYear + 1) }) { Text(">") }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                (0..11).forEach { month ->
                    FilterChip(
                        selected = pickerYear == selectedYear && month == selectedMonth,
                        onClick = { onSelectMonth(month) },
                        label = { Text("${month + 1}月") }
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    cell: CalendarCell.Day,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Column(
        modifier = Modifier
            .height(68.dp)
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .border(1.dp, borderColor, RoundedCornerShape(7.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(cell.day.toString(), fontWeight = FontWeight.Bold)
        }
        Text(
            if (cell.income > 0L) "+${AmountUtil.format(cell.income)}" else "+￥0.00",
            color = Color(0xFF1F7A4D),
            fontSize = 9.sp,
            maxLines = 1
        )
        Text(
            if (cell.expense > 0L) "-${AmountUtil.format(cell.expense)}" else "-￥0.00",
            color = Color(0xFFB3542E),
            fontSize = 9.sp,
            maxLines = 1
        )
    }
}

private sealed interface CalendarCell {
    data object Blank : CalendarCell

    data class Day(
        val day: Int,
        val income: Long,
        val expense: Long
    ) : CalendarCell
}

private fun List<TransactionRecord>.sumByType(start: Long, end: Long, type: RecordType): Long =
    filter { it.type == type && it.dateTime in start until end }.sumOf { it.effectiveAmount }

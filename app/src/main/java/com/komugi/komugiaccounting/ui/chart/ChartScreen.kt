package com.komugi.komugiaccounting.ui.chart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.komugi.komugiaccounting.util.AmountUtil
import com.komugi.komugiaccounting.util.DateTimeUtil
import java.util.Calendar

@Composable
fun ChartScreen(
    repository: AppDataRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val data by repository.data.collectAsState()
    var year by rememberSaveable { mutableIntStateOf(DateTimeUtil.year()) }
    val monthStats = (0..11).map { month ->
        val start = DateTimeUtil.startOfMonth(year, month)
        val end = DateTimeUtil.endExclusiveFromStart(start, Calendar.MONTH, 1)
        MonthStat(
            month = month + 1,
            income = data.records.sumByType(start, end, RecordType.INCOME),
            expense = data.records.sumByType(start, end, RecordType.EXPENSE)
        )
    }
    val totalIncome = monthStats.sumOf { it.income }
    val totalExpense = monthStats.sumOf { it.expense }
    val balance = totalIncome - totalExpense
    val maxValue = monthStats.maxOfOrNull { maxOf(it.income, it.expense) }?.takeIf { it > 0L } ?: 1L

    LazyColumn(
        modifier = modifier.padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Spacer(Modifier.height(10.dp)) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onBack) { Text("<") }
                Text("年度图表", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AnnualAmountBlock(
                    label = "年收入",
                    amount = totalIncome,
                    color = Color(0xFF1F7A4D),
                    modifier = Modifier.weight(1f)
                )
                AnnualAmountBlock(
                    label = "年支出",
                    amount = totalExpense,
                    color = Color(0xFFB3542E),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Text(
                    "结余 ${AmountUtil.format(balance)}",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("每月收入支出", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    monthStats.forEach { stat ->
                        MonthBar(stat = stat, maxValue = maxValue)
                    }
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = { year -= 1 }) { Text("<") }
                Text(
                    "${year}年",
                    modifier = Modifier.padding(horizontal = 18.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                OutlinedButton(onClick = { year += 1 }) { Text(">") }
            }
        }
        item { Spacer(Modifier.height(88.dp)) }
    }
}

@Composable
private fun AnnualAmountBlock(
    label: String,
    amount: Long,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            Text(AmountUtil.format(amount), color = color, fontWeight = FontWeight.Black, fontSize = 22.sp)
        }
    }
}

@Composable
private fun MonthBar(stat: MonthStat, maxValue: Long) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${stat.month}月", modifier = Modifier.width(42.dp), fontWeight = FontWeight.SemiBold)
            Bar(value = stat.income, maxValue = maxValue, color = Color(0xFF1F7A4D), modifier = Modifier.weight(1f))
            Text(AmountUtil.format(stat.income), modifier = Modifier.width(96.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(42.dp))
            Bar(value = stat.expense, maxValue = maxValue, color = Color(0xFFB3542E), modifier = Modifier.weight(1f))
            Text(AmountUtil.format(stat.expense), modifier = Modifier.width(96.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Bar(value: Long, maxValue: Long, color: Color, modifier: Modifier = Modifier) {
    val fraction = (value.toFloat() / maxValue.toFloat()).coerceIn(0.02f, 1f)
    Box(modifier = modifier.height(10.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(99.dp))) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(10.dp)
                .background(color, RoundedCornerShape(99.dp))
        )
    }
}

private data class MonthStat(
    val month: Int,
    val income: Long,
    val expense: Long
)

private fun List<TransactionRecord>.sumByType(start: Long, end: Long, type: RecordType): Long =
    filter { it.type == type && it.dateTime in start until end }.sumOf { it.effectiveAmount }

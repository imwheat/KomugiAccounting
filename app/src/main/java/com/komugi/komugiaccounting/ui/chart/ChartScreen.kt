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
import com.komugi.komugiaccounting.data.model.RecordType
import com.komugi.komugiaccounting.data.model.TransactionRecord
import com.komugi.komugiaccounting.data.repository.AppDataRepository
import com.komugi.komugiaccounting.util.AmountUtil
import com.komugi.komugiaccounting.util.DateTimeUtil
import java.util.Calendar

@Composable
fun ChartScreen(
    repository: AppDataRepository,
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
    val maxValue = monthStats.maxOfOrNull { maxOf(it.income, it.expense) }?.takeIf { it > 0L } ?: 1L
    val highestIncome = monthStats.maxByOrNull { it.income }
    val highestExpense = monthStats.maxByOrNull { it.expense }

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
                Text("年度图表", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { year -= 1 }) { Text("上一年") }
                    OutlinedButton(onClick = { year += 1 }) { Text("下一年") }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${year} 年汇总", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("收入 ${AmountUtil.format(totalIncome)}")
                    Text("支出 ${AmountUtil.format(totalExpense)}")
                    Text("结余 ${AmountUtil.format(totalIncome - totalExpense)}")
                    Text("最高收入月：${highestIncome?.month ?: "-"} 月 ${highestIncome?.income?.let { AmountUtil.format(it) } ?: "￥0.00"}")
                    Text("最高支出月：${highestExpense?.month ?: "-"} 月 ${highestExpense?.expense?.let { AmountUtil.format(it) } ?: "￥0.00"}")
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("12 个月收支", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    monthStats.forEach { stat ->
                        MonthBar(stat = stat, maxValue = maxValue)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(88.dp)) }
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

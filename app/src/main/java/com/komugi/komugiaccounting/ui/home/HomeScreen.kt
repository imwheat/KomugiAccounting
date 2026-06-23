package com.komugi.komugiaccounting.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.komugi.komugiaccounting.ui.components.RecordItem
import com.komugi.komugiaccounting.ui.components.StatCard
import com.komugi.komugiaccounting.data.repository.AppDataRepository
import com.komugi.komugiaccounting.ui.calendar.CalendarScreen
import com.komugi.komugiaccounting.ui.chart.ChartScreen
import com.komugi.komugiaccounting.ui.detail.DetailScreen
import com.komugi.komugiaccounting.ui.detail.DetailViewModel
import com.komugi.komugiaccounting.util.AmountUtil

@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    detailViewModel: DetailViewModel,
    repository: AppDataRepository,
    onEditRecord: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var page by rememberSaveable { mutableIntStateOf(0) }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("首页", "明细", "图表", "日历").forEachIndexed { index, title ->
                FilterChip(
                    selected = page == index,
                    onClick = { page = index },
                    label = { Text(title) }
                )
            }
        }
        when (page) {
            0 -> HomeOverviewScreen(
                viewModel = homeViewModel,
                onEditRecord = onEditRecord
            )
            1 -> DetailScreen(viewModel = detailViewModel, onEditRecord = onEditRecord)
            2 -> ChartScreen(repository = repository)
            3 -> CalendarScreen(repository = repository)
        }
    }
}

@Composable
private fun HomeOverviewScreen(
    viewModel: HomeViewModel,
    onEditRecord: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val data by viewModel.data.collectAsState()
    val categories = data.categories.associateBy { it.id }
    val members = data.members.associateBy { it.id }
    var monthOffset by rememberSaveable { mutableIntStateOf(0) }
    var expenseExpanded by rememberSaveable { mutableStateOf(false) }
    var incomeExpanded by rememberSaveable { mutableStateOf(false) }
    val expenseSummary = viewModel.monthExpenseSummary(data, monthOffset)
    val incomeSummary = viewModel.monthIncomeSummary(data, monthOffset)

    LazyColumn(
        modifier = modifier.padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Spacer(Modifier.height(10.dp)) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("小麦账本", fontSize = 34.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground)
                Text("本地保存的个人收支记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = monthOffset == 0, onClick = { monthOffset = 0 }, label = { Text("本月") })
                FilterChip(selected = monthOffset == -1, onClick = { monthOffset = -1 }, label = { Text("上月") })
            }
        }
        item { StatCard(viewModel.monthTitle(monthOffset), viewModel.monthStat(data, monthOffset)) }
        item {
            MonthlyCategorySummaryCard(
                title = "本月支出统计",
                recordCount = expenseSummary.recordCount,
                totalLabel = "总支出",
                totalAmount = expenseSummary.totalExpense,
                categories = expenseSummary.categories.map {
                    CategorySummaryItem(it.categoryName, it.amount, it.percent)
                },
                expanded = expenseExpanded,
                onExpandedChange = { expenseExpanded = it }
            )
        }
        item {
            MonthlyCategorySummaryCard(
                title = "本月收入统计",
                recordCount = incomeSummary.recordCount,
                totalLabel = "总收入",
                totalAmount = incomeSummary.totalIncome,
                categories = incomeSummary.categories.map {
                    CategorySummaryItem(it.categoryName, it.amount, it.percent)
                },
                expanded = incomeExpanded,
                onExpandedChange = { incomeExpanded = it }
            )
        }
        item { StatCard("今日统计", viewModel.todayStat(data)) }
        item { StatCard("本周统计", viewModel.weekStat(data)) }
        item { StatCard("本年统计", viewModel.yearStat(data)) }
        item { Text("最近记录", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        if (data.records.isEmpty()) {
            item { Text("还没有账目，点击底部 + 记第一笔。", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(12.dp)) }
        } else {
            items(data.records.take(8), key = { it.id }) { record ->
                RecordItem(
                    record = record,
                    category = categories[record.categoryId],
                    member = members[record.memberId],
                    onClick = { onEditRecord(record.id) }
                )
            }
        }
        item { Spacer(Modifier.height(88.dp)) }
    }
}

@Composable
private fun MonthlyCategorySummaryCard(
    title: String,
    recordCount: Int,
    totalLabel: String,
    totalAmount: Long,
    categories: List<CategorySummaryItem>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val visibleCategories = if (expanded) categories else categories.take(5)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("共${recordCount}笔记账", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("$totalLabel ${AmountUtil.format(totalAmount)}", fontWeight = FontWeight.Black, fontSize = 20.sp)
            if (categories.isEmpty()) {
                Text("这个月份还没有记录。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                visibleCategories.forEach { item ->
                    CategorySummaryRow(item)
                }
                if (categories.size > 5) {
                    OutlinedButton(
                        onClick = { onExpandedChange(!expanded) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (expanded) "收起" else "展开全部")
                    }
                }
            }
        }
    }
}

@Composable
private fun CategorySummaryRow(item: CategorySummaryItem) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(item.categoryName, fontWeight = FontWeight.SemiBold)
        Text(
            "${AmountUtil.format(item.amount)} · ${formatPercent(item.percent)}",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private data class CategorySummaryItem(
    val categoryName: String,
    val amount: Long,
    val percent: Double
)

private fun formatPercent(value: Double): String =
    "${(value * 100).coerceAtLeast(0.0).let { "%.1f".format(it) }}%"

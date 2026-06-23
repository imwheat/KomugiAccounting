package com.komugi.komugiaccounting.ui.chart

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.komugi.komugiaccounting.data.model.Category
import com.komugi.komugiaccounting.data.model.RecordType
import com.komugi.komugiaccounting.data.model.TransactionRecord
import com.komugi.komugiaccounting.data.repository.AppDataRepository
import com.komugi.komugiaccounting.ui.components.CategoryIconBadge
import com.komugi.komugiaccounting.ui.components.panelBackgroundColor
import com.komugi.komugiaccounting.ui.components.parseCategoryColor
import com.komugi.komugiaccounting.ui.detail.DetailFilterRequest
import com.komugi.komugiaccounting.util.AmountUtil
import com.komugi.komugiaccounting.util.DateTimeUtil
import java.util.Calendar
import java.util.Locale

@Composable
fun ChartScreen(
    repository: AppDataRepository,
    onBack: () -> Unit,
    onOpenDetail: (DetailFilterRequest) -> Unit,
    modifier: Modifier = Modifier
) {
    val data by repository.data.collectAsState()
    var year by rememberSaveable { mutableIntStateOf(DateTimeUtil.year()) }
    var expenseExpanded by rememberSaveable { mutableStateOf(false) }
    var incomeExpanded by rememberSaveable { mutableStateOf(false) }
    val yearStart = startOfYear(year)
    val yearEnd = DateTimeUtil.endExclusiveFromStart(yearStart, Calendar.YEAR, 1)
    val monthStats = (0..11).map { month ->
        val start = DateTimeUtil.startOfMonth(year, month)
        val end = DateTimeUtil.endExclusiveFromStart(start, Calendar.MONTH, 1)
        MonthStat(
            month = month + 1,
            start = start,
            endExclusive = end,
            income = data.records.sumByType(start, end, RecordType.INCOME),
            expense = data.records.sumByType(start, end, RecordType.EXPENSE)
        )
    }
    val totalIncome = monthStats.sumOf { it.income }
    val totalExpense = monthStats.sumOf { it.expense }
    val balance = totalIncome - totalExpense
    val maxValue = monthStats.maxOfOrNull { maxOf(it.income, it.expense) }?.takeIf { it > 0L } ?: 1L
    val expenseSummary = annualGroupSummary(
        records = data.records,
        categories = data.categories,
        type = RecordType.EXPENSE,
        start = yearStart,
        endExclusive = yearEnd
    )
    val incomeSummary = annualGroupSummary(
        records = data.records,
        categories = data.categories,
        type = RecordType.INCOME,
        start = yearStart,
        endExclusive = yearEnd
    )

    Column(modifier = modifier.padding(horizontal = 18.dp)) {
        LazyColumn(
            modifier = Modifier.weight(1f),
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
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenDetail(yearFilter(yearStart, yearEnd, RecordType.INCOME)) }
                    )
                    AnnualAmountBlock(
                        label = "年支出",
                        amount = totalExpense,
                        color = Color(0xFFB3542E),
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenDetail(yearFilter(yearStart, yearEnd, RecordType.EXPENSE)) }
                    )
                }
            }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenDetail(yearFilter(yearStart, yearEnd, null)) },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = panelBackgroundColor())
                ) {
                    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("结余", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                        Text(AmountUtil.format(balance), color = Color(0xFF2E5A87), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    }
                }
            }
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = panelBackgroundColor())
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("每月收入支出", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        monthStats.forEach { stat ->
                            MonthBar(
                                stat = stat,
                                maxValue = maxValue,
                                onClick = { onOpenDetail(yearFilter(stat.start, stat.endExclusive, null)) }
                            )
                        }
                    }
                }
            }
            item {
                AnnualGroupSummaryCard(
                    title = "年度支出统计",
                    recordCount = expenseSummary.recordCount,
                    totalLabel = "总支出",
                    totalAmount = expenseSummary.totalAmount,
                    totalAmountColor = Color(0xFFB3542E),
                    groups = expenseSummary.groups,
                    expanded = expenseExpanded,
                    onExpandedChange = { expenseExpanded = it },
                    onGroupClick = { ids -> onOpenDetail(groupFilter(yearStart, yearEnd, RecordType.EXPENSE, ids)) }
                )
            }
            item {
                AnnualGroupSummaryCard(
                    title = "年度收入统计",
                    recordCount = incomeSummary.recordCount,
                    totalLabel = "总收入",
                    totalAmount = incomeSummary.totalAmount,
                    totalAmountColor = Color(0xFF1F7A4D),
                    groups = incomeSummary.groups,
                    expanded = incomeExpanded,
                    onExpandedChange = { incomeExpanded = it },
                    onGroupClick = { ids -> onOpenDetail(groupFilter(yearStart, yearEnd, RecordType.INCOME, ids)) }
                )
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
        YearSwitcher(year = year, onPrevious = { year -= 1 }, onNext = { year += 1 })
    }
}

@Composable
private fun YearSwitcher(year: Int, onPrevious: () -> Unit, onNext: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = panelBackgroundColor())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onPrevious) { Text("<") }
            Text("${year}年", modifier = Modifier.padding(horizontal = 18.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = onNext) { Text(">") }
        }
    }
}

@Composable
private fun AnnualAmountBlock(label: String, amount: Long, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = panelBackgroundColor())
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            Text(AmountUtil.format(amount), color = color, fontWeight = FontWeight.Black, fontSize = 22.sp)
        }
    }
}

@Composable
private fun MonthBar(stat: MonthStat, maxValue: Long, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 5.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${stat.month}月", modifier = Modifier.width(42.dp), fontWeight = FontWeight.SemiBold)
            Bar(stat.income, maxValue, Color(0xFF1F7A4D), Modifier.weight(1f))
            Text(AmountUtil.format(stat.income), modifier = Modifier.width(96.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(42.dp))
            Bar(stat.expense, maxValue, Color(0xFFB3542E), Modifier.weight(1f))
            Text(AmountUtil.format(stat.expense), modifier = Modifier.width(96.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AnnualGroupSummaryCard(
    title: String,
    recordCount: Int,
    totalLabel: String,
    totalAmount: Long,
    totalAmountColor: Color,
    groups: List<GroupSummaryItem>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onGroupClick: (Set<String>) -> Unit
) {
    val visibleGroups = if (expanded) groups else groups.take(5)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = panelBackgroundColor())
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("共${recordCount}笔记账", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(totalLabel, fontWeight = FontWeight.Black, fontSize = 20.sp)
                Text(AmountUtil.format(totalAmount), color = totalAmountColor, fontWeight = FontWeight.Black, fontSize = 20.sp)
            }
            if (groups.isEmpty()) {
                Text("这一年还没有记录。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                visibleGroups.forEach { item ->
                    GroupSummaryRow(item = item, onClick = { onGroupClick(item.categoryIds) })
                }
                if (groups.size > 5) {
                    OutlinedButton(onClick = { onExpandedChange(!expanded) }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (expanded) "收起" else "展开全部")
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupSummaryRow(item: GroupSummaryItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.size(width = 58.dp, height = 68.dp)) {
            CategoryIconBadge(
                name = item.groupName,
                iconName = item.iconName,
                color = item.color,
                iconImageUri = item.iconImageUri,
                size = 46.dp
            )
            Text(item.groupName, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(AmountUtil.format(item.amount), fontWeight = FontWeight.Bold)
                Text(formatPercent(item.percent), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            GroupProgressBar(percent = item.percent, color = parseCategoryColor(item.color))
        }
    }
}

@Composable
private fun GroupProgressBar(percent: Double, color: Color) {
    val fraction = percent.toFloat().coerceIn(0f, 1f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(9.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(99.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(9.dp)
                .background(color, RoundedCornerShape(99.dp))
        )
    }
}

@Composable
private fun Bar(value: Long, maxValue: Long, color: Color, modifier: Modifier = Modifier) {
    val fraction = (value.toFloat() / maxValue.toFloat()).coerceIn(0.02f, 1f)
    Box(modifier = modifier.height(10.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(99.dp))) {
        Box(modifier = Modifier.fillMaxWidth(fraction).height(10.dp).background(color, RoundedCornerShape(99.dp)))
    }
}

private data class MonthStat(
    val month: Int,
    val start: Long,
    val endExclusive: Long,
    val income: Long,
    val expense: Long
)

private data class AnnualGroupSummary(
    val recordCount: Int,
    val totalAmount: Long,
    val groups: List<GroupSummaryItem>
)

private data class GroupSummaryItem(
    val groupName: String,
    val iconName: String,
    val color: String,
    val iconImageUri: String,
    val categoryIds: Set<String>,
    val amount: Long,
    val percent: Double
)

private fun annualGroupSummary(
    records: List<TransactionRecord>,
    categories: List<Category>,
    type: RecordType,
    start: Long,
    endExclusive: Long
): AnnualGroupSummary {
    val categoryById = categories.associateBy { it.id }
    val filtered = records.filter { it.type == type && it.dateTime in start until endExclusive && it.effectiveAmount > 0L }
    val total = filtered.sumOf { it.effectiveAmount }
    val byGroup = filtered.groupBy { record ->
        val category = categoryById[record.categoryId]
        category?.groupName?.takeIf { it.isNotBlank() } ?: category?.name ?: "未分组"
    }
    val groups = byGroup.map { (groupName, groupRecords) ->
        val categoryIds = groupRecords.map { it.categoryId }.toSet()
        val meta = groupMeta(categories, type, groupName, categoryIds)
        val amount = groupRecords.sumOf { it.effectiveAmount }
        GroupSummaryItem(
            groupName = groupName,
            iconName = meta?.iconName?.ifBlank { groupName.firstIconText() } ?: groupName.firstIconText(),
            color = meta?.color ?: "#9E9E9E",
            iconImageUri = meta?.iconImageUri.orEmpty(),
            categoryIds = categoryIds,
            amount = amount,
            percent = if (total > 0L) amount.toDouble() / total.toDouble() else 0.0
        )
    }.sortedByDescending { it.amount }
    return AnnualGroupSummary(recordCount = filtered.size, totalAmount = total, groups = groups)
}

private fun groupMeta(categories: List<Category>, type: RecordType, groupName: String, categoryIds: Set<String>): Category? {
    val groupCategories = categories.filter { it.type == type && it.groupName == groupName }
    return groupCategories.firstOrNull { it.name.startsWith("__group__") }
        ?: groupCategories.firstOrNull { it.id in categoryIds }
        ?: groupCategories.minByOrNull { it.sortOrder }
}

private fun List<TransactionRecord>.sumByType(start: Long, end: Long, type: RecordType): Long =
    filter { it.type == type && it.dateTime in start until end }.sumOf { it.effectiveAmount }

private fun startOfYear(year: Int): Long = Calendar.getInstance(Locale.CHINA).apply {
    set(Calendar.YEAR, year)
    set(Calendar.DAY_OF_YEAR, 1)
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun yearFilter(start: Long, endExclusive: Long, type: RecordType?): DetailFilterRequest =
    DetailFilterRequest(
        type = type,
        startDate = DateTimeUtil.formatDate(start),
        endDate = DateTimeUtil.formatDate(endExclusive - 1)
    )

private fun groupFilter(start: Long, endExclusive: Long, type: RecordType, categoryIds: Set<String>): DetailFilterRequest =
    DetailFilterRequest(
        type = type,
        categoryIds = categoryIds,
        startDate = DateTimeUtil.formatDate(start),
        endDate = DateTimeUtil.formatDate(endExclusive - 1)
    )

private fun formatPercent(value: Double): String =
    "${(value * 100).coerceAtLeast(0.0).let { "%.1f".format(it) }}%"

private fun String.firstIconText(): String = trim().firstOrNull()?.toString().orEmpty()

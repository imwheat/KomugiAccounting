package com.komugi.komugiaccounting.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.komugi.komugiaccounting.data.model.RecordType
import com.komugi.komugiaccounting.ui.components.RecordItem
import com.komugi.komugiaccounting.ui.components.StatCard
import com.komugi.komugiaccounting.data.repository.AppDataRepository
import com.komugi.komugiaccounting.ui.calendar.CalendarScreen
import com.komugi.komugiaccounting.ui.chart.ChartScreen
import com.komugi.komugiaccounting.ui.detail.DetailScreen
import com.komugi.komugiaccounting.ui.detail.DetailFilterRequest
import com.komugi.komugiaccounting.ui.detail.DetailViewModel
import com.komugi.komugiaccounting.util.AmountUtil
import com.komugi.komugiaccounting.util.DateTimeUtil

@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    detailViewModel: DetailViewModel,
    repository: AppDataRepository,
    onEditRecord: (String, Int?) -> Unit,
    onBottomBarVisibleChange: (Boolean) -> Unit,
    initialPage: Int? = null,
    onInitialPageConsumed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    var detailFilterRequest by remember { mutableStateOf<DetailFilterRequest?>(null) }
    var detailBackSignal by remember { mutableIntStateOf(0) }

    LaunchedEffect(page) {
        onBottomBarVisibleChange(page == 0)
        if (page == 0) detailBackSignal = 0
    }

    LaunchedEffect(initialPage) {
        initialPage?.let {
            page = it
            onInitialPageConsumed()
        }
    }

    BackHandler(enabled = page != 0) {
        if (page == 1) detailBackSignal += 1 else page = 0
    }

    Column(modifier = modifier) {
        when (page) {
            0 -> HomeOverviewScreen(
                viewModel = homeViewModel,
                onEditRecord = onEditRecord,
                onOpenPage = { targetPage ->
                    if (targetPage == 1) detailFilterRequest = null
                    if (targetPage == 1) detailBackSignal = 0
                    page = targetPage
                },
                onOpenDetail = { request ->
                    detailFilterRequest = request
                    detailBackSignal = 0
                    page = 1
                }
            )
            1 -> DetailScreen(
                viewModel = detailViewModel,
                onEditRecord = { recordId -> onEditRecord(recordId, 1) },
                filterRequest = detailFilterRequest,
                onBack = { page = 0 },
                backSignal = detailBackSignal,
                onBackSignalConsumed = { consumed ->
                    if (!consumed) page = 0
                },
                modifier = Modifier
            )
            2 -> ChartScreen(
                repository = repository,
                onBack = { page = 0 },
                modifier = Modifier.swipeRightToBack { page = 0 }
            )
            3 -> CalendarScreen(
                repository = repository,
                onEditRecord = { recordId -> onEditRecord(recordId, 3) },
                onBack = { page = 0 },
                modifier = Modifier.swipeRightToBack { page = 0 }
            )
        }
    }
}

@Composable
private fun HomeOverviewScreen(
    viewModel: HomeViewModel,
    onEditRecord: (String, Int?) -> Unit,
    onOpenPage: (Int) -> Unit,
    onOpenDetail: (DetailFilterRequest) -> Unit,
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
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = false, onClick = { onOpenPage(1) }, label = { Text("明细") })
                FilterChip(selected = false, onClick = { onOpenPage(2) }, label = { Text("图表") })
                FilterChip(selected = false, onClick = { onOpenPage(3) }, label = { Text("日历") })
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = monthOffset == 0, onClick = { monthOffset = 0 }, label = { Text("本月") })
                FilterChip(selected = monthOffset == -1, onClick = { monthOffset = -1 }, label = { Text("上月") })
            }
        }
        item {
            StatCard(
                title = viewModel.monthTitle(monthOffset),
                stat = viewModel.monthStat(data, monthOffset),
                onExpenseClick = { onOpenDetail(monthStatFilter(monthOffset, RecordType.EXPENSE)) },
                onIncomeClick = { onOpenDetail(monthStatFilter(monthOffset, RecordType.INCOME)) },
                onBalanceClick = { onOpenDetail(monthStatFilter(monthOffset, null)) }
            )
        }
        item {
            MonthlyCategorySummaryCard(
                title = "本月支出统计",
                recordCount = expenseSummary.recordCount,
                totalLabel = "总支出",
                totalAmount = expenseSummary.totalExpense,
                totalAmountColor = Color(0xFFB3542E),
                categories = expenseSummary.categories.map {
                    CategorySummaryItem(it.categoryId, it.categoryName, it.amount, it.percent)
                },
                expanded = expenseExpanded,
                onExpandedChange = { expenseExpanded = it },
                onCategoryClick = { categoryId ->
                    onOpenDetail(monthCategoryFilter(monthOffset, RecordType.EXPENSE, categoryId))
                }
            )
        }
        item {
            MonthlyCategorySummaryCard(
                title = "本月收入统计",
                recordCount = incomeSummary.recordCount,
                totalLabel = "总收入",
                totalAmount = incomeSummary.totalIncome,
                totalAmountColor = Color(0xFF1F7A4D),
                categories = incomeSummary.categories.map {
                    CategorySummaryItem(it.categoryId, it.categoryName, it.amount, it.percent)
                },
                expanded = incomeExpanded,
                onExpandedChange = { incomeExpanded = it },
                onCategoryClick = { categoryId ->
                    onOpenDetail(monthCategoryFilter(monthOffset, RecordType.INCOME, categoryId))
                }
            )
        }
        item {
            StatCard(
                title = "今日统计",
                stat = viewModel.todayStat(data),
                onExpenseClick = { onOpenDetail(dayStatFilter(RecordType.EXPENSE)) },
                onIncomeClick = { onOpenDetail(dayStatFilter(RecordType.INCOME)) },
                onBalanceClick = { onOpenDetail(dayStatFilter(null)) }
            )
        }
        item {
            StatCard(
                title = "本周统计",
                stat = viewModel.weekStat(data),
                onExpenseClick = { onOpenDetail(weekStatFilter(RecordType.EXPENSE)) },
                onIncomeClick = { onOpenDetail(weekStatFilter(RecordType.INCOME)) },
                onBalanceClick = { onOpenDetail(weekStatFilter(null)) }
            )
        }
        item {
            StatCard(
                title = "本年统计",
                stat = viewModel.yearStat(data),
                onExpenseClick = { onOpenDetail(yearStatFilter(RecordType.EXPENSE)) },
                onIncomeClick = { onOpenDetail(yearStatFilter(RecordType.INCOME)) },
                onBalanceClick = { onOpenDetail(yearStatFilter(null)) }
            )
        }
        item { Text("最近记录", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        if (data.records.isEmpty()) {
            item { Text("还没有账目，点击底部 + 记第一笔。", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(12.dp)) }
        } else {
            items(data.records.take(8), key = { it.id }) { record ->
                RecordItem(
                    record = record,
                    category = categories[record.categoryId],
                    member = members[record.memberId],
                    onClick = { onEditRecord(record.id, 0) }
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
    totalAmountColor: Color,
    categories: List<CategorySummaryItem>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onCategoryClick: (String) -> Unit,
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(totalLabel, fontWeight = FontWeight.Black, fontSize = 20.sp)
                Text(AmountUtil.format(totalAmount), color = totalAmountColor, fontWeight = FontWeight.Black, fontSize = 20.sp)
            }
            if (categories.isEmpty()) {
                Text("这个月份还没有记录。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                visibleCategories.forEach { item ->
                    CategorySummaryRow(item = item, onClick = { onCategoryClick(item.categoryId) })
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
private fun CategorySummaryRow(item: CategorySummaryItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(item.categoryName, fontWeight = FontWeight.SemiBold)
        Text(
            "${AmountUtil.format(item.amount)} · ${formatPercent(item.percent)}",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private data class CategorySummaryItem(
    val categoryId: String,
    val categoryName: String,
    val amount: Long,
    val percent: Double
)

private fun formatPercent(value: Double): String =
    "${(value * 100).coerceAtLeast(0.0).let { "%.1f".format(it) }}%"

private fun monthCategoryFilter(monthOffset: Int, type: RecordType, categoryId: String): DetailFilterRequest {
    val start = DateTimeUtil.monthOffsetStart(monthOffset)
    val endInclusive = DateTimeUtil.endExclusiveFromStart(start, java.util.Calendar.MONTH, 1) - 1
    return DetailFilterRequest(
        type = type,
        categoryId = categoryId,
        startDate = DateTimeUtil.formatDate(start),
        endDate = DateTimeUtil.formatDate(endInclusive)
    )
}

private fun monthStatFilter(monthOffset: Int, type: RecordType?): DetailFilterRequest {
    val start = DateTimeUtil.monthOffsetStart(monthOffset)
    val endExclusive = DateTimeUtil.endExclusiveFromStart(start, java.util.Calendar.MONTH, 1)
    return dateRangeFilter(start, endExclusive, type)
}

private fun dayStatFilter(type: RecordType?): DetailFilterRequest {
    val start = DateTimeUtil.startOfDay()
    val endExclusive = DateTimeUtil.endExclusiveFromStart(start, java.util.Calendar.DAY_OF_YEAR, 1)
    return dateRangeFilter(start, endExclusive, type)
}

private fun weekStatFilter(type: RecordType?): DetailFilterRequest {
    val start = DateTimeUtil.startOfWeek()
    val endExclusive = DateTimeUtil.endExclusiveFromStart(start, java.util.Calendar.DAY_OF_YEAR, 7)
    return dateRangeFilter(start, endExclusive, type)
}

private fun yearStatFilter(type: RecordType?): DetailFilterRequest {
    val start = DateTimeUtil.startOfYear()
    val endExclusive = DateTimeUtil.endExclusiveFromStart(start, java.util.Calendar.YEAR, 1)
    return dateRangeFilter(start, endExclusive, type)
}

private fun dateRangeFilter(start: Long, endExclusive: Long, type: RecordType?): DetailFilterRequest =
    DetailFilterRequest(
        type = type,
        startDate = DateTimeUtil.formatDate(start),
        endDate = DateTimeUtil.formatDate(endExclusive - 1)
    )

private fun Modifier.swipeRightToBack(onBack: () -> Unit): Modifier = pointerInput(Unit) {
    var totalDrag = 0f
    detectHorizontalDragGestures(
        onDragStart = { totalDrag = 0f },
        onHorizontalDrag = { _, dragAmount -> totalDrag += dragAmount },
        onDragEnd = {
            if (totalDrag > 90f) onBack()
        }
    )
}

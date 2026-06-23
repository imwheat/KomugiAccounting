package com.komugi.komugiaccounting.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.komugi.komugiaccounting.data.model.Category
import com.komugi.komugiaccounting.data.model.FilterParams
import com.komugi.komugiaccounting.data.model.Member
import com.komugi.komugiaccounting.data.model.RecordType
import com.komugi.komugiaccounting.data.model.SortMode
import com.komugi.komugiaccounting.data.model.TransactionRecord
import com.komugi.komugiaccounting.domain.FilterEngine
import com.komugi.komugiaccounting.domain.StatisticsCalculator
import com.komugi.komugiaccounting.ui.components.RecordItem
import com.komugi.komugiaccounting.util.AmountUtil
import com.komugi.komugiaccounting.util.DateTimeUtil
import java.util.Calendar

data class DetailFilterRequest(
    val type: RecordType?,
    val categoryId: String? = null,
    val startDate: String,
    val endDate: String
)

private enum class DetailSubPage {
    MAIN,
    FILTER,
    SORT,
    TIME,
    TYPE,
    CATEGORY,
    MEMBER,
    AMOUNT,
    REMARK,
    REFUND
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    onEditRecord: (String) -> Unit,
    filterRequest: DetailFilterRequest? = null,
    onBack: () -> Unit,
    backSignal: Int = 0,
    onBackSignalConsumed: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val data by viewModel.data.collectAsState()
    val categories = data.categories.associateBy { it.id }
    val members = data.members.associateBy { it.id }
    var subPage by rememberSaveable { mutableStateOf(DetailSubPage.MAIN) }
    var menuExpanded by rememberSaveable { mutableStateOf(false) }
    var searchVisible by rememberSaveable { mutableStateOf(false) }
    var selectedType by rememberSaveable { mutableStateOf<RecordType?>(null) }
    var selectedCategoryIds by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var selectedMemberIds by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var keyword by rememberSaveable { mutableStateOf("") }
    var remarkKeyword by rememberSaveable { mutableStateOf("") }
    var minAmount by rememberSaveable { mutableStateOf("") }
    var maxAmount by rememberSaveable { mutableStateOf("") }
    var startDate by rememberSaveable { mutableStateOf("") }
    var endDate by rememberSaveable { mutableStateOf("") }
    var sortMode by rememberSaveable { mutableStateOf(SortMode.TIME_DESC) }
    var includeRefunded by rememberSaveable { mutableStateOf(true) }

    fun goBackOneLevel(): Boolean {
        return if (subPage == DetailSubPage.MAIN) {
            false
        } else {
            subPage = if (subPage == DetailSubPage.FILTER || subPage == DetailSubPage.SORT) {
                DetailSubPage.MAIN
            } else {
                DetailSubPage.FILTER
            }
            true
        }
    }

    LaunchedEffect(backSignal) {
        if (backSignal > 0) onBackSignalConsumed(goBackOneLevel())
    }

    LaunchedEffect(filterRequest) {
        filterRequest?.let { request ->
            selectedType = request.type
            selectedCategoryIds = request.categoryId?.let { setOf(it) } ?: emptySet()
            selectedMemberIds = emptySet()
            keyword = ""
            remarkKeyword = ""
            minAmount = ""
            maxAmount = ""
            startDate = request.startDate
            endDate = request.endDate
            sortMode = SortMode.TIME_DESC
            includeRefunded = true
            subPage = DetailSubPage.MAIN
        }
    }

    val mergedKeyword = listOf(keyword, remarkKeyword).filter(String::isNotBlank).joinToString(" ")
    val minAmountCents = minAmount.takeIf { it.isNotBlank() }?.let { AmountUtil.parseToCents(it) }
    val maxAmountCents = maxAmount.takeIf { it.isNotBlank() }?.let { AmountUtil.parseToCents(it) }
    val startTime = startDate.takeIf { it.isNotBlank() }?.let { DateTimeUtil.parseDate(it) }
    val endTime = endDate.takeIf { it.isNotBlank() }?.let { DateTimeUtil.parseDate(it) }
        ?.let { DateTimeUtil.endExclusiveFromStart(it, Calendar.DAY_OF_MONTH, 1) }
    val filterError = when {
        minAmount.isNotBlank() && minAmountCents == null -> "最低金额格式不正确"
        maxAmount.isNotBlank() && maxAmountCents == null -> "最高金额格式不正确"
        startDate.isNotBlank() && startTime == null -> "开始日期格式应为 yyyy-MM-dd"
        endDate.isNotBlank() && endTime == null -> "结束日期格式应为 yyyy-MM-dd"
        else -> null
    }
    val filterParams = FilterParams(
        type = selectedType,
        categoryIds = selectedCategoryIds,
        memberIds = selectedMemberIds,
        minAmount = minAmountCents,
        maxAmount = maxAmountCents,
        startTime = startTime,
        endTime = endTime,
        keyword = mergedKeyword,
        sortMode = sortMode,
        includeRefunded = includeRefunded
    )
    val filteredRecords = if (filterError == null) {
        FilterEngine.apply(
            records = data.records,
            params = filterParams,
            categoryNames = categories.mapValues { it.value.name },
            memberNames = members.mapValues { it.value.name }
        )
    } else {
        emptyList()
    }
    val pageModifier = modifier.swipeRight {
        if (!goBackOneLevel()) onBack()
    }

    when (subPage) {
        DetailSubPage.MAIN -> DetailMainPage(
            filteredRecords = filteredRecords,
            categories = categories,
            members = members,
            searchVisible = searchVisible,
            keyword = keyword,
            filterError = filterError,
            menuExpanded = menuExpanded,
            onKeywordChange = { keyword = it },
            onToggleSearch = { searchVisible = !searchVisible },
            onMenuExpandedChange = { menuExpanded = it },
            onBack = onBack,
            onEditRecord = onEditRecord,
            onOpenFilter = {
                menuExpanded = false
                subPage = DetailSubPage.FILTER
            },
            onOpenSort = {
                menuExpanded = false
                subPage = DetailSubPage.SORT
            },
            modifier = pageModifier
        )
        DetailSubPage.FILTER -> FilterListPage(
            selectedType = selectedType,
            selectedCategoryText = categorySummary(selectedCategoryIds, data.categories),
            selectedMemberText = memberSummary(selectedMemberIds, data.members),
            timeText = timeSummary(startDate, endDate),
            amountText = amountSummary(minAmount, maxAmount),
            remarkKeyword = remarkKeyword,
            includeRefunded = includeRefunded,
            onBack = { subPage = DetailSubPage.MAIN },
            onOpen = { subPage = it },
            onClear = {
                selectedType = null
                selectedCategoryIds = emptySet()
                selectedMemberIds = emptySet()
                remarkKeyword = ""
                minAmount = ""
                maxAmount = ""
                startDate = ""
                endDate = ""
                includeRefunded = true
            },
            modifier = pageModifier
        )
        DetailSubPage.SORT -> ChoicePage(
            title = "排序",
            onBack = { subPage = DetailSubPage.MAIN },
            options = listOf(
                "时间降序" to { sortMode = SortMode.TIME_DESC },
                "时间升序" to { sortMode = SortMode.TIME_ASC },
                "金额降序" to { sortMode = SortMode.AMOUNT_DESC },
                "金额升序" to { sortMode = SortMode.AMOUNT_ASC }
            ),
            selectedLabel = sortMode.label(),
            modifier = pageModifier
        )
        DetailSubPage.TIME -> TimeFilterPage(startDate, endDate, { startDate = it }, { endDate = it }, { subPage = DetailSubPage.FILTER }, pageModifier)
        DetailSubPage.TYPE -> ChoicePage(
            title = "流水类型",
            onBack = { subPage = DetailSubPage.FILTER },
            options = listOf(
                "全部" to {
                    selectedType = null
                    selectedCategoryIds = emptySet()
                },
                "支出" to {
                    selectedType = RecordType.EXPENSE
                    selectedCategoryIds = emptySet()
                },
                "收入" to {
                    selectedType = RecordType.INCOME
                    selectedCategoryIds = emptySet()
                }
            ),
            selectedLabel = selectedType?.label() ?: "全部",
            modifier = pageModifier
        )
        DetailSubPage.CATEGORY -> CategoryFilterPage(data.categories.filter { !it.name.startsWith("__group__") && (selectedType == null || it.type == selectedType) }, selectedCategoryIds, { selectedCategoryIds = selectedCategoryIds.toggle(it) }, { subPage = DetailSubPage.FILTER }, pageModifier)
        DetailSubPage.MEMBER -> MemberFilterPage(data.members, selectedMemberIds, { selectedMemberIds = selectedMemberIds.toggle(it) }, { subPage = DetailSubPage.FILTER }, pageModifier)
        DetailSubPage.AMOUNT -> AmountFilterPage(minAmount, maxAmount, { minAmount = it }, { maxAmount = it }, { subPage = DetailSubPage.FILTER }, pageModifier)
        DetailSubPage.REMARK -> TextFilterPage("备注", remarkKeyword, { remarkKeyword = it }, { subPage = DetailSubPage.FILTER }, pageModifier)
        DetailSubPage.REFUND -> ChoicePage(
            title = "退款",
            onBack = { subPage = DetailSubPage.FILTER },
            options = listOf(
                "显示已退款" to { includeRefunded = true },
                "隐藏已退款" to { includeRefunded = false }
            ),
            selectedLabel = if (includeRefunded) "显示已退款" else "隐藏已退款",
            modifier = pageModifier
        )
    }
}

@Composable
private fun DetailMainPage(
    filteredRecords: List<TransactionRecord>,
    categories: Map<String, Category>,
    members: Map<String, Member>,
    searchVisible: Boolean,
    keyword: String,
    filterError: String?,
    menuExpanded: Boolean,
    onKeywordChange: (String) -> Unit,
    onToggleSearch: () -> Unit,
    onMenuExpandedChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onEditRecord: (String) -> Unit,
    onOpenFilter: () -> Unit,
    onOpenSort: () -> Unit,
    modifier: Modifier = Modifier
) {
    val groupedRecords = filteredRecords.groupBy { DateTimeUtil.startOfDay(it.dateTime) }.toList().sortedByDescending { it.first }
    LazyColumn(modifier = modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Spacer(Modifier.height(10.dp)) }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onBack) { Text("<") }
                    Text("账目明细", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onToggleSearch) { Text("搜索") }
                    Column {
                        OutlinedButton(
                            onClick = { onMenuExpandedChange(true) },
                            modifier = Modifier.size(width = 52.dp, height = 40.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) { Text("...") }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { onMenuExpandedChange(false) }) {
                            DropdownMenuItem(text = { Text("批量编辑（后续补充）") }, onClick = { onMenuExpandedChange(false) })
                            DropdownMenuItem(text = { Text("筛选") }, onClick = onOpenFilter)
                            DropdownMenuItem(text = { Text("排序") }, onClick = onOpenSort)
                        }
                    }
                }
            }
        }
        if (searchVisible) {
            item {
                OutlinedTextField(
                    value = keyword,
                    onValueChange = onKeywordChange,
                    label = { Text("搜索分类、备注、成员") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        filterError?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        if (filteredRecords.isEmpty()) {
            item { Text("暂无符合条件的明细记录。", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(12.dp)) }
        } else {
            groupedRecords.forEach { (dayStart, records) ->
                item(key = "day-$dayStart") { DayHeader(dayStart, records) }
                items(records, key = { it.id }) { record ->
                    RecordItem(record, categories[record.categoryId], members[record.memberId], onClick = { onEditRecord(record.id) })
                }
            }
        }
        item { Spacer(Modifier.height(88.dp)) }
    }
}

@Composable
private fun FilterListPage(
    selectedType: RecordType?,
    selectedCategoryText: String,
    selectedMemberText: String,
    timeText: String,
    amountText: String,
    remarkKeyword: String,
    includeRefunded: Boolean,
    onBack: () -> Unit,
    onOpen: (DetailSubPage) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Header("筛选", onBack) }
        item { FilterRow("时间", timeText, onClick = { onOpen(DetailSubPage.TIME) }) }
        item { FilterRow("分类", selectedCategoryText, onClick = { onOpen(DetailSubPage.CATEGORY) }) }
        item { FilterRow("流水类型", selectedType?.label() ?: "全部", onClick = { onOpen(DetailSubPage.TYPE) }) }
        item { FilterRow("退款", if (includeRefunded) "显示已退款" else "隐藏已退款", onClick = { onOpen(DetailSubPage.REFUND) }) }
        item { FilterRow("成员", selectedMemberText, onClick = { onOpen(DetailSubPage.MEMBER) }) }
        item { FilterRow("金额", amountText, onClick = { onOpen(DetailSubPage.AMOUNT) }) }
        item { FilterRow("备注", remarkKeyword.ifBlank { "不限" }, onClick = { onOpen(DetailSubPage.REMARK) }) }
        item { OutlinedButton(onClick = onClear, modifier = Modifier.fillMaxWidth()) { Text("清空筛选") } }
        item { Spacer(Modifier.height(88.dp)) }
    }
}

@Composable
private fun Header(title: String, onBack: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 10.dp)) {
        OutlinedButton(onClick = onBack) { Text("<") }
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun FilterRow(title: String, value: String, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(start = 18.dp))
        }
    }
}

@Composable
private fun ChoicePage(title: String, onBack: () -> Unit, options: List<Pair<String, () -> Unit>>, selectedLabel: String, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Header(title, onBack) }
        items(options, key = { it.first }) { (label, action) ->
            FilterChip(selected = label == selectedLabel, onClick = action, label = { Text(label) })
        }
    }
}

@Composable
private fun TimeFilterPage(startDate: String, endDate: String, onStartDateChange: (String) -> Unit, onEndDateChange: (String) -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Header("时间", onBack) }
        item { OutlinedButton(onClick = { setDateRange("month", onStartDateChange, onEndDateChange) }, modifier = Modifier.fillMaxWidth()) { Text("本月") } }
        item { OutlinedButton(onClick = { setDateRange("year", onStartDateChange, onEndDateChange) }, modifier = Modifier.fillMaxWidth()) { Text("今年") } }
        item { OutlinedButton(onClick = { setDateRange("lastYear", onStartDateChange, onEndDateChange) }, modifier = Modifier.fillMaxWidth()) { Text("去年") } }
        item { OutlinedButton(onClick = { onStartDateChange(""); onEndDateChange("") }, modifier = Modifier.fillMaxWidth()) { Text("任意") } }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = startDate, onValueChange = onStartDateChange, label = { Text("开始日期") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = endDate, onValueChange = onEndDateChange, label = { Text("结束日期") }, singleLine = true, modifier = Modifier.weight(1f))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryFilterPage(categories: List<Category>, selectedIds: Set<String>, onToggle: (String) -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Header("分类", onBack) }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.sortedBy { it.sortOrder }.forEach { category ->
                    FilterChip(selected = category.id in selectedIds, onClick = { onToggle(category.id) }, label = { Text(category.name) })
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MemberFilterPage(members: List<Member>, selectedIds: Set<String>, onToggle: (String) -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Header("成员", onBack) }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                members.forEach { member ->
                    FilterChip(selected = member.id in selectedIds, onClick = { onToggle(member.id) }, label = { Text(member.name) })
                }
            }
        }
    }
}

@Composable
private fun AmountFilterPage(minAmount: String, maxAmount: String, onMinAmountChange: (String) -> Unit, onMaxAmountChange: (String) -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Header("金额", onBack) }
        item { OutlinedTextField(value = minAmount, onValueChange = onMinAmountChange, label = { Text("最低金额") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(value = maxAmount, onValueChange = onMaxAmountChange, label = { Text("最高金额") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth()) }
    }
}

@Composable
private fun TextFilterPage(title: String, value: String, onValueChange: (String) -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Header(title, onBack) }
        item { OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text("关键词") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
    }
}

private fun Set<String>.toggle(value: String): Set<String> = if (value in this) this - value else this + value
private fun RecordType.label(): String = if (this == RecordType.EXPENSE) "支出" else "收入"
private fun SortMode.label(): String = when (this) {
    SortMode.TIME_DESC -> "时间降序"
    SortMode.TIME_ASC -> "时间升序"
    SortMode.AMOUNT_DESC -> "金额降序"
    SortMode.AMOUNT_ASC -> "金额升序"
}

private fun categorySummary(selectedIds: Set<String>, categories: List<Category>): String {
    if (selectedIds.isEmpty()) return "全部"
    val selected = categories.filter { it.id in selectedIds }
    val groupNames = selected.groupBy { it.groupName }.filter { (_, list) ->
        categories.filter { it.groupName == list.first().groupName }.all { it.id in selectedIds }
    }.keys.filter { it.isNotBlank() }
    return if (groupNames.isNotEmpty()) groupNames.joinToString("、") else selected.joinToString("、") { it.name }
}

private fun memberSummary(selectedIds: Set<String>, members: List<Member>): String =
    if (selectedIds.isEmpty()) "全部" else members.filter { it.id in selectedIds }.joinToString("、") { it.name }

private fun timeSummary(startDate: String, endDate: String): String = when {
    startDate.isBlank() && endDate.isBlank() -> "任意"
    startDate.isNotBlank() && endDate.isNotBlank() -> "$startDate 至 $endDate"
    startDate.isNotBlank() -> "$startDate 后"
    else -> "$endDate 前"
}

private fun amountSummary(minAmount: String, maxAmount: String): String = when {
    minAmount.isBlank() && maxAmount.isBlank() -> "不限"
    minAmount.isNotBlank() && maxAmount.isNotBlank() -> "$minAmount 至 $maxAmount"
    minAmount.isNotBlank() -> ">= $minAmount"
    else -> "<= $maxAmount"
}

private fun setDateRange(mode: String, onStartDateChange: (String) -> Unit, onEndDateChange: (String) -> Unit) {
    val start = when (mode) {
        "month" -> DateTimeUtil.startOfMonth()
        "year" -> DateTimeUtil.startOfYear()
        else -> Calendar.getInstance().apply {
            set(Calendar.YEAR, DateTimeUtil.year() - 1)
            set(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val endExclusive = if (mode == "month") DateTimeUtil.endExclusiveFromStart(start, Calendar.MONTH, 1) else DateTimeUtil.endExclusiveFromStart(start, Calendar.YEAR, 1)
    onStartDateChange(DateTimeUtil.formatDate(start))
    onEndDateChange(DateTimeUtil.formatDate(endExclusive - 1))
}

private fun Modifier.swipeRight(onBack: () -> Unit): Modifier = pointerInput(Unit) {
    var totalDrag = 0f
    detectHorizontalDragGestures(
        onDragStart = { totalDrag = 0f },
        onHorizontalDrag = { _, dragAmount -> totalDrag += dragAmount },
        onDragEnd = { if (totalDrag > 90f) onBack() }
    )
}

@Composable
private fun DayHeader(dayStart: Long, records: List<TransactionRecord>) {
    val stat = StatisticsCalculator.calculate(records, dayStart, DateTimeUtil.endExclusiveFromStart(dayStart, Calendar.DAY_OF_MONTH, 1))
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(DateTimeUtil.formatDate(dayStart), fontWeight = FontWeight.Bold)
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("收 ${AmountUtil.format(stat.income)}", color = Color(0xFF1F7A4D), fontWeight = FontWeight.SemiBold)
                    Text("支 ${AmountUtil.format(stat.expense)}", color = Color(0xFFB3542E), fontWeight = FontWeight.SemiBold)
                }
                Text("余 ${AmountUtil.format(stat.balance)}", color = Color(0xFF2E5A87), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

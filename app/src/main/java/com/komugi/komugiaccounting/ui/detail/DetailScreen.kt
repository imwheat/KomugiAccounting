package com.komugi.komugiaccounting.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import com.komugi.komugiaccounting.ui.components.categoryGroupOrder
import com.komugi.komugiaccounting.util.AmountUtil
import com.komugi.komugiaccounting.util.DateTimeUtil
import java.util.Calendar

data class DetailFilterRequest(
    val type: RecordType,
    val categoryId: String,
    val startDate: String,
    val endDate: String
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    onEditRecord: (String) -> Unit,
    filterRequest: DetailFilterRequest? = null,
    modifier: Modifier = Modifier
) {
    val data by viewModel.data.collectAsState()
    val categories = data.categories.associateBy { it.id }
    val members = data.members.associateBy { it.id }
    var selectedType by rememberSaveable { mutableStateOf<RecordType?>(null) }
    var selectedCategoryIds by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var selectedMemberIds by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var keyword by rememberSaveable { mutableStateOf("") }
    var minAmount by rememberSaveable { mutableStateOf("") }
    var maxAmount by rememberSaveable { mutableStateOf("") }
    var startDate by rememberSaveable { mutableStateOf("") }
    var endDate by rememberSaveable { mutableStateOf("") }
    var sortMode by rememberSaveable { mutableStateOf(SortMode.TIME_DESC) }

    LaunchedEffect(filterRequest) {
        filterRequest?.let { request ->
            selectedType = request.type
            selectedCategoryIds = setOf(request.categoryId)
            selectedMemberIds = emptySet()
            keyword = ""
            minAmount = ""
            maxAmount = ""
            startDate = request.startDate
            endDate = request.endDate
            sortMode = SortMode.TIME_DESC
        }
    }

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

    val filteredRecords = if (filterError == null) {
        FilterEngine.apply(
            data.records,
            FilterParams(
                type = selectedType,
                categoryIds = selectedCategoryIds,
                memberIds = selectedMemberIds,
                minAmount = minAmountCents,
                maxAmount = maxAmountCents,
                startTime = startTime,
                endTime = endTime,
                keyword = keyword,
                sortMode = sortMode
            )
        )
    } else {
        emptyList()
    }
    val groupedRecords = filteredRecords.groupBy { DateTimeUtil.startOfDay(it.dateTime) }
        .toList()
        .let { groups ->
            when (sortMode) {
                SortMode.TIME_ASC -> groups.sortedBy { it.first }
                else -> groups.sortedByDescending { it.first }
            }
        }

    LazyColumn(
        modifier = modifier.padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(Modifier.height(10.dp)) }
        item { Text("账目明细", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black) }
        item {
            FilterPanel(
                selectedType = selectedType,
                onSelectedTypeChange = {
                    selectedType = it
                    selectedCategoryIds = emptySet()
                },
                categories = data.categories,
                selectedCategoryIds = selectedCategoryIds,
                onToggleCategory = { selectedCategoryIds = selectedCategoryIds.toggle(it) },
                members = data.members,
                selectedMemberIds = selectedMemberIds,
                onToggleMember = { selectedMemberIds = selectedMemberIds.toggle(it) },
                keyword = keyword,
                onKeywordChange = { keyword = it },
                minAmount = minAmount,
                onMinAmountChange = { minAmount = it },
                maxAmount = maxAmount,
                onMaxAmountChange = { maxAmount = it },
                startDate = startDate,
                onStartDateChange = { startDate = it },
                endDate = endDate,
                onEndDateChange = { endDate = it },
                sortMode = sortMode,
                onSortModeChange = { sortMode = it },
                filterError = filterError,
                onClear = {
                    selectedType = null
                    selectedCategoryIds = emptySet()
                    selectedMemberIds = emptySet()
                    keyword = ""
                    minAmount = ""
                    maxAmount = ""
                    startDate = ""
                    endDate = ""
                    sortMode = SortMode.TIME_DESC
                }
            )
        }
        if (filteredRecords.isEmpty()) {
            item { Text("暂无符合条件的明细记录。", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(12.dp)) }
        } else {
            groupedRecords.forEach { (dayStart, records) ->
                item(key = "day-$dayStart") {
                    DayHeader(dayStart = dayStart, records = records)
                }
                items(records, key = { it.id }) { record ->
                    RecordItem(
                        record = record,
                        category = categories[record.categoryId],
                        member = members[record.memberId],
                        onClick = { onEditRecord(record.id) }
                    )
                }
            }
        }
        item { Spacer(Modifier.height(88.dp)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterPanel(
    selectedType: RecordType?,
    onSelectedTypeChange: (RecordType?) -> Unit,
    categories: List<Category>,
    selectedCategoryIds: Set<String>,
    onToggleCategory: (String) -> Unit,
    members: List<Member>,
    selectedMemberIds: Set<String>,
    onToggleMember: (String) -> Unit,
    keyword: String,
    onKeywordChange: (String) -> Unit,
    minAmount: String,
    onMinAmountChange: (String) -> Unit,
    maxAmount: String,
    onMaxAmountChange: (String) -> Unit,
    startDate: String,
    onStartDateChange: (String) -> Unit,
    endDate: String,
    onEndDateChange: (String) -> Unit,
    sortMode: SortMode,
    onSortModeChange: (SortMode) -> Unit,
    filterError: String?,
    onClear: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = selectedType == null, onClick = { onSelectedTypeChange(null) }, label = { Text("全部") })
                FilterChip(selected = selectedType == RecordType.EXPENSE, onClick = { onSelectedTypeChange(RecordType.EXPENSE) }, label = { Text("支出") })
                FilterChip(selected = selectedType == RecordType.INCOME, onClick = { onSelectedTypeChange(RecordType.INCOME) }, label = { Text("收入") })
            }
            OutlinedTextField(
                value = keyword,
                onValueChange = onKeywordChange,
                label = { Text("搜索备注") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Text("分类", fontWeight = FontWeight.SemiBold)
            categories
                .filter { selectedType == null || it.type == selectedType }
                .groupBy { it.groupName.ifBlank { "未分组" } }
                .toSortedMap(compareBy { categoryGroupOrder(it) })
                .forEach { (groupName, groupCategories) ->
                    Text(groupName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        groupCategories.sortedBy { it.sortOrder }.forEach { category ->
                            FilterChip(
                                selected = category.id in selectedCategoryIds,
                                onClick = { onToggleCategory(category.id) },
                                label = { Text(category.name) }
                            )
                        }
                    }
                }
            Text("成员", fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                members.forEach { member ->
                    FilterChip(
                        selected = member.id in selectedMemberIds,
                        onClick = { onToggleMember(member.id) },
                        label = { Text(member.name) }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = minAmount,
                    onValueChange = onMinAmountChange,
                    label = { Text("最低金额") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = maxAmount,
                    onValueChange = onMaxAmountChange,
                    label = { Text("最高金额") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = startDate,
                    onValueChange = onStartDateChange,
                    label = { Text("开始日期") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = endDate,
                    onValueChange = onEndDateChange,
                    label = { Text("结束日期") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Text("排序", fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SortMode.entries.forEach { mode ->
                    FilterChip(
                        selected = sortMode == mode,
                        onClick = { onSortModeChange(mode) },
                        label = { Text(mode.label()) }
                    )
                }
            }
            filterError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            OutlinedButton(onClick = onClear) { Text("清空筛选") }
        }
    }
}

private fun Set<String>.toggle(value: String): Set<String> =
    if (value in this) this - value else this + value

private fun SortMode.label(): String = when (this) {
    SortMode.TIME_DESC -> "时间倒序"
    SortMode.TIME_ASC -> "时间正序"
    SortMode.AMOUNT_DESC -> "金额高到低"
    SortMode.AMOUNT_ASC -> "金额低到高"
}

@Composable
private fun DayHeader(dayStart: Long, records: List<TransactionRecord>) {
    val stat = StatisticsCalculator.calculate(records, dayStart, DateTimeUtil.endExclusiveFromStart(dayStart, Calendar.DAY_OF_MONTH, 1))
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(DateTimeUtil.formatDate(dayStart), fontWeight = FontWeight.Bold)
            Text(
                "收 ${AmountUtil.format(stat.income)}  支 ${AmountUtil.format(stat.expense)}  余 ${AmountUtil.format(stat.balance)}",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

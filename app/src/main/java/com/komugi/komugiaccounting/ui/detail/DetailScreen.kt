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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.komugi.komugiaccounting.data.model.FilterParams
import com.komugi.komugiaccounting.data.model.RecordType
import com.komugi.komugiaccounting.data.model.SortMode
import com.komugi.komugiaccounting.data.model.TransactionRecord
import com.komugi.komugiaccounting.domain.FilterEngine
import com.komugi.komugiaccounting.domain.StatisticsCalculator
import com.komugi.komugiaccounting.ui.components.RecordItem
import com.komugi.komugiaccounting.util.AmountUtil
import com.komugi.komugiaccounting.util.DateTimeUtil
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    onEditRecord: (String) -> Unit,
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
    var pendingDeleteId by rememberSaveable { mutableStateOf<String?>(null) }
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
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = selectedType == null, onClick = { selectedType = null }, label = { Text("全部") })
                        FilterChip(selected = selectedType == RecordType.EXPENSE, onClick = { selectedType = RecordType.EXPENSE }, label = { Text("支出") })
                        FilterChip(selected = selectedType == RecordType.INCOME, onClick = { selectedType = RecordType.INCOME }, label = { Text("收入") })
                    }
                    OutlinedTextField(
                        value = keyword,
                        onValueChange = { keyword = it },
                        label = { Text("搜索备注") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("分类", fontWeight = FontWeight.SemiBold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        data.categories.filter { selectedType == null || it.type == selectedType }.sortedBy { it.sortOrder }.forEach { category ->
                            FilterChip(
                                selected = category.id in selectedCategoryIds,
                                onClick = { selectedCategoryIds = selectedCategoryIds.toggle(category.id) },
                                label = { Text(category.name) }
                            )
                        }
                    }
                    Text("成员", fontWeight = FontWeight.SemiBold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        data.members.forEach { member ->
                            FilterChip(
                                selected = member.id in selectedMemberIds,
                                onClick = { selectedMemberIds = selectedMemberIds.toggle(member.id) },
                                label = { Text(member.name) }
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = minAmount,
                            onValueChange = { minAmount = it },
                            label = { Text("最低金额") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = maxAmount,
                            onValueChange = { maxAmount = it },
                            label = { Text("最高金额") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = startDate,
                            onValueChange = { startDate = it },
                            label = { Text("开始日期") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = endDate,
                            onValueChange = { endDate = it },
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
                                onClick = { sortMode = mode },
                                label = { Text(mode.label()) }
                            )
                        }
                    }
                    filterError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    OutlinedButton(
                        onClick = {
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
                    ) { Text("清空筛选") }
                }
            }
        }
        if (filteredRecords.isEmpty()) {
            item { Text("暂无符合条件的明细记录。", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(12.dp)) }
        } else {
            groupedRecords.forEach { (dayStart, records) ->
                item(key = "day-$dayStart") {
                    DayHeader(dayStart = dayStart, records = records)
                }
                items(records, key = { it.id }) { record ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        RecordItem(record, categories[record.categoryId], members[record.memberId])
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { onEditRecord(record.id) }) {
                                Text("编辑")
                            }
                            OutlinedButton(onClick = {
                                if (pendingDeleteId == record.id) {
                                    viewModel.deleteRecord(record.id)
                                    pendingDeleteId = null
                                } else {
                                    pendingDeleteId = record.id
                                }
                            }) {
                                Text(if (pendingDeleteId == record.id) "确认删除" else "删除")
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(88.dp)) }
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
            modifier = Modifier.fillMaxWidth().padding(12.dp),
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

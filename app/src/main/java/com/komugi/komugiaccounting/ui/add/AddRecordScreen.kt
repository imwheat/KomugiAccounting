package com.komugi.komugiaccounting.ui.add

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.komugi.komugiaccounting.data.model.Category
import com.komugi.komugiaccounting.data.model.Member
import com.komugi.komugiaccounting.data.model.RecordType
import com.komugi.komugiaccounting.data.model.Template
import com.komugi.komugiaccounting.ui.components.DateTimePickerDialog
import com.komugi.komugiaccounting.ui.components.NumberKeyboard
import com.komugi.komugiaccounting.util.AmountUtil
import com.komugi.komugiaccounting.util.DateTimeUtil

private enum class AddTopTab {
    TEMPLATE,
    EXPENSE,
    INCOME
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddRecordScreen(
    viewModel: AddRecordViewModel,
    onSaved: (String) -> Unit,
    onBack: () -> Unit,
    recordId: String? = null,
    modifier: Modifier = Modifier
) {
    val data by viewModel.data.collectAsState()
    var topTab by rememberSaveable { mutableStateOf(AddTopTab.EXPENSE) }
    var type by rememberSaveable { mutableStateOf(RecordType.EXPENSE) }
    var amount by rememberSaveable { mutableStateOf("") }
    var dateTime by rememberSaveable { mutableStateOf(DateTimeUtil.formatDateTime(DateTimeUtil.now())) }
    var remark by rememberSaveable { mutableStateOf("") }
    var selectedCategoryId by rememberSaveable { mutableStateOf("") }
    var selectedMemberId by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    var loadedRecordId by rememberSaveable { mutableStateOf<String?>(null) }
    var showDateTimePicker by rememberSaveable { mutableStateOf(false) }
    var showNumberKeyboard by rememberSaveable { mutableStateOf(false) }
    var showCategoryPicker by rememberSaveable { mutableStateOf(false) }

    val typeCategories = data.categories.filter { it.enabled && it.type == type }.sortedBy { it.sortOrder }
    val selectedCategory = typeCategories.firstOrNull { it.id == selectedCategoryId }
    val members = data.members.filter { it.enabled }

    BackHandler {
        if (showCategoryPicker) {
            showCategoryPicker = false
        } else {
            onBack()
        }
    }

    LaunchedEffect(recordId, data.records) {
        if (recordId != loadedRecordId) {
            val record = viewModel.record(recordId)
            loadedRecordId = recordId
            if (record == null) {
                type = RecordType.EXPENSE
                topTab = AddTopTab.EXPENSE
                amount = ""
                dateTime = DateTimeUtil.formatDateTime(DateTimeUtil.now())
                remark = ""
                selectedCategoryId = ""
                selectedMemberId = ""
            } else {
                type = record.type
                topTab = if (record.type == RecordType.EXPENSE) AddTopTab.EXPENSE else AddTopTab.INCOME
                amount = AmountUtil.formatPlain(record.amount)
                dateTime = DateTimeUtil.formatDateTime(record.dateTime)
                remark = record.remark
                selectedCategoryId = record.categoryId
                selectedMemberId = record.memberId
            }
            error = null
            message = null
        }
    }

    LaunchedEffect(type, data.categories, data.settings, selectedCategoryId) {
        if (typeCategories.none { it.id == selectedCategoryId }) {
            val preferred = if (type == RecordType.EXPENSE) data.settings.lastExpenseCategoryId else data.settings.lastIncomeCategoryId
            selectedCategoryId = preferred?.takeIf { id -> typeCategories.any { it.id == id } }
                ?: typeCategories.firstOrNull()?.id.orEmpty()
        }
    }
    LaunchedEffect(data.members, data.settings, selectedMemberId) {
        if (data.members.none { it.id == selectedMemberId && it.enabled }) {
            selectedMemberId = data.settings.lastMemberId?.takeIf { id -> data.members.any { it.id == id && it.enabled } }
                ?: data.members.firstOrNull { it.enabled }?.id.orEmpty()
        }
    }

    fun selectType(nextType: RecordType) {
        type = nextType
        topTab = if (nextType == RecordType.EXPENSE) AddTopTab.EXPENSE else AddTopTab.INCOME
        showCategoryPicker = false
        error = null
        message = null
    }

    fun applyTemplate(template: Template) {
        type = template.type
        topTab = if (template.type == RecordType.EXPENSE) AddTopTab.EXPENSE else AddTopTab.INCOME
        amount = template.amount?.let { AmountUtil.formatPlain(it) }.orEmpty()
        selectedCategoryId = template.categoryId
        selectedMemberId = template.memberId
        remark = template.remark
        showCategoryPicker = false
        showNumberKeyboard = false
        error = null
        message = null
    }

    LazyColumn(
        modifier = modifier.padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Spacer(Modifier.height(10.dp)) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onBack) { Text("返回") }
                Text(if (recordId == null) "记一笔" else "编辑记录", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = topTab == AddTopTab.TEMPLATE, onClick = { topTab = AddTopTab.TEMPLATE; showCategoryPicker = false }, label = { Text("模板") })
                FilterChip(selected = topTab == AddTopTab.EXPENSE, onClick = { selectType(RecordType.EXPENSE) }, label = { Text("支出") })
                FilterChip(selected = topTab == AddTopTab.INCOME, onClick = { selectType(RecordType.INCOME) }, label = { Text("收入") })
            }
        }

        when {
            topTab == AddTopTab.TEMPLATE -> templateListItems(
                templates = data.templates,
                categories = data.categories.associateBy { it.id },
                members = data.members.associateBy { it.id },
                onTemplateClick = ::applyTemplate
            )

            showCategoryPicker -> categoryPickerItems(
                categories = typeCategories,
                recentCategoryIds = data.settings.recentCategoryIds,
                onBack = { showCategoryPicker = false },
                onSelect = { category ->
                    selectedCategoryId = category.id
                    showCategoryPicker = false
                }
            )

            else -> recordFormItems(
                recordId = recordId,
                amount = amount,
                onAmountChange = { amount = it },
                showNumberKeyboard = showNumberKeyboard,
                onAmountFocusChange = { showNumberKeyboard = it },
                selectedCategory = selectedCategory,
                onOpenCategoryPicker = { showCategoryPicker = true },
                members = members,
                selectedMemberId = selectedMemberId,
                onMemberSelected = { selectedMemberId = it },
                dateTime = dateTime,
                onOpenDateTimePicker = { showDateTimePicker = true },
                remark = remark,
                onRemarkChange = { remark = it },
                message = message,
                error = error,
                onSaveTemplate = {
                    message = null
                    error = viewModel.saveTemplate(type, amount, selectedCategoryId, selectedMemberId, remark)
                    if (error == null) message = "已存为模板"
                },
                onSaveAndContinue = {
                    message = null
                    error = viewModel.saveRecord(type, amount, selectedCategoryId, selectedMemberId, dateTime, remark, null)
                    if (error == null) {
                        amount = ""
                        remark = ""
                        dateTime = DateTimeUtil.formatDateTime(DateTimeUtil.now())
                        message = "保存成功，可继续记下一笔"
                    }
                },
                onSave = {
                    message = null
                    error = viewModel.saveRecord(type, amount, selectedCategoryId, selectedMemberId, dateTime, remark, recordId)
                    if (error == null) {
                        amount = ""
                        remark = ""
                        dateTime = DateTimeUtil.formatDateTime(DateTimeUtil.now())
                        onSaved(if (recordId == null) "保存成功" else "修改已保存")
                    }
                }
            )
        }
        item { Spacer(Modifier.height(88.dp)) }
    }

    if (showDateTimePicker) {
        DateTimePickerDialog(
            initialValue = dateTime,
            onDismiss = { showDateTimePicker = false },
            onConfirm = {
                dateTime = it
                showDateTimePicker = false
            }
        )
    }
}

private fun LazyListScope.recordFormItems(
    recordId: String?,
    amount: String,
    onAmountChange: (String) -> Unit,
    showNumberKeyboard: Boolean,
    onAmountFocusChange: (Boolean) -> Unit,
    selectedCategory: Category?,
    onOpenCategoryPicker: () -> Unit,
    members: List<Member>,
    selectedMemberId: String,
    onMemberSelected: (String) -> Unit,
    dateTime: String,
    onOpenDateTimePicker: () -> Unit,
    remark: String,
    onRemarkChange: (String) -> Unit,
    message: String?,
    error: String?,
    onSaveTemplate: () -> Unit,
    onSaveAndContinue: () -> Unit,
    onSave: () -> Unit
) {
    item {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = onAmountChange,
                    label = { Text("金额") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { onAmountFocusChange(it.isFocused) }
                )
                if (showNumberKeyboard) {
                    NumberKeyboard(value = amount, onValueChange = onAmountChange)
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("分类：", fontWeight = FontWeight.SemiBold)
                    OutlinedButton(modifier = Modifier.weight(1f), onClick = onOpenCategoryPicker) {
                        Text(selectedCategory?.displayPath() ?: "请选择分类")
                    }
                }

                Text("成员", fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    members.forEach { member ->
                        FilterChip(
                            selected = selectedMemberId == member.id,
                            onClick = { onMemberSelected(member.id) },
                            label = { Text(member.name) }
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("时间：", fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = dateTime,
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { if (it.isFocused) onOpenDateTimePicker() }
                            .clickable(onClick = onOpenDateTimePicker)
                    )
                }

                OutlinedTextField(
                    value = remark,
                    onValueChange = onRemarkChange,
                    label = { Text("备注") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(modifier = Modifier.weight(1f), onClick = onSaveTemplate) { Text("存为模板") }
                    if (recordId == null) {
                        OutlinedButton(modifier = Modifier.weight(1f), onClick = onSaveAndContinue) { Text("保存并继续") }
                    }
                    Button(modifier = Modifier.weight(1f), onClick = onSave) { Text(if (recordId == null) "完成保存" else "保存修改") }
                }
            }
        }
    }
}

private fun LazyListScope.categoryPickerItems(
    categories: List<Category>,
    recentCategoryIds: List<String>,
    onBack: () -> Unit,
    onSelect: (Category) -> Unit
) {
    val categoryById = categories.associateBy { it.id }
    val recentCategories = recentCategoryIds.mapNotNull { categoryById[it] }.take(10)
    item {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("选择分类", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            OutlinedButton(onClick = onBack) { Text("返回") }
        }
    }
    if (recentCategories.isNotEmpty()) {
        item { Text("最近选择", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        items(recentCategories, key = { "recent-${it.id}" }) { category ->
            CategoryOption(category = category, onClick = { onSelect(category) })
        }
    }
    categories
        .groupBy { it.groupName.ifBlank { "未分组" } }
        .toSortedMap(compareBy { groupOrder(it) })
        .forEach { (group, groupCategories) ->
            item { Text(group, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            items(groupCategories.sortedBy { it.sortOrder }, key = { it.id }) { category ->
                CategoryOption(category = category, onClick = { onSelect(category) })
            }
        }
}

@Composable
private fun CategoryOption(category: Category, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Text(category.displayPath(), modifier = Modifier.padding(14.dp), fontWeight = FontWeight.SemiBold)
    }
}

private fun LazyListScope.templateListItems(
    templates: List<Template>,
    categories: Map<String, Category>,
    members: Map<String, Member>,
    onTemplateClick: (Template) -> Unit
) {
    templateSection("支出模板", templates.filter { it.type == RecordType.EXPENSE }.sortedBy { it.name }, categories, members, onTemplateClick)
    templateSection("收入模板", templates.filter { it.type == RecordType.INCOME }.sortedBy { it.name }, categories, members, onTemplateClick)
}

private fun LazyListScope.templateSection(
    title: String,
    templates: List<Template>,
    categories: Map<String, Category>,
    members: Map<String, Member>,
    onTemplateClick: (Template) -> Unit
) {
    item { Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
    if (templates.isEmpty()) {
        item { Text("暂无$title。", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp)) }
    } else {
        items(templates, key = { it.id }) { template ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onTemplateClick(template) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(template.name, fontWeight = FontWeight.Bold)
                        Text(template.amount?.let { AmountUtil.format(it) } ?: "金额未固定", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        "${categories[template.categoryId]?.displayPath() ?: "未分类"} · ${members[template.memberId]?.name ?: "未知成员"}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (template.remark.isNotBlank()) Text(template.remark)
                }
            }
        }
    }
}

private fun Category.displayPath(): String =
    if (groupName.isBlank()) name else "$groupName > $name"

private fun groupOrder(groupName: String): Int = when (groupName) {
    "食品酒水" -> 1
    "行车交通" -> 2
    "居家物业" -> 3
    "交流通讯" -> 4
    "休闲娱乐" -> 5
    "衣服饰品" -> 6
    "医疗保险" -> 7
    "金融保险" -> 8
    "人情往来" -> 9
    "其他杂项" -> 10
    "职业收入" -> 11
    "其他收入" -> 12
    else -> 99
}

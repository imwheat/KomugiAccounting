package com.komugi.komugiaccounting.ui.add

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.komugi.komugiaccounting.data.model.Category
import com.komugi.komugiaccounting.data.model.Member
import com.komugi.komugiaccounting.data.model.RecordType
import com.komugi.komugiaccounting.data.model.Template
import com.komugi.komugiaccounting.ui.components.CategoryPickerContent
import com.komugi.komugiaccounting.ui.components.DatePickerDialog
import com.komugi.komugiaccounting.ui.components.NumberKeyboard
import com.komugi.komugiaccounting.ui.components.TimePickerDialog
import com.komugi.komugiaccounting.ui.components.categoryDisplayPath
import com.komugi.komugiaccounting.util.AmountUtil
import com.komugi.komugiaccounting.util.DateTimeUtil

private enum class AddTopTab { TEMPLATE, EXPENSE, INCOME }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddRecordScreen(
    viewModel: AddRecordViewModel,
    onSaved: (String) -> Unit,
    onBack: () -> Unit,
    recordId: String? = null,
    autoBookTodoId: String? = null,
    onAutoBookTodoChange: (String?) -> Unit = {},
    onCreateTemplate: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val data by viewModel.data.collectAsState()
    val focusManager = LocalFocusManager.current
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
    var loadedAutoBookTodoId by rememberSaveable { mutableStateOf<String?>(null) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var showNumberKeyboard by rememberSaveable { mutableStateOf(false) }
    var showCategoryPicker by rememberSaveable { mutableStateOf(false) }
    var pendingDelete by rememberSaveable { mutableStateOf(false) }

    fun hideNumberKeyboard() {
        showNumberKeyboard = false
        focusManager.clearFocus()
    }

    fun dismissNumberKeyboard() {
        showNumberKeyboard = false
    }

    val editingRecord = recordId?.let { id -> data.records.firstOrNull { it.id == id } }
    val activeAutoBookTodo = autoBookTodoId?.let { id -> data.autoBookTodos.firstOrNull { it.id == id } }
    val typeCategories = data.categories.filter { it.enabled && it.type == type && !it.name.startsWith("__group__") }.sortedBy { it.sortOrder }
    val selectedCategory = typeCategories.firstOrNull { it.id == selectedCategoryId }
    val members = data.members.filter { it.enabled }

    fun resetBlankForm() {
        amount = ""
        remark = ""
        dateTime = DateTimeUtil.formatDateTime(DateTimeUtil.now())
        pendingDelete = false
    }

    fun loadTodo(todoId: String?) {
        val todo = todoId?.let { id -> data.autoBookTodos.firstOrNull { it.id == id } }
        loadedAutoBookTodoId = todoId
        if (todo == null) {
            onAutoBookTodoChange(null)
            resetBlankForm()
            return
        }
        type = todo.type
        topTab = if (todo.type == RecordType.EXPENSE) AddTopTab.EXPENSE else AddTopTab.INCOME
        amount = AmountUtil.formatPlain(todo.amount)
        dateTime = DateTimeUtil.formatDateTime(todo.dateTime)
        remark = appendRemarkLine(remark, todo.ruleName)
        showCategoryPicker = false
        hideNumberKeyboard()
        error = null
        message = null
    }

    BackHandler {
        when {
            showNumberKeyboard -> hideNumberKeyboard()
            showCategoryPicker -> showCategoryPicker = false
            showDatePicker -> showDatePicker = false
            showTimePicker -> showTimePicker = false
            else -> onBack()
        }
    }

    LaunchedEffect(recordId, data.records) {
        if (recordId != loadedRecordId) {
            val record = viewModel.record(recordId)
            loadedRecordId = recordId
            pendingDelete = false
            if (record == null) {
                type = RecordType.EXPENSE
                topTab = AddTopTab.EXPENSE
                resetBlankForm()
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
            hideNumberKeyboard()
            error = null
            message = null
        }
    }

    LaunchedEffect(autoBookTodoId, data.autoBookTodos) {
        if (recordId == null && autoBookTodoId != loadedAutoBookTodoId) {
            loadTodo(autoBookTodoId)
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
        if (members.none { it.id == selectedMemberId }) {
            selectedMemberId = data.settings.lastMemberId?.takeIf { id -> members.any { it.id == id } }
                ?: members.firstOrNull()?.id.orEmpty()
        }
    }

    fun selectType(nextType: RecordType) {
        hideNumberKeyboard()
        type = nextType
        topTab = if (nextType == RecordType.EXPENSE) AddTopTab.EXPENSE else AddTopTab.INCOME
        showCategoryPicker = false
        pendingDelete = false
        error = null
        message = null
    }

    fun applyTemplate(template: Template) {
        hideNumberKeyboard()
        type = template.type
        topTab = if (template.type == RecordType.EXPENSE) AddTopTab.EXPENSE else AddTopTab.INCOME
        amount = template.amount?.let { AmountUtil.formatPlain(it) }.orEmpty()
        selectedCategoryId = template.categoryId
        selectedMemberId = template.memberId
        remark = template.remark
        showCategoryPicker = false
        pendingDelete = false
        error = null
        message = null
    }

    fun afterAutoBookTodoSaved(continueNext: Boolean) {
        val currentTodoId = autoBookTodoId
        val nextTodo = data.autoBookTodos.firstOrNull { it.id != currentTodoId }
        if (currentTodoId != null) viewModel.removeAutoBookTodo(currentTodoId)
        if (continueNext && nextTodo != null) {
            resetBlankForm()
            onAutoBookTodoChange(nextTodo.id)
            message = "已保存，继续处理下一条代办"
        } else {
            onAutoBookTodoChange(null)
            resetBlankForm()
            if (continueNext) message = "保存成功，可继续记下一笔"
        }
    }

    if (showCategoryPicker) {
        CategoryPickerContent(
            categories = typeCategories,
            recentCategoryIds = data.settings.recentCategoryIds,
            onBack = { showCategoryPicker = false },
            onSelect = {
                selectedCategoryId = it.id
                showCategoryPicker = false
            },
            selectedCategoryIds = setOf(selectedCategoryId),
            modifier = modifier
        )
    } else {
        LazyColumn(
            modifier = modifier.padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(Modifier.height(10.dp)) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = {
                        hideNumberKeyboard()
                        onBack()
                    }) { Text("<") }
                    Text(
                        if (recordId == null) "记一笔" else "编辑记录",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = topTab == AddTopTab.TEMPLATE,
                        onClick = {
                            hideNumberKeyboard()
                            topTab = AddTopTab.TEMPLATE
                            pendingDelete = false
                        },
                        label = { Text("模板") }
                    )
                    FilterChip(selected = topTab == AddTopTab.EXPENSE, onClick = { selectType(RecordType.EXPENSE) }, label = { Text("支出") })
                    FilterChip(selected = topTab == AddTopTab.INCOME, onClick = { selectType(RecordType.INCOME) }, label = { Text("收入") })
                }
            }

            if (topTab == AddTopTab.TEMPLATE) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button(onClick = onCreateTemplate) { Text("新建") }
                    }
                }
                item { TemplateSection("支出模板", data.templates.filter { it.type == RecordType.EXPENSE }.sortedBy { it.name }, data.categories.associateBy { it.id }, data.members.associateBy { it.id }, ::applyTemplate) }
                item { TemplateSection("收入模板", data.templates.filter { it.type == RecordType.INCOME }.sortedBy { it.name }, data.categories.associateBy { it.id }, data.members.associateBy { it.id }, ::applyTemplate) }
            } else {
                item {
                    RecordFormCard(
                        recordId = recordId,
                        isRefunded = editingRecord?.isRefunded == true,
                        pendingDelete = pendingDelete,
                        amount = amount,
                        onAmountChange = { amount = it },
                        showNumberKeyboard = showNumberKeyboard,
                        onAmountClick = { showNumberKeyboard = true },
                        onDismissNumberKeyboard = { hideNumberKeyboard() },
                        selectedCategory = selectedCategory,
                        onOpenCategoryPicker = {
                            hideNumberKeyboard()
                            showCategoryPicker = true
                        },
                        members = members,
                        selectedMemberId = selectedMemberId,
                        onMemberSelected = {
                            hideNumberKeyboard()
                            selectedMemberId = it
                        },
                        date = datePart(dateTime),
                        time = timePart(dateTime),
                        onOpenDatePicker = {
                            hideNumberKeyboard()
                            showDatePicker = true
                        },
                        onOpenTimePicker = {
                            hideNumberKeyboard()
                            showTimePicker = true
                        },
                        remark = remark,
                        onRemarkChange = { remark = it },
                        onRemarkFocus = { dismissNumberKeyboard() },
                        message = message,
                        error = error,
                        onSaveTemplate = {
                            hideNumberKeyboard()
                            pendingDelete = false
                            message = null
                            error = viewModel.saveTemplate(type, amount, selectedCategoryId, selectedMemberId, remark)
                            if (error == null) message = "已存为模板"
                        },
                        onSaveAndContinue = {
                            hideNumberKeyboard()
                            message = null
                            error = viewModel.saveRecord(type, amount, selectedCategoryId, selectedMemberId, dateTime, remark, null)
                            if (error == null) afterAutoBookTodoSaved(continueNext = true)
                        },
                        onSave = {
                            hideNumberKeyboard()
                            pendingDelete = false
                            message = null
                            error = viewModel.saveRecord(type, amount, selectedCategoryId, selectedMemberId, dateTime, remark, recordId)
                            if (error == null) {
                                autoBookTodoId?.let {
                                    viewModel.removeAutoBookTodo(it)
                                }
                                resetBlankForm()
                                onSaved(if (recordId == null) "保存成功" else "修改已保存")
                                if (autoBookTodoId != null) {
                                    onAutoBookTodoChange(null)
                                }
                            }
                        },
                        onToggleRefund = {
                            val id = recordId ?: return@RecordFormCard
                            hideNumberKeyboard()
                            pendingDelete = false
                            viewModel.setRecordRefunded(id, editingRecord?.isRefunded != true)
                        },
                        onDelete = {
                            val id = recordId ?: return@RecordFormCard
                            hideNumberKeyboard()
                            if (pendingDelete) {
                                viewModel.deleteRecord(id)
                                onSaved("记录已删除")
                            } else {
                                pendingDelete = true
                            }
                        }
                    )
                }
            }
            item { Spacer(Modifier.height(88.dp)) }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            initialDate = datePart(dateTime),
            onDismiss = { showDatePicker = false },
            onConfirm = {
                dateTime = "$it ${timePart(dateTime)}"
                showDatePicker = false
            }
        )
    }
    if (showTimePicker) {
        TimePickerDialog(
            initialTime = timePart(dateTime),
            onDismiss = { showTimePicker = false },
            onConfirm = {
                dateTime = "${datePart(dateTime)} $it"
                showTimePicker = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecordFormCard(
    recordId: String?,
    isRefunded: Boolean,
    pendingDelete: Boolean,
    amount: String,
    onAmountChange: (String) -> Unit,
    showNumberKeyboard: Boolean,
    onAmountClick: () -> Unit,
    onDismissNumberKeyboard: () -> Unit,
    selectedCategory: Category?,
    onOpenCategoryPicker: () -> Unit,
    members: List<Member>,
    selectedMemberId: String,
    onMemberSelected: (String) -> Unit,
    date: String,
    time: String,
    onOpenDatePicker: () -> Unit,
    onOpenTimePicker: () -> Unit,
    remark: String,
    onRemarkChange: (String) -> Unit,
    onRemarkFocus: () -> Unit,
    message: String?,
    error: String?,
    onSaveTemplate: () -> Unit,
    onSaveAndContinue: () -> Unit,
    onSave: () -> Unit,
    onToggleRefund: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(modifier = Modifier.fillMaxWidth().clickable(onClick = onAmountClick)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = {},
                    label = { Text("金额") },
                    enabled = false,
                    readOnly = true,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (showNumberKeyboard) {
                NumberKeyboard(
                    value = amount,
                    onValueChange = onAmountChange,
                    onDismiss = onDismissNumberKeyboard,
                    onCancel = onDismissNumberKeyboard
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("分类：", fontWeight = FontWeight.SemiBold)
                OutlinedButton(modifier = Modifier.weight(1f), onClick = onOpenCategoryPicker) {
                    Text(selectedCategory?.let(::categoryDisplayPath) ?: "请选择分类")
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
                Text("日期：", fontWeight = FontWeight.SemiBold)
                OutlinedButton(modifier = Modifier.weight(1f), onClick = onOpenDatePicker) {
                    Text(date)
                }
                Text("时间：", fontWeight = FontWeight.SemiBold)
                OutlinedButton(modifier = Modifier.weight(1f), onClick = onOpenTimePicker) {
                    Text(time)
                }
            }

            OutlinedTextField(
                value = remark,
                onValueChange = onRemarkChange,
                label = { Text("备注") },
                minLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { if (it.isFocused) onRemarkFocus() }
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
            if (recordId != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(modifier = Modifier.weight(1f), onClick = onToggleRefund) {
                        Text(if (isRefunded) "取消退款" else "退款")
                    }
                    OutlinedButton(modifier = Modifier.weight(1f), onClick = onDelete) {
                        Text(if (pendingDelete) "确认删除" else "删除")
                    }
                }
                if (pendingDelete) {
                    Text("再次点击确认删除。", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun TemplateSection(
    title: String,
    templates: List<Template>,
    categories: Map<String, Category>,
    members: Map<String, Member>,
    onTemplateClick: (Template) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (templates.isEmpty()) {
            Text("暂无$title。", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp))
        } else {
            templates.forEach { template ->
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
                            "${categories[template.categoryId]?.let(::categoryDisplayPath) ?: "未分类"} · ${members[template.memberId]?.name ?: "未知成员"}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (template.remark.isNotBlank()) Text(template.remark)
                    }
                }
            }
        }
    }
}

private fun appendRemarkLine(current: String, line: String): String {
    val cleanLine = line.trim()
    if (cleanLine.isBlank()) return current
    return current.trim().ifBlank { cleanLine }.let { existing ->
        if (existing == cleanLine || existing.lines().contains(cleanLine)) existing else "$existing\n$cleanLine"
    }
}

private fun datePart(dateTime: String): String =
    dateTime.split(" ").getOrNull(0)?.takeIf { it.isNotBlank() } ?: DateTimeUtil.formatDate(DateTimeUtil.now())

private fun timePart(dateTime: String): String =
    dateTime.split(" ").getOrNull(1)?.takeIf { it.isNotBlank() } ?: DateTimeUtil.formatTime(DateTimeUtil.now())

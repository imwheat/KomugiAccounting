package com.komugi.komugiaccounting.ui.automation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.komugi.komugiaccounting.data.model.AutoBookRule
import com.komugi.komugiaccounting.data.model.AutoBookTodo
import com.komugi.komugiaccounting.data.model.AutomationFrequency
import com.komugi.komugiaccounting.data.model.AutomationRule
import com.komugi.komugiaccounting.data.model.Category
import com.komugi.komugiaccounting.data.model.RecordType
import com.komugi.komugiaccounting.data.repository.AppDataRepository
import com.komugi.komugiaccounting.ui.components.CategoryPickerContent
import com.komugi.komugiaccounting.ui.components.categoryDisplayPath
import com.komugi.komugiaccounting.util.AmountUtil
import com.komugi.komugiaccounting.util.DateTimeUtil
import java.util.UUID

private sealed interface AutomationPage {
    data object Main : AutomationPage
    data object TodoList : AutomationPage
    data object AutoBookRules : AutomationPage
    data class AutoBookEdit(val ruleId: String?) : AutomationPage
    data class RuleList(val type: RecordType) : AutomationPage
    data class RuleEdit(val type: RecordType, val ruleId: String?) : AutomationPage
    data class CategoryPicker(val type: RecordType, val ruleId: String?) : AutomationPage
}

@Composable
fun AutomationScreen(
    repository: AppDataRepository,
    onBottomBarVisibleChange: (Boolean) -> Unit,
    initialTodoList: Boolean = false,
    onInitialTodoListConsumed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var page by remember { mutableStateOf<AutomationPage>(AutomationPage.Main) }
    var draftCategoryId by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) { repository.runDueAutomationRules() }
    LaunchedEffect(initialTodoList) {
        if (initialTodoList) {
            page = AutomationPage.TodoList
            onInitialTodoListConsumed()
        }
    }
    LaunchedEffect(page) { onBottomBarVisibleChange(page == AutomationPage.Main) }

    BackHandler(enabled = page != AutomationPage.Main) {
        page = when (val current = page) {
            AutomationPage.Main -> AutomationPage.Main
            AutomationPage.TodoList -> AutomationPage.Main
            AutomationPage.AutoBookRules -> AutomationPage.Main
            is AutomationPage.AutoBookEdit -> AutomationPage.AutoBookRules
            is AutomationPage.CategoryPicker -> AutomationPage.RuleEdit(current.type, current.ruleId)
            is AutomationPage.RuleEdit -> AutomationPage.RuleList(current.type)
            is AutomationPage.RuleList -> AutomationPage.Main
        }
    }

    when (val current = page) {
        AutomationPage.Main -> AutomationMainScreen(
            repository = repository,
            onOpenRuleList = { page = AutomationPage.RuleList(it) },
            onOpenAutoBook = { page = AutomationPage.AutoBookRules },
            onOpenTodos = { page = AutomationPage.TodoList },
            modifier = modifier
        )
        AutomationPage.TodoList -> AutoBookTodoListScreen(repository, onBack = { page = AutomationPage.Main }, modifier)
        AutomationPage.AutoBookRules -> AutoBookRuleListScreen(
            repository = repository,
            onBack = { page = AutomationPage.Main },
            onCreate = { page = AutomationPage.AutoBookEdit(null) },
            onEdit = { page = AutomationPage.AutoBookEdit(it) },
            modifier = modifier
        )
        is AutomationPage.AutoBookEdit -> AutoBookRuleEditScreen(repository, current.ruleId, onBack = { page = AutomationPage.AutoBookRules }, modifier)
        is AutomationPage.RuleList -> AutomationRuleListScreen(
            repository = repository,
            type = current.type,
            onBack = { page = AutomationPage.Main },
            onCreate = { page = AutomationPage.RuleEdit(current.type, null) },
            onEdit = { page = AutomationPage.RuleEdit(current.type, it) },
            modifier = modifier
        )
        is AutomationPage.RuleEdit -> AutomationRuleEditScreen(
            repository = repository,
            type = current.type,
            ruleId = current.ruleId,
            overrideCategoryId = draftCategoryId.takeIf { it.isNotBlank() },
            onCategoryConsumed = { draftCategoryId = "" },
            onOpenCategoryPicker = { page = AutomationPage.CategoryPicker(current.type, current.ruleId) },
            onBack = { page = AutomationPage.RuleList(current.type) },
            modifier = modifier
        )
        is AutomationPage.CategoryPicker -> {
            val data by repository.data.collectAsState()
            CategoryPickerContent(
                categories = data.categories.filter { it.enabled && it.type == current.type && !it.name.startsWith("__group__") }.sortedBy { it.sortOrder },
                recentCategoryIds = data.settings.recentCategoryIds,
                selectedCategoryIds = emptySet(),
                onBack = { page = AutomationPage.RuleEdit(current.type, current.ruleId) },
                onSelect = {
                    draftCategoryId = it.id
                    page = AutomationPage.RuleEdit(current.type, current.ruleId)
                },
                modifier = modifier
            )
        }
    }
}

@Composable
private fun AutomationMainScreen(
    repository: AppDataRepository,
    onOpenRuleList: (RecordType) -> Unit,
    onOpenAutoBook: () -> Unit,
    onOpenTodos: () -> Unit,
    modifier: Modifier = Modifier
) {
    val data by repository.data.collectAsState()
    LazyColumn(modifier = modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("自动化", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black) }
        item {
            val rules = data.automationRules.filter { it.type == RecordType.EXPENSE }
            AutomationOptionCard("自动支出", rules.count { it.enabled }, rules.size) { onOpenRuleList(RecordType.EXPENSE) }
        }
        item {
            val rules = data.automationRules.filter { it.type == RecordType.INCOME }
            AutomationOptionCard("自动收入", rules.count { it.enabled }, rules.size) { onOpenRuleList(RecordType.INCOME) }
        }
        item {
            AutomationOptionCard("自动记账", data.autoBookRules.count { it.enabled }, data.autoBookRules.size, onOpenAutoBook)
        }
        item {
            AutomationOptionCard("自动记账代办", data.autoBookTodos.size, data.autoBookTodos.size, onOpenTodos)
        }
    }
}

@Composable
private fun AutomationOptionCard(title: String, enabledCount: Int, totalCount: Int, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("启用 ${enabledCount} 个，共有 ${totalCount} 个", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AutoBookRuleListScreen(
    repository: AppDataRepository,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onEdit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val data by repository.data.collectAsState()
    LazyColumn(modifier = modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = onBack) { Text("<") }
                    Text("自动记账", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                }
                Button(onClick = onCreate) { Text("新建") }
            }
        }
        if (data.autoBookRules.isEmpty()) {
            item { Text("还没有规则。", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(12.dp)) }
        } else {
            items(data.autoBookRules, key = { it.id }) { rule ->
                Card(
                    modifier = Modifier.fillMaxWidth().alpha(if (rule.enabled) 1f else 0.48f).clickable { onEdit(rule.id) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(rule.name, fontWeight = FontWeight.Bold)
                            Text("标题：${rule.titleKeyword.ifBlank { "不限" }}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("文本：${rule.textPattern}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(if (rule.type == RecordType.EXPENSE) "支出" else "收入", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = rule.enabled, onCheckedChange = { repository.setAutoBookRuleEnabled(rule.id, it) })
                    }
                }
            }
        }
    }
}

@Composable
private fun AutoBookRuleEditScreen(repository: AppDataRepository, ruleId: String?, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val data by repository.data.collectAsState()
    val editing = data.autoBookRules.firstOrNull { it.id == ruleId }
    var name by rememberSaveable(ruleId) { mutableStateOf(editing?.name.orEmpty()) }
    var titleKeyword by rememberSaveable(ruleId) { mutableStateOf(editing?.titleKeyword.orEmpty()) }
    var textPattern by rememberSaveable(ruleId) { mutableStateOf(editing?.textPattern ?: "支取人民币XXX元") }
    var type by rememberSaveable(ruleId) { mutableStateOf(editing?.type ?: RecordType.EXPENSE) }
    var enabled by rememberSaveable(ruleId) { mutableStateOf(editing?.enabled ?: true) }
    var error by rememberSaveable(ruleId) { mutableStateOf<String?>(null) }
    var pendingDelete by rememberSaveable(ruleId) { mutableStateOf(false) }

    LazyColumn(modifier = modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onBack) { Text("<") }
                Text(if (editing == null) "新建自动记账规则" else "编辑自动记账规则", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("规则名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(titleKeyword, { titleKeyword = it }, label = { Text("根据标题筛选") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(textPattern, { textPattern = it }, label = { Text("通知文本规则，金额位置用 XXX") }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = type == RecordType.EXPENSE, onClick = { type = RecordType.EXPENSE }, label = { Text("支出") })
                        FilterChip(selected = type == RecordType.INCOME, onClick = { type = RecordType.INCOME }, label = { Text("收入") })
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("启用")
                        Switch(checked = enabled, onCheckedChange = { enabled = it })
                    }
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val result = repository.upsertAutoBookRule(
                                AutoBookRule(
                                    id = editing?.id ?: UUID.randomUUID().toString(),
                                    name = name,
                                    titleKeyword = titleKeyword,
                                    textPattern = textPattern,
                                    type = type,
                                    enabled = enabled
                                )
                            )
                            if (result == null) onBack() else error = result
                        }
                    ) { Text(if (editing == null) "添加规则" else "保存规则") }
                    if (editing != null) {
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                if (pendingDelete) {
                                    repository.deleteAutoBookRule(editing.id)
                                    onBack()
                                } else {
                                    pendingDelete = true
                                }
                            }
                        ) { Text(if (pendingDelete) "确认删除" else "删除") }
                    }
                }
            }
        }
    }
}

@Composable
private fun AutoBookTodoListScreen(repository: AppDataRepository, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val data by repository.data.collectAsState()
    LazyColumn(modifier = modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onBack) { Text("<") }
                Text("自动记账代办", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            }
        }
        if (data.autoBookTodos.isEmpty()) {
            item { Text("没有待处理。", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(12.dp)) }
        } else {
            items(data.autoBookTodos, key = { it.id }) { todo ->
                AutoBookTodoCard(todo = todo, templates = data.templates, repository = repository)
            }
        }
    }
}

@Composable
private fun AutoBookTodoCard(todo: AutoBookTodo, templates: List<com.komugi.komugiaccounting.data.model.Template>, repository: AppDataRepository) {
    val color = if (todo.type == RecordType.INCOME) Color(0xFF1F7A4D) else Color(0xFFB3542E)
    var showTemplates by rememberSaveable(todo.id) { mutableStateOf(false) }
    val matchedTemplates = templates.filter { it.type == todo.type && it.amount == todo.amount }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(todo.ruleName, fontWeight = FontWeight.Bold)
                Text(AmountUtil.format(todo.amount), color = color, fontWeight = FontWeight.Black)
            }
            Text(DateTimeUtil.formatDisplayDateTime(todo.dateTime), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(todo.notificationText, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { repository.ignoreAutoBookTodo(todo.id) }) { Text("忽略") }
                OutlinedButton(onClick = { showTemplates = !showTemplates }) { Text("快捷加入") }
                OutlinedButton(onClick = { showTemplates = !showTemplates }) { Text("编辑加入") }
            }
            if (showTemplates) {
                if (matchedTemplates.isEmpty()) {
                    Text("没有匹配金额和类型的模板。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    matchedTemplates.forEach { template ->
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { repository.addAutoBookTodoWithTemplate(todo.id, template.id) }
                        ) { Text(template.name) }
                    }
                }
            }
        }
    }
}

@Composable
private fun AutomationRuleListScreen(
    repository: AppDataRepository,
    type: RecordType,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onEdit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val data by repository.data.collectAsState()
    val categories = data.categories.associateBy { it.id }
    val members = data.members.associateBy { it.id }
    val rules = data.automationRules.filter { it.type == type }
    val title = if (type == RecordType.EXPENSE) "自动支出" else "自动收入"

    LazyColumn(modifier = modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = onBack) { Text("<") }
                    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                }
                Button(onClick = onCreate) { Text("新建") }
            }
        }
        items(rules, key = { it.id }) { rule ->
            AutomationRuleCard(rule, categories[rule.categoryId], members[rule.memberId]?.name ?: "未知成员", { onEdit(rule.id) }) {
                repository.setAutomationRuleEnabled(rule.id, it)
            }
        }
    }
}

@Composable
private fun AutomationRuleCard(rule: AutomationRule, category: Category?, memberName: String, onClick: () -> Unit, onEnabledChange: (Boolean) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().alpha(if (rule.enabled) 1f else 0.48f).clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(rule.name, fontWeight = FontWeight.Bold)
                    Text(AmountUtil.format(rule.amount), fontWeight = FontWeight.Black)
                }
                Text("${rule.frequency.label(rule.month, rule.day)} · ${category?.let(::categoryDisplayPath) ?: "未分类"} · $memberName", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = rule.enabled, onCheckedChange = onEnabledChange)
        }
    }
}

@Composable
private fun AutomationRuleEditScreen(
    repository: AppDataRepository,
    type: RecordType,
    ruleId: String?,
    overrideCategoryId: String?,
    onCategoryConsumed: () -> Unit,
    onOpenCategoryPicker: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val data by repository.data.collectAsState()
    val editingRule = data.automationRules.firstOrNull { it.id == ruleId }
    val typeCategories = data.categories.filter { it.enabled && it.type == type && !it.name.startsWith("__group__") }.sortedBy { it.sortOrder }
    val enabledMembers = data.members.filter { it.enabled }
    var name by rememberSaveable(ruleId) { mutableStateOf(editingRule?.name ?: if (type == RecordType.EXPENSE) "自动支出" else "自动收入") }
    var frequency by rememberSaveable(ruleId) { mutableStateOf(editingRule?.frequency ?: AutomationFrequency.DAILY) }
    var monthText by rememberSaveable(ruleId) { mutableStateOf((editingRule?.month ?: 1).toString()) }
    var dayText by rememberSaveable(ruleId) { mutableStateOf((editingRule?.day ?: 1).toString()) }
    var amount by rememberSaveable(ruleId) { mutableStateOf(editingRule?.amount?.let(AmountUtil::formatPlain).orEmpty()) }
    var categoryId by rememberSaveable(ruleId) { mutableStateOf(editingRule?.categoryId.orEmpty()) }
    var memberId by rememberSaveable(ruleId) { mutableStateOf(editingRule?.memberId.orEmpty()) }
    var remark by rememberSaveable(ruleId) { mutableStateOf(editingRule?.remark.orEmpty()) }
    var enabled by rememberSaveable(ruleId) { mutableStateOf(editingRule?.enabled ?: true) }
    var error by rememberSaveable(ruleId) { mutableStateOf<String?>(null) }
    LaunchedEffect(overrideCategoryId) { overrideCategoryId?.let { categoryId = it; onCategoryConsumed() } }
    LaunchedEffect(typeCategories, categoryId) { if (typeCategories.none { it.id == categoryId }) categoryId = typeCategories.firstOrNull()?.id.orEmpty() }
    LaunchedEffect(enabledMembers, memberId) { if (enabledMembers.none { it.id == memberId }) memberId = enabledMembers.firstOrNull()?.id.orEmpty() }

    LazyColumn(modifier = modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) { OutlinedButton(onClick = onBack) { Text("<") }; Text(if (editingRule == null) "新建订阅" else "编辑订阅", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black) } }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("标题") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(frequency == AutomationFrequency.DAILY, { frequency = AutomationFrequency.DAILY }, label = { Text("每天") })
                        FilterChip(frequency == AutomationFrequency.MONTHLY, { frequency = AutomationFrequency.MONTHLY }, label = { Text("每月") })
                        FilterChip(frequency == AutomationFrequency.YEARLY, { frequency = AutomationFrequency.YEARLY }, label = { Text("每年") })
                    }
                    if (frequency == AutomationFrequency.YEARLY) OutlinedTextField(monthText, { monthText = it }, label = { Text("月份") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    if (frequency != AutomationFrequency.DAILY) OutlinedTextField(dayText, { dayText = it }, label = { Text("日期") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(amount, { amount = it }, label = { Text("金额") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                    OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onOpenCategoryPicker) { Text(typeCategories.firstOrNull { it.id == categoryId }?.let(::categoryDisplayPath) ?: "请选择分类") }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { enabledMembers.forEach { FilterChip(memberId == it.id, { memberId = it.id }, label = { Text(it.name) }) } }
                    OutlinedTextField(remark, { remark = it }, label = { Text("备注") }, modifier = Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("启用"); Switch(enabled, { enabled = it }) }
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Button(modifier = Modifier.fillMaxWidth(), onClick = {
                        val cents = AmountUtil.parseToCents(amount)
                        if (cents == null) { error = "金额格式不正确"; return@Button }
                        val result = repository.upsertAutomationRule(AutomationRule(editingRule?.id ?: UUID.randomUUID().toString(), name, type, frequency, monthText.toIntOrNull() ?: 1, dayText.toIntOrNull() ?: 1, cents, categoryId, memberId, remark, enabled, editingRule?.lastRunDate))
                        if (result == null) onBack() else error = result
                    }) { Text("保存") }
                }
            }
        }
    }
}

private fun AutomationFrequency.label(month: Int, day: Int): String = when (this) {
    AutomationFrequency.DAILY -> "每天"
    AutomationFrequency.MONTHLY -> "每月${day}号"
    AutomationFrequency.YEARLY -> "每年${month}月${day}号"
}

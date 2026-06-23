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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.komugi.komugiaccounting.data.model.AutomationFrequency
import com.komugi.komugiaccounting.data.model.AutomationRule
import com.komugi.komugiaccounting.data.model.Category
import com.komugi.komugiaccounting.data.model.RecordType
import com.komugi.komugiaccounting.data.repository.AppDataRepository
import com.komugi.komugiaccounting.ui.components.CategoryPickerContent
import com.komugi.komugiaccounting.ui.components.categoryDisplayPath
import com.komugi.komugiaccounting.util.AmountUtil
import java.util.UUID

private sealed interface AutomationPage {
    data object Main : AutomationPage
    data class RuleList(val type: RecordType) : AutomationPage
    data class RuleEdit(val type: RecordType, val ruleId: String?) : AutomationPage
    data class CategoryPicker(val type: RecordType, val ruleId: String?) : AutomationPage
}

@Composable
fun AutomationScreen(
    repository: AppDataRepository,
    onBottomBarVisibleChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var page by rememberSaveable { mutableStateOf<AutomationPage>(AutomationPage.Main) }
    var draftCategoryId by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) {
        repository.runDueAutomationRules()
    }

    LaunchedEffect(page) {
        onBottomBarVisibleChange(page == AutomationPage.Main)
    }

    BackHandler(enabled = page != AutomationPage.Main) {
        page = when (val current = page) {
            AutomationPage.Main -> AutomationPage.Main
            is AutomationPage.CategoryPicker -> AutomationPage.RuleEdit(current.type, current.ruleId)
            is AutomationPage.RuleEdit -> AutomationPage.RuleList(current.type)
            is AutomationPage.RuleList -> AutomationPage.Main
        }
    }

    when (val current = page) {
        AutomationPage.Main -> AutomationMainScreen(
            repository = repository,
            onOpen = { page = AutomationPage.RuleList(it) },
            modifier = modifier
        )
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
            val categories = data.categories
                .filter { it.enabled && it.type == current.type && !it.name.startsWith("__group__") }
                .sortedBy { it.sortOrder }
            CategoryPickerContent(
                categories = categories,
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
    onOpen: (RecordType) -> Unit,
    modifier: Modifier = Modifier
) {
    val data by repository.data.collectAsState()
    LazyColumn(modifier = modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("自动化", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black) }
        item {
            val rules = data.automationRules.filter { it.type == RecordType.EXPENSE }
            AutomationOptionCard(
                title = "自动支出",
                enabledCount = rules.count { it.enabled },
                totalCount = rules.size,
                onClick = { onOpen(RecordType.EXPENSE) }
            )
        }
        item {
            val rules = data.automationRules.filter { it.type == RecordType.INCOME }
            AutomationOptionCard(
                title = "自动收入",
                enabledCount = rules.count { it.enabled },
                totalCount = rules.size,
                onClick = { onOpen(RecordType.INCOME) }
            )
        }
        item {
            AutomationOptionCard(
                title = "自动记账",
                enabledCount = 0,
                totalCount = 0,
                onClick = {}
            )
        }
    }
}

@Composable
private fun AutomationOptionCard(title: String, enabledCount: Int, totalCount: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("启动订阅${enabledCount}个，共有订阅${totalCount}个", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        if (rules.isEmpty()) {
            item { Text("还没有订阅。", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(12.dp)) }
        } else {
            items(rules, key = { it.id }) { rule ->
                AutomationRuleCard(
                    rule = rule,
                    category = categories[rule.categoryId],
                    memberName = members[rule.memberId]?.name ?: "未知成员",
                    onClick = { onEdit(rule.id) },
                    onEnabledChange = { repository.setAutomationRuleEnabled(rule.id, it) }
                )
            }
        }
        item { Spacer(Modifier.height(88.dp)) }
    }
}

@Composable
private fun AutomationRuleCard(
    rule: AutomationRule,
    category: Category?,
    memberName: String,
    onClick: () -> Unit,
    onEnabledChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().alpha(if (rule.enabled) 1f else 0.48f).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(rule.name, fontWeight = FontWeight.Bold)
                    Text(AmountUtil.format(rule.amount), fontWeight = FontWeight.Black)
                }
                Text("${rule.frequency.label(rule.month, rule.day)} · ${category?.let(::categoryDisplayPath) ?: "未分类"} · $memberName", color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (rule.remark.isNotBlank()) Text(rule.remark, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = rule.enabled,
                onCheckedChange = onEnabledChange
            )
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
    var pendingDelete by rememberSaveable(ruleId) { mutableStateOf(false) }

    LaunchedEffect(overrideCategoryId) {
        overrideCategoryId?.let {
            categoryId = it
            onCategoryConsumed()
        }
    }
    LaunchedEffect(typeCategories, categoryId) {
        if (typeCategories.none { it.id == categoryId }) categoryId = typeCategories.firstOrNull()?.id.orEmpty()
    }
    LaunchedEffect(enabledMembers, memberId) {
        if (enabledMembers.none { it.id == memberId }) memberId = enabledMembers.firstOrNull()?.id.orEmpty()
    }

    val selectedCategory = typeCategories.firstOrNull { it.id == categoryId }
    val title = if (editingRule == null) "新建${if (type == RecordType.EXPENSE) "自动支出" else "自动收入"}" else "编辑订阅"

    LazyColumn(modifier = modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onBack) { Text("<") }
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("标题") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Text("周期", fontWeight = FontWeight.SemiBold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = frequency == AutomationFrequency.DAILY, onClick = { frequency = AutomationFrequency.DAILY }, label = { Text("每天") })
                        FilterChip(selected = frequency == AutomationFrequency.MONTHLY, onClick = { frequency = AutomationFrequency.MONTHLY }, label = { Text("每月") })
                        FilterChip(selected = frequency == AutomationFrequency.YEARLY, onClick = { frequency = AutomationFrequency.YEARLY }, label = { Text("每年") })
                    }
                    if (frequency == AutomationFrequency.YEARLY) {
                        OutlinedTextField(value = monthText, onValueChange = { monthText = it }, label = { Text("月份") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    }
                    if (frequency != AutomationFrequency.DAILY) {
                        OutlinedTextField(value = dayText, onValueChange = { dayText = it }, label = { Text("日期") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    }
                    OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("金额") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("分类", fontWeight = FontWeight.SemiBold)
                        OutlinedButton(modifier = Modifier.weight(1f), onClick = onOpenCategoryPicker) {
                            Text(selectedCategory?.let(::categoryDisplayPath) ?: "请选择分类")
                        }
                    }
                    Text("成员", fontWeight = FontWeight.SemiBold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        enabledMembers.forEach { member ->
                            FilterChip(selected = memberId == member.id, onClick = { memberId = member.id }, label = { Text(member.name) })
                        }
                    }
                    OutlinedTextField(value = remark, onValueChange = { remark = it }, label = { Text("备注") }, modifier = Modifier.fillMaxWidth())
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("启用")
                        Switch(checked = enabled, onCheckedChange = { enabled = it })
                    }
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val cents = AmountUtil.parseToCents(amount)
                            val month = monthText.toIntOrNull()?.coerceIn(1, 12) ?: 1
                            val day = dayText.toIntOrNull()?.coerceIn(1, 31) ?: 1
                            if (cents == null) {
                                error = "金额格式不正确"
                                return@Button
                            }
                            val result = repository.upsertAutomationRule(
                                AutomationRule(
                                    id = editingRule?.id ?: UUID.randomUUID().toString(),
                                    name = name,
                                    type = type,
                                    frequency = frequency,
                                    month = month,
                                    day = day,
                                    amount = cents,
                                    categoryId = categoryId,
                                    memberId = memberId,
                                    remark = remark,
                                    enabled = enabled,
                                    lastRunDate = editingRule?.lastRunDate
                                )
                            )
                            if (result == null) onBack() else error = result
                        }
                    ) { Text(if (editingRule == null) "添加订阅" else "保存订阅") }
                    if (editingRule != null) {
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                if (pendingDelete) {
                                    repository.deleteAutomationRule(editingRule.id)
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
        item { Spacer(Modifier.height(88.dp)) }
    }
}

private fun AutomationFrequency.label(month: Int, day: Int): String = when (this) {
    AutomationFrequency.DAILY -> "每天"
    AutomationFrequency.MONTHLY -> "每月${day}号"
    AutomationFrequency.YEARLY -> "每年${month}月${day}号"
}

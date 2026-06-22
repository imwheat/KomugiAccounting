package com.komugi.komugiaccounting.ui.template

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.komugi.komugiaccounting.data.model.Category
import com.komugi.komugiaccounting.data.model.Member
import com.komugi.komugiaccounting.data.model.RecordType
import com.komugi.komugiaccounting.data.model.Template
import com.komugi.komugiaccounting.data.repository.AppDataRepository
import com.komugi.komugiaccounting.ui.components.CategoryPickerContent
import com.komugi.komugiaccounting.ui.components.categoryDisplayPath
import com.komugi.komugiaccounting.util.AmountUtil
import java.util.UUID

@Composable
fun TemplateScreen(
    repository: AppDataRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editingTemplateId by rememberSaveable { mutableStateOf<String?>(null) }
    var isEditing by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = isEditing) {
        isEditing = false
        editingTemplateId = null
    }

    if (isEditing) {
        TemplateEditScreen(
            repository = repository,
            templateId = editingTemplateId,
            onBack = {
                isEditing = false
                editingTemplateId = null
            },
            modifier = modifier
        )
    } else {
        TemplateListScreen(
            repository = repository,
            onCreate = {
                editingTemplateId = null
                isEditing = true
            },
            onEdit = { template ->
                editingTemplateId = template.id
                isEditing = true
            },
            modifier = modifier
        )
    }
}

@Composable
private fun TemplateListScreen(
    repository: AppDataRepository,
    onCreate: () -> Unit,
    onEdit: (Template) -> Unit,
    modifier: Modifier = Modifier
) {
    val data by repository.data.collectAsState()
    var pendingDeleteId by rememberSaveable { mutableStateOf<String?>(null) }
    val categories = data.categories.associateBy { it.id }
    val members = data.members.associateBy { it.id }

    LazyColumn(
        modifier = modifier.padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("模板管理", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Button(onClick = onCreate) { Text("新建") }
            }
        }
        templateSection(
            title = "支出模板",
            templates = data.templates.filter { it.type == RecordType.EXPENSE }.sortedBy { it.name },
            allTemplatesEmpty = data.templates.isEmpty(),
            categories = categories,
            members = members,
            pendingDeleteId = pendingDeleteId,
            onPendingDeleteChange = { pendingDeleteId = it },
            onEdit = onEdit,
            onDelete = { repository.deleteTemplate(it.id) }
        )
        templateSection(
            title = "收入模板",
            templates = data.templates.filter { it.type == RecordType.INCOME }.sortedBy { it.name },
            allTemplatesEmpty = data.templates.isEmpty(),
            categories = categories,
            members = members,
            pendingDeleteId = pendingDeleteId,
            onPendingDeleteChange = { pendingDeleteId = it },
            onEdit = onEdit,
            onDelete = { repository.deleteTemplate(it.id) }
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.templateSection(
    title: String,
    templates: List<Template>,
    allTemplatesEmpty: Boolean,
    categories: Map<String, Category>,
    members: Map<String, Member>,
    pendingDeleteId: String?,
    onPendingDeleteChange: (String?) -> Unit,
    onEdit: (Template) -> Unit,
    onDelete: (Template) -> Unit
) {
    item { Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
    if (templates.isEmpty()) {
        item {
            val text = if (allTemplatesEmpty) "还没有模板，点击右上角新建。" else "暂无$title。"
            Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp))
        }
    } else {
        items(templates, key = { it.id }) { template ->
            TemplateCard(
                template = template,
                categoryName = categories[template.categoryId]?.let(::categoryDisplayPath) ?: "未分类",
                memberName = members[template.memberId]?.name ?: "未知成员",
                pendingDelete = pendingDeleteId == template.id,
                onEdit = { onEdit(template) },
                onDelete = {
                    if (pendingDeleteId == template.id) {
                        onDelete(template)
                        onPendingDeleteChange(null)
                    } else {
                        onPendingDeleteChange(template.id)
                    }
                }
            )
        }
    }
}

@Composable
private fun TemplateCard(
    template: Template,
    categoryName: String,
    memberName: String,
    pendingDelete: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(template.name, fontWeight = FontWeight.Bold)
                Text(template.amount?.let { AmountUtil.format(it) } ?: "金额未固定", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("$categoryName · $memberName", color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (template.remark.isNotBlank()) Text(template.remark)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onEdit) { Text("编辑") }
                OutlinedButton(onClick = onDelete) { Text(if (pendingDelete) "确认删除" else "删除") }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TemplateEditScreen(
    repository: AppDataRepository,
    templateId: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val data by repository.data.collectAsState()
    val editingTemplate = data.templates.firstOrNull { it.id == templateId }
    var type by rememberSaveable(templateId) { mutableStateOf(editingTemplate?.type ?: RecordType.EXPENSE) }
    var name by rememberSaveable(templateId) { mutableStateOf(editingTemplate?.name.orEmpty()) }
    var amount by rememberSaveable(templateId) { mutableStateOf(editingTemplate?.amount?.let { AmountUtil.formatPlain(it) }.orEmpty()) }
    var remark by rememberSaveable(templateId) { mutableStateOf(editingTemplate?.remark.orEmpty()) }
    var categoryId by rememberSaveable(templateId) { mutableStateOf(editingTemplate?.categoryId.orEmpty()) }
    var memberId by rememberSaveable(templateId) { mutableStateOf(editingTemplate?.memberId.orEmpty()) }
    var error by rememberSaveable(templateId) { mutableStateOf<String?>(null) }
    var showCategoryPicker by rememberSaveable(templateId) { mutableStateOf(false) }

    val typeCategories = data.categories.filter { it.enabled && it.type == type }.sortedBy { it.sortOrder }
    val selectedCategory = typeCategories.firstOrNull { it.id == categoryId }
    val enabledMembers = data.members.filter { it.enabled }
    if (categoryId.isBlank() || typeCategories.none { it.id == categoryId }) categoryId = typeCategories.firstOrNull()?.id.orEmpty()
    if (memberId.isBlank() || enabledMembers.none { it.id == memberId }) memberId = enabledMembers.firstOrNull()?.id.orEmpty()

    BackHandler(enabled = showCategoryPicker) {
        showCategoryPicker = false
    }

    if (showCategoryPicker) {
        CategoryPickerContent(
            categories = typeCategories,
            recentCategoryIds = data.settings.recentCategoryIds,
            selectedCategoryIds = setOf(categoryId),
            onBack = { showCategoryPicker = false },
            onSelect = {
                categoryId = it.id
                showCategoryPicker = false
            },
            modifier = modifier
        )
        return
    }

    LazyColumn(
        modifier = modifier.padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onBack) { Text("返回") }
                Text(if (editingTemplate == null) "新建模板" else "编辑模板", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(type == RecordType.EXPENSE, onClick = { type = RecordType.EXPENSE; categoryId = "" }, label = { Text("支出") })
                        FilterChip(type == RecordType.INCOME, onClick = { type = RecordType.INCOME; categoryId = "" }, label = { Text("收入") })
                    }
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("模板名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("金额，可留空") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("分类：", fontWeight = FontWeight.SemiBold)
                        OutlinedButton(modifier = Modifier.weight(1f), onClick = { showCategoryPicker = true }) {
                            Text(selectedCategory?.let(::categoryDisplayPath) ?: "请选择分类")
                        }
                    }
                    Text("成员", fontWeight = FontWeight.SemiBold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        enabledMembers.forEach { member ->
                            FilterChip(
                                selected = memberId == member.id,
                                onClick = { memberId = member.id },
                                label = { Text(member.name) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = remark,
                        onValueChange = { remark = it },
                        label = { Text("备注") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val cents = amount.takeIf { it.isNotBlank() }?.let { AmountUtil.parseToCents(it) }
                            when {
                                name.isBlank() -> error = "请输入模板名称"
                                amount.isNotBlank() && cents == null -> error = "金额格式不正确"
                                typeCategories.none { it.id == categoryId } -> error = "请选择分类"
                                enabledMembers.none { it.id == memberId } -> error = "请选择成员"
                                else -> {
                                    repository.upsertTemplate(
                                        Template(
                                            id = editingTemplate?.id ?: UUID.randomUUID().toString(),
                                            name = name,
                                            type = type,
                                            amount = cents,
                                            categoryId = categoryId,
                                            memberId = memberId,
                                            remark = remark
                                        )
                                    )
                                    onBack()
                                }
                            }
                        }
                    ) { Text(if (editingTemplate == null) "新建模板" else "保存模板") }
                }
            }
        }
    }
}

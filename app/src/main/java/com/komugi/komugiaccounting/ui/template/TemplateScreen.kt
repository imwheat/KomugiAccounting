package com.komugi.komugiaccounting.ui.template

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.foundation.text.KeyboardOptions
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
import com.komugi.komugiaccounting.data.model.RecordType
import com.komugi.komugiaccounting.data.model.Template
import com.komugi.komugiaccounting.data.repository.AppDataRepository
import com.komugi.komugiaccounting.util.AmountUtil
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TemplateScreen(
    repository: AppDataRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val data by repository.data.collectAsState()
    var type by rememberSaveable { mutableStateOf(RecordType.EXPENSE) }
    var name by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var remark by rememberSaveable { mutableStateOf("") }
    var categoryId by rememberSaveable { mutableStateOf("") }
    var memberId by rememberSaveable { mutableStateOf("") }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingDeleteId by rememberSaveable { mutableStateOf<String?>(null) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    val typeCategories = data.categories.filter { it.enabled && it.type == type }.sortedBy { it.sortOrder }
    val enabledMembers = data.members.filter { it.enabled }
    if (categoryId.isBlank()) categoryId = typeCategories.firstOrNull()?.id.orEmpty()
    if (memberId.isBlank()) memberId = enabledMembers.firstOrNull()?.id.orEmpty()

    fun resetForm() {
        editingId = null
        name = ""
        amount = ""
        remark = ""
        categoryId = typeCategories.firstOrNull()?.id.orEmpty()
        memberId = enabledMembers.firstOrNull()?.id.orEmpty()
        error = null
    }

    LazyColumn(
        modifier = modifier.padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onBack) { Text("返回") }
                Text("模板管理", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
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
                        label = { Text("金额，可空") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("分类", fontWeight = FontWeight.SemiBold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        typeCategories.forEach { category ->
                            FilterChip(
                                selected = categoryId == category.id,
                                onClick = { categoryId = category.id },
                                label = { Text(category.name) }
                            )
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            val cents = amount.takeIf { it.isNotBlank() }?.let { AmountUtil.parseToCents(it) }
                            when {
                                name.isBlank() -> error = "请输入模板名称"
                                amount.isNotBlank() && cents == null -> error = "金额格式不正确"
                                typeCategories.none { it.id == categoryId } -> error = "请选择分类"
                                enabledMembers.none { it.id == memberId } -> error = "请选择成员"
                                else -> {
                                    repository.upsertTemplate(
                                        Template(
                                            id = editingId ?: UUID.randomUUID().toString(),
                                            name = name,
                                            type = type,
                                            amount = cents,
                                            categoryId = categoryId,
                                            memberId = memberId,
                                            remark = remark
                                        )
                                    )
                                    resetForm()
                                }
                            }
                        }) { Text(if (editingId == null) "新增模板" else "保存模板") }
                        OutlinedButton(onClick = ::resetForm) { Text("清空") }
                    }
                }
            }
        }
        item { Text("已有模板", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        items(data.templates.sortedWith(compareBy<Template> { it.type.name }.thenBy { it.name }), key = { it.id }) { template ->
            val category = data.categories.firstOrNull { it.id == template.categoryId }
            val member = data.members.firstOrNull { it.id == template.memberId }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(template.name, fontWeight = FontWeight.Bold)
                    Text("${if (template.type == RecordType.EXPENSE) "支出" else "收入"} · ${category?.name ?: "未分类"} · ${member?.name ?: "未知成员"}")
                    Text(template.amount?.let { AmountUtil.format(it) } ?: "金额未固定")
                    if (template.remark.isNotBlank()) Text(template.remark, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            editingId = template.id
                            type = template.type
                            name = template.name
                            amount = template.amount?.let { AmountUtil.formatPlain(it) }.orEmpty()
                            categoryId = template.categoryId
                            memberId = template.memberId
                            remark = template.remark
                            error = null
                        }) { Text("编辑") }
                        OutlinedButton(onClick = {
                            if (pendingDeleteId == template.id) {
                                repository.deleteTemplate(template.id)
                                pendingDeleteId = null
                            } else {
                                pendingDeleteId = template.id
                            }
                        }) { Text(if (pendingDeleteId == template.id) "确认删除" else "删除") }
                    }
                }
            }
        }
    }
}

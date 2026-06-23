package com.komugi.komugiaccounting.ui.category

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.komugi.komugiaccounting.data.model.Category
import com.komugi.komugiaccounting.data.model.RecordType
import com.komugi.komugiaccounting.data.repository.AppDataRepository

private enum class CreateMode {
    GROUP,
    CATEGORY
}

@Composable
fun CategoryScreen(
    repository: AppDataRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedType by rememberSaveable { mutableStateOf(RecordType.EXPENSE) }
    var isCreating by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = isCreating) {
        isCreating = false
    }

    if (isCreating) {
        CategoryCreateScreen(
            repository = repository,
            selectedType = selectedType,
            onSelectedTypeChange = { selectedType = it },
            onBack = { isCreating = false },
            modifier = modifier
        )
    } else {
        CategoryListScreen(
            repository = repository,
            selectedType = selectedType,
            onSelectedTypeChange = { selectedType = it },
            onBack = onBack,
            onCreate = { isCreating = true },
            modifier = modifier
        )
    }
}

@Composable
private fun CategoryListScreen(
    repository: AppDataRepository,
    selectedType: RecordType,
    onSelectedTypeChange: (RecordType) -> Unit,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val data by repository.data.collectAsState()
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    val editingNames = remember { mutableStateMapOf<String, String>() }
    val categories = data.categories
        .filter { it.type == selectedType && !it.isGroupPlaceholder() }
        .sortedWith(compareBy<Category> { it.sortOrder }.thenBy { it.groupName }.thenBy { it.name })

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
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = onBack) { Text("<") }
                    Text("分类管理", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                }
                Button(onClick = onCreate) { Text("新建") }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedType == RecordType.EXPENSE,
                    onClick = {
                        onSelectedTypeChange(RecordType.EXPENSE)
                        message = null
                    },
                    label = { Text("支出分类") }
                )
                FilterChip(
                    selected = selectedType == RecordType.INCOME,
                    onClick = {
                        onSelectedTypeChange(RecordType.INCOME)
                        message = null
                    },
                    label = { Text("收入分类") }
                )
            }
        }
        message?.let {
            item { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 4.dp)) }
        }
        items(categories, key = { it.id }) { category ->
            val index = categories.indexOfFirst { it.id == category.id }
            CategoryRow(
                category = category,
                index = index,
                lastIndex = categories.lastIndex,
                editName = editingNames.getOrPut(category.id) { category.name },
                onEditName = { editingNames[category.id] = it },
                onSave = {
                    message = repository.updateCategory(category.id, editingNames[category.id].orEmpty())
                },
                onEnabledChange = {
                    repository.setCategoryEnabled(category.id, it)
                    message = null
                },
                onMoveUp = {
                    repository.moveCategory(category.id, -1)
                    message = null
                },
                onMoveDown = {
                    repository.moveCategory(category.id, 1)
                    message = null
                },
                onDelete = {
                    message = repository.deleteCategory(category.id) ?: "分类已删除"
                }
            )
        }
    }
}

@Composable
private fun CategoryCreateScreen(
    repository: AppDataRepository,
    selectedType: RecordType,
    onSelectedTypeChange: (RecordType) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val data by repository.data.collectAsState()
    var mode by rememberSaveable { mutableStateOf(CreateMode.CATEGORY) }
    var name by rememberSaveable { mutableStateOf("") }
    var selectedGroup by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val groups = data.categories
        .filter { it.type == selectedType }
        .map { it.groupName }
        .filter { it.isNotBlank() }
        .distinct()
        .sorted()

    LaunchedEffect(selectedType, groups) {
        if (selectedGroup !in groups) selectedGroup = groups.firstOrNull().orEmpty()
    }

    LazyColumn(
        modifier = modifier.padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onBack) { Text("<") }
                Text("新建分类", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedType == RecordType.EXPENSE,
                    onClick = {
                        onSelectedTypeChange(RecordType.EXPENSE)
                        error = null
                    },
                    label = { Text("支出") }
                )
                FilterChip(
                    selected = selectedType == RecordType.INCOME,
                    onClick = {
                        onSelectedTypeChange(RecordType.INCOME)
                        error = null
                    },
                    label = { Text("收入") }
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = mode == CreateMode.CATEGORY,
                    onClick = {
                        mode = CreateMode.CATEGORY
                        error = null
                    },
                    label = { Text("分类") }
                )
                FilterChip(
                    selected = mode == CreateMode.GROUP,
                    onClick = {
                        mode = CreateMode.GROUP
                        error = null
                    },
                    label = { Text("分组") }
                )
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            error = null
                        },
                        label = { Text(if (mode == CreateMode.GROUP) "分组名称" else "分类名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (mode == CreateMode.CATEGORY) {
                        Text("所属分组", fontWeight = FontWeight.SemiBold)
                        if (groups.isEmpty()) {
                            Text("请先新建分组", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            androidx.compose.foundation.layout.FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                groups.forEach { group ->
                                    FilterChip(
                                        selected = selectedGroup == group,
                                        onClick = {
                                            selectedGroup = group
                                            error = null
                                        },
                                        label = { Text(group) }
                                    )
                                }
                            }
                        }
                    }
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val result = if (mode == CreateMode.GROUP) {
                                repository.addCategoryGroup(name, selectedType)
                            } else {
                                repository.addCategory(name, selectedType, selectedGroup)
                            }
                            if (result == null) {
                                onBack()
                            } else {
                                error = result
                            }
                        }
                    ) { Text("添加") }
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(
    category: Category,
    index: Int,
    lastIndex: Int,
    editName: String,
    onEditName: (String) -> Unit,
    onSave: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(if (category.isSystem) "系统预置分类" else "自定义分类", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("分组：${category.groupName.ifBlank { "未分组" }}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = editName,
                    onValueChange = onEditName,
                    label = { Text("名称") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = onSave) { Text("保存") }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (category.enabled) "记账页显示" else "记账页隐藏")
                Switch(
                    checked = category.enabled,
                    onCheckedChange = onEnabledChange
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(enabled = index > 0, onClick = onMoveUp) { Text("上移") }
                OutlinedButton(enabled = index in 0 until lastIndex, onClick = onMoveDown) { Text("下移") }
                OutlinedButton(onClick = onDelete) { Text("删除") }
            }
        }
    }
}

private fun Category.isGroupPlaceholder(): Boolean = name.startsWith("__group__")

package com.komugi.komugiaccounting.ui.category

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.komugi.komugiaccounting.data.model.Category
import com.komugi.komugiaccounting.data.model.RecordType
import com.komugi.komugiaccounting.data.repository.AppDataRepository
import com.komugi.komugiaccounting.ui.components.CategoryIconBadge

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
    val styleStates = remember { mutableStateMapOf<String, StyleEditState>() }
    val grouped = data.categories
        .filter { it.type == selectedType }
        .groupBy { it.groupName.ifBlank { "未分组" } }
        .toSortedMap()

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
        grouped.forEach { (groupName, groupCategories) ->
            val visibleCategories = groupCategories.filterNot { it.isGroupPlaceholder() }.sortedBy { it.sortOrder }
            val groupMeta = groupCategories.firstOrNull { it.isGroupPlaceholder() } ?: visibleCategories.firstOrNull()
            item(key = "group-$selectedType-$groupName") {
                GroupStyleCard(
                    type = selectedType,
                    groupName = groupName,
                    meta = groupMeta,
                    state = styleStates.getOrPut("group-$selectedType-$groupName") {
                        StyleEditState(
                            iconName = groupMeta?.iconName.orEmpty().ifBlank { groupName.firstIconText() },
                            color = groupMeta?.color ?: defaultColor(selectedType),
                            iconImageUri = groupMeta?.iconImageUri.orEmpty()
                        )
                    },
                    onStateChange = { styleStates["group-$selectedType-$groupName"] = it },
                    onSave = { state ->
                        message = repository.updateCategoryGroupStyle(selectedType, groupName, state.iconName, state.color, state.iconImageUri)
                    }
                )
            }
            items(visibleCategories, key = { it.id }) { category ->
                val index = visibleCategories.indexOfFirst { it.id == category.id }
                val state = styleStates.getOrPut(category.id) {
                    StyleEditState(
                        iconName = category.iconName.ifBlank { category.name.firstIconText() },
                        color = category.color.ifBlank { defaultColor(selectedType) },
                        iconImageUri = category.iconImageUri
                    )
                }
                CategoryRow(
                    category = category,
                    index = index,
                    lastIndex = visibleCategories.lastIndex,
                    editName = editingNames.getOrPut(category.id) { category.name },
                    styleState = state,
                    onEditName = { editingNames[category.id] = it },
                    onStyleChange = { styleStates[category.id] = it },
                    onSave = {
                        message = repository.updateCategory(category.id, editingNames[category.id].orEmpty())
                            ?: repository.updateCategoryStyle(category.id, state.iconName, state.color, state.iconImageUri)
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
}

@OptIn(ExperimentalLayoutApi::class)
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
                FilterChip(selected = selectedType == RecordType.EXPENSE, onClick = { onSelectedTypeChange(RecordType.EXPENSE); error = null }, label = { Text("支出") })
                FilterChip(selected = selectedType == RecordType.INCOME, onClick = { onSelectedTypeChange(RecordType.INCOME); error = null }, label = { Text("收入") })
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = mode == CreateMode.CATEGORY, onClick = { mode = CreateMode.CATEGORY; error = null }, label = { Text("分类") })
                FilterChip(selected = mode == CreateMode.GROUP, onClick = { mode = CreateMode.GROUP; error = null }, label = { Text("分组") })
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
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                groups.forEach { group ->
                                    FilterChip(selected = selectedGroup == group, onClick = { selectedGroup = group; error = null }, label = { Text(group) })
                                }
                            }
                        }
                    }
                    Text("默认图标会使用名称的第一个字。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val result = if (mode == CreateMode.GROUP) {
                                repository.addCategoryGroup(name, selectedType)
                            } else {
                                repository.addCategory(name, selectedType, selectedGroup)
                            }
                            if (result == null) onBack() else error = result
                        }
                    ) { Text("添加") }
                }
            }
        }
    }
}

@Composable
private fun GroupStyleCard(
    type: RecordType,
    groupName: String,
    meta: Category?,
    state: StyleEditState,
    onStateChange: (StyleEditState) -> Unit,
    onSave: (StyleEditState) -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                CategoryIconBadge(
                    name = groupName,
                    iconName = state.iconName,
                    color = state.color,
                    iconImageUri = state.iconImageUri
                )
                Column(Modifier.weight(1f)) {
                    Text(groupName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    Text(if (type == RecordType.EXPENSE) "支出分组" else "收入分组", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            StyleEditor(
                name = groupName,
                state = state,
                onStateChange = onStateChange,
                onSave = { onSave(state) }
            )
            if (meta == null) {
                Text("这个分组还没有分类。", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    styleState: StyleEditState,
    onEditName: (String) -> Unit,
    onStyleChange: (StyleEditState) -> Unit,
    onSave: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                CategoryIconBadge(
                    name = editName,
                    iconName = styleState.iconName,
                    color = styleState.color,
                    iconImageUri = styleState.iconImageUri
                )
                Column(Modifier.weight(1f)) {
                    Text(if (category.isSystem) "系统预置分类" else "自定义分类", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("分组：${category.groupName.ifBlank { "未分组" }}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = editName, onValueChange = onEditName, label = { Text("名称") }, singleLine = true, modifier = Modifier.weight(1f))
                Button(onClick = onSave) { Text("保存") }
            }
            StyleEditor(
                name = editName,
                state = styleState,
                onStateChange = onStyleChange,
                onSave = onSave
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (category.enabled) "记账页显示" else "记账页隐藏")
                Switch(checked = category.enabled, onCheckedChange = onEnabledChange)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(enabled = index > 0, onClick = onMoveUp) { Text("上移") }
                OutlinedButton(enabled = index in 0 until lastIndex, onClick = onMoveDown) { Text("下移") }
                OutlinedButton(onClick = onDelete) { Text("删除") }
            }
        }
    }
}

@Composable
private fun StyleEditor(
    name: String,
    state: StyleEditState,
    onStateChange: (StyleEditState) -> Unit,
    onSave: () -> Unit
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        onStateChange(state.copy(iconImageUri = uri.toString()))
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = state.iconName,
                onValueChange = { onStateChange(state.copy(iconName = it)) },
                label = { Text("图标文字") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = state.color,
                onValueChange = { onStateChange(state.copy(color = it)) },
                label = { Text("主题色") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { launcher.launch(arrayOf("image/*")) }) { Text("选择图片") }
            OutlinedButton(onClick = { onStateChange(state.copy(iconImageUri = "", iconName = name.firstIconText())) }) { Text("清除图片") }
            Button(onClick = onSave) { Text("保存样式") }
        }
    }
}

private data class StyleEditState(
    val iconName: String,
    val color: String,
    val iconImageUri: String
)

private fun Category.isGroupPlaceholder(): Boolean = name.startsWith("__group__")

private fun String.firstIconText(): String = trim().firstOrNull()?.toString().orEmpty()

private fun defaultColor(type: RecordType): String =
    if (type == RecordType.EXPENSE) "#FF7043" else "#66BB6A"

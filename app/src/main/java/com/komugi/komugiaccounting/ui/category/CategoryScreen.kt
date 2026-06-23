package com.komugi.komugiaccounting.ui.category

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.komugi.komugiaccounting.data.model.Category
import com.komugi.komugiaccounting.data.model.RecordType
import com.komugi.komugiaccounting.data.repository.AppDataRepository
import com.komugi.komugiaccounting.ui.components.CategoryIconBadge

private enum class CreateMode { GROUP, CATEGORY }
private enum class ColorPickerMode { RGB, HSV }

@Composable
fun CategoryScreen(repository: AppDataRepository, onBack: () -> Unit, modifier: Modifier = Modifier) {
    var selectedType by rememberSaveable { mutableStateOf(RecordType.EXPENSE) }
    var isCreating by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = isCreating) { isCreating = false }
    if (isCreating) {
        CategoryCreateScreen(repository, selectedType, { selectedType = it }, { isCreating = false }, modifier)
    } else {
        CategoryListScreen(repository, selectedType, { selectedType = it }, onBack, { isCreating = true }, modifier)
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
    var editingGroupKey by rememberSaveable { mutableStateOf<String?>(null) }
    var groupDraft by rememberSaveable { mutableStateOf<GroupEditState?>(null) }
    var pendingGroupCancel by rememberSaveable { mutableStateOf<String?>(null) }
    var editingCategoryId by rememberSaveable { mutableStateOf<String?>(null) }
    var categoryDraft by rememberSaveable { mutableStateOf<CategoryEditState?>(null) }
    val grouped = data.categories.filter { it.type == selectedType }.groupBy { it.groupName.ifBlank { "未分组" } }.toSortedMap()

    fun saveGroup(oldGroupName: String, draft: GroupEditState): Boolean {
        val result = repository.updateCategoryGroup(
            type = selectedType,
            oldGroupName = oldGroupName,
            newGroupName = draft.name,
            iconName = draft.iconName.ifBlank { draft.name.firstIconText() },
            color = draft.color,
            iconImageUri = draft.iconImageUri,
            enabled = draft.enabled
        )
        message = result ?: "分组已保存"
        if (result == null) {
            editingGroupKey = null
            groupDraft = null
            return true
        }
        return false
    }

    fun saveCategory(categoryId: String, draft: CategoryEditState): Boolean {
        val result = repository.updateCategory(categoryId, draft.name)
            ?: repository.updateCategoryStyle(categoryId, draft.iconName.ifBlank { draft.name.firstIconText() }, draft.color, draft.iconImageUri)
        message = result ?: "分类已保存"
        if (result == null) {
            repository.setCategoryEnabled(categoryId, draft.enabled)
            editingCategoryId = null
            categoryDraft = null
            return true
        }
        return false
    }

    LazyColumn(modifier = modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = onBack) { Text("<") }
                    Text("分类管理", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                }
                Button(onClick = onCreate) { Text("新建") }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = selectedType == RecordType.EXPENSE, onClick = { onSelectedTypeChange(RecordType.EXPENSE); message = null }, label = { Text("支出分类") })
                FilterChip(selected = selectedType == RecordType.INCOME, onClick = { onSelectedTypeChange(RecordType.INCOME); message = null }, label = { Text("收入分类") })
            }
        }
        message?.let { item { Text(it, color = if (it.contains("已")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) } }
        grouped.forEach { (groupName, groupCategories) ->
            val visibleCategories = groupCategories.filterNot { it.isGroupPlaceholder() }.sortedBy { it.sortOrder }
            val groupMeta = groupCategories.firstOrNull { it.isGroupPlaceholder() } ?: visibleCategories.firstOrNull()
            val groupEnabled = visibleCategories.ifEmpty { groupCategories }.any { it.enabled }
            val groupKey = "$selectedType-$groupName"
            item("group-$groupKey") {
                if (editingGroupKey == groupKey && groupDraft != null) {
                    GroupEditCard(
                        original = GroupEditState.from(groupName, groupMeta, groupEnabled, selectedType),
                        draft = groupDraft!!,
                        onDraftChange = { groupDraft = it },
                        onSave = { saveGroup(groupName, it) },
                        onCancel = {
                            if (it != GroupEditState.from(groupName, groupMeta, groupEnabled, selectedType)) {
                                pendingGroupCancel = groupKey
                            } else {
                                editingGroupKey = null
                                groupDraft = null
                            }
                        }
                    )
                } else {
                    GroupInfoCard(
                        type = selectedType,
                        groupName = groupName,
                        meta = groupMeta,
                        categoryCount = visibleCategories.size,
                        enabled = groupEnabled,
                        onEnabledChange = { repository.setCategoryGroupEnabled(selectedType, groupName, it); message = null },
                        onEdit = { editingGroupKey = groupKey; groupDraft = GroupEditState.from(groupName, groupMeta, groupEnabled, selectedType) }
                    )
                }
            }
            items(visibleCategories, key = { it.id }) { category ->
                if (editingCategoryId == category.id && categoryDraft != null) {
                    CategoryEditCard(
                        category = category,
                        index = visibleCategories.indexOfFirst { it.id == category.id },
                        lastIndex = visibleCategories.lastIndex,
                        draft = categoryDraft!!,
                        onDraftChange = { categoryDraft = it },
                        onSave = { saveCategory(category.id, it) },
                        onCancel = { editingCategoryId = null; categoryDraft = null },
                        onMoveUp = { repository.moveCategory(category.id, -1); message = null },
                        onMoveDown = { repository.moveCategory(category.id, 1); message = null },
                        onDelete = {
                            message = repository.deleteCategory(category.id) ?: "分类已删除"
                            editingCategoryId = null
                            categoryDraft = null
                        }
                    )
                } else {
                    CategoryInfoCard(
                        category = category,
                        onEnabledChange = { repository.setCategoryEnabled(category.id, it); message = null },
                        onEdit = { editingCategoryId = category.id; categoryDraft = CategoryEditState.from(category, selectedType) }
                    )
                }
            }
        }
    }

    val pendingKey = pendingGroupCancel
    val draft = groupDraft
    if (pendingKey != null && draft != null) {
        AlertDialog(
            onDismissRequest = { pendingGroupCancel = null },
            title = { Text("应用本次修改？") },
            text = { Text("分组信息已经修改，是否保存本次修改？") },
            confirmButton = { Button(onClick = { if (saveGroup(pendingKey.substringAfter("-"), draft)) pendingGroupCancel = null }) { Text("保存") } },
            dismissButton = { OutlinedButton(onClick = { pendingGroupCancel = null; editingGroupKey = null; groupDraft = null }) { Text("取消修改") } }
        )
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
    val groups = data.categories.filter { it.type == selectedType }.map { it.groupName }.filter { it.isNotBlank() }.distinct().sorted()
    LaunchedEffect(selectedType, groups) { if (selectedGroup !in groups) selectedGroup = groups.firstOrNull().orEmpty() }
    LazyColumn(modifier = modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Header("新建分类", onBack) }
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
                    OutlinedTextField(value = name, onValueChange = { name = it; error = null }, label = { Text(if (mode == CreateMode.GROUP) "分组名称" else "分类名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    if (mode == CreateMode.CATEGORY) {
                        Text("所属分组", fontWeight = FontWeight.SemiBold)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            groups.forEach { group -> FilterChip(selected = selectedGroup == group, onClick = { selectedGroup = group; error = null }, label = { Text(group) }) }
                        }
                    }
                    Text("默认图标会使用名称的第一个字。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Button(modifier = Modifier.fillMaxWidth(), onClick = {
                        val result = if (mode == CreateMode.GROUP) repository.addCategoryGroup(name, selectedType) else repository.addCategory(name, selectedType, selectedGroup)
                        if (result == null) onBack() else error = result
                    }) { Text("添加") }
                }
            }
        }
    }
}

@Composable
private fun Header(title: String, onBack: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = onBack) { Text("<") }
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun GroupInfoCard(type: RecordType, groupName: String, meta: Category?, categoryCount: Int, enabled: Boolean, onEnabledChange: (Boolean) -> Unit, onEdit: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().alpha(if (enabled) 1f else 0.48f).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            CategoryIconBadge(name = groupName, iconName = groupName.firstIconText(), color = meta?.color ?: defaultColor(type), iconImageUri = meta?.iconImageUri.orEmpty())
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(groupName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Text(if (type == RecordType.EXPENSE) "支出分组" else "收入分组", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${categoryCount}个分类", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = enabled, onCheckedChange = onEnabledChange)
            OutlinedButton(onClick = onEdit) { Text("编辑") }
        }
    }
}

@Composable
private fun GroupEditCard(original: GroupEditState, draft: GroupEditState, onDraftChange: (GroupEditState) -> Unit, onSave: (GroupEditState) -> Unit, onCancel: (GroupEditState) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                CategoryIconBadge(name = draft.name, iconName = draft.iconName.ifBlank { draft.name.firstIconText() }, color = draft.color, iconImageUri = draft.iconImageUri)
                Text("编辑分组", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
            }
            OutlinedTextField(value = draft.name, onValueChange = { onDraftChange(draft.copy(name = it)) }, label = { Text("分组名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            StyleEditor(name = draft.name, state = StyleEditState(draft.iconName.ifBlank { draft.name.firstIconText() }, draft.color, draft.iconImageUri), onStateChange = { onDraftChange(draft.copy(iconName = it.iconName, color = it.color, iconImageUri = it.iconImageUri)) }, showIconText = true)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (draft.enabled) "分组启用" else "分组停用")
                Switch(checked = draft.enabled, onCheckedChange = { onDraftChange(draft.copy(enabled = it)) })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onSave(draft) }) { Text("保存") }
                OutlinedButton(onClick = { onCancel(draft) }) { Text(if (draft == original) "返回" else "取消") }
            }
        }
    }
}

@Composable
private fun CategoryInfoCard(category: Category, onEnabledChange: (Boolean) -> Unit, onEdit: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().alpha(if (category.enabled) 1f else 0.48f).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            CategoryIconBadge(category = category)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(category.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("分组：${category.groupName.ifBlank { "未分组" }}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = category.enabled, onCheckedChange = onEnabledChange)
            OutlinedButton(onClick = onEdit) { Text("编辑") }
        }
    }
}

@Composable
private fun CategoryEditCard(
    category: Category,
    index: Int,
    lastIndex: Int,
    draft: CategoryEditState,
    onDraftChange: (CategoryEditState) -> Unit,
    onSave: (CategoryEditState) -> Unit,
    onCancel: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                CategoryIconBadge(name = draft.name, iconName = draft.iconName.ifBlank { draft.name.firstIconText() }, color = draft.color, iconImageUri = draft.iconImageUri)
                Column(Modifier.weight(1f)) {
                    Text("编辑分类", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    Text("分组：${category.groupName.ifBlank { "未分组" }}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            OutlinedTextField(value = draft.name, onValueChange = { onDraftChange(draft.copy(name = it)) }, label = { Text("分类名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            StyleEditor(name = draft.name, state = StyleEditState(draft.iconName.ifBlank { draft.name.firstIconText() }, draft.color, draft.iconImageUri), onStateChange = { onDraftChange(draft.copy(iconName = it.iconName, color = it.color, iconImageUri = it.iconImageUri)) }, showIconText = true)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (draft.enabled) "记账页显示" else "记账页隐藏")
                Switch(checked = draft.enabled, onCheckedChange = { onDraftChange(draft.copy(enabled = it)) })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(enabled = index > 0, onClick = onMoveUp) { Text("上移") }
                OutlinedButton(enabled = index in 0 until lastIndex, onClick = onMoveDown) { Text("下移") }
                OutlinedButton(onClick = onDelete) { Text("删除") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onSave(draft) }) { Text("保存") }
                OutlinedButton(onClick = onCancel) { Text("取消") }
            }
        }
    }
}

@Composable
private fun StyleEditor(name: String, state: StyleEditState, onStateChange: (StyleEditState) -> Unit, showIconText: Boolean) {
    val context = LocalContext.current
    var showColorPicker by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        onStateChange(state.copy(iconImageUri = uri.toString()))
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (showIconText) {
                OutlinedTextField(value = state.iconName, onValueChange = { onStateChange(state.copy(iconName = it)) }, label = { Text("图标文字") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            OutlinedTextField(value = state.color, onValueChange = { onStateChange(state.copy(color = it)) }, label = { Text("主题色") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = { showColorPicker = true }) { Text("取色") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { launcher.launch(arrayOf("image/*")) }) { Text("选择图片") }
            OutlinedButton(onClick = { onStateChange(state.copy(iconImageUri = "", iconName = name.firstIconText())) }) { Text("清除图片") }
        }
    }
    if (showColorPicker) {
        ColorPickerDialog(initialColor = state.color, onDismiss = { showColorPicker = false }, onConfirm = { onStateChange(state.copy(color = it)); showColorPicker = false })
    }
}

@Composable
private fun ColorPickerDialog(initialColor: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    val initialRgb = remember(initialColor) { parseHexColorToRgb(initialColor) }
    val initialHsv = remember(initialRgb) { rgbToHsv(initialRgb[0], initialRgb[1], initialRgb[2]) }
    var mode by rememberSaveable(initialColor) { mutableStateOf(ColorPickerMode.RGB) }
    var red by rememberSaveable(initialColor) { mutableStateOf(initialRgb[0].toFloat()) }
    var green by rememberSaveable(initialColor) { mutableStateOf(initialRgb[1].toFloat()) }
    var blue by rememberSaveable(initialColor) { mutableStateOf(initialRgb[2].toFloat()) }
    var hue by rememberSaveable(initialColor) { mutableStateOf(initialHsv[0]) }
    var saturation by rememberSaveable(initialColor) { mutableStateOf(initialHsv[1]) }
    var value by rememberSaveable(initialColor) { mutableStateOf(initialHsv[2]) }
    fun syncRgbFromHsv() { hsvToRgb(hue, saturation, value).also { red = it[0].toFloat(); green = it[1].toFloat(); blue = it[2].toFloat() } }
    fun syncHsvFromRgb() { rgbToHsv(red.toInt(), green.toInt(), blue.toInt()).also { hue = it[0]; saturation = it[1]; value = it[2] } }
    val hexColor = rgbToHex(red.toInt(), green.toInt(), blue.toInt())
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择颜色") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.fillMaxWidth().height(54.dp).background(Color(red.toInt(), green.toInt(), blue.toInt())))
                Text(hexColor, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = mode == ColorPickerMode.RGB, onClick = { mode = ColorPickerMode.RGB; syncRgbFromHsv() }, label = { Text("RGB") })
                    FilterChip(selected = mode == ColorPickerMode.HSV, onClick = { mode = ColorPickerMode.HSV; syncHsvFromRgb() }, label = { Text("HSV") })
                }
                if (mode == ColorPickerMode.RGB) {
                    ColorSlider("R", red, 0f..255f) { red = it; syncHsvFromRgb() }
                    ColorSlider("G", green, 0f..255f) { green = it; syncHsvFromRgb() }
                    ColorSlider("B", blue, 0f..255f) { blue = it; syncHsvFromRgb() }
                } else {
                    ColorSlider("H", hue, 0f..360f) { hue = it; syncRgbFromHsv() }
                    ColorSlider("S", saturation, 0f..1f) { saturation = it; syncRgbFromHsv() }
                    ColorSlider("V", value, 0f..1f) { value = it; syncRgbFromHsv() }
                }
            }
        },
        confirmButton = { Button(onClick = { onConfirm(hexColor) }) { Text("确定") } },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun ColorSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("$label ${if (range.endInclusive <= 1f) "%.2f".format(value) else value.toInt()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(value = value.coerceIn(range.start, range.endInclusive), onValueChange = onChange, valueRange = range)
    }
}

private data class StyleEditState(val iconName: String, val color: String, val iconImageUri: String)
private data class GroupEditState(val name: String, val iconName: String, val color: String, val iconImageUri: String, val enabled: Boolean) {
    companion object {
        fun from(groupName: String, meta: Category?, enabled: Boolean, type: RecordType) =
            GroupEditState(groupName, groupName.firstIconText(), meta?.color ?: defaultColor(type), meta?.iconImageUri.orEmpty(), enabled)
    }
}
private data class CategoryEditState(val name: String, val iconName: String, val color: String, val iconImageUri: String, val enabled: Boolean) {
    companion object {
        fun from(category: Category, type: RecordType) =
            CategoryEditState(category.name, category.iconName.ifBlank { category.name.firstIconText() }, category.color.ifBlank { defaultColor(type) }, category.iconImageUri, category.enabled)
    }
}

private fun Category.isGroupPlaceholder(): Boolean = name.startsWith("__group__")
private fun String.firstIconText(): String = trim().firstOrNull()?.toString().orEmpty()
private fun defaultColor(type: RecordType): String = if (type == RecordType.EXPENSE) "#FF7043" else "#66BB6A"
private fun parseHexColorToRgb(value: String): IntArray {
    val color = runCatching { android.graphics.Color.parseColor(value) }.getOrDefault(android.graphics.Color.parseColor("#9E9E9E"))
    return intArrayOf(android.graphics.Color.red(color), android.graphics.Color.green(color), android.graphics.Color.blue(color))
}
private fun rgbToHex(red: Int, green: Int, blue: Int): String = "#%02X%02X%02X".format(red.coerceIn(0, 255), green.coerceIn(0, 255), blue.coerceIn(0, 255))
private fun rgbToHsv(red: Int, green: Int, blue: Int): FloatArray = FloatArray(3).also { android.graphics.Color.RGBToHSV(red.coerceIn(0, 255), green.coerceIn(0, 255), blue.coerceIn(0, 255), it) }
private fun hsvToRgb(hue: Float, saturation: Float, value: Float): IntArray {
    val color = android.graphics.Color.HSVToColor(floatArrayOf(hue.coerceIn(0f, 360f), saturation.coerceIn(0f, 1f), value.coerceIn(0f, 1f)))
    return intArrayOf(android.graphics.Color.red(color), android.graphics.Color.green(color), android.graphics.Color.blue(color))
}

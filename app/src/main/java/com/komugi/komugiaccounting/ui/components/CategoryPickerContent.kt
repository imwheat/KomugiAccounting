package com.komugi.komugiaccounting.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import com.komugi.komugiaccounting.data.model.Category

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryPickerContent(
    categories: List<Category>,
    recentCategoryIds: List<String>,
    onBack: () -> Unit,
    onSelect: (Category) -> Unit,
    selectedCategoryIds: Set<String> = emptySet(),
    modifier: Modifier = Modifier
) {
    val visibleCategories = categories.filterNot { it.isGroupPlaceholder() }
    val categoryById = visibleCategories.associateBy { it.id }
    val recentCategories = recentCategoryIds.mapNotNull { categoryById[it] }.take(10)
    val grouped = visibleCategories
        .groupBy { it.groupName.ifBlank { "未分组" } }
        .toSortedMap(compareBy { categoryGroupOrder(it) })

    LazyColumn(
        modifier = modifier.padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
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
            item {
                CategoryGroupSection(
                    title = "最近选择",
                    categories = recentCategories,
                    selectedCategoryIds = selectedCategoryIds,
                    onSelect = onSelect
                )
            }
        }
        grouped.forEach { (groupName, groupCategories) ->
            item {
                CategoryGroupSection(
                    title = groupName,
                    categories = groupCategories.sortedBy { it.sortOrder },
                    selectedCategoryIds = selectedCategoryIds,
                    onSelect = onSelect
                )
            }
        }
    }
}

fun categoryDisplayPath(category: Category): String =
    if (category.groupName.isBlank()) category.name else "${category.groupName} > ${category.name}"

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryGroupSection(
    title: String,
    categories: List<Category>,
    selectedCategoryIds: Set<String>,
    onSelect: (Category) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            categories.forEach { category ->
                CategoryOptionButton(
                    category = category,
                    selected = category.id in selectedCategoryIds,
                    onClick = { onSelect(category) }
                )
            }
        }
    }
}

@Composable
private fun CategoryOptionButton(category: Category, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .defaultMinSize(minWidth = 72.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else panelBackgroundColor()
        ),
        border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            CategoryIconBadge(category = category, size = 42.dp)
            Text(
                category.name,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

fun categoryGroupOrder(groupName: String): Int = when (groupName) {
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

private fun Category.isGroupPlaceholder(): Boolean = name.startsWith("__group__")

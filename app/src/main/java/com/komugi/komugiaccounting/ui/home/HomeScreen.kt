package com.komugi.komugiaccounting.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.komugi.komugiaccounting.ui.components.RecordItem
import com.komugi.komugiaccounting.ui.components.StatCard
import com.komugi.komugiaccounting.util.AmountUtil

@Composable
fun HomeScreen(viewModel: HomeViewModel, modifier: Modifier = Modifier) {
    val data by viewModel.data.collectAsState()
    val categories = data.categories.associateBy { it.id }
    val members = data.members.associateBy { it.id }
    var monthOffset by rememberSaveable { mutableIntStateOf(0) }
    val highestExpense = viewModel.highestExpenseCategory(data, monthOffset)

    LazyColumn(
        modifier = modifier.padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Spacer(Modifier.height(10.dp)) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("小麦账本", fontSize = 34.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground)
                Text("本地保存的个人收支记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = monthOffset == 0, onClick = { monthOffset = 0 }, label = { Text("本月") })
                FilterChip(selected = monthOffset == -1, onClick = { monthOffset = -1 }, label = { Text("上月") })
            }
        }
        item { StatCard(viewModel.monthTitle(monthOffset), viewModel.monthStat(data, monthOffset)) }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("最高支出分类", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (highestExpense == null) {
                        Text("这个月份还没有支出记录。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text(highestExpense.first.name, fontWeight = FontWeight.Black, fontSize = 22.sp)
                        Text(AmountUtil.format(highestExpense.second), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item { StatCard("今日统计", viewModel.todayStat(data)) }
        item { StatCard("本周统计", viewModel.weekStat(data)) }
        item { StatCard("本年统计", viewModel.yearStat(data)) }
        item { Text("最近记录", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        if (data.records.isEmpty()) {
            item { Text("还没有账目，点击底部 + 记第一笔。", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(12.dp)) }
        } else {
            items(data.records.take(8), key = { it.id }) { record ->
                RecordItem(record, categories[record.categoryId], members[record.memberId])
            }
        }
        item { Spacer(Modifier.height(88.dp)) }
    }
}

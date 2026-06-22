package com.komugi.komugiaccounting.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.komugi.komugiaccounting.ui.components.RecordItem
import com.komugi.komugiaccounting.ui.components.StatCard

@Composable
fun HomeScreen(viewModel: HomeViewModel, modifier: Modifier = Modifier) {
    val data by viewModel.data.collectAsState()
    val categories = data.categories.associateBy { it.id }
    val members = data.members.associateBy { it.id }

    LazyColumn(
        modifier = modifier.padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Spacer(Modifier.height(10.dp)) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("小麦账本", fontSize = 34.sp, fontWeight = FontWeight.Black, color = Color(0xFF263226))
                Text("本地保存的个人收支记录", color = Color(0xFF5E665B))
            }
        }
        item { StatCard("本月统计", viewModel.monthStat(data)) }
        item { StatCard("今日统计", viewModel.todayStat(data)) }
        item { StatCard("本周统计", viewModel.weekStat(data)) }
        item { StatCard("本年统计", viewModel.yearStat(data)) }
        item { Text("最近记录", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        if (data.records.isEmpty()) {
            item { Text("还没有账目，点击底部 + 记第一笔。", color = Color(0xFF697066), modifier = Modifier.padding(12.dp)) }
        } else {
            items(data.records.take(8), key = { it.id }) { record ->
                RecordItem(record, categories[record.categoryId], members[record.memberId])
            }
        }
        item { Spacer(Modifier.height(88.dp)) }
    }
}

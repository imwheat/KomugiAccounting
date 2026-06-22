package com.komugi.komugiaccounting.ui.detail

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.komugi.komugiaccounting.ui.components.RecordItem

@Composable
fun DetailScreen(viewModel: DetailViewModel, modifier: Modifier = Modifier) {
    val data by viewModel.data.collectAsState()
    val categories = data.categories.associateBy { it.id }
    val members = data.members.associateBy { it.id }

    LazyColumn(
        modifier = modifier.padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(Modifier.height(10.dp)) }
        item { Text("账目明细", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black) }
        if (data.records.isEmpty()) {
            item { Text("暂无明细记录。", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(12.dp)) }
        } else {
            items(data.records, key = { it.id }) { record ->
                RecordItem(record, categories[record.categoryId], members[record.memberId])
            }
        }
        item { Spacer(Modifier.height(88.dp)) }
    }
}

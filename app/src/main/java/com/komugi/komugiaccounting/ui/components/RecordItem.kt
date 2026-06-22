package com.komugi.komugiaccounting.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.komugi.komugiaccounting.data.model.Category
import com.komugi.komugiaccounting.data.model.Member
import com.komugi.komugiaccounting.data.model.RecordType
import com.komugi.komugiaccounting.data.model.TransactionRecord
import com.komugi.komugiaccounting.util.AmountUtil
import com.komugi.komugiaccounting.util.DateTimeUtil

@Composable
fun RecordItem(
    record: TransactionRecord,
    category: Category?,
    member: Member?,
    modifier: Modifier = Modifier
) {
    val isIncome = record.type == RecordType.INCOME
    val color = if (isIncome) Color(0xFF1F7A4D) else Color(0xFFB3542E)
    val sign = if (isIncome) "+" else "-"
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${category?.name ?: "未分类"} · ${member?.name ?: "未知成员"}", fontWeight = FontWeight.Bold)
                Text(DateTimeUtil.formatDisplayDateTime(record.dateTime), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                if (record.remark.isNotBlank()) Text(record.remark, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("$sign${AmountUtil.format(record.amount)}", color = color, fontWeight = FontWeight.Black, fontSize = 18.sp)
        }
    }
}

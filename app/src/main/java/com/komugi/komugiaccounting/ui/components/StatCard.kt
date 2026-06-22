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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.komugi.komugiaccounting.data.model.StatResult
import com.komugi.komugiaccounting.util.AmountUtil

@Composable
fun StatCard(title: String, stat: StatResult, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.92f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                StatValue("支出", stat.expense, Color(0xFFB3542E))
                StatValue("收入", stat.income, Color(0xFF1F7A4D))
                StatValue("结余", stat.balance, Color(0xFF2E5A87))
            }
        }
    }
}

@Composable
private fun StatValue(label: String, amount: Long, color: Color) {
    Column {
        Text(label, color = Color(0xFF697066), style = MaterialTheme.typography.labelMedium)
        Text(AmountUtil.format(amount), color = color, fontWeight = FontWeight.Black)
    }
}

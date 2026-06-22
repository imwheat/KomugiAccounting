package com.komugi.komugiaccounting.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun NumberKeyboard(
    value: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onCancel: () -> Unit = onDismiss,
    modifier: Modifier = Modifier
) {
    fun input(token: String) {
        onValueChange(appendAmountToken(value, token))
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.height(40.dp)) {
                    Text("取消")
                }
                IconButton(onClick = onDismiss) {
                    Text("⌄", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
            listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9")
            ).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { token ->
                        KeyButton(text = token, onClick = { input(token) }, modifier = Modifier.weight(1f))
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                KeyButton(text = ".", onClick = { input(".") }, modifier = Modifier.weight(1f))
                KeyButton(text = "0", onClick = { input("0") }, modifier = Modifier.weight(1f))
                KeyButton(text = "⌫", onClick = { onValueChange(value.dropLast(1)) }, modifier = Modifier.weight(1f))
            }
            OutlinedButton(
                onClick = { onValueChange("") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Text("清空")
            }
        }
    }
}

@Composable
private fun KeyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(50.dp)
    ) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}

private fun appendAmountToken(current: String, token: String): String {
    if (token == "." && current.contains(".")) return current
    val next = when {
        token == "." && current.isBlank() -> "0."
        current == "0" && token != "." -> token
        else -> current + token
    }
    val dotIndex = next.indexOf('.')
    return if (dotIndex >= 0 && next.length - dotIndex - 1 > 2) current else next
}

package com.komugi.komugiaccounting.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun NumberKeyboard(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    fun input(token: String) {
        onValueChange(appendAmountToken(value, token))
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
            modifier = Modifier.fillMaxWidth().height(46.dp)
        ) {
            Text("清空")
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

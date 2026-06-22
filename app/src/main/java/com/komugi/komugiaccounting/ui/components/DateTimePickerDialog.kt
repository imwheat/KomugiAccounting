package com.komugi.komugiaccounting.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.komugi.komugiaccounting.util.DateTimeUtil

@Composable
fun DateTimePickerDialog(
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val initialParts = initialValue.split(" ")
    var date by rememberSaveable(initialValue) {
        mutableStateOf(initialParts.getOrNull(0) ?: DateTimeUtil.formatDate(DateTimeUtil.now()))
    }
    var time by rememberSaveable(initialValue) {
        mutableStateOf(initialParts.getOrNull(1) ?: DateTimeUtil.formatTime(DateTimeUtil.now()))
    }
    val combined = "$date $time"
    val error = combined.takeIf { DateTimeUtil.parseDateTime(it) == null }?.let { "格式应为 yyyy-MM-dd HH:mm" }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择时间") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("日期 yyyy-MM-dd") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text("时间 HH:mm") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        val now = DateTimeUtil.now()
                        date = DateTimeUtil.formatDate(now)
                        time = DateTimeUtil.formatTime(now)
                    }) { Text("现在") }
                    OutlinedButton(onClick = {
                        date = DateTimeUtil.formatDate(DateTimeUtil.now())
                    }) { Text("今天") }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(
                enabled = error == null,
                onClick = { onConfirm(combined) }
            ) { Text("确认") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

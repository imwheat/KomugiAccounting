package com.komugi.komugiaccounting.ui.export

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.komugi.komugiaccounting.data.repository.AppDataRepository
import com.komugi.komugiaccounting.util.DateTimeUtil

@Composable
fun ExportScreen(
    repository: AppDataRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingImportJson by rememberSaveable { mutableStateOf<String?>(null) }

    val jsonExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(repository.exportJson().toByteArray(Charsets.UTF_8))
            } ?: error("无法打开导出文件")
        }.onSuccess {
            message = "JSON 备份已导出"
        }.onFailure {
            message = "导出失败：${it.message ?: "未知错误"}"
        }
    }

    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(repository.exportRecordsCsv().toByteArray(Charsets.UTF_8))
            } ?: error("无法打开导出文件")
        }.onSuccess {
            message = "账单 CSV 已导出"
        }.onFailure {
            message = "导出失败：${it.message ?: "未知错误"}"
        }
    }

    val xlsxExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(repository.exportWorkbookXlsx())
            } ?: error("无法打开导出文件")
        }.onSuccess {
            message = "Excel 工作簿已导出"
        }.onFailure {
            message = "导出失败：${it.message ?: "未知错误"}"
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                ?: error("无法读取文件")
        }.onSuccess {
            pendingImportJson = it
        }.onFailure {
            message = "读取失败：${it.message ?: "未知错误"}"
        }
    }

    Column(modifier = modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onBack) { Text("返回") }
            Text("导出与备份", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Excel 明细", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("导出 Excel 工作簿或 CSV 明细文件，可直接用表格软件打开。")
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val name = "komugi-report-${DateTimeUtil.formatDate(DateTimeUtil.now())}.xlsx"
                        xlsxExportLauncher.launch(name)
                    }
                ) { Text("导出 Excel 工作簿") }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val name = "komugi-records-${DateTimeUtil.formatDate(DateTimeUtil.now())}.csv"
                        csvExportLauncher.launch(name)
                    }
                ) { Text("导出账单 CSV") }
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("JSON 备份", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("导出的 JSON 包含账单、成员、分类、模板和设置。恢复会覆盖当前本地数据。")
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val name = "komugi-backup-${DateTimeUtil.formatDate(DateTimeUtil.now())}.json"
                        jsonExportLauncher.launch(name)
                    }
                ) { Text("导出 JSON 备份") }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { importLauncher.launch(arrayOf("application/json", "text/*", "*/*")) }
                ) { Text("从 JSON 恢复") }
            }
        }
        message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
    }

    pendingImportJson?.let { json ->
        AlertDialog(
            onDismissRequest = { pendingImportJson = null },
            title = { Text("确认恢复") },
            text = { Text("恢复会覆盖当前所有本地数据。") },
            confirmButton = {
                Button(onClick = {
                    repository.importJson(json)
                        .onSuccess { message = "JSON 备份已恢复" }
                        .onFailure { message = "恢复失败：${it.message ?: "格式不正确"}" }
                    pendingImportJson = null
                }) { Text("确认恢复") }
            },
            dismissButton = {
                OutlinedButton(onClick = { pendingImportJson = null }) { Text("取消") }
            }
        )
    }
}

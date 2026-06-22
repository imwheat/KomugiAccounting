package com.komugi.komugiaccounting.ui.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
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
import androidx.compose.material3.OutlinedTextField
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
import java.io.File
import java.util.Calendar

@Composable
fun ExportScreen(
    repository: AppDataRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingImportJson by rememberSaveable { mutableStateOf<String?>(null) }
    var startDate by rememberSaveable { mutableStateOf("") }
    var endDate by rememberSaveable { mutableStateOf("") }

    fun range(): ExportRange? {
        val start = startDate.takeIf { it.isNotBlank() }?.let { DateTimeUtil.parseDate(it) }
        val end = endDate.takeIf { it.isNotBlank() }?.let { DateTimeUtil.parseDate(it) }
            ?.let { DateTimeUtil.endExclusiveFromStart(it, Calendar.DAY_OF_MONTH, 1) }
        message = when {
            startDate.isNotBlank() && start == null -> "开始日期格式应为 yyyy-MM-dd"
            endDate.isNotBlank() && end == null -> "结束日期格式应为 yyyy-MM-dd"
            start != null && end != null && start >= end -> "开始日期必须早于结束日期"
            else -> null
        }
        return if (message == null) ExportRange(start, end) else null
    }

    fun saveToDownloads(fileName: String, mimeType: String, bytes: ByteArray) {
        runCatching {
            val uri = saveBytesToDownloads(context, fileName, mimeType, bytes)
            openExportResult(context, uri, mimeType)
            uri
        }.onSuccess {
            message = "已保存到 Downloads"
        }.onFailure {
            message = "保存失败：${it.message ?: "未知错误"}"
        }
    }

    val jsonExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(repository.exportJson().toByteArray(Charsets.UTF_8))
            } ?: error("无法打开导出文件")
            uri
        }.onSuccess {
            message = "JSON 备份已导出"
            openExportResult(context, it, "application/json")
        }.onFailure {
            message = "导出失败：${it.message ?: "未知错误"}"
        }
    }

    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val exportRange = range() ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(repository.exportRecordsCsv(exportRange.startTime, exportRange.endTime).toByteArray(Charsets.UTF_8))
            } ?: error("无法打开导出文件")
            uri
        }.onSuccess {
            message = "账单 CSV 已导出"
            openExportResult(context, it, "text/csv")
        }.onFailure {
            message = "导出失败：${it.message ?: "未知错误"}"
        }
    }

    val xlsxExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val exportRange = range() ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(repository.exportWorkbookXlsx(exportRange.startTime, exportRange.endTime))
            } ?: error("无法打开导出文件")
            uri
        }.onSuccess {
            message = "Excel 工作簿已导出"
            openExportResult(context, it, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
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
                Text("导出 Excel 工作簿或 CSV 明细文件。日期范围留空时导出全部记录。")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startDate,
                        onValueChange = { startDate = it },
                        label = { Text("开始日期") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endDate,
                        onValueChange = { endDate = it },
                        label = { Text("结束日期") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val exportRange = range() ?: return@Button
                        val name = "komugi-report-${DateTimeUtil.formatDate(DateTimeUtil.now())}.xlsx"
                        xlsxExportLauncher.launch(name)
                    }
                ) { Text("选择位置导出 Excel") }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val exportRange = range() ?: return@OutlinedButton
                        val name = "komugi-report-${DateTimeUtil.formatDate(DateTimeUtil.now())}.xlsx"
                        saveToDownloads(
                            fileName = name,
                            mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            bytes = repository.exportWorkbookXlsx(exportRange.startTime, exportRange.endTime)
                        )
                    }
                ) { Text("保存 Excel 到 Downloads") }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val exportRange = range() ?: return@Button
                        val name = "komugi-records-${DateTimeUtil.formatDate(DateTimeUtil.now())}.csv"
                        csvExportLauncher.launch(name)
                    }
                ) { Text("选择位置导出 CSV") }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val exportRange = range() ?: return@OutlinedButton
                        val name = "komugi-records-${DateTimeUtil.formatDate(DateTimeUtil.now())}.csv"
                        saveToDownloads(
                            fileName = name,
                            mimeType = "text/csv",
                            bytes = repository.exportRecordsCsv(exportRange.startTime, exportRange.endTime).toByteArray(Charsets.UTF_8)
                        )
                    }
                ) { Text("保存 CSV 到 Downloads") }
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
                ) { Text("选择位置导出 JSON") }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val name = "komugi-backup-${DateTimeUtil.formatDate(DateTimeUtil.now())}.json"
                        saveToDownloads(
                            fileName = name,
                            mimeType = "application/json",
                            bytes = repository.exportJson().toByteArray(Charsets.UTF_8)
                        )
                    }
                ) { Text("保存 JSON 到 Downloads") }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { importLauncher.launch(arrayOf("application/json", "text/*", "*/*")) }
                ) { Text("从 JSON 恢复") }
            }
        }
        message?.let {
            Text(it, color = if (it.contains("失败") || it.contains("格式") || it.contains("必须")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
        }
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

private data class ExportRange(
    val startTime: Long?,
    val endTime: Long?
)

private fun saveBytesToDownloads(context: Context, fileName: String, mimeType: String, bytes: ByteArray): Uri {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        return saveBytesToExternalExports(context, fileName, bytes)
    }

    val resolver = context.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
        put(MediaStore.Downloads.MIME_TYPE, mimeType)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Downloads.RELATIVE_PATH, "Download/KomugiAccounting")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
    }
    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: error("无法创建下载文件")
    runCatching {
        resolver.openOutputStream(uri)?.use { it.write(bytes) } ?: error("无法写入下载文件")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
    }.onFailure {
        resolver.delete(uri, null, null)
        throw it
    }
    return uri
}

private fun saveBytesToExternalExports(context: Context, fileName: String, bytes: ByteArray): Uri {
    val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        ?: context.getExternalFilesDir(null)
        ?: error("无法访问外部存储目录")
    val exportDir = File(baseDir, "exports").apply {
        if (!exists() && !mkdirs()) error("无法创建导出目录")
    }
    val exportFile = File(exportDir, fileName)
    exportFile.writeBytes(bytes)
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        exportFile
    )
}

private fun openExportResult(context: Context, uri: Uri, mimeType: String) {
    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(viewIntent, "打开或分享导出文件").apply {
        putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(shareIntent))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching { context.startActivity(chooser) }
}

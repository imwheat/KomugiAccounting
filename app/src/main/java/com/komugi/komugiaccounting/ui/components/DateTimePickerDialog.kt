package com.komugi.komugiaccounting.ui.components

import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import android.widget.TextView
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import com.komugi.komugiaccounting.R
import com.komugi.komugiaccounting.util.DateTimeUtil
import java.util.Calendar

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

@Composable
fun DatePickerDialog(
    initialDate: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val initialTime = DateTimeUtil.parseDate(initialDate) ?: DateTimeUtil.startOfDay()
    val calendar = Calendar.getInstance().apply { timeInMillis = initialTime }
    var year by rememberSaveable(initialDate) { mutableStateOf(calendar.get(Calendar.YEAR)) }
    var month by rememberSaveable(initialDate) { mutableStateOf(calendar.get(Calendar.MONTH)) }
    var day by rememberSaveable(initialDate) { mutableStateOf(calendar.get(Calendar.DAY_OF_MONTH)) }
    val containerColor = MaterialTheme.colorScheme.surface
    val contentColor = MaterialTheme.colorScheme.onSurface

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = containerColor,
        titleContentColor = contentColor,
        textContentColor = contentColor,
        title = { Text("选择日期") },
        text = {
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { context ->
                    android.widget.DatePicker(ContextThemeWrapper(context, R.style.Theme_KomugiAccounting_Picker)).apply {
                        setBackgroundColor(containerColor.toArgb())
                        setPickerTextColor(contentColor.toArgb())
                        init(year, month, day) { _, selectedYear, selectedMonth, selectedDay ->
                            year = selectedYear
                            month = selectedMonth
                            day = selectedDay
                        }
                        post { setPickerTextColor(contentColor.toArgb()) }
                    }
                },
                update = { picker ->
                    picker.setBackgroundColor(containerColor.toArgb())
                    picker.updateDate(year, month, day)
                    picker.setPickerTextColor(contentColor.toArgb())
                    picker.post { picker.setPickerTextColor(contentColor.toArgb()) }
                }
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val selected = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month)
                        set(Calendar.DAY_OF_MONTH, day)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    onConfirm(DateTimeUtil.formatDate(selected.timeInMillis))
                }
            ) { Text("确认") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
fun TimePickerDialog(
    initialTime: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val parts = initialTime.split(":")
    var hour by rememberSaveable(initialTime) { mutableStateOf(parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 0) }
    var minute by rememberSaveable(initialTime) { mutableStateOf(parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0) }
    val containerColor = MaterialTheme.colorScheme.surface
    val contentColor = MaterialTheme.colorScheme.onSurface

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = containerColor,
        titleContentColor = contentColor,
        textContentColor = contentColor,
        title = { Text("选择时间") },
        text = {
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { context ->
                    val themedContext = ContextThemeWrapper(context, R.style.Theme_KomugiAccounting_Picker)
                    android.widget.LinearLayout(themedContext).apply {
                        orientation = android.widget.LinearLayout.HORIZONTAL
                        gravity = android.view.Gravity.CENTER
                        setBackgroundColor(containerColor.toArgb())
                        val hourPicker = android.widget.NumberPicker(themedContext).apply {
                            minValue = 0
                            maxValue = 23
                            value = hour
                            displayedValues = (0..23).map { it.toString().padStart(2, '0') }.toTypedArray()
                            setPickerTextColor(contentColor.toArgb())
                            setOnValueChangedListener { _, _, newValue -> hour = newValue }
                        }
                        val minutePicker = android.widget.NumberPicker(themedContext).apply {
                            minValue = 0
                            maxValue = 59
                            value = minute
                            displayedValues = (0..59).map { it.toString().padStart(2, '0') }.toTypedArray()
                            setPickerTextColor(contentColor.toArgb())
                            setOnValueChangedListener { _, _, newValue -> minute = newValue }
                        }
                        addView(hourPicker)
                        addView(minutePicker)
                        post { setPickerTextColor(contentColor.toArgb()) }
                    }
                },
                update = { picker ->
                    picker.setBackgroundColor(containerColor.toArgb())
                    picker.setPickerTextColor(contentColor.toArgb())
                    picker.post { picker.setPickerTextColor(contentColor.toArgb()) }
                }
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm("${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}")
                }
            ) { Text("确认") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

private fun View.setPickerTextColor(color: Int) {
    when (this) {
        is TextView -> setTextColor(color)
        is NumberPicker -> {
            setTextColorCompat(color)
            forEachChild { it.setPickerTextColor(color) }
        }
        is ViewGroup -> forEachChild { it.setPickerTextColor(color) }
    }
}

private fun NumberPicker.setTextColorCompat(color: Int) {
    runCatching {
        NumberPicker::class.java.getDeclaredField("mSelectorWheelPaint").apply {
            isAccessible = true
            (get(this@setTextColorCompat) as? android.graphics.Paint)?.color = color
        }
        invalidate()
    }
}

private fun ViewGroup.forEachChild(block: (View) -> Unit) {
    for (index in 0 until childCount) {
        block(getChildAt(index))
    }
}

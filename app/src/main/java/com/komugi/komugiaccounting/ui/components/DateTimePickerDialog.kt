package com.komugi.komugiaccounting.ui.components

import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import android.widget.TextView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val selected = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, day)
    }
    val firstDay = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val daysInMonth = firstDay.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstOffset = firstDay.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = containerColor,
        titleContentColor = contentColor,
        textContentColor = contentColor,
        title = { Text("选择日期") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = {
                        val previous = Calendar.getInstance().apply {
                            set(Calendar.YEAR, year)
                            set(Calendar.MONTH, month)
                            set(Calendar.DAY_OF_MONTH, 1)
                            add(Calendar.MONTH, -1)
                        }
                        year = previous.get(Calendar.YEAR)
                        month = previous.get(Calendar.MONTH)
                        day = day.coerceAtMost(previous.getActualMaximum(Calendar.DAY_OF_MONTH))
                    }) { Text("<") }
                    Text(
                        "${year}年${month + 1}月",
                        color = contentColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                    TextButton(onClick = {
                        val next = Calendar.getInstance().apply {
                            set(Calendar.YEAR, year)
                            set(Calendar.MONTH, month)
                            set(Calendar.DAY_OF_MONTH, 1)
                            add(Calendar.MONTH, 1)
                        }
                        year = next.get(Calendar.YEAR)
                        month = next.get(Calendar.MONTH)
                        day = day.coerceAtMost(next.getActualMaximum(Calendar.DAY_OF_MONTH))
                    }) { Text(">") }
                }
                Row(Modifier.fillMaxWidth()) {
                    listOf("日", "一", "二", "三", "四", "五", "六").forEach { label ->
                        Text(
                            label,
                            color = contentColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                for (week in 0 until 6) {
                    Row(Modifier.fillMaxWidth()) {
                        for (weekday in 0 until 7) {
                            val cell = week * 7 + weekday
                            val dayNumber = cell - firstOffset + 1
                            if (dayNumber in 1..daysInMonth) {
                                TextButton(
                                    onClick = { day = dayNumber },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(
                                        dayNumber.toString(),
                                        color = if (dayNumber == day) MaterialTheme.colorScheme.primary else contentColor,
                                        fontWeight = if (dayNumber == day) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Clip,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                Text("", modifier = Modifier.weight(1f).height(36.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selected.apply {
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
    val pickerTheme = if (contentColor.luminance() > 0.5f) R.style.Theme_KomugiAccounting_Picker_Dark else R.style.Theme_KomugiAccounting_Picker

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
                    val themedContext = ContextThemeWrapper(context, pickerTheme)
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
                        postDelayed({ setPickerTextColor(contentColor.toArgb()) }, 80)
                        postDelayed({ setPickerTextColor(contentColor.toArgb()) }, 200)
                    }
                },
                update = { picker ->
                    picker.setBackgroundColor(containerColor.toArgb())
                    picker.setPickerTextColor(contentColor.toArgb())
                    picker.post { picker.setPickerTextColor(contentColor.toArgb()) }
                    picker.postDelayed({ picker.setPickerTextColor(contentColor.toArgb()) }, 80)
                    picker.postDelayed({ picker.setPickerTextColor(contentColor.toArgb()) }, 200)
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

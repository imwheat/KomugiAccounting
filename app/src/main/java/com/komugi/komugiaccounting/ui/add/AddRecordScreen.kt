package com.komugi.komugiaccounting.ui.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.komugi.komugiaccounting.data.model.RecordType
import com.komugi.komugiaccounting.util.DateTimeUtil

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddRecordScreen(
    viewModel: AddRecordViewModel,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val data by viewModel.data.collectAsState()
    var type by rememberSaveable { mutableStateOf(RecordType.EXPENSE) }
    var amount by rememberSaveable { mutableStateOf("") }
    var dateTime by rememberSaveable { mutableStateOf(DateTimeUtil.formatDateTime(DateTimeUtil.now())) }
    var remark by rememberSaveable { mutableStateOf("") }
    var newMember by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    val typeCategories = data.categories.filter { it.enabled && it.type == type }.sortedBy { it.sortOrder }
    var selectedCategoryId by rememberSaveable { mutableStateOf("") }
    var selectedMemberId by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(type, data.categories, data.settings) {
        val preferred = if (type == RecordType.EXPENSE) data.settings.lastExpenseCategoryId else data.settings.lastIncomeCategoryId
        selectedCategoryId = preferred?.takeIf { id -> typeCategories.any { it.id == id } }
            ?: typeCategories.firstOrNull()?.id.orEmpty()
    }
    LaunchedEffect(data.members, data.settings) {
        selectedMemberId = data.settings.lastMemberId?.takeIf { id -> data.members.any { it.id == id && it.enabled } }
            ?: data.members.firstOrNull { it.enabled }?.id.orEmpty()
    }

    LazyColumn(
        modifier = modifier.padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Spacer(Modifier.height(10.dp)) }
        item { Text("记一笔", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black) }
        item {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.94f))
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(type == RecordType.EXPENSE, onClick = { type = RecordType.EXPENSE }, label = { Text("支出") })
                        FilterChip(type == RecordType.INCOME, onClick = { type = RecordType.INCOME }, label = { Text("收入") })
                    }
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("金额") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("分类", fontWeight = FontWeight.SemiBold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        typeCategories.forEach { category ->
                            FilterChip(
                                selected = selectedCategoryId == category.id,
                                onClick = { selectedCategoryId = category.id },
                                label = { Text(category.name) }
                            )
                        }
                    }
                    Text("成员", fontWeight = FontWeight.SemiBold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        data.members.filter { it.enabled }.forEach { member ->
                            FilterChip(
                                selected = selectedMemberId == member.id,
                                onClick = { selectedMemberId = member.id },
                                label = { Text(member.name) }
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newMember,
                            onValueChange = { newMember = it },
                            label = { Text("创建成员/角色") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(onClick = {
                            viewModel.addMember(newMember)
                            newMember = ""
                        }) { Text("添加") }
                    }
                    OutlinedTextField(
                        value = dateTime,
                        onValueChange = { dateTime = it },
                        label = { Text("时间：yyyy-MM-dd HH:mm") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = remark,
                        onValueChange = { remark = it },
                        label = { Text("备注") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            error = viewModel.saveRecord(type, amount, selectedCategoryId, selectedMemberId, dateTime, remark)
                            if (error == null) {
                                amount = ""
                                remark = ""
                                dateTime = DateTimeUtil.formatDateTime(DateTimeUtil.now())
                                onSaved()
                            }
                        }
                    ) { Text("完成保存") }
                }
            }
        }
        item { Spacer(Modifier.height(88.dp)) }
    }
}

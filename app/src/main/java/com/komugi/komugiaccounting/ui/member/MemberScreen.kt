package com.komugi.komugiaccounting.ui.member

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.komugi.komugiaccounting.data.repository.AppDataRepository

@Composable
fun MemberScreen(
    repository: AppDataRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isCreating by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = isCreating) {
        isCreating = false
    }

    if (isCreating) {
        MemberCreateScreen(
            repository = repository,
            onBack = { isCreating = false },
            modifier = modifier
        )
    } else {
        MemberListScreen(
            repository = repository,
            onBack = onBack,
            onCreate = { isCreating = true },
            modifier = modifier
        )
    }
}

@Composable
private fun MemberListScreen(
    repository: AppDataRepository,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val data by repository.data.collectAsState()
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    val editingNames = remember { mutableStateMapOf<String, String>() }

    LazyColumn(
        modifier = modifier.padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = onBack) { Text("<") }
                    Text("成员管理", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                }
                Button(onClick = onCreate) { Text("新建") }
            }
        }
        message?.let {
            item { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 4.dp)) }
        }
        items(data.members, key = { it.id }) { member ->
            val editName = editingNames.getOrPut(member.id) { member.name }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(if (member.isSystem) "系统预置成员" else "自定义成员", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editingNames[member.id] = it },
                            label = { Text("名称") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(onClick = {
                            message = repository.updateMember(member.id, editName)
                        }) { Text("保存") }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (member.enabled) "记账页显示" else "记账页隐藏")
                        Switch(
                            checked = member.enabled,
                            onCheckedChange = {
                                repository.setMemberEnabled(member.id, it)
                                message = null
                            }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            message = repository.deleteMember(member.id) ?: "成员已删除"
                        }) { Text("删除") }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberCreateScreen(
    repository: AppDataRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier.padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onBack) { Text("<") }
                Text("新建成员", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            error = null
                        },
                        label = { Text("成员名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val result = repository.addMember(name)
                            if (result == null) {
                                onBack()
                            } else {
                                error = result
                            }
                        }
                    ) { Text("添加") }
                }
            }
        }
    }
}

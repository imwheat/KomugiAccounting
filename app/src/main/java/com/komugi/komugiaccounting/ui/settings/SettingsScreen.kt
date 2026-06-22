package com.komugi.komugiaccounting.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.komugi.komugiaccounting.data.model.ThemeMode
import com.komugi.komugiaccounting.data.repository.AppDataRepository
import com.komugi.komugiaccounting.ui.category.CategoryScreen
import com.komugi.komugiaccounting.ui.export.ExportScreen
import com.komugi.komugiaccounting.ui.member.MemberScreen
import com.komugi.komugiaccounting.ui.template.TemplateScreen

@Composable
fun SettingsScreen(
    repository: AppDataRepository,
    modifier: Modifier = Modifier
) {
    var page by rememberSaveable { mutableStateOf(SettingsPage.Main) }

    BackHandler(enabled = page != SettingsPage.Main) {
        page = SettingsPage.Main
    }

    when (page) {
        SettingsPage.Main -> SettingsHomeScreen(
            repository = repository,
            onOpenMembers = { page = SettingsPage.Members },
            onOpenCategories = { page = SettingsPage.Categories },
            onOpenTemplates = { page = SettingsPage.Templates },
            onOpenExport = { page = SettingsPage.Export },
            modifier = modifier
        )
        SettingsPage.Members -> MemberScreen(repository = repository, onBack = { page = SettingsPage.Main }, modifier = modifier)
        SettingsPage.Categories -> CategoryScreen(repository = repository, onBack = { page = SettingsPage.Main }, modifier = modifier)
        SettingsPage.Templates -> TemplateScreen(repository = repository, onBack = { page = SettingsPage.Main }, modifier = modifier)
        SettingsPage.Export -> ExportScreen(repository = repository, onBack = { page = SettingsPage.Main }, modifier = modifier)
    }
}

@Composable
private fun SettingsHomeScreen(
    repository: AppDataRepository,
    onOpenMembers: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenTemplates: () -> Unit,
    onOpenExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val data by repository.data.collectAsState()

    Column(modifier = modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("设置", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("主题", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeModeChip(
                        label = "黑色",
                        selected = data.settings.themeMode == ThemeMode.DARK,
                        onClick = { repository.updateSettings(data.settings.copy(themeMode = ThemeMode.DARK)) }
                    )
                    ThemeModeChip(
                        label = "白色",
                        selected = data.settings.themeMode == ThemeMode.LIGHT,
                        onClick = { repository.updateSettings(data.settings.copy(themeMode = ThemeMode.LIGHT)) }
                    )
                    ThemeModeChip(
                        label = "跟随系统",
                        selected = data.settings.themeMode == ThemeMode.SYSTEM,
                        onClick = { repository.updateSettings(data.settings.copy(themeMode = ThemeMode.SYSTEM)) }
                    )
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("管理", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                SettingsEntry("成员管理", "${data.members.count { it.enabled }} 个启用", onOpenMembers)
                SettingsEntry("分类管理", "${data.categories.count { it.enabled }} 个启用", onOpenCategories)
                SettingsEntry("模板管理", "${data.templates.size} 个模板", onOpenTemplates)
                SettingsEntry("导出与备份", "JSON", onOpenExport)
            }
        }
    }
}

@Composable
private fun ThemeModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) }
    )
}

@Composable
private fun SettingsEntry(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private enum class SettingsPage {
    Main,
    Members,
    Categories,
    Templates,
    Export
}

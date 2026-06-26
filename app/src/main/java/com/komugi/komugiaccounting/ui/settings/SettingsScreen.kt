package com.komugi.komugiaccounting.ui.settings

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.activity.compose.BackHandler
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.komugi.komugiaccounting.MainActivity
import com.komugi.komugiaccounting.NotificationKeepAlive
import com.komugi.komugiaccounting.data.model.ThemeMode
import com.komugi.komugiaccounting.data.repository.AppDataRepository
import com.komugi.komugiaccounting.ui.category.CategoryScreen
import com.komugi.komugiaccounting.ui.export.ExportScreen
import com.komugi.komugiaccounting.ui.member.MemberScreen

@Composable
fun SettingsScreen(
    repository: AppDataRepository,
    onOpenTemplates: () -> Unit,
    onBottomBarVisibleChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var page by rememberSaveable { mutableStateOf(SettingsPage.Main) }

    LaunchedEffect(page) {
        onBottomBarVisibleChange(page == SettingsPage.Main)
    }

    BackHandler(enabled = page != SettingsPage.Main) {
        page = SettingsPage.Main
    }

    when (page) {
        SettingsPage.Main -> SettingsHomeScreen(
            repository = repository,
            onOpenMembers = { page = SettingsPage.Members },
            onOpenCategories = { page = SettingsPage.Categories },
            onOpenTemplates = onOpenTemplates,
            onOpenExport = { page = SettingsPage.Export },
            modifier = modifier
        )
        SettingsPage.Members -> MemberScreen(repository = repository, onBack = { page = SettingsPage.Main }, modifier = modifier)
        SettingsPage.Categories -> CategoryScreen(repository = repository, onBack = { page = SettingsPage.Main }, modifier = modifier)
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
    val context = LocalContext.current
    var batteryStatusToken by rememberSaveable { mutableStateOf(0) }
    var notificationStatusToken by rememberSaveable { mutableStateOf(0) }
    var showBatteryDisableDialog by rememberSaveable { mutableStateOf(false) }
    val batteryWhitelisted = remember(context, batteryStatusToken) {
        context.isIgnoringBatteryOptimizations()
    }
    val postNotificationsGranted = remember(context, notificationStatusToken) {
        context.isPostNotificationsGranted()
    }
    val appNotificationsEnabled = remember(context, notificationStatusToken) {
        NotificationKeepAlive.isNotificationChannelEnabled(context)
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("后台运行", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("电池优化白名单", fontWeight = FontWeight.SemiBold)
                        Text(
                            if (batteryWhitelisted) "已在系统白名单" else "未在系统白名单",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = data.settings.batteryOptimizationWhitelistEnabled,
                        onCheckedChange = { enabled ->
                            repository.updateSettings(data.settings.copy(batteryOptimizationWhitelistEnabled = enabled))
                            if (enabled) {
                                (context as? MainActivity)?.requestIgnoreBatteryOptimizations()
                            } else {
                                showBatteryDisableDialog = batteryWhitelisted
                            }
                            batteryStatusToken += 1
                        }
                    )
                }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        (context as? MainActivity)?.requestIgnoreBatteryOptimizations()
                        batteryStatusToken += 1
                    }
                ) {
                    Text(if (batteryWhitelisted) "重新检查白名单状态" else "加入电池优化白名单")
                }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { NotificationKeepAlive.openAutoStartSettings(context) }
                ) {
                    Text("自启动权限")
                }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { NotificationKeepAlive.openBackgroundCleanupSettings(context) }
                ) {
                    Text("加入一键清理白名单")
                }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        NotificationKeepAlive.keepAlive(context, forceResetListener = true)
                        NotificationKeepAlive.openNotificationListenerSettings(context)
                    }
                ) {
                    Text("修复通知监听连接")
                }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (!postNotificationsGranted) {
                            context.findMainActivity()?.requestPostNotificationsPermission()
                        } else {
                            NotificationKeepAlive.openAppNotificationSettings(context)
                        }
                        notificationStatusToken += 1
                    }
                ) {
                    Text(
                        when {
                            !postNotificationsGranted -> "申请通知权限"
                            !appNotificationsEnabled -> "打开系统通知开关"
                            else -> "通知权限已开启"
                        }
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
                SettingsEntry("分类管理", "${data.categories.count { it.enabled && !it.name.startsWith("__group__") }} 个启用", onOpenCategories)
                SettingsEntry("模板管理", "${data.templates.size} 个模板", onOpenTemplates)
                SettingsEntry("导出与备份", "JSON", onOpenExport)
            }
        }
    }

    if (showBatteryDisableDialog) {
        AlertDialog(
            onDismissRequest = { showBatteryDisableDialog = false },
            title = { Text("需要到系统设置移除") },
            text = { Text("应用只能请求加入电池优化白名单，不能静默移除。关闭此开关后，应用不会再主动请求加入；如需移除，请在系统电池优化设置中手动操作。") },
            confirmButton = {
                TextButton(onClick = { showBatteryDisableDialog = false }) {
                    Text("知道了")
                }
            }
        )
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
    Export
}

private fun Context.isIgnoringBatteryOptimizations(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
    val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
    return powerManager.isIgnoringBatteryOptimizations(packageName)
}

private fun Context.isPostNotificationsGranted(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
}

private fun Context.findMainActivity(): MainActivity? = when (this) {
    is MainActivity -> this
    is ContextWrapper -> baseContext.findMainActivity()
    else -> null
}

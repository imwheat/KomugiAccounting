package com.komugi.komugiaccounting

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.komugi.komugiaccounting.data.model.ThemeMode
import com.komugi.komugiaccounting.data.repository.AppDataRepository
import com.komugi.komugiaccounting.navigation.Screen
import com.komugi.komugiaccounting.ui.add.AddRecordScreen
import com.komugi.komugiaccounting.ui.add.AddRecordViewModel
import com.komugi.komugiaccounting.ui.automation.AutomationScreen
import com.komugi.komugiaccounting.ui.detail.DetailViewModel
import com.komugi.komugiaccounting.ui.home.HomeScreen
import com.komugi.komugiaccounting.ui.home.HomeViewModel
import com.komugi.komugiaccounting.ui.settings.SettingsScreen
import com.komugi.komugiaccounting.ui.template.TemplateScreen
import com.komugi.komugiaccounting.ui.theme.KomugiAccountingTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
    }

    private var batteryOptimizationRequestShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化通知监听服务和前台服务
        initializeServices()

        enableEdgeToEdge()
        setContent {
            val repository = remember { AppDataRepository.get(applicationContext) }
            val data by repository.data.collectAsState()
            val darkTheme = when (data.settings.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            KomugiAccountingTheme(darkTheme = darkTheme, dynamicColor = false) {
                AccountingApp(repository)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 每次恢复时确保服务在运行
        initializeServices()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            // 权限授予后重新初始化服务
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                initializeServices()
            }
        }
    }

    /**
     * 初始化通知监听服务和前台服务
     */
    private fun initializeServices() {
        // 1. 检查并请求通知权限（Android 13+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    PERMISSION_REQUEST_CODE
                )
                // 权限未授予，但仍尝试启动服务（可能无法显示通知）
            }
        }

        // 2. 检查通知监听权限并绑定服务
        if (NotificationAutoBookService.isNotificationListenerEnabled(this)) {
            NotificationAutoBookService.ensureBound(applicationContext)
            // 启动前台服务
            NotificationListenerForegroundService.start(this)
        } else {
            // 引导用户开启通知监听权限
            // 可以在首次启动时跳转到设置页面，或通过界面提示
            // 这里只绑定服务，用户需要手动开启权限
            NotificationAutoBookService.ensureBound(applicationContext)
        }

        val repository = AppDataRepository.get(applicationContext)
        AutoBookTodoBadgeNotifier.sync(applicationContext, repository.data.value.autoBookTodos.size)
        if (repository.data.value.settings.batteryOptimizationWhitelistEnabled) {
            requestIgnoreBatteryOptimizations(oncePerActivity = true)
        }
    }

    /**
     * 请求忽略电池优化（可以在设置界面调用）
     */
    fun requestIgnoreBatteryOptimizations(oncePerActivity: Boolean = false) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !isIgnoringBatteryOptimizations()) {
            if (oncePerActivity && batteryOptimizationRequestShown) return
            batteryOptimizationRequestShown = true
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        }
    }

    fun isIgnoringBatteryOptimizations(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        val powerManager = getSystemService(POWER_SERVICE) as android.os.PowerManager
        return powerManager.isIgnoringBatteryOptimizations(packageName)
    }
}

@Composable
fun AccountingApp(repository: AppDataRepository) {
    var currentScreen by remember { mutableStateOf(Screen.Home) }
    var previousScreens by remember { mutableStateOf<List<Screen>>(emptyList()) }
    var editingRecordId by remember { mutableStateOf<String?>(null) }
    var pendingHomePage by remember { mutableStateOf<Int?>(null) }
    var pendingAutoBookTodoId by remember { mutableStateOf<String?>(null) }
    var openTemplateCreate by remember { mutableStateOf(false) }
    var templateCreateOpenedFromAdd by remember { mutableStateOf(false) }
    var quickAddAutoBookTodoId by remember { mutableStateOf<String?>(null) }
    var templateCreateOpenedFromAutoBookSelection by remember { mutableStateOf(false) }
    var showHomeBottomBar by remember { mutableStateOf(true) }
    var showAutomationBottomBar by remember { mutableStateOf(true) }
    var showSettingsBottomBar by remember { mutableStateOf(true) }
    var openAutomationTodoList by remember { mutableStateOf(false) }
    var automationTodoOpenedFromHome by remember { mutableStateOf(false) }
    val homeViewModel = remember(repository) { HomeViewModel(repository) }
    val addRecordViewModel = remember(repository) { AddRecordViewModel(repository) }
    val detailViewModel = remember(repository) { DetailViewModel(repository) }
    val bottomScreens = listOf(Screen.Home, Screen.Template, Screen.Add, Screen.Automation, Screen.Settings)
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    fun navigateTo(screen: Screen, addToBackStack: Boolean = true) {
        if (screen == currentScreen) return
        if (addToBackStack) previousScreens = previousScreens + currentScreen
        currentScreen = screen
    }

    fun navigateBack() {
        val previous = previousScreens.lastOrNull()
        if (previous == null) {
            currentScreen = Screen.Home
        } else {
            previousScreens = previousScreens.dropLast(1)
            currentScreen = previous
        }
    }

    fun goHome() {
        previousScreens = emptyList()
        currentScreen = Screen.Home
    }

    fun returnToAutomationTodoList() {
        openAutomationTodoList = true
        automationTodoOpenedFromHome = false
        navigateBack()
    }

    BackHandler(enabled = currentScreen != Screen.Home) {
        navigateBack()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (
                currentScreen != Screen.Add &&
                !(currentScreen == Screen.Template && quickAddAutoBookTodoId != null) &&
                (currentScreen != Screen.Home || showHomeBottomBar) &&
                (currentScreen != Screen.Automation || showAutomationBottomBar) &&
                (currentScreen != Screen.Settings || showSettingsBottomBar)
            ) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)) {
                    bottomScreens.forEach { screen ->
                        NavigationBarItem(
                            selected = currentScreen == screen,
                            onClick = {
                                if (screen == Screen.Add) editingRecordId = null
                                if (screen == Screen.Add) pendingAutoBookTodoId = null
                                navigateTo(screen)
                            },
                            icon = { Text(screen.navIcon()) },
                            label = { Text(screen.navLabel()) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.Home -> HomeScreen(
                    homeViewModel = homeViewModel,
                    detailViewModel = detailViewModel,
                    repository = repository,
                    onEditRecord = { recordId, returnHomePage ->
                        editingRecordId = recordId
                        pendingHomePage = returnHomePage
                        navigateTo(Screen.Add)
                    },
                    onOpenTodoList = {
                        openAutomationTodoList = true
                        automationTodoOpenedFromHome = true
                        navigateTo(Screen.Automation)
                    },
                    onBottomBarVisibleChange = { showHomeBottomBar = it },
                    initialPage = pendingHomePage,
                    onInitialPageConsumed = { pendingHomePage = null }
                )
                Screen.Template -> TemplateScreen(
                    repository = repository,
                    onBack = ::navigateBack,
                    initialCreate = openTemplateCreate,
                    onInitialCreateConsumed = { openTemplateCreate = false },
                    returnToPreviousAfterInitialCreate = templateCreateOpenedFromAdd,
                    onInitialCreateFinished = {
                        when {
                            templateCreateOpenedFromAutoBookSelection -> {
                                templateCreateOpenedFromAutoBookSelection = false
                            }
                            templateCreateOpenedFromAdd -> {
                                templateCreateOpenedFromAdd = false
                                navigateBack()
                            }
                        }
                    },
                    autoBookTodoIdForSelect = quickAddAutoBookTodoId,
                    onCreateTemplateFromAutoBookSelection = {
                        openTemplateCreate = true
                        templateCreateOpenedFromAutoBookSelection = true
                    },
                    onAutoBookTemplateSelected = { templateId ->
                        val todoId = quickAddAutoBookTodoId
                        if (todoId != null) {
                            val message = repository.addAutoBookTodoWithTemplate(todoId, templateId) ?: "已快捷加入"
                            quickAddAutoBookTodoId = null
                            returnToAutomationTodoList()
                            coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                        }
                    },
                    onAutoBookTemplateSelectionBack = {
                        quickAddAutoBookTodoId = null
                        returnToAutomationTodoList()
                    }
                )
                Screen.Automation -> AutomationScreen(
                    repository = repository,
                    onBottomBarVisibleChange = { showAutomationBottomBar = it },
                    onEditAutoBookTodo = { todoId ->
                        editingRecordId = null
                        pendingAutoBookTodoId = todoId
                        navigateTo(Screen.Add)
                    },
                    onQuickAddAutoBookTodo = { todoId ->
                        quickAddAutoBookTodoId = todoId
                        navigateTo(Screen.Template)
                    },
                    initialTodoList = openAutomationTodoList,
                    onInitialTodoListConsumed = { openAutomationTodoList = false },
                    onInitialTodoListBack = {
                        automationTodoOpenedFromHome = false
                        navigateBack()
                    },
                    initialTodoListBackToPreviousScreen = automationTodoOpenedFromHome
                )
                Screen.Add -> AddRecordScreen(
                    viewModel = addRecordViewModel,
                    onSaved = { message ->
                        if (pendingAutoBookTodoId != null) {
                            navigateBack()
                        } else {
                            goHome()
                        }
                        coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                    },
                    onBack = ::navigateBack,
                    recordId = editingRecordId,
                    autoBookTodoId = pendingAutoBookTodoId,
                    onAutoBookTodoChange = { pendingAutoBookTodoId = it },
                    onCreateTemplate = {
                        openTemplateCreate = true
                        templateCreateOpenedFromAdd = true
                        navigateTo(Screen.Template)
                    }
                )
                Screen.Settings -> SettingsScreen(
                    repository = repository,
                    onOpenTemplates = { navigateTo(Screen.Template) },
                    onBottomBarVisibleChange = { showSettingsBottomBar = it }
                )
            }
        }
    }
}

private fun Screen.navIcon(): String = when (this) {
    Screen.Home -> "首"
    Screen.Template -> "模"
    Screen.Add -> "+"
    Screen.Automation -> "自"
    Screen.Settings -> "设"
}

private fun Screen.navLabel(): String = when (this) {
    Screen.Add -> "记一笔"
    else -> title
}

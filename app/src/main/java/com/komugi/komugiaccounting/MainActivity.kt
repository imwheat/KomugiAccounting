package com.komugi.komugiaccounting

import android.os.Bundle
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                        templateCreateOpenedFromAdd = false
                        navigateBack()
                    },
                    autoBookTodoIdForSelect = quickAddAutoBookTodoId,
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

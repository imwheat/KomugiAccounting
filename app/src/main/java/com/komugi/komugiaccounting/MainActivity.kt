package com.komugi.komugiaccounting

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.komugi.komugiaccounting.R
import com.komugi.komugiaccounting.data.model.ThemeMode
import com.komugi.komugiaccounting.data.repository.AppDataRepository
import com.komugi.komugiaccounting.navigation.Screen
import com.komugi.komugiaccounting.ui.add.AddRecordScreen
import com.komugi.komugiaccounting.ui.add.AddRecordViewModel
import com.komugi.komugiaccounting.ui.detail.DetailViewModel
import com.komugi.komugiaccounting.ui.home.HomeScreen
import com.komugi.komugiaccounting.ui.home.HomeViewModel
import com.komugi.komugiaccounting.ui.settings.SettingsScreen
import com.komugi.komugiaccounting.ui.template.TemplateScreen
import com.komugi.komugiaccounting.ui.theme.KomugiAccountingTheme
import kotlinx.coroutines.delay
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
                SplashHost {
                    AccountingApp(repository)
                }
            }
        }
    }
}

@Composable
private fun SplashHost(content: @Composable () -> Unit) {
    var showSplash by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(650)
        showSplash = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        content()
        if (showSplash) {
            Image(
                painter = painterResource(R.drawable.splash_logo),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun AccountingApp(repository: AppDataRepository) {
    var currentScreen by remember { mutableStateOf(Screen.Home) }
    var previousScreens by remember { mutableStateOf<List<Screen>>(emptyList()) }
    var editingRecordId by remember { mutableStateOf<String?>(null) }
    var showHomeBottomBar by remember { mutableStateOf(true) }
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

    BackHandler(enabled = currentScreen != Screen.Home) {
        navigateBack()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (currentScreen != Screen.Add && (currentScreen != Screen.Home || showHomeBottomBar)) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)) {
                    bottomScreens.forEach { screen ->
                        NavigationBarItem(
                            selected = currentScreen == screen,
                            onClick = {
                                if (screen == Screen.Add) editingRecordId = null
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
                    onEditRecord = { recordId ->
                        editingRecordId = recordId
                        navigateTo(Screen.Add)
                    },
                    onBottomBarVisibleChange = { showHomeBottomBar = it }
                )
                Screen.Template -> TemplateScreen(
                    repository = repository,
                    onBack = ::navigateBack
                )
                Screen.Automation -> AutomationPlaceholder()
                Screen.Add -> AddRecordScreen(
                    viewModel = addRecordViewModel,
                    onSaved = { message ->
                        goHome()
                        coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                    },
                    onBack = ::navigateBack,
                    recordId = editingRecordId
                )
                Screen.Settings -> SettingsScreen(
                    repository = repository,
                    onOpenTemplates = { navigateTo(Screen.Template) }
                )
            }
        }
    }
}

@Composable
private fun AutomationPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("自动化", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Text("后续补充", color = MaterialTheme.colorScheme.onSurfaceVariant)
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

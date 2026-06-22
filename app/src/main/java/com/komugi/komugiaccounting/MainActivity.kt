package com.komugi.komugiaccounting

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
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
import com.komugi.komugiaccounting.ui.calendar.CalendarScreen
import com.komugi.komugiaccounting.ui.chart.ChartScreen
import com.komugi.komugiaccounting.ui.detail.DetailScreen
import com.komugi.komugiaccounting.ui.detail.DetailViewModel
import com.komugi.komugiaccounting.ui.home.HomeScreen
import com.komugi.komugiaccounting.ui.home.HomeViewModel
import com.komugi.komugiaccounting.ui.settings.SettingsScreen
import com.komugi.komugiaccounting.ui.theme.KomugiAccountingTheme
import androidx.compose.foundation.isSystemInDarkTheme
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
    var editingRecordId by remember { mutableStateOf<String?>(null) }
    val homeViewModel = remember(repository) { HomeViewModel(repository) }
    val addRecordViewModel = remember(repository) { AddRecordViewModel(repository) }
    val detailViewModel = remember(repository) { DetailViewModel(repository) }
    val bottomScreens = listOf(Screen.Home, Screen.Detail, Screen.Chart, Screen.Calendar, Screen.Settings)
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val goHome = { currentScreen = Screen.Home }

    BackHandler(enabled = currentScreen != Screen.Home) {
        goHome()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editingRecordId = null
                currentScreen = Screen.Add
            }) {
                Text("+")
            }
        },
        bottomBar = {
            if (currentScreen != Screen.Add) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)) {
                    bottomScreens.forEach { screen ->
                        NavigationBarItem(
                            selected = currentScreen == screen,
                            onClick = { currentScreen = screen },
                            icon = { Text(screen.navIcon()) },
                            label = { Text(screen.title) }
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
                Screen.Home -> HomeScreen(homeViewModel)
                Screen.Detail -> DetailScreen(
                    viewModel = detailViewModel,
                    onEditRecord = { recordId ->
                        editingRecordId = recordId
                        currentScreen = Screen.Add
                    }
                )
                Screen.Add -> AddRecordScreen(
                    viewModel = addRecordViewModel,
                    onSaved = { message ->
                        goHome()
                        coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                    },
                    onBack = goHome,
                    recordId = editingRecordId
                )
                Screen.Chart -> ChartScreen(repository = repository)
                Screen.Calendar -> CalendarScreen(repository = repository)
                Screen.Settings -> SettingsScreen(repository = repository)
            }
        }
    }
}

private fun Screen.navIcon(): String = when (this) {
    Screen.Home -> "首"
    Screen.Detail -> "明"
    Screen.Add -> "+"
    Screen.Chart -> "图"
    Screen.Calendar -> "历"
    Screen.Settings -> "设"
}

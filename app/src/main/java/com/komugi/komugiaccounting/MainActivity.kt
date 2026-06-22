package com.komugi.komugiaccounting

import android.os.Bundle
import androidx.activity.ComponentActivity
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KomugiAccountingTheme(dynamicColor = false) {
                val repository = remember { AppDataRepository.get(applicationContext) }
                AccountingApp(repository)
            }
        }
    }
}

@Composable
fun AccountingApp(repository: AppDataRepository) {
    var currentScreen by remember { mutableStateOf(Screen.Home) }
    val homeViewModel = remember(repository) { HomeViewModel(repository) }
    val addRecordViewModel = remember(repository) { AddRecordViewModel(repository) }
    val detailViewModel = remember(repository) { DetailViewModel(repository) }
    val bottomScreens = listOf(Screen.Home, Screen.Detail, Screen.Chart, Screen.Calendar, Screen.Settings)

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { currentScreen = Screen.Add }) {
                Text("+")
            }
        },
        bottomBar = {
            if (currentScreen != Screen.Add) {
                NavigationBar(containerColor = Color.White.copy(alpha = 0.94f)) {
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
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFF6E7C8), Color(0xFFEFF6EA), MaterialTheme.colorScheme.background)
                    )
                )
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.Home -> HomeScreen(homeViewModel)
                Screen.Detail -> DetailScreen(detailViewModel)
                Screen.Add -> AddRecordScreen(
                    viewModel = addRecordViewModel,
                    onSaved = { currentScreen = Screen.Home }
                )
                Screen.Chart -> ChartScreen()
                Screen.Calendar -> CalendarScreen()
                Screen.Settings -> SettingsScreen()
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

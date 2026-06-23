package com.komugi.komugiaccounting.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun panelBackgroundColor(): Color =
    if (MaterialTheme.colorScheme.background == Color.Black) {
        Color(0xFF1C1C1E)
    } else {
        Color(0xFFF2F3F5)
    }

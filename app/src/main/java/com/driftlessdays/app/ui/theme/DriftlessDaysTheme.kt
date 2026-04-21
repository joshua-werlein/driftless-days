package com.driftlessdays.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DriftlessColorScheme = darkColorScheme(
    primary = Color(0xFF3C3489),
    secondary = Color(0xFFAFA9EC),
    tertiary = Color(0xFFEF9F27),
    background = Color(0xFF1A1A2E),
    surface = Color(0xFF16213E),
)

@Composable
fun DriftlessDaysTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DriftlessColorScheme,
        content = content
    )
}
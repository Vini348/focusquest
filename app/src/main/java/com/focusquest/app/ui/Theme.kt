package com.focusquest.app.ui

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val QuestColors = darkColorScheme(
    primary = Color(0xFF9B7BFF),
    onPrimary = Color(0xFF160F2B),
    secondary = Color(0xFFB9A6FF),
    background = Color(0xFF0D0F14),
    surface = Color(0xFF171A22),
    surfaceVariant = Color(0xFF222633),
    onBackground = Color(0xFFF5F5F7),
    onSurface = Color(0xFFF5F5F7),
    onSurfaceVariant = Color(0xFFB5B8C3),
    error = Color(0xFFFF626D)
)

@Composable
fun FocusQuestTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = QuestColors, content = content)
}

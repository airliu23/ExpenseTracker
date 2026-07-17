package com.example.expensetracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Green40,
    onPrimary = CardLight,
    primaryContainer = Green80,
    secondary = Blue40,
    onSecondary = CardLight,
    background = SurfaceLight,
    onBackground = TextPrimary,
    surface = CardLight,
    onSurface = TextPrimary,
    error = ExpenseRed,
    onError = CardLight
)

private val DarkColorScheme = darkColorScheme(
    primary = Green80,
    onPrimary = TextPrimary,
    primaryContainer = Green40,
    secondary = Blue80,
    onSecondary = TextPrimary,
    background = SurfaceDark,
    onBackground = CardLight,
    surface = SurfaceDark,
    onSurface = CardLight,
    error = ExpenseRed,
    onError = CardLight
)

@Composable
fun ExpenseTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    // enableEdgeToEdge() 已在 MainActivity 调用，系统栏由系统管理

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}

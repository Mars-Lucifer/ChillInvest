package com.example.chillinvest.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AppColorScheme = darkColorScheme(
    primary = AppPrimary,
    onPrimary = AppPrimaryText,
    background = AppBackground,
    onBackground = AppPrimary,
    surface = AppSurface,
    onSurface = AppPrimary,
    outline = AppBorder,
    secondary = AppSurface,
    onSecondary = AppPrimary,
    tertiary = AppDarkAccent
)

@Composable
fun ChillInvestTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = Typography,
        content = content
    )
}

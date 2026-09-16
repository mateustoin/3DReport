package com.threedreport.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.threedreport.core.model.ThemeMode

/** Aplica o tema claro/escuro conforme [mode] (resolvendo [ThemeMode.SYSTEM] pelo tema do SO). */
@Composable
fun AppTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val isDark = when (mode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (isDark) AppDarkColorScheme else AppLightColorScheme,
        content = content,
    )
}

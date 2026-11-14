package com.threedreport.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import com.threedreport.core.model.ThemeMode

/**
 * Aplica o tema claro/escuro conforme [mode] (resolvendo [ThemeMode.SYSTEM] pelo tema do SO) e entrega
 * tudo do sistema de design: o `ColorScheme` e os estilos nos papéis do Material 3, mais o que não cabe
 * neles ([AppColors], [AppTypography], [AppSpacing]), lido por [MaterialTheme.appColors] e companhia.
 */
@Composable
fun AppTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val isDark = when (mode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val appType = rememberAppTypography()
    val material = remember(appType) { appType.toMaterial() }
    CompositionLocalProvider(
        LocalAppColors provides if (isDark) DarkAppColors else LightAppColors,
        LocalAppTypography provides appType,
        LocalAppSpacing provides AppSpacing(),
    ) {
        MaterialTheme(
            colorScheme = if (isDark) AppDarkColorScheme else AppLightColorScheme,
            typography = material,
            shapes = AppShapes,
            content = content,
        )
    }
}

/** Papéis de cor que não cabem no `ColorScheme` do Material 3. */
val MaterialTheme.appColors: AppColors
    @Composable get() = LocalAppColors.current

/** Estilos nomeados do sistema de design (`displayPrice`, `headline`, `overline`, ...). */
val MaterialTheme.appType: AppTypography
    @Composable get() = LocalAppTypography.current

/** Escala de espaço (4, 8, 12, 16, 20, 24, 32). */
val MaterialTheme.spacing: AppSpacing
    @Composable get() = LocalAppSpacing.current

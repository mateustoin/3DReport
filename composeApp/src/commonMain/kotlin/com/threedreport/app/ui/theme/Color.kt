package com.threedreport.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Paleta customizada do app: azul petróleo como cor primária (tecnologia/
 * precisão) e laranja âmbar como secundária (evoca filamento/PLA), no lugar
 * do roxo padrão do Material3.
 *
 * O esquema é completo (decisão 108): terciária (verde-azulado), variantes de contorno, cores inversas e
 * os `surfaceContainer*` que cards, menus e diálogos do Material 3 usam. Antes só metade era definida, e o
 * resto caía no roxo padrão, que deixava os cards do Kanban lilás.
 *
 * O âmbar do tema claro é um tom mais escuro que o da marca (0xFFB56A1A): texto pequeno nele sobre
 * branco não chegava ao contraste mínimo de leitura (4,5:1).
 */
val AppLightColorScheme = lightColorScheme(
    primary = Color(0xFF0B5FA8),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD3E4FF),
    onPrimaryContainer = Color(0xFF001C3A),
    secondary = Color(0xFF9A5A10),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDDB8),
    onSecondaryContainer = Color(0xFF3A2400),
    tertiary = Color(0xFF00696E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFB9ECEE),
    onTertiaryContainer = Color(0xFF002022),
    background = Color(0xFFFDFCFF),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFDFCFF),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFDFE2EB),
    onSurfaceVariant = Color(0xFF43474E),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF73777F),
    outlineVariant = Color(0xFFC3C6CF),
    surfaceTint = Color(0xFF0B5FA8),
    inverseSurface = Color(0xFF2F3033),
    inverseOnSurface = Color(0xFFF1F0F4),
    inversePrimary = Color(0xFF9FC9FF),
    scrim = Color(0xFF000000),
    surfaceDim = Color(0xFFD9DADF),
    surfaceBright = Color(0xFFFDFCFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF4F4F8),
    surfaceContainer = Color(0xFFEEEEF2),
    surfaceContainerHigh = Color(0xFFE8E8EC),
    surfaceContainerHighest = Color(0xFFE2E2E6),
)

val AppDarkColorScheme = darkColorScheme(
    primary = Color(0xFF9FC9FF),
    onPrimary = Color(0xFF00315C),
    primaryContainer = Color(0xFF004883),
    onPrimaryContainer = Color(0xFFD3E4FF),
    secondary = Color(0xFFFFB868),
    onSecondary = Color(0xFF5E3900),
    secondaryContainer = Color(0xFF864F00),
    onSecondaryContainer = Color(0xFFFFDDB8),
    tertiary = Color(0xFF80D4D9),
    onTertiary = Color(0xFF003739),
    tertiaryContainer = Color(0xFF004F53),
    onTertiaryContainer = Color(0xFFB9ECEE),
    background = Color(0xFF1A1C1E),
    onBackground = Color(0xFFE3E2E6),
    surface = Color(0xFF1A1C1E),
    onSurface = Color(0xFFE3E2E6),
    surfaceVariant = Color(0xFF43474E),
    onSurfaceVariant = Color(0xFFC3C6CF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF8D9199),
    outlineVariant = Color(0xFF43474E),
    surfaceTint = Color(0xFF9FC9FF),
    inverseSurface = Color(0xFFE3E2E6),
    inverseOnSurface = Color(0xFF2F3033),
    inversePrimary = Color(0xFF0B5FA8),
    scrim = Color(0xFF000000),
    surfaceDim = Color(0xFF1A1C1E),
    surfaceBright = Color(0xFF38393C),
    surfaceContainerLowest = Color(0xFF0F1113),
    surfaceContainerLow = Color(0xFF1E2023),
    surfaceContainer = Color(0xFF222427),
    surfaceContainerHigh = Color(0xFF2C2E31),
    surfaceContainerHighest = Color(0xFF37393C),
)

package com.threedreport.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Paleta customizada do app: azul petróleo como cor primária (tecnologia/
 * precisão) e laranja âmbar como secundária (evoca filamento/PLA), no lugar
 * do roxo padrão do Material3.
 */
val AppLightColorScheme = lightColorScheme(
    primary = Color(0xFF0B5FA8),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD3E4FF),
    onPrimaryContainer = Color(0xFF001C3A),
    secondary = Color(0xFFB56A1A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDDB8),
    onSecondaryContainer = Color(0xFF3A2400),
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
)

package com.threedreport.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Cores da direção "Oficina" (docs/design-system.md, decisão 127): azul-marinho e âmbar. Os hexadecimais
 * só existem neste arquivo; o resto do app pede pelo papel (`MaterialTheme.colorScheme` pro que cabe no
 * Material 3, [AppColors] pro resto).
 */

// ---------------------------------------------------------------------------------------------
// Papéis que cabem no Material 3
// ---------------------------------------------------------------------------------------------

val AppLightColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF0B5FA8),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE6EFF8), // tint
    onPrimaryContainer = Color(0xFF0B3B66),
    secondary = Color(0xFFC97A12), // âmbar como texto sobre fundo claro
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFCEBD0),
    onSecondaryContainer = Color(0xFF7A4706),
    tertiary = Color(0xFF1F7A4D), // "Pronto"
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFDDF1E6),
    onTertiaryContainer = Color(0xFF0F4D2E),
    background = Color(0xFFF6F8FB),
    onBackground = Color(0xFF0F1B2B),
    surface = Color(0xFFF6F8FB),
    onSurface = Color(0xFF0F1B2B),
    surfaceVariant = Color(0xFFECF0F5),
    onSurfaceVariant = Color(0xFF536173),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFBDAD6),
    onErrorContainer = Color(0xFF8E1B13),
    outline = Color(0xFF8793A3),
    outlineVariant = Color(0xFFDCE2EA), // line
    surfaceTint = Color(0xFF0B5FA8),
    inverseSurface = Color(0xFF0F1B2B),
    inverseOnSurface = Color(0xFFE8EEF5),
    inversePrimary = Color(0xFF7DB8F5),
    scrim = Color(0xFF000000),
    surfaceDim = Color(0xFFE3E8EF),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF), // card
    surfaceContainerLow = Color(0xFFF1F4F8),
    surfaceContainer = Color(0xFFECF0F5), // column
    surfaceContainerHigh = Color(0xFFFFFFFF), // diálogos e menus sobem pro tom do card
    surfaceContainerHighest = Color(0xFFE4E9F0),
)

val AppDarkColorScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFF7DB8F5),
    onPrimary = Color(0xFF06213A),
    primaryContainer = Color(0xFF16283D), // tint
    onPrimaryContainer = Color(0xFFCFE6FF),
    secondary = Color(0xFFF2A541),
    onSecondary = Color(0xFF2A1A02),
    secondaryContainer = Color(0xFF4A3210),
    onSecondaryContainer = Color(0xFFFFD79A),
    tertiary = Color(0xFF6FCF9F), // "Pronto"
    onTertiary = Color(0xFF05301D),
    tertiaryContainer = Color(0xFF17382A),
    onTertiaryContainer = Color(0xFFBFEFD6),
    background = Color(0xFF0B1420),
    onBackground = Color(0xFFE8EEF5),
    surface = Color(0xFF0B1420),
    onSurface = Color(0xFFE8EEF5),
    surfaceVariant = Color(0xFF0F1B2A),
    onSurfaceVariant = Color(0xFF9DAEC2),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF55201B),
    errorContainer = Color(0xFF55201B),
    onErrorContainer = Color(0xFFFFC2BB),
    outline = Color(0xFF5B7090),
    outlineVariant = Color(0xFF22324A), // line
    surfaceTint = Color(0xFF7DB8F5),
    inverseSurface = Color(0xFFE8EEF5),
    inverseOnSurface = Color(0xFF0B1420),
    inversePrimary = Color(0xFF0B5FA8),
    scrim = Color(0xFF000000),
    surfaceDim = Color(0xFF0B1420),
    surfaceBright = Color(0xFF1A2A3F),
    surfaceContainerLowest = Color(0xFF111D2C), // card
    surfaceContainerLow = Color(0xFF0E1927),
    surfaceContainer = Color(0xFF0F1B2A), // column
    surfaceContainerHigh = Color(0xFF16263A),
    surfaceContainerHighest = Color(0xFF1C2E45),
)

// ---------------------------------------------------------------------------------------------
// Papéis que não cabem no Material 3
// ---------------------------------------------------------------------------------------------

/** Selo de prazo: fundo e texto (sempre acompanhados do texto, nunca só a cor). */
@Immutable
data class DeadlineColors(val background: Color, val content: Color)

/** As quatro fatias da barra de composição do preço, na ordem da barra. */
@Immutable
data class CompositionColors(
    val materialAndMachine: Color,
    val laborAndServices: Color,
    val feesAndTaxes: Color,
    val profit: Color,
)

@Immutable
data class AppColors(
    // Superfícies e texto
    val card: Color,
    val column: Color,
    val line: Color,
    val tint: Color,
    /** Bloco dentro de uma superfície `card` (cada cor do filamento no painel): `column` no claro, `tint` no escuro, um tom acima do painel. */
    val cardBlock: Color,
    // Marca
    val navy: Color,
    val navyRaised: Color,
    val onNavy: Color,
    val onNavyMuted: Color,
    val amber: Color,
    val onAmber: Color,
    /** Âmbar legível como texto sobre o fundo da tela (o `amber` puro não tem contraste no claro). */
    val amberText: Color,
    /** Vermelho legível sobre o azul-marinho (prejuízo, aviso). */
    val onNavyDanger: Color,
    // Situação do pedido
    val statusQuoted: Color,
    val statusApproved: Color,
    val statusPrinting: Color,
    val statusReady: Color,
    // Prazo
    val deadlineOnTime: DeadlineColors,
    val deadlineToday: DeadlineColors,
    val deadlineLate: DeadlineColors,
    // Composição do preço
    val composition: CompositionColors,
)

private val OnNavy = Color(0xFFEAF1F8)
private val OnNavyMuted = Color(0xFFA9BCD0)
private val Amber = Color(0xFFF2A541)
private val OnAmber = Color(0xFF2A1A02)
private val NavyDanger = Color(0xFFFFB4AB)
private val QuotedGrey = Color(0xFF8292A6)

private val Composition = CompositionColors(
    materialAndMachine = Color(0xFF7DB8F5),
    laborAndServices = Color(0xFF4C8FD0),
    feesAndTaxes = Color(0xFF6C7F95),
    profit = Color(0xFFF2A541),
)

val LightAppColors = AppColors(
    card = Color(0xFFFFFFFF),
    column = Color(0xFFECF0F5),
    line = Color(0xFFDCE2EA),
    tint = Color(0xFFE6EFF8),
    cardBlock = Color(0xFFECF0F5),
    navy = Color(0xFF0C2742),
    navyRaised = Color(0xFF143657),
    onNavy = OnNavy,
    onNavyMuted = OnNavyMuted,
    amber = Amber,
    onAmber = OnAmber,
    amberText = Color(0xFFC97A12),
    onNavyDanger = NavyDanger,
    statusQuoted = QuotedGrey,
    statusApproved = Color(0xFF0B5FA8),
    statusPrinting = Color(0xFFC97A12),
    statusReady = Color(0xFF1F7A4D),
    deadlineOnTime = DeadlineColors(Color(0xFFE3ECF5), Color(0xFF3B4A5C)),
    deadlineToday = DeadlineColors(Color(0xFFFCEBD0), Color(0xFF7A4706)),
    deadlineLate = DeadlineColors(Color(0xFFFBDAD6), Color(0xFF8E1B13)),
    composition = Composition,
)

val DarkAppColors = AppColors(
    card = Color(0xFF111D2C),
    column = Color(0xFF0F1B2A),
    line = Color(0xFF22324A),
    tint = Color(0xFF16283D),
    cardBlock = Color(0xFF16283D),
    navy = Color(0xFF081526),
    navyRaised = Color(0xFF12263D),
    onNavy = OnNavy,
    onNavyMuted = OnNavyMuted,
    amber = Amber,
    onAmber = OnAmber,
    amberText = Color(0xFFF2A541),
    onNavyDanger = NavyDanger,
    statusQuoted = QuotedGrey,
    statusApproved = Color(0xFF7DB8F5),
    statusPrinting = Color(0xFFF2A541),
    statusReady = Color(0xFF6FCF9F),
    deadlineOnTime = DeadlineColors(Color(0xFF1C2C40), Color(0xFFB7C6D8)),
    deadlineToday = DeadlineColors(Color(0xFF4A3210), Color(0xFFFFD79A)),
    deadlineLate = DeadlineColors(Color(0xFF55201B), Color(0xFFFFC2BB)),
    composition = Composition,
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

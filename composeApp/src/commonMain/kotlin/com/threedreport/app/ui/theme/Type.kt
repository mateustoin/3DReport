package com.threedreport.app.ui.theme

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.inter_medium
import _3dreport.composeapp.generated.resources.inter_regular
import _3dreport.composeapp.generated.resources.inter_semibold
import _3dreport.composeapp.generated.resources.space_grotesk
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font

/** Algarismos tabulares da própria fonte: números de dinheiro, peso e tempo alinham em coluna. */
const val TABULAR_FIGURES = "tnum"

/** Os estilos nomeados do sistema de design (tabela "Tipografia" de docs/design-system.md). */
@Immutable
data class AppTypography(
    val displayPrice: TextStyle,
    val headline: TextStyle,
    val statValue: TextStyle,
    val panelTitle: TextStyle,
    val sectionTitle: TextStyle,
    val body: TextStyle,
    val bodyStrong: TextStyle,
    val label: TextStyle,
    val caption: TextStyle,
    val overline: TextStyle,
    /** Marca "3DReport" da barra lateral. */
    val brand: TextStyle,
)

/** Inter (400, 500, 600) e Space Grotesk (500, 600, 700), embutidas no app. */
@Composable
private fun rememberInter(): FontFamily = FontFamily(
    Font(Res.font.inter_regular, FontWeight.Normal),
    Font(Res.font.inter_medium, FontWeight.Medium),
    Font(Res.font.inter_semibold, FontWeight.SemiBold),
)

// Um arquivo só, de eixo de peso: o repositório oficial não publica o 600 estático.
@Composable
private fun rememberSpaceGrotesk(): FontFamily = FontFamily(
    Font(Res.font.space_grotesk, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(Res.font.space_grotesk, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(Res.font.space_grotesk, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
)

@Composable
fun rememberAppTypography(): AppTypography {
    val inter = rememberInter()
    val grotesk = rememberSpaceGrotesk()
    return remember(inter, grotesk) {
        AppTypography(
            displayPrice = TextStyle(
                fontFamily = grotesk, fontWeight = FontWeight.Bold, fontSize = 52.sp, lineHeight = 57.sp,
                letterSpacing = (-0.03).em, fontFeatureSettings = TABULAR_FIGURES,
            ),
            headline = TextStyle(
                fontFamily = grotesk, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp,
                letterSpacing = (-0.01).em,
            ),
            statValue = TextStyle(
                fontFamily = grotesk, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp,
                fontFeatureSettings = TABULAR_FIGURES,
            ),
            panelTitle = TextStyle(fontFamily = grotesk, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
            sectionTitle = TextStyle(fontFamily = grotesk, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
            body = TextStyle(fontFamily = inter, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
            bodyStrong = TextStyle(fontFamily = inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
            label = TextStyle(fontFamily = inter, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
            caption = TextStyle(fontFamily = inter, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
            overline = TextStyle(
                fontFamily = inter, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.08.em,
            ),
            brand = TextStyle(fontFamily = grotesk, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 24.sp),
        )
    }
}

/**
 * Os mesmos estilos nos papéis do Material 3, pra os componentes e as telas que ainda não foram
 * redesenhadas herdarem a aparência nova.
 */
fun AppTypography.toMaterial(): Typography = Typography(
    displayLarge = displayPrice,
    displayMedium = displayPrice,
    displaySmall = statValue.copy(fontSize = 30.sp, lineHeight = 36.sp),
    headlineLarge = headline,
    headlineMedium = headline,
    headlineSmall = statValue,
    titleLarge = statValue,
    titleMedium = sectionTitle,
    titleSmall = bodyStrong,
    bodyLarge = body,
    bodyMedium = body,
    bodySmall = caption,
    labelLarge = body.copy(fontWeight = FontWeight.Medium),
    labelMedium = label,
    labelSmall = label.copy(fontSize = 11.sp, lineHeight = 16.sp),
)

val LocalAppTypography = staticCompositionLocalOf<AppTypography> { error("AppTheme não foi aplicado") }

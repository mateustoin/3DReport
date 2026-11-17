package com.threedreport.app.platform

import androidx.compose.ui.graphics.toArgb
import com.threedreport.app.ui.theme.AppLightColorScheme
import com.threedreport.app.ui.theme.DarkAppColors
import com.threedreport.app.ui.theme.LightAppColors
import androidx.compose.ui.graphics.Color as ComposeColor
import java.awt.Color as AwtColor

/**
 * As cores dos documentos que o cliente recebe (PDF e imagem), num ponto só. As de marca e de tema vêm do tema do app
 * (`ui/theme/Color.kt`), pra não repetir hexadecimal; as do PDF, que é do vendedor e não leva cor de marca, são só tinta
 * e cinzas e ficam aqui mesmo.
 */
internal object DocumentColors {
    private fun ComposeColor.awt(): AwtColor = AwtColor(toArgb(), true)

    // Tema claro e marca, pra imagem.
    val navy: AwtColor = LightAppColors.navy.awt()

    /** Faixa da imagem: o `navy` do tema escuro, mais fundo que o fundo. */
    val navyDeep: AwtColor = DarkAppColors.navy.awt()
    val onNavy: AwtColor = LightAppColors.onNavy.awt()
    val onNavyMuted: AwtColor = LightAppColors.onNavyMuted.awt()
    val amber: AwtColor = LightAppColors.amber.awt()
    val amberText: AwtColor = LightAppColors.amberText.awt()
    val screenBackground: AwtColor = AppLightColorScheme.background.awt()
    val text: AwtColor = AppLightColorScheme.onBackground.awt()
    val textMuted: AwtColor = AppLightColorScheme.onSurfaceVariant.awt()
    val card: AwtColor = LightAppColors.card.awt()
    val line: AwtColor = LightAppColors.line.awt()

    // Tinta e cinzas do PDF do vendedor.
    val ink = AwtColor(0x10, 0x15, 0x1C)
    val inkMuted = AwtColor(0x5B, 0x66, 0x73)
    val rule = AwtColor(0xD5, 0xDB, 0xE3)
    val rowRule = AwtColor(0xE6, 0xE9, 0xEE)
    val panel = AwtColor(0xF1, 0xF3, 0xF6)
    val pageBorder = AwtColor(190, 190, 190)
}

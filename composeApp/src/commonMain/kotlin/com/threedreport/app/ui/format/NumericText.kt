package com.threedreport.app.ui.format

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.threedreport.app.ui.theme.TABULAR_FIGURES

/**
 * Texto pra um valor numérico (preço, peso, tempo, %) com algarismos tabulares da própria fonte do app
 * (`tnum`): os dígitos têm largura fixa e alinham em coluna, sem trocar de fonte (docs/design-system.md).
 * Usado sempre ao lado de um rótulo em texto normal, nunca sozinho sem contexto.
 */
@Composable
fun NumericText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
) {
    Text(
        text = text,
        modifier = modifier,
        style = style.copy(fontFeatureSettings = TABULAR_FIGURES),
        color = color,
        fontWeight = fontWeight,
        textAlign = textAlign,
    )
}

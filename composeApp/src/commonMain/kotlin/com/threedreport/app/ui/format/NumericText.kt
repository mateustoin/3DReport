package com.threedreport.app.ui.format

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign

/**
 * Texto pra um valor numérico (preço, peso, tempo, %) em fonte monoespaçada — dá aos números do
 * app um tratamento visual consistente de "leitura de instrumento" (dígitos de largura fixa,
 * alinhados), já que produzir esses números com precisão é o motivo do app existir. Usado sempre
 * ao lado de um rótulo em texto normal, nunca sozinho sem contexto (ver telas de Orçamento,
 * Dashboard, Kanban e Histórico).
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
        style = style.copy(fontFamily = FontFamily.Monospace),
        color = color,
        fontWeight = fontWeight,
        textAlign = textAlign,
    )
}

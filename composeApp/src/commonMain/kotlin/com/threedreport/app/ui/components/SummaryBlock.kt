package com.threedreport.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.threedreport.app.ui.format.NumericText
import com.threedreport.app.ui.theme.AppDimens
import com.threedreport.app.ui.theme.appColors
import com.threedreport.app.ui.theme.appType
import com.threedreport.app.ui.theme.spacing

/**
 * Bloco de resumo: o número em `statValue` e a legenda em `caption`, fundo `card` e borda `line` (raio 10). [valueColor]
 * pinta só o número (a cor de prazo, no "atrasado"); a legenda continua dizendo o que ele é.
 */
@Composable
fun SummaryBlock(value: String, label: String, modifier: Modifier = Modifier, valueColor: Color = Color.Unspecified) {
    val space = MaterialTheme.spacing
    Column(
        modifier = modifier
            .background(MaterialTheme.appColors.card, MaterialTheme.shapes.medium)
            .border(AppDimens.borderWidth, MaterialTheme.appColors.line, MaterialTheme.shapes.medium)
            .padding(horizontal = space.md, vertical = space.sm),
    ) {
        NumericText(
            value,
            style = MaterialTheme.appType.statValue,
            color = if (valueColor == Color.Unspecified) MaterialTheme.colorScheme.onSurface else valueColor,
            modifier = Modifier,
        )
        Text(label, style = MaterialTheme.appType.caption, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

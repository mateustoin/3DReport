package com.threedreport.app.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.threedreport.app.ui.text.UiText
import com.threedreport.app.ui.text.asString
import com.threedreport.app.ui.theme.AppDimens
import com.threedreport.app.ui.theme.appColors
import com.threedreport.app.ui.theme.appType
import com.threedreport.app.ui.theme.spacing

/**
 * O rodapé fixo do modo de seleção: quantos estão marcados e as ações ([actions], à direita). Fica fora da lista rolada,
 * porque com ela rolada os botões de exportar ficavam fora da vista.
 */
@Composable
fun SelectionBar(summary: String, modifier: Modifier = Modifier, actions: @Composable () -> Unit) {
    val colors = MaterialTheme.appColors
    val space = MaterialTheme.spacing
    Column(modifier = modifier.fillMaxWidth().background(colors.card)) {
        HorizontalDivider(color = colors.line, thickness = AppDimens.borderWidth)
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = space.xxl, vertical = space.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(space.xs),
        ) {
            Text(summary, style = MaterialTheme.appType.body, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            actions()
        }
    }
}

/** A faixa de progresso no topo da tela enquanto um PDF ou imagem é gerado: uma barra fina e o que está sendo feito. */
@Composable
fun BusyBar(text: UiText) {
    val space = MaterialTheme.spacing
    Column(modifier = Modifier.fillMaxWidth()) {
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.appColors.line,
        )
        Text(
            text.asString(),
            style = MaterialTheme.appType.caption,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = space.xxl, vertical = space.xxs),
        )
    }
}

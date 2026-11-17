package com.threedreport.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.threedreport.app.ui.text.UiText
import com.threedreport.app.ui.text.asString
import com.threedreport.app.ui.theme.appColors
import com.threedreport.app.ui.theme.appType
import com.threedreport.app.ui.theme.spacing

/**
 * A faixa de progresso no topo da tela enquanto algo demora (gerar um PDF ou uma imagem): uma barra fina em `primary`
 * sobre `line` e, embaixo, o que está sendo feito em `caption`.
 */
@Composable
fun BusyBar(text: UiText, modifier: Modifier = Modifier) {
    val space = MaterialTheme.spacing
    Column(modifier = modifier.fillMaxWidth()) {
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

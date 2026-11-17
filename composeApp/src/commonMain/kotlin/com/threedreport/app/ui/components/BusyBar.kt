package com.threedreport.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
        AppProgressBar(progress = null)
        Text(
            text.asString(),
            style = MaterialTheme.appType.caption,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = space.xxl, vertical = space.xxs),
        )
    }
}

/**
 * A barra de progresso do kit: faixa fina em `primary` sobre `line`. Com [progress] (0 a 1) mostra quanto já foi feito;
 * sem ele, anda sem fim. Quem usa dá a descrição pra leitor de tela (a barra não leva texto próprio).
 */
@Composable
fun AppProgressBar(progress: Float?, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.appColors.line
    if (progress == null) {
        LinearProgressIndicator(modifier = modifier.fillMaxWidth(), color = color, trackColor = track, gapSize = 0.dp)
    } else {
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = modifier.fillMaxWidth(),
            color = color,
            trackColor = track,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
    }
}

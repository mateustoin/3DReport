package com.threedreport.app.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.threedreport.app.ui.components.AppDivider
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
        AppDivider()
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

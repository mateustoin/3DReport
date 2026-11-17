package com.threedreport.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.threedreport.app.ui.format.NumericText
import com.threedreport.app.ui.theme.AppDimens
import com.threedreport.app.ui.theme.appType
import com.threedreport.app.ui.theme.spacing

/**
 * Linha só de leitura, no molde da linha de lista (não é cartão, não clica): [title] em `body` à esquerda, [value] em
 * `bodyStrong` com algarismos tabulares à direita e divisor de 1 px embaixo. Pra pares que se leem juntos, como um atalho e o que ele faz.
 */
@Composable
fun InfoRow(title: String, value: String, modifier: Modifier = Modifier) {
    val space = MaterialTheme.spacing
    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = AppDimens.infoRowHeight).padding(horizontal = space.sm, vertical = space.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(space.md),
        ) {
            Text(title, style = MaterialTheme.appType.body, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            NumericText(value, style = MaterialTheme.appType.bodyStrong, color = MaterialTheme.colorScheme.onSurface)
        }
        AppDivider()
    }
}

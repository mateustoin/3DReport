package com.threedreport.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Ajuda de um campo em duas camadas (decisão 108): uma linha curta sempre à vista e o detalhe atrás de
 * "Saiba mais". A tela de Orçamento tinha uma dúzia de parágrafos sempre abertos, e o que importava
 * se perdia no meio; o conteúdo continua o mesmo, só não compete com os campos.
 */
@Composable
fun FieldHelp(short: String, detail: String? = null, modifier: Modifier = Modifier) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                short,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (detail != null) {
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Menos" else "Saiba mais", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        if (expanded && detail != null) {
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
    }
}

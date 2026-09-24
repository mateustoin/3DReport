package com.threedreport.app.ui.services

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Escolha entre cobrar o serviço por peça ou uma vez pelo pedido. Usado no cadastro (padrão do
 * serviço) e no orçamento (ajuste daquele pedido), pra os dois lugares falarem igual.
 */
@Composable
fun ServiceChargeSelector(
    chargedPerOrder: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = !chargedPerOrder, onClick = { onChange(false) }, label = { Text("Por peça") })
        FilterChip(selected = chargedPerOrder, onClick = { onChange(true) }, label = { Text("Uma vez no pedido") })
    }
}

/** Como o serviço é cobrado, em texto curto pra listas ("por peça" / "por pedido"). */
fun chargeLabel(chargedPerOrder: Boolean): String = if (chargedPerOrder) "por pedido" else "por peça"

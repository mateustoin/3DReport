package com.threedreport.app.ui.quote

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.common_charge_per_order
import _3dreport.composeapp.generated.resources.common_charge_per_piece
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.threedreport.app.ui.components.SegmentedChoice
import org.jetbrains.compose.resources.stringResource

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
    SegmentedChoice(
        options = listOf(stringResource(Res.string.common_charge_per_piece), stringResource(Res.string.common_charge_per_order)),
        selectedIndex = if (chargedPerOrder) 1 else 0,
        onSelect = { onChange(it == 1) },
        modifier = modifier,
    )
}

package com.threedreport.app.ui.quote

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.quote_stock_warning_empty
import _3dreport.composeapp.generated.resources.quote_stock_warning_empty_reserved
import _3dreport.composeapp.generated.resources.quote_stock_warning_left
import _3dreport.composeapp.generated.resources.quote_stock_warning_left_reserved
import androidx.compose.runtime.Composable
import com.threedreport.app.ui.components.AttentionNotice
import com.threedreport.app.ui.stock.stockAmountText
import org.jetbrains.compose.resources.stringResource

/**
 * O aviso em âmbar de estoque (item 13 da Leva 12): "Falta PLA Voolt Vermelho: este pedido usa 320 g e sobram 250 g livres (180 g
 * já reservados pra pedidos aprovados)." Só avisa; não impede salvar. Os números vêm prontos do `core`.
 */
@Composable
internal fun StockWarningNotice(warning: QuoteStockWarning) {
    val used = stockAmountText(warning.ref, warning.needed)
    val free = stockAmountText(warning.ref, warning.available)
    val reserved = stockAmountText(warning.ref, warning.reserved)
    val hasReserved = warning.reserved > 0
    val text = when {
        warning.available > 0 && hasReserved -> stringResource(Res.string.quote_stock_warning_left_reserved, warning.name, used, free, reserved)
        warning.available > 0 -> stringResource(Res.string.quote_stock_warning_left, warning.name, used, free)
        hasReserved -> stringResource(Res.string.quote_stock_warning_empty_reserved, warning.name, used, reserved)
        else -> stringResource(Res.string.quote_stock_warning_empty, warning.name, used)
    }
    AttentionNotice(text)
}

package com.threedreport.app.ui.stock

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.stock_status_low
import _3dreport.composeapp.generated.resources.stock_status_out
import com.threedreport.app.data.StockRepository
import com.threedreport.core.model.StockItemRef
import com.threedreport.core.report.StockReport
import com.threedreport.core.report.StockState
import org.jetbrains.compose.resources.StringResource

/**
 * O saldo de um item controlado e a situação dele, pra uma linha de outra tela (o selo da cor em Filamentos, o detalhe do
 * insumo em Insumos) mostrar o saldo e ficar vermelha quando está baixo ou sem saldo. Os dois cadastros usam este tipo e
 * [badges]: a conta é a do `core`, e o texto da situação sai de um lugar só ([statusRes]).
 */
data class StockBadge(val amount: Double, val state: StockState) {
    val needsAttention: Boolean get() = state != StockState.OK
    val isEmpty: Boolean get() = state == StockState.OUT
}

/** O [StockBadge] de cada item controlado que [accepts], pelo saldo dos movimentos gravados e o limite de cada um. */
fun StockRepository.badges(accepts: (StockItemRef) -> Boolean): Map<StockItemRef, StockBadge> {
    val balances = StockReport.balances(movements.value)
    return items.value.filter { accepts(it.ref) }.associate { item ->
        val balance = balances[item.ref] ?: 0.0
        item.ref to StockBadge(balance, StockReport.state(item, balance))
    }
}

/** A palavra da situação pra leitor de tela e pra detalhe de linha: "sem saldo" ou "baixo"; `null` com o saldo em ordem. */
fun StockBadge.statusRes(): StringResource? = when {
    isEmpty -> Res.string.stock_status_out
    needsAttention -> Res.string.stock_status_low
    else -> null
}

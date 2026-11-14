package com.threedreport.app.ui.format

import com.threedreport.core.model.Currency
import com.threedreport.core.model.SalesChannel

/**
 * "20%", "20% + R$ 4,00 por item" ou "N faixas de preço" (decisão 121), pra listar os canais: no
 * cadastro (Configurações) e no seletor do Orçamento, com o mesmo texto nos dois lugares. Função
 * pura (não `@Composable`), pra também poder ser usada dentro do `itemLabel` de um dropdown.
 */
fun channelFeeSummary(channel: SalesChannel, currency: Currency): String = when {
    channel.tiers.isNotEmpty() -> "${channel.tiers.size} faixas de preço"
    channel.fixedFeePerItem > 0 -> "${channel.feeRate.toPercentText()} + ${channel.fixedFeePerItem.toCurrencyText(currency)} por item"
    else -> channel.feeRate.toPercentText()
}

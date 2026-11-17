package com.threedreport.app.ui.format

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.settings_channels_fee_tiers
import _3dreport.composeapp.generated.resources.settings_channels_fee_with_fixed
import androidx.compose.runtime.Composable
import com.threedreport.core.model.Currency
import com.threedreport.core.model.SalesChannel
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * "20%", "20% + R$ 4,00 por item" ou "2 faixas de preço" (decisão 121), pra listar os canais: no
 * cadastro (Configurações) e no seletor do Orçamento, com o mesmo texto nos dois lugares.
 */
@Composable
fun channelFeeSummary(channel: SalesChannel, currency: Currency): String = when {
    channel.tiers.isNotEmpty() -> pluralStringResource(Res.plurals.settings_channels_fee_tiers, channel.tiers.size, channel.tiers.size)

    channel.fixedFeePerItem > 0 -> stringResource(
        Res.string.settings_channels_fee_with_fixed,
        channel.feeRate.toPercentText(),
        channel.fixedFeePerItem.toCurrencyText(currency),
    )

    else -> channel.feeRate.toPercentText()
}

/**
 * O resumo de cada canal de [channels], por id, lido de uma vez dentro do composable: o `itemLabel` de um seletor não é
 * `@Composable`, então consulta este mapa em vez de chamar [channelFeeSummary].
 */
@Composable
fun channelFeeSummaries(channels: List<SalesChannel>, currency: Currency): Map<String, String> =
    channels.associate { it.id to channelFeeSummary(it, currency) }

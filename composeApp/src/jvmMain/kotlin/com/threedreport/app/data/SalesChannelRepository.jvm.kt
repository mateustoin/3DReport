package com.threedreport.app.data

import com.threedreport.core.model.SalesChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

actual class SalesChannelRepository actual constructor() {
    private val file = File(appDataDir(), "channels.json")
    private val state = MutableStateFlow(readJsonFile(file, migratedFromLegacyMarketplaceFee()))

    init {
        // A migração precisa virar arquivo na hora: enquanto ela só existisse em memória, zerar a
        // taxa antiga em Configurações faria o canal migrado sumir na próxima abertura.
        if (!file.exists() && state.value.isNotEmpty()) persist()
    }

    actual val channels: StateFlow<List<SalesChannel>> = state.asStateFlow()

    actual fun add(channel: SalesChannel) {
        state.value = state.value + channel
        persist()
    }

    actual fun update(channel: SalesChannel) {
        state.value = state.value.map { if (it.id == channel.id) channel else it }
        persist()
    }

    actual fun delete(id: String) {
        state.value = state.value.filterNot { it.id == id }
        persist()
    }

    private fun persist() = writeJsonFile(file, state.value)
}

/**
 * Catálogo inicial de quem já usava a taxa única de marketplace: vira um canal com a mesma taxa,
 * pra atualizar o app não zerar em silêncio uma configuração que já afetava o preço. Usado só
 * quando `channels.json` ainda não existe; quem nunca configurou taxa começa sem canal nenhum.
 */
private fun migratedFromLegacyMarketplaceFee(): List<SalesChannel> {
    val settingsFile = File(appDataDir(), "settings.json")
    if (!settingsFile.exists()) return emptyList()

    val legacyFeeRate = readJsonFile(settingsFile, LEGACY_PROBE_SETTINGS).marketplaceFeeRate
    if (legacyFeeRate <= 0.0) return emptyList()

    return listOf(SalesChannel(id = "marketplace-legado", name = "Marketplace", feeRate = legacyFeeRate))
}

/**
 * Só serve de valor de retorno pra leitura acima quando o arquivo está ilegível: o único campo
 * lido é `marketplaceFeeRate`, e zero significa "não havia taxa pra migrar".
 */
private val LEGACY_PROBE_SETTINGS = com.threedreport.core.model.PricingSettings(
    energyPricePerKwh = 0.0,
    failureRate = 0.0,
    finishingRate = 0.0,
    profitMargin = 0.0,
    marketplaceFeeRate = 0.0,
)

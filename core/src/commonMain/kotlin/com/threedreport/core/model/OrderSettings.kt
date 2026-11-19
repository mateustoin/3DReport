package com.threedreport.core.model

import kotlinx.serialization.Serializable

/**
 * Configurações de Pedidos (Leva 11): como o pedido é cobrado e acompanhado depois do orçamento. Não são custo
 * (por isso fora de [PricingSettings]) nem aparência do documento (fora de [BrandingSettings]). Nada daqui muda preço.
 *
 * @property defaultDepositRate sinal padrão, em fração do total (0,3 = 30%), aplicado ao salvar um pedido novo
 *   (decisão 146, ver [SavedQuote.initialDeposit]). Nasce zero: sem sinal, os documentos ficam como antes.
 * @property staleQuoteDays depois de quantos dias em Orçado o orçamento ganha o destaque de parado (decisão 149,
 *   ver [SavedQuote.isStale]). Zero desliga.
 * @property trackingUrlTemplate site de rastreio com `{codigo}` (ou `{code}`) no lugar do código (decisão 150). `null` = o padrão do
 *   app ([DEFAULT_TRACKING_URL_TEMPLATE]), que pode mudar numa versão futura se o site mudar; texto vazio = a pessoa
 *   tirou o site, e o pedido fica sem link de rastreio.
 */
@Serializable
data class OrderSettings(
    val defaultDepositRate: Double = 0.0,
    val staleQuoteDays: Int = DEFAULT_STALE_QUOTE_DAYS,
    val trackingUrlTemplate: String? = null,
) {
    init {
        require(defaultDepositRate in 0.0..1.0) { "defaultDepositRate fora de 0..1: $defaultDepositRate" }
        require(staleQuoteDays in 0..MAX_STALE_QUOTE_DAYS) { "staleQuoteDays fora de 0..$MAX_STALE_QUOTE_DAYS: $staleQuoteDays" }
    }

    /**
     * O site de rastreio em uso: o escolhido, ou o padrão quando a pessoa nunca mexeu. O padrão são os Correios e só vale
     * com a moeda padrão em real (decisão 16b da Leva 11B): em outra moeda ele é vazio, e o pedido fica sem link até a
     * pessoa pôr um site. Vazio = sem link.
     */
    fun effectiveTrackingUrlTemplate(defaultCurrency: Currency): String =
        (trackingUrlTemplate ?: defaultTrackingUrlTemplate(defaultCurrency)).trim()

    /**
     * Endereço de rastreio do código [code], com o código no lugar de `{codigo}` (codificado pra URL). `null` sem site
     * ou sem código. Um site sem `{codigo}` abre como está. [defaultCurrency] é a moeda padrão de agora: o site padrão
     * depende dela (os Correios só em real), então quem chama sempre diz qual é.
     */
    fun trackingUrl(code: String, defaultCurrency: Currency): String? {
        val template = effectiveTrackingUrlTemplate(defaultCurrency).ifEmpty { return null }
        val trimmed = code.trim().ifEmpty { return null }
        return template.replace(CODE_MARKER, percentEncode(trimmed)).replace(ENGLISH_CODE_MARKER, percentEncode(trimmed))
    }

    companion object {
        const val DEFAULT_STALE_QUOTE_DAYS = 3
        const val MAX_STALE_QUOTE_DAYS = 365

        /** Marcador do código no site de rastreio. */
        const val CODE_MARKER = "{codigo}"

        /** O mesmo marcador em inglês (decisão 16e): modelo com `{code}` funciona igual. */
        const val ENGLISH_CODE_MARKER = "{code}"

        /**
         * Rastreio dos Correios, conferido em 2026-11-17: o endereço abre a página oficial com o código já no campo
         * (`objetos=`). A consulta em si ainda pede um clique em "Consultar" e passa pela verificação de segurança do
         * site (Cloudflare Turnstile), que o próprio site faz.
         */
        const val DEFAULT_TRACKING_URL_TEMPLATE = "https://rastreamento.correios.com.br/app/index.php?objetos=$CODE_MARKER"

        /** O padrão do app pra [defaultCurrency]: os Correios em real, vazio nas outras moedas. */
        fun defaultTrackingUrlTemplate(defaultCurrency: Currency): String =
            if (defaultCurrency.hasBrazilOnlyFeatures) DEFAULT_TRACKING_URL_TEMPLATE else ""

        private const val HEX = "0123456789ABCDEF"

        private fun percentEncode(text: String): String = buildString {
            for (byte in text.encodeToByteArray()) {
                val value = byte.toInt() and 0xFF
                val char = value.toChar()
                val unreserved = char in 'A'..'Z' || char in 'a'..'z' || char in '0'..'9' || char in "-_.~"
                if (value < 128 && unreserved) append(char) else append('%').append(HEX[value shr 4]).append(HEX[value and 0x0F])
            }
        }
    }
}

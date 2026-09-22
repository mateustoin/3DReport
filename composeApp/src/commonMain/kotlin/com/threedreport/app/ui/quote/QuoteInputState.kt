package com.threedreport.app.ui.quote

/** Entradas da tela de Orçamento controladas pelo usuário (o resto vem dos repositórios). */
data class QuoteInputState(
    val filamentId: String? = null,
    val filamentColorId: String? = null,
    val printerId: String? = null,
    val lengthMetersText: String = "",
    val printTimeMinutesText: String = "",
    /** Minutos do seu trabalho nesta peça (ver `PrintJob.laborMinutes`); só afeta o preço se houver taxa horária configurada. */
    val laborMinutesText: String = "",
    /** Quantas peças iguais o cliente quer. Vazio ou inválido conta como 1. */
    val quantityText: String = "",
    /** Minutos de preparo cobrados uma vez pelo pedido inteiro (ver `Quote.setupMinutes`). */
    val setupMinutesText: String = "",
    val selectedServiceIds: Set<String> = emptySet(),
    /** Canal de venda escolhido, ou `null` na venda direta (sem taxa). */
    val salesChannelId: String? = null,
    /** Frete cobrado do cliente neste pedido (ver `SavedQuote.shippingCost`). */
    val shippingCostText: String = "",
    /** Mensagem sobre a última tentativa de importar dados de um G-code, exibida abaixo do botão. */
    val gcodeImportMessage: String? = null,
) {
    /** [quantityText] como número; campo vazio, texto inválido ou zero contam como uma peça. */
    val quantity: Int
        get() = quantityText.trim().toIntOrNull()?.coerceAtLeast(1) ?: 1
}

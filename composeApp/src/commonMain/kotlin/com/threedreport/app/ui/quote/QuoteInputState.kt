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
    /**
     * Serviços marcados neste orçamento, pelo id do `Service` no catálogo, na ordem em que foram
     * marcados. O valor é digitado aqui (ver [ServiceInput]).
     */
    val selectedServices: Map<String, ServiceInput> = emptyMap(),
    /** Canal de venda escolhido, ou `null` na venda direta (sem taxa). */
    val salesChannelId: String? = null,
    /** Frete cobrado do cliente neste pedido (ver `SavedQuote.shippingCost`). */
    val shippingCostText: String = "",
    /**
     * Preço total fechado com o cliente na conversa, quando diferente do que a margem daria.
     * Vazio significa usar o preço de tabela. Ver `PricingCalculator.calculate`.
     */
    val targetTotalText: String = "",
    /** Mensagem sobre a última tentativa de importar dados de um G-code, exibida abaixo do botão. */
    val gcodeImportMessage: String? = null,
    /** O que estava escolhido antes da última importação de G-code, pra "Desfazer" devolver. */
    val selectionBeforeGCode: SelectionBeforeGCode? = null,
) {
    /** [quantityText] como número; campo vazio, texto inválido ou zero contam como uma peça. */
    val quantity: Int
        get() = quantityText.trim().toIntOrNull()?.coerceAtLeast(1) ?: 1
}

/** Impressora, filamento e cor escolhidos antes de importar um G-code (ver `QuoteViewModel.undoGCodeImport`). */
data class SelectionBeforeGCode(val filamentId: String?, val filamentColorId: String?, val printerId: String?)

/**
 * Um serviço marcado no orçamento, como o usuário deixou na tela.
 *
 * @property name nome do serviço, usado quando ele não existe mais no catálogo (orçamento reaberto
 *   depois de o serviço ser excluído), pra não sumir do pedido ao salvar de novo.
 * @property priceText valor cobrado; vem preenchido com o sugerido do catálogo, se houver. Vazio
 *   bloqueia o salvar em vez de contar como zero (ver `QuoteResult.missingServicePrice`).
 * @property chargedPerOrder `true` cobra uma vez pelo pedido, `false` multiplica pela quantidade.
 */
data class ServiceInput(
    val name: String,
    val priceText: String = "",
    val chargedPerOrder: Boolean = false,
)

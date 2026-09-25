package com.threedreport.app.ui.quote

import com.threedreport.app.ui.format.parseDecimal
import com.threedreport.core.model.QuoteKind

/**
 * Uma linha de filamento de uma impressão: qual filamento, qual cor e quanto dele. Uma peça
 * multicolor tem uma linha por filamento (decisão 105).
 */
data class FilamentInput(
    val filamentId: String? = null,
    val colorId: String? = null,
    val lengthText: String = "",
)

/**
 * Uma impressão do pedido como está na tela: uma mesa, numa impressora (ver `PrintJob`). A 2.0 mostra
 * só a primeira; o estado já é uma lista pra o pedido com várias impressões não refazer tudo.
 *
 * @property filaments nunca vazia. Com uma linha só, a tela é a de sempre.
 * @property runsText quantas vezes a mesma mesa roda; vazio conta como 1.
 * @property gcodeImportMessage mensagem sobre a última importação de G-code nesta impressão.
 * @property beforeGCode como a impressão estava antes da primeira importação de G-code, pra
 *   "Desfazer" devolver tudo de uma vez (inclusive as linhas de filamento).
 */
data class PrintInput(
    val name: String = "",
    val printerId: String? = null,
    val filaments: List<FilamentInput> = listOf(FilamentInput()),
    val printTimeText: String = "",
    val runsText: String = "",
    val gcodeImportMessage: String? = null,
    val beforeGCode: PrintInput? = null,
) {
    val runs: Int
        get() = runsText.trim().toIntOrNull()?.coerceAtLeast(1) ?: 1
}

/** Entradas da tela de Orçamento controladas pelo usuário (o resto vem dos repositórios). */
data class QuoteInputState(
    /** As impressões do pedido, na ordem da tela. Nunca vazia. */
    val prints: List<PrintInput> = listOf(PrintInput()),
    /**
     * Minutos do seu trabalho no pedido **inteiro**, sem multiplicar pela quantidade nem pelas
     * impressões (ver `Quote.laborMinutes`); só afeta o preço se houver taxa horária configurada.
     */
    val laborMinutesText: String = "",
    /** Quantas peças iguais o cliente quer. Vazio ou inválido conta como 1. */
    val quantityText: String = "",
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
    /**
     * Preço anunciado, por unidade, do produto de onde este pedido está nascendo pelo "Vender"
     * (decisão 102). Com [targetTotalText] vazio, a peça sai por ele vezes a quantidade, e serviços
     * e frete somam por fora. Fica separado do preço fechado porque aquele é o total que o cliente
     * paga, frete incluso: usar o anunciado ali descontaria o frete da peça.
     */
    val announcedUnitPrice: Double? = null,
    /**
     * Se o que está sendo montado é um pedido de cliente ou um produto do catálogo (decisão 101).
     * Fica aqui, e não no formulário de salvar, porque muda o cálculo: produto não tem frete nem
     * preço fechado com cliente (ver [isProduct]). Continua escolhido depois de salvar, pra quem
     * cadastra vários produtos seguidos não precisar escolher de novo.
     */
    val kind: QuoteKind = QuoteKind.ORDER,
) {
    /** [quantityText] como número; campo vazio, texto inválido ou zero contam como uma peça. */
    val quantity: Int
        get() = quantityText.trim().toIntOrNull()?.coerceAtLeast(1) ?: 1

    val isProduct: Boolean
        get() = kind == QuoteKind.PRODUCT

    /**
     * Nenhum minuto de trabalho informado: com a hora configurada, é o caso em que ela não muda o
     * preço. Só faz sentido mostrar com `laborRatePerHour > 0`.
     */
    val isLaborTimeMissing: Boolean
        get() = (parseDecimal(laborMinutesText) ?: 0.0) <= 0.0
}

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

package com.threedreport.app.ui.history

import com.threedreport.app.platform.formatDate
import com.threedreport.app.ui.format.minutesToDurationText
import com.threedreport.app.ui.format.toCurrencyText
import com.threedreport.core.model.Currency
import com.threedreport.core.model.Quote
import com.threedreport.core.model.SavedQuote

/**
 * Texto simplificado pra copiar/colar (ex.: WhatsApp, marketplace): nome,
 * valor de venda e, se houver serviços escolhidos, cada um deles
 * (multiplicados pela quantidade, ver [SavedQuote.totalWithServices]), o
 * frete (se houver) e o total. Sem foto nem link interno.
 *
 * A linha "Total" aparece sempre que há serviço ou frete, mesmo que só um
 * dos dois exista, senão o cliente veria "Venda" e "Frete" soltos sem a soma.
 *
 * Quando [Quote.quantity] é maior que 1, vem "N peças · X cada", sempre a
 * partir do **total** que o cliente paga (com serviços e frete, portanto),
 * igual ao que a tela de Orçamento mostra. Calcular esse "cada" em cima do
 * valor de venda faria a mesma peça aparecer com dois preços unitários
 * diferentes na tela e na mensagem enviada ao cliente.
 *
 * Fecha com o prazo de entrega (se houver) e o tempo de impressão (só com
 * [showPrintTime], ver `BrandingSettings.showPrintTime`), nessa ordem: o
 * prazo é o que o cliente usa pra decidir, o tempo é detalhe.
 *
 * Com quantidade 1, sem frete e sem prazo (os padrões), a saída é idêntica à
 * de antes desses campos existirem: nenhuma linha nova aparece.
 */
internal fun SavedQuote.toCopyPasteText(currency: Currency = Currency.BRL, showPrintTime: Boolean = false): String = buildString {
    val quantity = quote.quantity
    appendLine(name)
    append("Venda: ").append(quote.salePrice.toCurrencyText(currency))
    services.forEach { service ->
        appendLine()
        append(service.name)
        if (quantity > 1) append(" (× ").append(quantity).append(")")
        append(": ").append((service.price * quantity).toCurrencyText(currency))
    }
    if (shippingCost > 0) {
        appendLine()
        append("Frete: ").append(shippingCost.toCurrencyText(currency))
    }
    if (services.isNotEmpty() || shippingCost > 0) {
        appendLine()
        append("Total: ").append(totalWithServices.toCurrencyText(currency))
    }
    if (quantity > 1) {
        appendLine()
        append(quantity).append(" peças · ").append((totalWithServices / quantity).toCurrencyText(currency)).append(" cada")
    }
    deliveryDateText()?.let {
        appendLine()
        append(it)
    }
    if (showPrintTime) {
        appendLine()
        append(printTimeText())
    }
}

/** "Prazo de entrega: até 30/09/2026", ou `null` sem prazo. Mesmo texto no PDF e no copiar/colar. */
internal fun SavedQuote.deliveryDateText(): String? =
    deliveryDateEpochDay?.let { "Prazo de entrega: até ${formatDate(it)}" }

/**
 * "Tempo de impressão: 6 h 30 min", com "(N peças)" quando há mais de uma, porque o tempo é do
 * pedido inteiro. "Impressão", e não "fabricação": o número é tempo de máquina, e "fabricação"
 * sugeriria um prazo que ele não é.
 */
internal fun SavedQuote.printTimeText(): String = buildString {
    append("Tempo de impressão: ").append(totalPrintTimeMinutes.minutesToDurationText())
    if (quote.quantity > 1) append(" (").append(quote.quantity).append(" peças)")
}

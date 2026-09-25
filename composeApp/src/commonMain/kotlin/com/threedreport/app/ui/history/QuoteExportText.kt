package com.threedreport.app.ui.history

import com.threedreport.app.platform.formatDate
import com.threedreport.app.ui.format.minutesToDurationText
import com.threedreport.app.ui.format.toCurrencyText
import com.threedreport.core.model.Currency
import com.threedreport.core.model.Quote
import com.threedreport.core.model.SavedQuote

/**
 * Texto simplificado pra copiar/colar (ex.: WhatsApp, marketplace): título, valor das peças e, se
 * houver serviços escolhidos, cada um deles (os por peça com "× N", os por pedido só com o nome, ver
 * [SavedQuote.totalWithServices]), o frete (se houver) e o total. Sem foto nem link interno.
 *
 * O título é o nome da peça com o número do pedido ("Suporte · #0042"), que é como o cliente se
 * refere ao orçamento depois; nome automático ("Orçamento - 24/09/2026 14:30") nunca vai pro cliente
 * (decisão 108).
 *
 * A linha "Total" aparece sempre que há serviço ou frete, mesmo que só um dos dois exista, senão o
 * cliente veria "Valor" e "Frete" soltos sem a soma.
 *
 * Com mais de uma peça, o valor vem como conta ("10 × R$ 6,02 = R$ 60,20"): o preço de cada peça é o
 * das peças, sem frete, que é do pedido (decisão 108; antes o "cada" dividia o frete também).
 *
 * Fecha com o prazo de entrega (se houver) e o tempo de impressão (só com [showPrintTime], ver
 * `BrandingSettings.showPrintTime`), nessa ordem: o prazo é o que o cliente usa pra decidir, o tempo
 * é detalhe.
 */
internal fun SavedQuote.toCopyPasteText(currency: Currency = this.currency, showPrintTime: Boolean = false): String = buildString {
    val quantity = quote.quantity
    appendLine(clientTitle())
    append("Valor: ")
    if (quantity > 1) {
        append(quantity).append(" × ").append(quote.unitSalePrice.toCurrencyText(currency)).append(" = ")
    }
    append(quote.salePrice.toCurrencyText(currency))
    services.forEach { service ->
        appendLine()
        append(service.name)
        if (quantity > 1 && !service.chargedPerOrder) append(" (× ").append(quantity).append(")")
        append(": ").append(service.total(quantity).toCurrencyText(currency))
    }
    if (shippingCost > 0) {
        appendLine()
        append("Frete: ").append(shippingCost.toCurrencyText(currency))
    }
    if (services.isNotEmpty() || shippingCost > 0) {
        appendLine()
        append("Total: ").append(totalWithServices.toCurrencyText(currency))
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

/**
 * Título do orçamento pro cliente: o nome da peça, com o número quando é pedido ("Suporte · #0042").
 * Nome automático vira "Orçamento #0042" (ou "Produto", no catálogo), porque "Orçamento - 24/09/2026
 * 14:30" é rótulo interno, não nome de peça.
 */
internal fun SavedQuote.clientTitle(withNumber: Boolean = true): String {
    val number = displayNumber?.takeIf { isOrder && withNumber }
    return when {
        hasAutoName && !isOrder -> "Produto"
        hasAutoName -> listOfNotNull("Orçamento", number).joinToString(" ")
        number != null -> "$name · $number"
        else -> name
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

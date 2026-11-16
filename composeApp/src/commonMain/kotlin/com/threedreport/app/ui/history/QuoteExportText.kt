package com.threedreport.app.ui.history

import com.threedreport.app.platform.ClientDocumentTexts
import com.threedreport.app.platform.formatDate
import com.threedreport.app.ui.format.minutesToDurationText
import com.threedreport.app.ui.format.toCurrencyText
import com.threedreport.core.model.Currency
import com.threedreport.core.model.QuoteService
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
 *
 * Os textos vêm de [texts] (recursos traduzíveis), carregados por quem manda a mensagem.
 */
internal fun SavedQuote.toCopyPasteText(
    texts: ClientDocumentTexts,
    currency: Currency = this.currency,
    showPrintTime: Boolean = false,
): String = buildString {
    val quantity = quote.quantity
    appendLine(clientTitle(texts))
    val sale = quote.salePrice.toCurrencyText(currency)
    append(texts.value(if (quantity > 1) texts.valueTimesQuantity(quantity, quote.unitSalePrice.toCurrencyText(currency), sale) else sale))
    services.forEach { service ->
        appendLine()
        append(texts.lineAmount(service.label(texts, quantity), service.total(quantity).toCurrencyText(currency)))
    }
    if (shippingAbsorbed) {
        appendLine()
        append(texts.shippingFree)
    } else if (shippingCost > 0) {
        appendLine()
        append(texts.shipping(chargedShipping.toCurrencyText(currency)))
    }
    if (services.isNotEmpty() || shippingCost > 0) {
        appendLine()
        append(texts.total(totalWithServices.toCurrencyText(currency)))
    }
    // Entrega expressa (decisão 125) junto do prazo, que é como o cliente lê os dois num relance; sem
    // prazo, vira linha própria em vez de sumir.
    deliveryHighlightText(texts)?.let {
        appendLine()
        append(it)
    }
    if (showPrintTime) {
        appendLine()
        append(printTimeText(texts))
    }
}

/** O nome do serviço na linha do valor: "Pintura (× 3)" quando é cobrado por peça num pedido de mais de uma. */
internal fun QuoteService.label(texts: ClientDocumentTexts, quantity: Int): String =
    if (quantity > 1 && !chargedPerOrder) texts.servicePerPiece(name, quantity) else name

/**
 * O prazo com a entrega expressa (decisão 125): "Prazo de entrega: até 30/09/2026 (entrega expressa)", só o prazo,
 * só "Entrega expressa", ou `null` sem nenhum dos dois. Mesmo texto no PDF e na mensagem.
 */
internal fun SavedQuote.deliveryHighlightText(texts: ClientDocumentTexts): String? {
    val delivery = deliveryDateText(texts)
    return when {
        delivery != null && quote.rush -> texts.deliveryRush(delivery)
        delivery != null -> delivery
        quote.rush -> texts.rush
        else -> null
    }
}

/**
 * Título do orçamento pro cliente: o nome da peça, com o número quando é pedido ("Suporte · #0042").
 * Nome automático vira "Orçamento #0042" (ou "Produto", no catálogo), porque "Orçamento - 24/09/2026
 * 14:30" é rótulo interno, não nome de peça.
 */
internal fun SavedQuote.clientTitle(texts: ClientDocumentTexts, withNumber: Boolean = true): String {
    val number = displayNumber?.takeIf { isOrder && withNumber }
    return when {
        hasAutoName && !isOrder -> texts.autoProductTitle
        hasAutoName -> texts.autoOrderTitle(number)
        number != null -> texts.titleNumbered(name, number)
        else -> name
    }
}

/** "Prazo de entrega: até 30/09/2026", ou `null` sem prazo. Mesmo texto no PDF e no copiar/colar. */
internal fun SavedQuote.deliveryDateText(texts: ClientDocumentTexts): String? =
    deliveryDateEpochDay?.let { texts.delivery(formatDate(it)) }

/**
 * "Tempo de impressão: 6 h 30 min", com "(N peças)" quando há mais de uma, porque o tempo é do
 * pedido inteiro. "Impressão", e não "fabricação": o número é tempo de máquina, e "fabricação"
 * sugeriria um prazo que ele não é.
 */
internal fun SavedQuote.printTimeText(texts: ClientDocumentTexts): String {
    val line = texts.printTime(totalPrintTimeMinutes.minutesToDurationText())
    return if (quote.quantity > 1) texts.printTimeForPieces(line, quote.quantity) else line
}

package com.threedreport.app.ui.history

import com.threedreport.app.ui.format.toCurrencyText
import com.threedreport.core.model.Currency
import com.threedreport.core.model.Quote
import com.threedreport.core.model.SavedQuote

/**
 * Texto simplificado pra copiar/colar (ex.: WhatsApp, marketplace): nome,
 * valor de venda e, se houver serviços escolhidos, cada um deles
 * (multiplicados pela quantidade, ver [SavedQuote.totalWithServices]) + o
 * total. Sem foto nem link interno.
 *
 * Quando [Quote.quantity] é maior que 1, fecha com "N peças · X cada",
 * sempre no fim e sempre a partir do **total** que o cliente paga (com
 * serviços, portanto), igual ao que a tela de Orçamento mostra. Calcular
 * esse "cada" em cima do valor de venda faria a mesma peça aparecer com dois
 * preços unitários diferentes na tela e na mensagem enviada ao cliente.
 *
 * Com quantidade 1 (o padrão), a saída é idêntica à de antes desse campo
 * existir: nenhuma linha nova aparece.
 */
internal fun SavedQuote.toCopyPasteText(currency: Currency = Currency.BRL): String = buildString {
    val quantity = quote.quantity
    appendLine(name)
    append("Venda: ").append(quote.salePrice.toCurrencyText(currency))
    if (services.isNotEmpty()) {
        services.forEach { service ->
            appendLine()
            append(service.name)
            if (quantity > 1) append(" (× ").append(quantity).append(")")
            append(": ").append((service.price * quantity).toCurrencyText(currency))
        }
        appendLine()
        append("Total: ").append(totalWithServices.toCurrencyText(currency))
    }
    if (quantity > 1) {
        appendLine()
        append(quantity).append(" peças · ").append((totalWithServices / quantity).toCurrencyText(currency)).append(" cada")
    }
}

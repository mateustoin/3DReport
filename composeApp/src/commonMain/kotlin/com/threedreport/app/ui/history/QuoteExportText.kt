package com.threedreport.app.ui.history

import com.threedreport.app.ui.format.toBrl
import com.threedreport.core.model.SavedQuote

/**
 * Texto simplificado pra copiar/colar (ex.: WhatsApp, marketplace): nome,
 * valor de venda e, se houver serviços escolhidos, cada um deles + o total
 * (venda + serviços). Sem foto nem link interno.
 */
internal fun SavedQuote.toCopyPasteText(): String = buildString {
    appendLine(name)
    append("Venda: ").append(quote.salePrice.toBrl())
    if (services.isNotEmpty()) {
        services.forEach { service ->
            appendLine()
            append(service.name).append(": ").append(service.price.toBrl())
        }
        appendLine()
        append("Total: ").append(totalWithServices.toBrl())
    }
}

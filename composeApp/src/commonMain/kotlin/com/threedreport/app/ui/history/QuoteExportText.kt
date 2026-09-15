package com.threedreport.app.ui.history

import com.threedreport.app.ui.format.toBrl
import com.threedreport.core.model.SavedQuote

/** Texto simplificado pra copiar/colar (ex.: WhatsApp, marketplace): nome e valor de venda, sem foto nem link interno. */
internal fun SavedQuote.toCopyPasteText(): String = buildString {
    appendLine(name)
    append("Venda: ").append(quote.salePrice.toBrl())
}

package com.threedreport.app.ui.quote

import com.threedreport.core.model.Filament
import com.threedreport.core.model.PrinterProfile
import com.threedreport.core.model.Quote

/**
 * Resultado derivado de [QuoteInputState] + os catálogos/configurações atuais.
 * [filament]/[printer] são os itens efetivamente usados (com fallback para o
 * primeiro do catálogo quando nada foi escolhido ainda), para exibição nos
 * dropdowns mesmo antes do cálculo estar completo.
 */
data class QuoteResult(
    val filament: Filament? = null,
    val printer: PrinterProfile? = null,
    val quote: Quote? = null,
    val errorMessage: String? = null,
)

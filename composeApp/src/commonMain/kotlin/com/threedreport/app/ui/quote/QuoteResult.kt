package com.threedreport.app.ui.quote

import com.threedreport.core.model.Filament
import com.threedreport.core.model.PrinterProfile
import com.threedreport.core.model.Quote
import com.threedreport.core.model.Service

/**
 * Resultado derivado de [QuoteInputState] + os catálogos/configurações atuais.
 * [filament]/[printer] são os itens efetivamente usados (com fallback para o
 * primeiro do catálogo quando nada foi escolhido ainda), para exibição nos
 * dropdowns mesmo antes do cálculo estar completo. [selectedServices] são os
 * serviços opcionais marcados para esta peça (não têm fallback — começam
 * vazios).
 */
data class QuoteResult(
    val filament: Filament? = null,
    val printer: PrinterProfile? = null,
    val quote: Quote? = null,
    val errorMessage: String? = null,
    val selectedServices: List<Service> = emptyList(),
) {
    /** Soma dos serviços escolhidos. */
    val servicesTotal: Double
        get() = selectedServices.sumOf { it.price }

    /** Valor de venda + serviços — o que de fato será cobrado do cliente. */
    val grandTotal: Double?
        get() = quote?.let { it.salePrice + servicesTotal }
}
